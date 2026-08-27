package de.bydora.tes.treueshop;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;

import java.util.List;

/**
 * The four mob-egg-tier Treueshop sub-interfaces (spec §3.2.1.1, Belohnung 8.1/9.1/10.1/11.1), one
 * {@link TreueshopMobBundle} per tier. Species, per-egg names and flavor text are transcribed
 * verbatim from the {@code /debug dump} captures of the reference build's {@code Mobs_V1-V4}
 * chests (see {@code docs/treueshop-system.md}); grant amounts (1, or 2 for the large-animal
 * options in Freundliche Mobs I) come from the PDF's developer description, since the reference
 * chest only ever displays a single example egg per slot.
 */
public final class TreueshopMobBundleCatalog {

    public static final TreueshopMobBundle FREUNDLICHE_MOBS_I = new TreueshopMobBundle(
            "freundliche-mobs-1", 200, title("Freundliche Mobs I"), List.of(
                    egg(Material.ARMADILLO_SPAWN_EGG, "Gürteltier-Spawnei", 1,
                            "Rollt sich ein, wenn es Probleme", "sieht. Also sehr oft."),
                    egg(Material.FOX_SPAWN_EGG, "Fuchs-Spawnei", 1,
                            "Sieht süß aus, plant aber definitiv", "irgendwas gegen dich."),
                    egg(Material.ALLAY_SPAWN_EGG, "Hilfsgeist-Spawnei", 1,
                            "Bringt dir deine Items zurück und", "ist sozial kompetenter als du."),
                    egg(Material.PANDA_SPAWN_EGG, "Panda-Spawnei", 1,
                            "Isst Bambus und bewertet dein Leben", "still und enttäuscht."),
                    egg(Material.POLAR_BEAR_SPAWN_EGG, "Eisbär-Spawnei", 1,
                            "Sieht freundlich aus, bis du", "zu nah an seine Fischbox kommst."),
                    egg(Material.CAT_SPAWN_EGG, "Katze-Spawnei", 1,
                            "Ignoriert dich aktiv, aber liebt", "dein Eigentum trotzdem."),
                    egg(Material.WOLF_SPAWN_EGG, "Wolf-Spawnei", 1,
                            "Treu bis zum Tod. Oder bis ein", "Skelett in der Nähe ist."),
                    egg(Material.OCELOT_SPAWN_EGG, "Ozelot-Spawnei", 1,
                            "Extrem scheu und verschwunden", "bevor du 'Hallo' sagen kannst."),
                    egg(Material.CAMEL_SPAWN_EGG, "Kamel-Spawnei", 1,
                            "Trägt dich durch die Wüste und", "urteilt still über deine Ausdauer."),
                    egg(Material.BEE_SPAWN_EGG, "Bienen-Spawnei", 1,
                            "Produziert Honig und Chaos im", "gleichen unklaren Verhältnis."),
                    egg(Material.COW_SPAWN_EGG, "Kuh-Spawnei", 2,
                            "Gibt Milch und existiert einfach", "so vor sich hin. Sehr entspannt."),
                    egg(Material.HORSE_SPAWN_EGG, "Pferd-Spawnei", 2,
                            "Schnell, stolz und ignoriert dich", "bis du es sattelst."),
                    egg(Material.DONKEY_SPAWN_EGG, "Esel-Spawnei", 2,
                            "Trägt alles, meckert aber innerlich", "die ganze Zeit."),
                    egg(Material.CHICKEN_SPAWN_EGG, "Huhn-Spawnei", 2,
                            "Rennt panisch herum und tut so", "als hätte es einen Plan."),
                    egg(Material.PIG_SPAWN_EGG, "Schwein-Spawnei", 2,
                            "Einfach ein Schwein. Sehr ehrlich.", "Kein Drama, nur Geräusche."),
                    egg(Material.RABBIT_SPAWN_EGG, "Hase-Spawnei", 2,
                            "Süß, schnell und verschwindet", "immer genau dann, wenn du schaust.")
            ));

    public static final TreueshopMobBundle FREUNDLICHE_MOBS_II = new TreueshopMobBundle(
            "freundliche-mobs-2", 200, title("Freundliche Mobs II"), List.of(
                    egg(Material.VILLAGER_SPAWN_EGG, "Dorfbewohner-Spawnei", 1,
                            "Er arbeitet nicht viel, aber will", "trotzdem ständig überzahlt werden."),
                    egg(Material.TURTLE_SPAWN_EGG, "Schildkröten-Spawnei", 1,
                            "Extrem langsam, aber mit", "Selbstbewusstsein eines Rennpferds."),
                    egg(Material.PARROT_SPAWN_EGG, "Papageien-Spawnei", 1,
                            "Wiederholt alles, inklusive deiner", "schlechtesten Entscheidungen."),
                    egg(Material.MOOSHROOM_SPAWN_EGG, "Pilzkuh-Spawnei", 1,
                            "Kuh, aber mit Pilzen. Niemand", "weiß genau warum, aber okay."),
                    egg(Material.GOAT_SPAWN_EGG, "Ziegen-Spawnei", 1,
                            "Existiert nur, um dich von", "hohen Bergen zu schubsen."),
                    egg(Material.LLAMA_SPAWN_EGG, "Lama-Spawnei", 1,
                            "Spuckt dich an, wenn es deine", "innere Aura nicht mag.")
            ));

    public static final TreueshopMobBundle FEINDLICHE_MOBS_I = new TreueshopMobBundle(
            "feindliche-mobs-1", 250, title("Feindliche Mobs I"), List.of(
                    egg(Material.ZOMBIE_SPAWN_EGG, "Zombie-Spawnei", 1,
                            "Für gemütliche Gartentreffen", "mit sehr unbequemen Gästen."),
                    egg(Material.SKELETON_SPAWN_EGG, "Skelett-Spawnei", 1,
                            "Für Leute, die nachts getroffen", "werden wollen, aber schüchtern sind."),
                    egg(Material.CREEPER_SPAWN_EGG, "Creeper-Spawnei", 1,
                            "Perfekt für Freunde, die sagen", "'Ich baue sicher' und dann weinen."),
                    egg(Material.SPIDER_SPAWN_EGG, "Spinnen-Spawnei", 1,
                            "Für Kellerbewohner, die keine", "Miete zahlen, aber trotzdem wohnen.")
            ));

    /**
     * Grants a Warden, not the reference build's original Guardian — the {@code Mobs_V4} chest was
     * updated to a {@code WARDEN_SPAWN_EGG} slot (spec v1.3, 27.08.2026 revision), overriding the
     * earlier Guardian reconciliation recorded in {@code docs/treueshop-system.md}.
     */
    public static final TreueshopMobBundle FEINDLICHE_MOBS_II = new TreueshopMobBundle(
            "feindliche-mobs-2", 275, title("Feindliche Mobs II"), List.of(
                    egg(Material.WARDEN_SPAWN_EGG, "Warden-Spawnei", 1,
                            "„Du hörst es, bevor du es bereust.“"),
                    egg(Material.CREAKING_SPAWN_EGG, "Knarz-Spawnei", 1,
                            "Es knackt im Wald. Nein, es ist", "kein Baum, es ist dein Problem."),
                    egg(Material.PHANTOM_SPAWN_EGG, "Phantom-Spawnei", 1,
                            "Kommt nur wenn du schlafen willst", "und hasst deine Lebensentscheidungen."),
                    egg(Material.PIGLIN_SPAWN_EGG, "Piglin-Spawnei", 1,
                            "Handelt fair, solange du nicht", "vergisst zu bezahlen."),
                    egg(Material.PIGLIN_BRUTE_SPAWN_EGG, "Piglin-Barbar-Spawnei", 1,
                            "Er verhandelt nicht. Er gewinnt.", "Immer. Auch beim Tod."),
                    egg(Material.WITHER_SKELETON_SPAWN_EGG, "Witherskelett-Spawnei", 1,
                            "Bringt Feuer, Knochen und schlechte", "Laune in dein Netherleben.")
            ));

    private TreueshopMobBundleCatalog() {
    }

    private static TreueshopMobBundle.MobEggOption egg(Material material, String name, int amount, String... flavorLines) {
        return new TreueshopMobBundle.MobEggOption(material, title(name), flavor(flavorLines), amount);
    }

    private static Component title(String text) {
        return Component.text(text, NamedTextColor.WHITE, TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false);
    }

    private static List<Component> flavor(String... lines) {
        return List.of(lines).stream()
                .map(line -> (Component) Component.text(line, NamedTextColor.GRAY, TextDecoration.ITALIC))
                .toList();
    }
}
