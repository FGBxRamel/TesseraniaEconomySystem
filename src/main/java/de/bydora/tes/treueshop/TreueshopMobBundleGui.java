package de.bydora.tes.treueshop;

import de.bydora.tes.TesseraniaEconomySystem;
import de.bydora.tes.util.Messages;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import xyz.xenondevs.invui.gui.Gui;
import xyz.xenondevs.invui.item.Item;
import xyz.xenondevs.invui.window.Window;

/**
 * A bundled mob-egg sub-interface (spec §3.2.1.2, "Subinterface Mo1-Mo4"): a single purchase
 * button for the given {@link TreueshopMobBundle}, plus the shared balance display and
 * "⮜ Zurück" button. One 9x4 layout serves all four tiers — the reference build's per-egg chests
 * (à-la-carte, 9x5/9x6) don't apply once the reward is a single bundled purchase (see
 * {@code docs/treueshop-system.md}).
 */
public final class TreueshopMobBundleGui {

    private static final String[] STRUCTURE = {
            "ggggggggg",
            "gggg#gggg",
            "ggggggggg",
            "bpppppppz"
    };

    private TreueshopMobBundleGui() {
    }

    public static void open(TesseraniaEconomySystem plugin, Player player, TreueshopMobBundle bundle) {
        Gui gui = Gui.builder()
                .setStructure(STRUCTURE)
                .addIngredient('g', TreueshopComponents.filler(Material.GRAY_STAINED_GLASS_PANE))
                .addIngredient('p', TreueshopComponents.filler(Material.PURPLE_STAINED_GLASS_PANE))
                .addIngredient('b', TreueshopComponents.balanceItem(plugin))
                .addIngredient('z', TreueshopComponents.backButton(plugin))
                .addIngredient('#', bundleItem(plugin, bundle))
                .build();

        Window.builder()
                .setViewer(player)
                .setTitle(PlainTextComponentSerializer.plainText().serialize(bundle.title()))
                .setUpperGui(gui)
                .build()
                .open();
    }

    private static Item bundleItem(TesseraniaEconomySystem plugin, TreueshopMobBundle bundle) {
        return Item.builder()
                .setItemProvider(TreueshopComponents.rewardIcon(plugin, bundle))
                .addClickHandler((item, click) -> purchase(plugin, click.player(), bundle))
                .build();
    }

    private static void purchase(TesseraniaEconomySystem plugin, Player player, TreueshopMobBundle bundle) {
        int cost = plugin.tesConfig().treueshopRewardCost(bundle.costConfigId(), bundle.defaultCost());
        TreueshopRewardService.PurchaseResult result = TreueshopRewardService.purchase(
                plugin, player, cost, () -> TreueshopItemGrants.grantMobBundle(plugin, player, bundle));
        if (result == TreueshopRewardService.PurchaseResult.INSUFFICIENT_TP) {
            player.sendMessage(Messages.treueshopInsufficientTp());
            return;
        }
        String bundleName = PlainTextComponentSerializer.plainText().serialize(bundle.title());
        player.sendMessage(Messages.treueshopRewardPurchased(bundleName));
        open(plugin, player, bundle);
    }
}
