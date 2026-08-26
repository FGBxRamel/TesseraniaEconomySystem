package de.bydora.tes.treueshop;

import de.bydora.tes.TesseraniaEconomySystem;
import de.bydora.tes.data.PlayerRecord;
import de.bydora.tes.gui.CustomHeads;
import de.bydora.tes.util.Messages;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import xyz.xenondevs.invui.gui.Gui;
import xyz.xenondevs.invui.item.Item;
import xyz.xenondevs.invui.item.ItemBuilder;
import xyz.xenondevs.invui.window.Window;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;

/**
 * The Treuepunkteshop main interface (spec §3.2, "Ebene 1"), laid out per the reference build at
 * -406 -11 -3390: a 9x4 grid of the 12 top-level rewards from {@link TreueshopRewardCatalog}, a
 * sunflower showing the caller's Treuepunkte balance, and a "Levelinterface" button. That button
 * is a documented no-op until Stage 4 builds the level interface — same pattern as
 * {@link de.bydora.tes.reward.RewardInventoryGui}'s "⮜ Levelinterface" item. Only the direct-effect
 * rewards (Segen der Zwerge, Kraftelixier) are purchasable so far; the rest render with real
 * name/lore/cost but no click handler yet, pending later Stage 3 branches.
 */
public final class TreueshopGui {

    private static final int COLUMNS = 9;
    private static final int ROWS = 4;
    private static final char FILLER = 'g';
    private static final char BALANCE = 'b';
    private static final char LEVEL_INTERFACE = 'h';

    private TreueshopGui() {
    }

    public static void open(TesseraniaEconomySystem plugin, Player player) {
        char[][] grid = new char[ROWS][COLUMNS];
        for (char[] row : grid) {
            Arrays.fill(row, FILLER);
        }

        Map<Character, TreueshopReward> rewardsByKey = new LinkedHashMap<>();
        char nextKey = 'A';
        for (TreueshopReward reward : TreueshopRewardCatalog.mainInterfaceRewards()) {
            char key = nextKey++;
            grid[reward.row() - 1][reward.column() - 1] = key;
            rewardsByKey.put(key, reward);
        }
        grid[ROWS - 1][0] = BALANCE;
        grid[ROWS - 1][COLUMNS - 1] = LEVEL_INTERFACE;

        String[] structure = new String[ROWS];
        for (int r = 0; r < ROWS; r++) {
            structure[r] = new String(grid[r]);
        }

        Gui.Builder<?, ?> builder = Gui.builder()
                .setStructure(structure)
                .addIngredient(FILLER, Item.simple(new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).setName(Component.text(" "))))
                .addIngredient(BALANCE, balanceItem(plugin))
                .addIngredient(LEVEL_INTERFACE, levelInterfaceItem());
        for (Map.Entry<Character, TreueshopReward> entry : rewardsByKey.entrySet()) {
            builder.addIngredient(entry.getKey(), rewardItem(plugin, entry.getValue()));
        }

        Window.builder()
                .setViewer(player)
                .setTitle("Treuepunkteshop")
                .setUpperGui(builder.build())
                .build()
                .open();
    }

    private static Item balanceItem(TesseraniaEconomySystem plugin) {
        return Item.builder()
                .setItemProvider(viewer -> new ItemBuilder(Material.SUNFLOWER)
                        .setName(Component.text("Treuepunkte (TP)", NamedTextColor.YELLOW, TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false))
                        .addLoreLines(
                                Component.text(currentTreuepunkte(plugin, viewer) + " Punkte", NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false),
                                Component.empty(),
                                Component.text("'Treuepunkte können passiv beim Einkauf", NamedTextColor.GRAY, TextDecoration.ITALIC),
                                Component.text("in anderen Spieler-Shops generiert werden.'", NamedTextColor.GRAY, TextDecoration.ITALIC)))
                .build();
    }

    private static int currentTreuepunkte(TesseraniaEconomySystem plugin, Player viewer) {
        return plugin.playerRepository().findByUuid(viewer.getUniqueId()).map(PlayerRecord::treuepunkte).orElse(0);
    }

    private static Item levelInterfaceItem() {
        // Same custom-textured head as the "➤ Weiter" pagination button (confirmed against the
        // reference build) — setCustomName, not setName, per CustomHeads/PaginationControls'
        // documented reasoning: a PLAYER_HEAD with a profile ignores the item_name component.
        return Item.builder()
                .setItemProvider(new ItemBuilder(CustomHeads.nextPageHead())
                        .setCustomName(Component.text("Levelinterface", NamedTextColor.WHITE, TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false)))
                .addClickHandler((item, click) -> {
                })
                .build();
    }

    private static Item rewardItem(TesseraniaEconomySystem plugin, TreueshopReward reward) {
        List<Component> lore = new ArrayList<>();
        if (reward.hasCost()) {
            int cost = plugin.tesConfig().treueshopRewardCost(reward.costConfigId(), reward.defaultCost());
            lore.add(Component.text("Kosten: " + cost + " TP", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
            lore.add(Component.empty());
        }
        lore.addAll(reward.flavorLore());

        Item.Builder<?> builder = Item.builder()
                .setItemProvider(new ItemBuilder(reward.icon())
                        .setName(reward.title())
                        .addLoreLines(lore));

        directEffect(reward).ifPresent(effect ->
                builder.addClickHandler((item, click) -> purchase(plugin, click.player(), reward, effect)));

        return builder.build();
    }

    private static Optional<BiConsumer<TesseraniaEconomySystem, Player>> directEffect(TreueshopReward reward) {
        if (!reward.hasCost()) {
            return Optional.empty();
        }
        return switch (reward.costConfigId()) {
            case "segen-der-zwerge" -> Optional.of(TreueshopEffects::applySegenDerZwerge);
            case "kraftelixier" -> Optional.of(TreueshopEffects::applyKraftelixier);
            default -> Optional.empty();
        };
    }

    private static void purchase(TesseraniaEconomySystem plugin, Player player, TreueshopReward reward,
            BiConsumer<TesseraniaEconomySystem, Player> effect) {
        int cost = plugin.tesConfig().treueshopRewardCost(reward.costConfigId(), reward.defaultCost());
        TreueshopRewardService.PurchaseResult result = TreueshopRewardService.purchase(
                plugin, player, cost, () -> effect.accept(plugin, player));
        if (result == TreueshopRewardService.PurchaseResult.INSUFFICIENT_TP) {
            player.sendMessage(Messages.treueshopInsufficientTp());
            return;
        }
        String rewardName = PlainTextComponentSerializer.plainText().serialize(reward.title());
        player.sendMessage(Messages.treueshopRewardPurchased(rewardName));
        open(plugin, player);
    }
}
