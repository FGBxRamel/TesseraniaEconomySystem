package de.bydora.tes.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;

/**
 * Centralized German, admin/player-facing chat messages for the {@code /tes} command family.
 */
public final class Messages {

    private Messages() {
    }

    public static Component noPermission() {
        return Component.text("Dazu hast du keine Berechtigung.", NamedTextColor.RED);
    }

    public static Component usage(String usage) {
        return Component.text("Verwendung: " + usage, NamedTextColor.RED);
    }

    public static Component playerNeverSeen(String name) {
        return Component.text("Der Spieler \"" + name + "\" ist dem Server nicht bekannt.", NamedTextColor.RED);
    }

    public static Component alreadyRegistered(String name) {
        return Component.text(name + " ist bereits im Belohnungssystem registriert.", NamedTextColor.RED);
    }

    public static Component notRegistered(String name) {
        return Component.text(name + " ist nicht im Belohnungssystem registriert.", NamedTextColor.RED);
    }

    public static Component registered(String name) {
        return Component.text(name + " wurde erfolgreich im Belohnungssystem registriert.", NamedTextColor.GREEN);
    }

    public static Component paused(String name) {
        return Component.text(name + " wurde pausiert. Das Belohnungssystem ist für diesen Spieler bis zum Entpausieren inaktiv.", NamedTextColor.YELLOW);
    }

    public static Component alreadyPaused(String name) {
        return Component.text(name + " ist bereits pausiert.", NamedTextColor.RED);
    }

    public static Component unpaused(String name) {
        return Component.text(name + " wurde entpausiert. Das Belohnungssystem ist für diesen Spieler wieder aktiv.", NamedTextColor.GREEN);
    }

    public static Component notPaused(String name) {
        return Component.text(name + " ist nicht pausiert.", NamedTextColor.RED);
    }

    /**
     * The clickable re-confirmation prompt shown before {@code /tes spieler remove} takes effect.
     */
    public static Component removeConfirmPrompt(String name, String token, long ttlSeconds) {
        Component button = Component.text("[Bestätigen]", NamedTextColor.RED, net.kyori.adventure.text.format.TextDecoration.BOLD)
                .clickEvent(ClickEvent.runCommand("/tes spieler remove " + name + " " + token))
                .hoverEvent(HoverEvent.showText(Component.text(
                        "Löscht ALLE Daten von " + name + " unwiderruflich (Statistiken, virtuelle Inventare, TP/EP/Level). Läuft in " + ttlSeconds + " Sekunden ab.",
                        NamedTextColor.RED)));
        return Component.text("Achtung: ", NamedTextColor.RED, net.kyori.adventure.text.format.TextDecoration.BOLD)
                .append(Component.text("Das Entfernen von " + name + " löscht unwiderruflich alle zugehörigen Daten. ", NamedTextColor.RED))
                .append(button);
    }

    public static Component removeConfirmExpiredOrInvalid() {
        return Component.text("Keine ausstehende Bestätigung gefunden oder abgelaufen. Bitte den Befehl erneut ausführen.", NamedTextColor.RED);
    }

    public static Component removed(String name) {
        return Component.text(name + " wurde endgültig aus dem Belohnungssystem entfernt. Alle zugehörigen Daten wurden gelöscht.", NamedTextColor.GREEN);
    }

}
