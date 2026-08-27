package de.bydora.tes.treueshop;

import de.bydora.tes.TesseraniaEconomySystem;
import de.bydora.tes.util.Messages;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import xyz.xenondevs.invui.gui.Gui;
import xyz.xenondevs.invui.item.Item;
import xyz.xenondevs.invui.window.Window;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A mob-egg tier sub-interface (spec §3.2.1.2, "Subinterface Mo1-Mo4"): a grid of individually
 * purchasable {@link TreueshopMobBundle.MobEggOption}s, plus the shared balance display and
 * "⮜ Zurück" button. One layout serves all four tiers, packing each tier's options left-to-right,
 * top-to-bottom across up to two content rows (Freundliche Mobs I's 16 options is the largest
 * tier, fitting 9+7 across both).
 */
public final class TreueshopMobBundleGui {

    private static final int COLUMNS = 9;
    private static final int OPTION_ROWS = 2;
    private static final char FILLER = 'g';
    private static final char PADDING = 'p';
    private static final char BALANCE = 'b';
    private static final char BACK = 'z';

    private TreueshopMobBundleGui() {
    }

    public static void open(TesseraniaEconomySystem plugin, Player player, TreueshopMobBundle bundle) {
        char[][] grid = new char[OPTION_ROWS + 2][COLUMNS];
        for (char[] row : grid) {
            Arrays.fill(row, FILLER);
        }

        List<TreueshopMobBundle.MobEggOption> options = bundle.options();
        Map<Character, TreueshopMobBundle.MobEggOption> optionsByKey = new LinkedHashMap<>();
        char nextKey = 'A';
        for (int i = 0; i < options.size(); i++) {
            char key = nextKey++;
            grid[i / COLUMNS][i % COLUMNS] = key;
            optionsByKey.put(key, options.get(i));
        }

        char[] footer = grid[OPTION_ROWS + 1];
        Arrays.fill(footer, PADDING);
        footer[0] = BALANCE;
        footer[COLUMNS - 1] = BACK;

        String[] structure = new String[grid.length];
        for (int r = 0; r < grid.length; r++) {
            structure[r] = new String(grid[r]);
        }

        Gui.Builder<?, ?> builder = Gui.builder()
                .setStructure(structure)
                .addIngredient(FILLER, TreueshopComponents.filler(Material.GRAY_STAINED_GLASS_PANE))
                .addIngredient(PADDING, TreueshopComponents.filler(Material.PURPLE_STAINED_GLASS_PANE))
                .addIngredient(BALANCE, TreueshopComponents.balanceItem(plugin))
                .addIngredient(BACK, TreueshopComponents.backButton(plugin));
        for (Map.Entry<Character, TreueshopMobBundle.MobEggOption> entry : optionsByKey.entrySet()) {
            builder.addIngredient(entry.getKey(), optionItem(plugin, bundle, entry.getValue()));
        }

        Window.builder()
                .setViewer(player)
                .setTitle(PlainTextComponentSerializer.plainText().serialize(bundle.title()))
                .setUpperGui(builder.build())
                .build()
                .open();
    }

    private static Item optionItem(TesseraniaEconomySystem plugin, TreueshopMobBundle bundle, TreueshopMobBundle.MobEggOption option) {
        return Item.builder()
                .setItemProvider(TreueshopComponents.rewardIcon(plugin, bundle, option))
                .addClickHandler((item, click) -> purchase(plugin, click.player(), bundle, option))
                .build();
    }

    private static void purchase(TesseraniaEconomySystem plugin, Player player, TreueshopMobBundle bundle, TreueshopMobBundle.MobEggOption option) {
        int cost = plugin.tesConfig().treueshopRewardCost(bundle.costConfigId(), bundle.defaultCost());
        TreueshopRewardService.PurchaseResult result = TreueshopRewardService.purchase(
                plugin, player, cost, () -> TreueshopItemGrants.grantMobEgg(plugin, player, option));
        if (result == TreueshopRewardService.PurchaseResult.INSUFFICIENT_TP) {
            player.sendMessage(Messages.treueshopInsufficientTp());
            return;
        }
        String optionName = PlainTextComponentSerializer.plainText().serialize(option.title());
        player.sendMessage(Messages.treueshopRewardPurchased(optionName));
        open(plugin, player, bundle);
    }
}
