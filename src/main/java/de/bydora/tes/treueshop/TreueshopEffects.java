package de.bydora.tes.treueshop;

import de.bydora.tes.TesseraniaEconomySystem;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * Direct potion-effect Treueshop rewards, applied immediately to the purchasing player (spec
 * §3.2.1.1) rather than routed through the Belohnungsinventar like the item-grant rewards.
 */
public final class TreueshopEffects {

    private TreueshopEffects() {
    }

    /**
     * Belohnung 3, "Segen der Zwerge": Haste II for the configured duration. Stacks with any
     * remaining duration from a still-active effect rather than resetting it.
     */
    public static void applySegenDerZwerge(TesseraniaEconomySystem plugin, Player player) {
        int durationTicks = plugin.tesConfig().treueshopHasteMinutes() * 60 * 20;
        applyOrExtend(player, PotionEffectType.HASTE, durationTicks, 1);
    }

    /**
     * Belohnung 7, "Kraftelixier": Regeneration II, Resistenz II, Stärke (I) and Held des Dorfes
     * (I), all for the configured duration. Stacks with any remaining duration from a
     * still-active effect rather than resetting it.
     */
    public static void applyKraftelixier(TesseraniaEconomySystem plugin, Player player) {
        int durationTicks = plugin.tesConfig().treueshopKraftelixierMinutes() * 60 * 20;
        applyOrExtend(player, PotionEffectType.REGENERATION, durationTicks, 1);
        applyOrExtend(player, PotionEffectType.RESISTANCE, durationTicks, 1);
        applyOrExtend(player, PotionEffectType.STRENGTH, durationTicks, 0);
        applyOrExtend(player, PotionEffectType.HERO_OF_THE_VILLAGE, durationTicks, 0);
    }

    /**
     * Applies {@code type} for {@code durationTicks}, adding on top of any remaining duration
     * from an already-active effect of the same type instead of overwriting it (Bukkit's
     * {@link Player#addPotionEffect(PotionEffect)} otherwise resets the timer on reuse).
     */
    private static void applyOrExtend(Player player, PotionEffectType type, int durationTicks, int amplifier) {
        PotionEffect existing = player.getPotionEffect(type);
        int remainingTicks = existing != null ? existing.getDuration() : 0;
        player.addPotionEffect(new PotionEffect(type, remainingTicks + durationTicks, amplifier));
    }

    /**
     * Belohnung 2.1-2.4, the XP-Terminal boosts: grants raw vanilla Minecraft experience points
     * (not TES's own Erfahrungspunkte counter — see {@link TreueshopRewardCatalog}'s Javadoc on
     * {@code XP_TERMINAL_REWARDS} for why).
     */
    public static void applyXpBoost(Player player, int amount) {
        player.giveExp(amount);
    }
}
