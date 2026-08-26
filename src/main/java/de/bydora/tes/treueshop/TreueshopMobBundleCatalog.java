package de.bydora.tes.treueshop;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;

import java.util.List;

/**
 * The four bundled mob-egg Treueshop rewards (spec §3.2.1.1, Belohnung 8.1/9.1/10.1/11.1), one
 * per {@link TreueshopMobBundle} tier. Egg species/amounts are transcribed from the PDF's own
 * developer descriptions (the reference build's {@code Mobs_V1-V4} chests show the same species
 * but priced à-la-carte, per the bundling decision — see {@code docs/treueshop-system.md}), except
 * Feindliche Mobs II, which grants a Guardian instead of the PDF's Warden per the user-confirmed
 * reconciliation (the reference build's Mobs_V4 chest has no Warden slot at all).
 */
public final class TreueshopMobBundleCatalog {

    public static final TreueshopMobBundle FREUNDLICHE_MOBS_I = new TreueshopMobBundle(
            "freundliche-mobs-1", 200, Material.CHICKEN_SPAWN_EGG, title("Freundliche Mobs I"), List.of(
                    flavor("Je 1x: Hilfsgeist, Gürteltier, Ozelot,"),
                    flavor("Panda, Eisbär, Katze, Wolf, Fuchs,"),
                    flavor("Dromedar, Biene"),
                    blank(),
                    flavor("Je 2x: Kuh, Esel, Huhn, Schwein,"),
                    flavor("Pferd, Kaninchen")
            ), List.of(
                    new TreueshopMobBundle.EggGrant(Material.ALLAY_SPAWN_EGG, 1),
                    new TreueshopMobBundle.EggGrant(Material.ARMADILLO_SPAWN_EGG, 1),
                    new TreueshopMobBundle.EggGrant(Material.OCELOT_SPAWN_EGG, 1),
                    new TreueshopMobBundle.EggGrant(Material.PANDA_SPAWN_EGG, 1),
                    new TreueshopMobBundle.EggGrant(Material.POLAR_BEAR_SPAWN_EGG, 1),
                    new TreueshopMobBundle.EggGrant(Material.CAT_SPAWN_EGG, 1),
                    new TreueshopMobBundle.EggGrant(Material.WOLF_SPAWN_EGG, 1),
                    new TreueshopMobBundle.EggGrant(Material.FOX_SPAWN_EGG, 1),
                    new TreueshopMobBundle.EggGrant(Material.CAMEL_SPAWN_EGG, 1),
                    new TreueshopMobBundle.EggGrant(Material.BEE_SPAWN_EGG, 1),
                    new TreueshopMobBundle.EggGrant(Material.COW_SPAWN_EGG, 2),
                    new TreueshopMobBundle.EggGrant(Material.DONKEY_SPAWN_EGG, 2),
                    new TreueshopMobBundle.EggGrant(Material.CHICKEN_SPAWN_EGG, 2),
                    new TreueshopMobBundle.EggGrant(Material.PIG_SPAWN_EGG, 2),
                    new TreueshopMobBundle.EggGrant(Material.HORSE_SPAWN_EGG, 2),
                    new TreueshopMobBundle.EggGrant(Material.RABBIT_SPAWN_EGG, 2)
            ));

    public static final TreueshopMobBundle FREUNDLICHE_MOBS_II = new TreueshopMobBundle(
            "freundliche-mobs-2", 200, Material.PARROT_SPAWN_EGG, title("Freundliche Mobs II"), List.of(
                    flavor("Je 1x: Ziege, Lama, Pilzkuh,"),
                    flavor("Papagei, Schildkröte, Dorfbewohner")
            ), List.of(
                    new TreueshopMobBundle.EggGrant(Material.GOAT_SPAWN_EGG, 1),
                    new TreueshopMobBundle.EggGrant(Material.LLAMA_SPAWN_EGG, 1),
                    new TreueshopMobBundle.EggGrant(Material.MOOSHROOM_SPAWN_EGG, 1),
                    new TreueshopMobBundle.EggGrant(Material.PARROT_SPAWN_EGG, 1),
                    new TreueshopMobBundle.EggGrant(Material.TURTLE_SPAWN_EGG, 1),
                    new TreueshopMobBundle.EggGrant(Material.VILLAGER_SPAWN_EGG, 1)
            ));

    public static final TreueshopMobBundle FEINDLICHE_MOBS_I = new TreueshopMobBundle(
            "feindliche-mobs-1", 250, Material.CREEPER_SPAWN_EGG, title("Feindliche Mobs I"), List.of(
                    flavor("Je 1x: Zombie, Skelett,"),
                    flavor("Spinne, Creeper")
            ), List.of(
                    new TreueshopMobBundle.EggGrant(Material.ZOMBIE_SPAWN_EGG, 1),
                    new TreueshopMobBundle.EggGrant(Material.SKELETON_SPAWN_EGG, 1),
                    new TreueshopMobBundle.EggGrant(Material.SPIDER_SPAWN_EGG, 1),
                    new TreueshopMobBundle.EggGrant(Material.CREEPER_SPAWN_EGG, 1)
            ));

    public static final TreueshopMobBundle FEINDLICHE_MOBS_II = new TreueshopMobBundle(
            "feindliche-mobs-2", 275, Material.GUARDIAN_SPAWN_EGG, title("Feindliche Mobs II"), List.of(
                    flavor("Je 1x: Phantom, Knarz, Piglin,"),
                    flavor("Piglinbrut, Witherskelett, Guardian")
            ), List.of(
                    new TreueshopMobBundle.EggGrant(Material.PHANTOM_SPAWN_EGG, 1),
                    new TreueshopMobBundle.EggGrant(Material.CREAKING_SPAWN_EGG, 1),
                    new TreueshopMobBundle.EggGrant(Material.PIGLIN_SPAWN_EGG, 1),
                    new TreueshopMobBundle.EggGrant(Material.PIGLIN_BRUTE_SPAWN_EGG, 1),
                    new TreueshopMobBundle.EggGrant(Material.WITHER_SKELETON_SPAWN_EGG, 1),
                    new TreueshopMobBundle.EggGrant(Material.GUARDIAN_SPAWN_EGG, 1)
            ));

    private TreueshopMobBundleCatalog() {
    }

    private static Component title(String text) {
        return Component.text(text, NamedTextColor.WHITE, TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false);
    }

    private static Component flavor(String text) {
        return Component.text(text, NamedTextColor.GRAY, TextDecoration.ITALIC);
    }

    private static Component blank() {
        return Component.empty();
    }
}
