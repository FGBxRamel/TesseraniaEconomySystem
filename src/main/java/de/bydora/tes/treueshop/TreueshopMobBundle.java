package de.bydora.tes.treueshop;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;

import java.util.List;

/**
 * One of the four mob-egg-tier Treueshop sub-interfaces (spec §3.2.1.1, Belohnung 8.1/9.1/10.1/11.1):
 * a shared Treuepunkte cost, and a menu of individually-purchasable {@link MobEggOption}s. Each
 * option is bought on its own — a purchase grants only that one species, not every option in the
 * tier — per spec v1.3's "Der Spieler kann auswählen, ob er ... erwirbt" wording and the reference
 * build's per-egg-priced {@code Mobs_V1-V4} chests (see {@code docs/treueshop-system.md}).
 *
 * @param costConfigId the {@code treueshop.rewards.<id>.cost} config key, shared by every option
 * @param defaultCost  the shipped default cost, used as the config fallback
 */
public record TreueshopMobBundle(
        String costConfigId,
        int defaultCost,
        Component title,
        List<MobEggOption> options
) {

    /**
     * @param amount how many eggs a single purchase of this option grants (1, except the
     *               large-animal options in Freundliche Mobs I, which grant 2 per spec §3.2.1.1)
     * @param row    1-3, the sub-interface's content row, transcribed from the reference build's
     *               {@code Mobs_V1-V4} chest dumps (that chest's own row 1, 2 and 4 respectively —
     *               row 3 is always an empty spacer there and isn't reproduced as its own row)
     * @param column 1-9, transcribed as-is from the dump; a gap at column 5 recurs across every
     *               tier's content row, splitting it into two visually-grouped halves
     */
    public record MobEggOption(Material eggMaterial, Component title, List<Component> flavorLore, int amount, int row, int column) {
    }
}
