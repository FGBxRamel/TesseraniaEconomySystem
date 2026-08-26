package de.bydora.tes.treueshop;

import de.bydora.tes.TesseraniaEconomySystem;
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

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;

/**
 * The Treuepunkteshop main interface (spec §3.2, "Ebene 1"), laid out per the reference build at
 * -406 -11 -3390: a 9x4 grid of the 12 top-level rewards from {@link TreueshopRewardCatalog}, a
 * sunflower showing the caller's Treuepunkte balance, and a "Levelinterface" button. That button
 * is a documented no-op until Stage 4 builds the level interface — same pattern as
 * {@link de.bydora.tes.reward.RewardInventoryGui}'s "⮜ Levelinterface" item. Rewards with no cost
 * of their own open a sub-interface ({@link #openSubInterface}); the rest purchase directly via
 * {@link #directEffect}. Only Prozessverstärker and Handelsbonus aren't wired up yet.
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
                .addIngredient(FILLER, TreueshopComponents.filler(Material.GRAY_STAINED_GLASS_PANE))
                .addIngredient(BALANCE, TreueshopComponents.balanceItem(plugin))
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
        if ("handelsbonus".equals(reward.costConfigId())) {
            return TreueshopHandelsbonus.item(plugin, reward);
        }

        Item.Builder<?> builder = Item.builder().setItemProvider(TreueshopComponents.rewardIcon(plugin, reward));

        if (reward.opensSubInterface()) {
            builder.addClickHandler((item, click) -> openSubInterface(plugin, click.player(), reward.subInterfaceId()));
        } else {
            directEffect(reward).ifPresent(effect ->
                    builder.addClickHandler((item, click) -> purchase(plugin, click.player(), reward, effect,
                            () -> open(plugin, click.player()))));
        }

        return builder.build();
    }

    private static void openSubInterface(TesseraniaEconomySystem plugin, Player player, String subInterfaceId) {
        switch (subInterfaceId) {
            case "xp-terminal" -> TreueshopXpTerminalGui.open(plugin, player);
            case "mo1" -> TreueshopMobBundleGui.open(plugin, player, TreueshopMobBundleCatalog.FREUNDLICHE_MOBS_I);
            case "mo2" -> TreueshopMobBundleGui.open(plugin, player, TreueshopMobBundleCatalog.FREUNDLICHE_MOBS_II);
            case "mo3" -> TreueshopMobBundleGui.open(plugin, player, TreueshopMobBundleCatalog.FEINDLICHE_MOBS_I);
            case "mo4" -> TreueshopMobBundleGui.open(plugin, player, TreueshopMobBundleCatalog.FEINDLICHE_MOBS_II);
            default -> throw new IllegalStateException("Unknown Treueshop sub-interface: " + subInterfaceId);
        }
    }

    private static Optional<BiConsumer<TesseraniaEconomySystem, Player>> directEffect(TreueshopReward reward) {
        if (!reward.hasCost()) {
            return Optional.empty();
        }
        return switch (reward.costConfigId()) {
            case "segen-der-zwerge" -> Optional.of(TreueshopEffects::applySegenDerZwerge);
            case "kraftelixier" -> Optional.of(TreueshopEffects::applyKraftelixier);
            case "spawner" -> Optional.of(TreueshopItemGrants::grantSpawner);
            case "erntewelt" -> Optional.of(TreueshopItemGrants::grantErntewelt);
            case "glutzone" -> Optional.of(TreueshopItemGrants::grantGlutzone);
            case "prozessverstaerker" -> Optional.of(TreueshopItemGrants::grantProzessverstaerker);
            default -> Optional.empty();
        };
    }

    /**
     * Spends {@code reward}'s cost and applies its effect, notifying the player either way.
     * {@code onSuccess} lets each Treueshop screen decide what to reopen (itself, or the main
     * interface) to refresh the balance display.
     */
    static void purchase(TesseraniaEconomySystem plugin, Player player, TreueshopReward reward,
            BiConsumer<TesseraniaEconomySystem, Player> effect, Runnable onSuccess) {
        int cost = plugin.tesConfig().treueshopRewardCost(reward.costConfigId(), reward.defaultCost());
        TreueshopRewardService.PurchaseResult result = TreueshopRewardService.purchase(
                plugin, player, cost, () -> effect.accept(plugin, player));
        if (result == TreueshopRewardService.PurchaseResult.INSUFFICIENT_TP) {
            player.sendMessage(Messages.treueshopInsufficientTp());
            return;
        }
        String rewardName = PlainTextComponentSerializer.plainText().serialize(reward.title());
        player.sendMessage(Messages.treueshopRewardPurchased(rewardName));
        onSuccess.run();
    }
}
