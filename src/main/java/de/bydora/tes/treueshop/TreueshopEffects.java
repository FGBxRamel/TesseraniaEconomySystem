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
     * Belohnung 3, "Segen der Zwerge": Haste II for the configured duration.
     */
    public static void applySegenDerZwerge(TesseraniaEconomySystem plugin, Player player) {
        int durationTicks = plugin.tesConfig().treueshopHasteMinutes() * 60 * 20;
        player.addPotionEffect(new PotionEffect(PotionEffectType.HASTE, durationTicks, 1));
    }

    /**
     * Belohnung 7, "Kraftelixier": Regeneration II, Resistenz II, Stärke (I) and Held des Dorfes
     * (I), all for the configured duration.
     */
    public static void applyKraftelixier(TesseraniaEconomySystem plugin, Player player) {
        int durationTicks = plugin.tesConfig().treueshopKraftelixierMinutes() * 60 * 20;
        player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, durationTicks, 1));
        player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, durationTicks, 1));
        player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, durationTicks, 0));
        player.addPotionEffect(new PotionEffect(PotionEffectType.HERO_OF_THE_VILLAGE, durationTicks, 0));
    }
}
