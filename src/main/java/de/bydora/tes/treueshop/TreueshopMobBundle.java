package de.bydora.tes.treueshop;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;

import java.util.List;

/**
 * One of the four bundled mob-egg Treueshop rewards (spec §3.2.1.1, Belohnung 8.1/9.1/10.1/11.1):
 * a single Treuepunkte-cost purchase that grants every {@link EggGrant} in {@link #eggs()} at
 * once. Bundled rather than à-la-carte per the reconciliation decision in
 * {@code docs/treueshop-system.md} — the reference build's chests price each egg individually,
 * but the PDF describes one bundle purchase per tier, and the tie-break (asked of the user)
 * favored the PDF since either shape is just a data change here.
 *
 * @param costConfigId the {@code treueshop.rewards.<id>.cost} config key
 * @param defaultCost  the shipped default cost, used as the config fallback
 */
public record TreueshopMobBundle(
        String costConfigId,
        int defaultCost,
        Material icon,
        Component title,
        List<Component> flavorLore,
        List<EggGrant> eggs
) {

    public record EggGrant(Material eggMaterial, int amount) {
    }
}
