package de.bydora.tes.treueshop;

import de.bydora.tes.TesseraniaEconomySystem;
import de.bydora.tes.data.PlayerRecord;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import xyz.xenondevs.invui.item.Item;
import xyz.xenondevs.invui.item.ItemBuilder;

import java.util.ArrayList;
import java.util.List;

/**
 * UI elements shared by the Treueshop main interface and its sub-interfaces (XP-Terminal, the
 * four mob-bundle menus): the Treuepunkte balance display and the "⮜ Zurück" button back to the
 * main interface, plus the common reward-icon-with-cost-lore builder every reward grid uses.
 */
final class TreueshopComponents {

    private TreueshopComponents() {
    }

    static Item balanceItem(TesseraniaEconomySystem plugin) {
        return Item.builder()
                .setItemProvider(viewer -> new ItemBuilder(Material.SUNFLOWER)
                        .setName(Component.text("Treuepunkte (TP)", NamedTextColor.YELLOW, TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false))
                        .addLoreLines(
                                Component.text(currentTreuepunkte(plugin, viewer) + " Punkte", NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false),
                                Component.empty(),
                                Component.text("Treuepunkte können passiv beim Einkauf", NamedTextColor.GRAY, TextDecoration.ITALIC),
                                Component.text("in anderen Spieler-Shops generiert werden.", NamedTextColor.GRAY, TextDecoration.ITALIC)))
                .build();
    }

    private static int currentTreuepunkte(TesseraniaEconomySystem plugin, org.bukkit.entity.Player viewer) {
        return plugin.playerRepository().findByUuid(viewer.getUniqueId()).map(PlayerRecord::treuepunkte).orElse(0);
    }

    static Item filler(Material paneColor) {
        return Item.simple(new ItemBuilder(paneColor).setName(Component.text(" ")));
    }

    /**
     * The "⮜ Zurück" button every sub-interface (spec §3.2.1.2) uses to return to the main
     * interface — {@code SPECTRAL_ARROW}, matching the reference build rather than the PDF's
     * "Goldener Pfeil" text.
     */
    static Item backButton(TesseraniaEconomySystem plugin) {
        return Item.builder()
                .setItemProvider(new ItemBuilder(Material.SPECTRAL_ARROW)
                        .setName(Component.text("⮜ Zurück", NamedTextColor.WHITE, TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false)))
                .addClickHandler((item, click) -> TreueshopGui.open(plugin, click.player()))
                .build();
    }

    /**
     * Builds the icon/name/lore for a reward grid slot: a "Kosten: X TP" line (resolved through
     * config) prepended when the reward has a cost, followed by its flavor lore. Callers add
     * their own click handler on top.
     */
    static ItemBuilder rewardIcon(TesseraniaEconomySystem plugin, TreueshopReward reward) {
        List<Component> lore = new ArrayList<>();
        if (reward.hasCost()) {
            int cost = plugin.tesConfig().treueshopRewardCost(reward.costConfigId(), reward.defaultCost());
            lore.add(Component.text("Kosten: " + cost + " TP", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
            lore.add(Component.empty());
        }
        lore.addAll(reward.flavorLore());

        return new ItemBuilder(reward.icon())
                .setName(reward.title())
                .addLoreLines(lore);
    }

    /**
     * Same as {@link #rewardIcon(TesseraniaEconomySystem, TreueshopReward)}, for a bundled
     * mob-egg reward — always has a cost, unlike the generic {@link TreueshopReward}.
     */
    static ItemBuilder rewardIcon(TesseraniaEconomySystem plugin, TreueshopMobBundle bundle) {
        int cost = plugin.tesConfig().treueshopRewardCost(bundle.costConfigId(), bundle.defaultCost());
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("Kosten: " + cost + " TP", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
        lore.add(Component.empty());
        lore.addAll(bundle.flavorLore());

        return new ItemBuilder(bundle.icon())
                .setName(bundle.title())
                .addLoreLines(lore);
    }
}
