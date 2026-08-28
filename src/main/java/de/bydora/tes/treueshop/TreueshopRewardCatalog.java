package de.bydora.tes.treueshop;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;

import java.util.List;

/**
 * The 12 top-level Treueshop rewards (spec §3.2.1.1), transcribed verbatim from the reference
 * build's {@code /debug dump} at -406 -11 -3390 (see {@code docs/gui-reference-capture.md}) rather
 * than the PDF's own text, which is occasionally under-specified or textually inconsistent. Any
 * reward that only opens a sub-interface (XP-Terminal, Freundliche/Feindliche Mobs I/II) carries
 * no cost here — that cost lives on whatever it grants once its sub-interface is built.
 */
public final class TreueshopRewardCatalog {

    private static final List<TreueshopReward> MAIN_INTERFACE_REWARDS = List.of(
            new TreueshopReward("prozessverstaerker", 25, 1, 1, Material.FURNACE, title("Prozessverstärker"), plugin -> List.of(
                    flavor("Item zum Boosten von Funktionsblöcken"),
                    flavor("Effekt stapelbar bei mehrfacher Nutzung"),
                    blank(),
                    flavor("Boostbare Blöcke:"),
                    flavor("Ofen → 2x Geschwindigkeit (" + plugin.tesConfig().treueshopProzessverstaerkerBoostMinutes() + " min)"),
                    flavor("Bienenstock → erhöhte Produktion")
            )),
            new TreueshopReward(null, 0, 5, 1, Material.CHICKEN_SPAWN_EGG, title("Freundliche Mobs I"), plugin -> List.of(
                    flavor("Öffnet das Tier-Submenü (T1)."),
                    flavor("Grundlegende freundliche Kreaturen."),
                    blank(),
                    flavor("„Alles beginnt mit etwas Fedrigem.“")
            ), "mo1"),
            new TreueshopReward("spawner", 1000, 9, 1, Material.TRIAL_SPAWNER, title("Spawner"), plugin -> List.of(
                    flavor("Du erhältst einen Spawner."),
                    flavor("Kann mit Spawneiern bestückt werden."),
                    flavor("Erzeugt kontrollierte Monsterwellen."),
                    blank(),
                    flavor("„Kontrolliertes Chaos in Blöcken.“")
            )),
            new TreueshopReward(null, 0, 1, 2, Material.EXPERIENCE_BOTTLE, title("XP-Terminal"), plugin -> List.of(
                    flavor("Öffnet das XP-Reward System"),
                    flavor("zur Verwaltung von Minecraft XP."),
                    blank(),
                    flavor("Wähle aus verschiedenen XP-Boosts"),
                    flavor("und Erfahrungs-Upgrades.")
            ), "xp-terminal"),
            new TreueshopReward("segen-der-zwerge", 100, 3, 2, Material.GOLDEN_PICKAXE, title("Segen der Zwerge"), plugin -> List.of(
                    flavor("„Die Erde gehört denen, die"),
                    flavor("schneller graben als sie denkt.“"),
                    blank(),
                    flavor("Gewährt Eile II für " + plugin.tesConfig().treueshopHasteMinutes() + " Minuten."),
                    flavor("Perfekt für tiefere Höhlenzüge.")
            )),
            new TreueshopReward(null, 0, 5, 2, Material.PARROT_SPAWN_EGG, title("Freundliche Mobs II"), plugin -> List.of(
                    flavor("Öffnet das Tier-Submenü (T2)."),
                    flavor("Erweiterte freundliche Kreaturen."),
                    blank(),
                    flavor("„Mehr Leben. Mehr Chaos. Mehr Farbe.“")
            ), "mo2"),
            new TreueshopReward("erntewelt", 160, 7, 2, Material.GRASS_BLOCK, title("Erntewelt", NamedTextColor.GREEN), plugin -> List.of(
                    flavor("3 Stunden Zugang zu einer"),
                    flavor("speziellen Ressourcenwelt."),
                    flavor("Mit großen Biomen und"),
                    flavor("erhöhten Ressourcen-Spawnraten."),
                    blank(),
                    flavor("Der Timer läuft nur aktiv"),
                    flavor("während deines Aufenthalts."),
                    blank(),
                    flavor("Reset alle 48 Stunden."),
                    flavor("Neue Welt wird generiert.")
            )),
            new TreueshopReward("handelsbonus", 150, 9, 2, Material.DIAMOND, title("Handelsbonus"), plugin -> {
                int discount = plugin.tesConfig().treueshopHandelsbonusDiscountDiamonds();
                int examplePrice = discount + 35;
                int exampleRemainder = Math.max(0, discount - 1);
                return List.of(
                        flavor("Money, Money, Money!"),
                        blank(),
                        flavor("Ermöglicht einen " + discount + "-Diamanten Rabatt"),
                        flavor("auf alle Einkäufe bis zum Aufbrauch."),
                        blank(),
                        flavor("Max. 2 aktive Spieler gleichzeitig."),
                        flavor("Rabatt wird pro Kauf automatisch"),
                        flavor("verrechnet und reduziert."),
                        flavor("Nicht verbrauchte Werte bleiben erhalten."),
                        blank(),
                        flavor("Finanzierung erfolgt über die Staatskasse."),
                        blank(),
                        flavor("Beispiel:"),
                        flavor("Kauf " + examplePrice + " → zahlt " + (examplePrice - discount) + ", Rest 0."),
                        flavor("Kauf 1 → zahlt 0, Rest " + exampleRemainder + ".")
                );
            }),
            new TreueshopReward("kraftelixier", 175, 3, 3, Material.GOLDEN_APPLE, title("Kraftelixier"), plugin -> List.of(
                    flavor("Ultimativer Boost für dein"),
                    flavor("Immunsystem!"),
                    blank(),
                    flavor("„Schmeckt nach Sieg und Magie.“"),
                    blank(),
                    flavor("Effekte für " + plugin.tesConfig().treueshopKraftelixierMinutes() + " Minuten:"),
                    flavor("Regeneration II"),
                    flavor("Resistenz II"),
                    flavor("Stärke"),
                    flavor("Held des Dorfes")
            )),
            new TreueshopReward(null, 0, 5, 3, Material.CREEPER_SPAWN_EGG, title("Feindliche Mobs I"), plugin -> List.of(
                    flavor("Öffnet das Kampf-Submenü (T1)."),
                    flavor("Einfache gefährliche Kreaturen."),
                    blank(),
                    flavor("„Explosiv. Direkt. Ehrlich.“")
            ), "mo3"),
            new TreueshopReward("glutzone", 160, 7, 3, Material.NETHERRACK, title("Glutzone", NamedTextColor.GOLD), plugin -> List.of(
                    flavor("3 Stunden Zugang zu einer"),
                    flavor("speziellen Nether-Ressourcenwelt."),
                    flavor("Gefüllt mit angepassten"),
                    flavor("Ressourcen-Boosts und Gefahren."),
                    blank(),
                    flavor("Der Timer läuft nur aktiv"),
                    flavor("während deines Aufenthalts."),
                    blank(),
                    flavor("Reset alle 48 Stunden."),
                    flavor("Neue Welt wird generiert.")
            )),
            new TreueshopReward(null, 0, 5, 4, Material.WARDEN_SPAWN_EGG, title("Feindliche Mobs II"), plugin -> List.of(
                    flavor("Öffnet das Kampf-Submenü (T2)."),
                    flavor("Elite & Bossartige Kreaturen."),
                    blank(),
                    flavor("„Du hörst es, bevor du es bereust.“")
            ), "mo4")
    );

    /**
     * The four XP-Terminal boosts (spec §3.2.1.1, Belohnung 2.1-2.4), transcribed verbatim from
     * the reference build's Subinterface-(XP) dump at -412 -12 -3387 — {@link Material#EXPERIENCE_BOTTLE}
     * icons, not the PDF's "Kopf mit 1/2/3/4" text (same reference-build-wins rule as the main
     * catalog). Each grants real vanilla Minecraft experience points via
     * {@link TreueshopEffects#applyXpBoost}, not TES's own Erfahrungspunkte counter — the PDF's
     * "(~ 50 Level)" etc. subtitles line up with the vanilla XP curve, not the level-system's
     * {@code f(x) = 30·sqrt(x/30000)} formula, and its dev-facing text says "(points)" to
     * disambiguate vanilla XP points from vanilla XP levels, a distinction that only exists for
     * the vanilla currency.
     */
    private static final List<TreueshopReward> XP_TERMINAL_REWARDS = List.of(
            new TreueshopReward("xp-boost-1", 50, 2, 2, Material.EXPERIENCE_BOTTLE, title("XP-Boost (Stufe I)"), plugin -> List.of(
                    flavor("Du erhältst 6.000 XP"),
                    flavor("Dies entspricht etwa 50 Leveln")
            )),
            new TreueshopReward("xp-boost-2", 75, 4, 2, Material.EXPERIENCE_BOTTLE, title("XP-Boost (Stufe II)"), plugin -> List.of(
                    flavor("Du erhältst 12.500 XP"),
                    flavor("Dies entspricht etwa 69 Leveln")
            )),
            new TreueshopReward("xp-boost-3", 100, 6, 2, Material.EXPERIENCE_BOTTLE, title("XP-Boost (Stufe III)"), plugin -> List.of(
                    flavor("Du erhältst 30.000 XP"),
                    flavor("Dies entspricht etwa 100 Leveln")
            )),
            new TreueshopReward("xp-boost-4", 125, 8, 2, Material.EXPERIENCE_BOTTLE, title("XP-Boost (Stufe IV)"), plugin -> List.of(
                    flavor("Du erhältst 50.000 XP"),
                    flavor("Dies entspricht etwa 120 Leveln")
            ))
    );

    private TreueshopRewardCatalog() {
    }

    public static List<TreueshopReward> mainInterfaceRewards() {
        return MAIN_INTERFACE_REWARDS;
    }

    public static List<TreueshopReward> xpTerminalRewards() {
        return XP_TERMINAL_REWARDS;
    }

    private static Component title(String text) {
        return title(text, NamedTextColor.WHITE);
    }

    private static Component title(String text, NamedTextColor color) {
        return Component.text(text, color, TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false);
    }

    private static Component flavor(String text) {
        return Component.text(text, NamedTextColor.GRAY, TextDecoration.ITALIC);
    }

    private static Component blank() {
        return Component.empty();
    }
}
