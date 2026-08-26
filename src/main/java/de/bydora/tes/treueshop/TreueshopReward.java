package de.bydora.tes.treueshop;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;

import java.util.List;

/**
 * Metadata for one of the Treueshop main interface's top-level rewards (spec §3.2.1.1): grid
 * position (1-indexed column/row, matching the reference build's own {@code col|row} notation),
 * icon, display title, and flavor lore. Rewards with no {@link #costConfigId()} (e.g. the ones
 * that just open a sub-interface) have no Treuepunkte cost of their own.
 *
 * @param costConfigId the {@code treueshop.rewards.<id>.cost} config key, or {@code null} if this
 *                      reward has no direct cost
 * @param defaultCost   the shipped default cost, used as the config fallback
 */
public record TreueshopReward(
        String costConfigId,
        int defaultCost,
        int column,
        int row,
        Material icon,
        Component title,
        List<Component> flavorLore
) {

    public boolean hasCost() {
        return costConfigId != null;
    }
}
