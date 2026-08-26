package de.bydora.tes.treueshop;

import de.bydora.tes.TesseraniaEconomySystem;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import xyz.xenondevs.invui.gui.Gui;
import xyz.xenondevs.invui.item.Item;
import xyz.xenondevs.invui.window.Window;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The XP-Terminal sub-interface (spec §3.2.1.2, "Subinterface XP"), opened from the Treueshop
 * main interface's XP-Terminal button: the four XP-Boost rewards from
 * {@link TreueshopRewardCatalog#xpTerminalRewards()}, laid out at their reference-build columns
 * (2/4/6/8) plus the shared balance display and "⮜ Zurück" button. Uses a uniform 9x4 grid rather
 * than the reference build's 9x6 (rows 5-6 there are unused red-glass scaffolding, not part of
 * the functional layout).
 */
public final class TreueshopXpTerminalGui {

    private static final int COLUMNS = 9;
    private static final int ROWS = 4;
    private static final char FILLER = 'g';
    private static final char DIVIDER = 'p';
    private static final char BALANCE = 'b';
    private static final char BACK = 'z';

    private TreueshopXpTerminalGui() {
    }

    public static void open(TesseraniaEconomySystem plugin, Player player) {
        char[][] grid = new char[ROWS][COLUMNS];
        for (char[] row : grid) {
            Arrays.fill(row, FILLER);
        }

        Map<Character, TreueshopReward> rewardsByKey = new LinkedHashMap<>();
        char nextKey = 'A';
        for (TreueshopReward reward : TreueshopRewardCatalog.xpTerminalRewards()) {
            char key = nextKey++;
            grid[reward.row() - 1][reward.column() - 1] = key;
            rewardsByKey.put(key, reward);
        }
        Arrays.fill(grid[ROWS - 1], DIVIDER);
        grid[ROWS - 1][0] = BALANCE;
        grid[ROWS - 1][COLUMNS - 1] = BACK;

        String[] structure = new String[ROWS];
        for (int r = 0; r < ROWS; r++) {
            structure[r] = new String(grid[r]);
        }

        Gui.Builder<?, ?> builder = Gui.builder()
                .setStructure(structure)
                .addIngredient(FILLER, TreueshopComponents.filler(Material.GRAY_STAINED_GLASS_PANE))
                .addIngredient(DIVIDER, TreueshopComponents.filler(Material.PURPLE_STAINED_GLASS_PANE))
                .addIngredient(BALANCE, TreueshopComponents.balanceItem(plugin))
                .addIngredient(BACK, TreueshopComponents.backButton(plugin));
        for (Map.Entry<Character, TreueshopReward> entry : rewardsByKey.entrySet()) {
            builder.addIngredient(entry.getKey(), xpBoostItem(plugin, entry.getValue()));
        }

        Window.builder()
                .setViewer(player)
                .setTitle("XP-Terminal")
                .setUpperGui(builder.build())
                .build()
                .open();
    }

    private static Item xpBoostItem(TesseraniaEconomySystem plugin, TreueshopReward reward) {
        int amount = xpAmount(reward.costConfigId());
        return Item.builder()
                .setItemProvider(TreueshopComponents.rewardIcon(plugin, reward))
                .addClickHandler((item, click) -> TreueshopGui.purchase(plugin, click.player(), reward,
                        (p, player) -> TreueshopEffects.applyXpBoost(player, amount),
                        () -> open(plugin, click.player())))
                .build();
    }

    private static int xpAmount(String costConfigId) {
        return switch (costConfigId) {
            case "xp-boost-1" -> 6000;
            case "xp-boost-2" -> 12500;
            case "xp-boost-3" -> 30000;
            case "xp-boost-4" -> 50000;
            default -> throw new IllegalStateException("Unknown XP-Terminal reward: " + costConfigId);
        };
    }
}
