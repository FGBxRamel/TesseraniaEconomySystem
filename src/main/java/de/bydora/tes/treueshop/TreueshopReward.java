package de.bydora.tes.treueshop;

import de.bydora.tes.TesseraniaEconomySystem;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;

import java.util.List;
import java.util.function.Function;

/**
 * Metadata for one of the Treueshop main interface's top-level rewards (spec §3.2.1.1): grid
 * position (1-indexed column/row, matching the reference build's own {@code col|row} notation),
 * icon, display title, and flavor lore. Rewards with no {@link #costConfigId()} (e.g. the ones
 * that just open a sub-interface) have no Treuepunkte cost of their own.
 *
 * <p>The same record is reused for a sub-interface's own leaf rewards (e.g. the four XP-Terminal
 * boosts) — {@link #column()}/{@link #row()} then address that sub-interface's own grid, not the
 * main interface's.
 *
 * <p>{@link #flavorLore()} is a function rather than a fixed {@code List<Component>} so that lore
 * mentioning a config-driven value (e.g. Prozessverstärker's boost duration, Handelsbonus' discount
 * and worked example) reads the current config on every render instead of baking in the shipped
 * default at class-init time. Most rewards ignore the argument and just return a constant list.
 *
 * @param costConfigId    the {@code treueshop.rewards.<id>.cost} config key, or {@code null} if
 *                         this reward has no direct cost
 * @param defaultCost      the shipped default cost, used as the config fallback
 * @param subInterfaceId   which sub-interface this reward opens (e.g. {@code "xp-terminal"},
 *                          {@code "mo1"}), or {@code null} if it doesn't open one. Independent of
 *                          {@link #hasCost()}: every current sub-interface opener happens to be
 *                          costless (the cost lives on what the sub-interface sells instead), but
 *                          nothing requires that.
 */
public record TreueshopReward(
        String costConfigId,
        int defaultCost,
        int column,
        int row,
        Material icon,
        Component title,
        Function<TesseraniaEconomySystem, List<Component>> flavorLore,
        String subInterfaceId
) {

    public TreueshopReward(String costConfigId, int defaultCost, int column, int row, Material icon,
            Component title, Function<TesseraniaEconomySystem, List<Component>> flavorLore) {
        this(costConfigId, defaultCost, column, row, icon, title, flavorLore, null);
    }

    public boolean hasCost() {
        return costConfigId != null;
    }

    public boolean opensSubInterface() {
        return subInterfaceId != null;
    }
}
