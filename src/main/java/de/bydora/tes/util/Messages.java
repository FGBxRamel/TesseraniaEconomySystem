package de.bydora.tes.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

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
        Component button = Component.text("»» BESTÄTIGEN ««", NamedTextColor.GOLD, TextDecoration.BOLD, TextDecoration.UNDERLINED)
                .clickEvent(ClickEvent.runCommand("/tes spieler remove " + name + " " + token))
                .hoverEvent(HoverEvent.showText(Component.text(
                        "Löscht ALLE Daten von " + name + " unwiderruflich (Statistiken, virtuelle Inventare, TP/EP/Level). Läuft in " + ttlSeconds + " Sekunden ab.",
                        NamedTextColor.RED)));
        return Component.text("⚠ Achtung: ", NamedTextColor.RED, TextDecoration.BOLD)
                .append(Component.text("Das Entfernen von " + name + " löscht unwiderruflich alle zugehörigen Daten.", NamedTextColor.RED))
                .append(Component.newline())
                .append(button);
    }

    public static Component removeConfirmExpiredOrInvalid() {
        return Component.text("Keine ausstehende Bestätigung gefunden oder abgelaufen. Bitte den Befehl erneut ausführen.", NamedTextColor.RED);
    }

    public static Component removed(String name) {
        return Component.text(name + " wurde endgültig aus dem Belohnungssystem entfernt. Alle zugehörigen Daten wurden gelöscht.", NamedTextColor.GREEN);
    }

    /**
     * Confirms an admin TP/EP adjustment, e.g. {@code punkteUpdated("Treuepunkte", "Foo", 42)}.
     */
    public static Component punkteUpdated(String kontoLabel, String name, int newValue) {
        return Component.text(name + "s " + kontoLabel + " wurden angepasst. Neuer Stand: " + newValue, NamedTextColor.GREEN);
    }

    public static Component shopBlockProtected() {
        return Component.text("Dieses Objekt ist ein Shop und kann nicht zerstört werden.", NamedTextColor.RED);
    }

    // ---- Shop session (chat-driven /tes shop erstellen|bearbeiten flow, spec §3.1.1.1) ----

    private static Component shopHint(String text) {
        return Component.text(text, NamedTextColor.YELLOW)
                .append(Component.newline())
                .append(Component.text("(Jederzeit \"abbrechen\" eingeben, um den Vorgang zu beenden.)", NamedTextColor.GRAY));
    }

    public static Component shopSessionExpired() {
        return Component.text("Deine Shop-Sitzung ist abgelaufen. Bitte den Befehl erneut ausführen.", NamedTextColor.RED);
    }

    public static Component shopSessionCancelled() {
        return Component.text("Vorgang abgebrochen.", NamedTextColor.YELLOW);
    }

    public static Component shopSessionAlreadyActive() {
        return Component.text("Du hast bereits eine laufende Shop-Sitzung. Erst \"abbrechen\" eingeben oder abschließen.", NamedTextColor.RED);
    }

    public static Component shopCreateStart(String world) {
        return shopHint("Shop-Erstellung in Welt \"" + world + "\" gestartet. Wie soll die ID des Shops lauten? "
                + "(Buchstaben, Zahlen, \"_\"/\"-\", max. 32 Zeichen, muss eindeutig sein)");
    }

    public static Component shopEditStart(String world, String id, String currentName) {
        return shopHint("Bearbeitung von Shop \"" + id + "\" in Welt \"" + world + "\" gestartet. Neuer Name? (aktuell: \"" + currentName + "\")");
    }

    public static Component shopIdInvalid() {
        return Component.text("Ungültige ID. Erlaubt sind Buchstaben, Zahlen, \"_\" und \"-\" (max. 32 Zeichen).", NamedTextColor.RED);
    }

    public static Component shopIdTaken(String id) {
        return Component.text("Die ID \"" + id + "\" ist in dieser Welt bereits vergeben.", NamedTextColor.RED);
    }

    public static Component shopPromptName() {
        return shopHint("Wie soll der Shop heißen?");
    }

    public static Component shopNameInvalid() {
        return Component.text("Der Name darf nicht leer sein und maximal 48 Zeichen lang sein.", NamedTextColor.RED);
    }

    public static Component shopPromptOwners() {
        return shopHint("Weitere Besitzer (durch Leerzeichen getrennt) oder \"keine\" eingeben. Du bist bereits automatisch Besitzer.");
    }

    public static Component shopPromptPosition() {
        return shopHint("Rechtsklicke die Truhe/Doppeltruhe/Redstone-Truhe/das Fass/die Shulkerbox, die zum Shop werden soll.");
    }

    public static Component shopWrongWorldForPosition() {
        return Component.text("Dieser Block befindet sich nicht in der Welt der Shop-Sitzung.", NamedTextColor.RED);
    }

    public static Component shopInvalidContainer() {
        return Component.text("Dieser Blocktyp kann nicht zu einem Shop umgewandelt werden.", NamedTextColor.RED);
    }

    public static Component shopPositionAlreadyUsed() {
        return Component.text("An dieser Position existiert bereits ein Shop.", NamedTextColor.RED);
    }

    public static Component shopPromptItem() {
        return shopHint("Welches Item soll verkauft werden? Materialname eingeben (z. B. \"dirt\") oder \"hand\" für das Item in deiner Hand.");
    }

    public static Component shopItemInvalid(String text) {
        return Component.text("Unbekanntes Item: \"" + text + "\".", NamedTextColor.RED);
    }

    public static Component shopPromptPrice() {
        return shopHint("Preis pro Slot in Talern (ganze Zahl, größer als 0)?");
    }

    public static Component shopPriceInvalid() {
        return Component.text("Ungültiger Preis. Bitte eine ganze Zahl größer als 0 eingeben.", NamedTextColor.RED);
    }

    public static Component shopPromptTeleport() {
        return shopHint("Teleportpunkt festlegen? \"hier\" für deinen aktuellen Standort, sonst \"nein\".");
    }

    public static Component shopPromptConfirm(Component summary) {
        return summary.append(Component.newline())
                .append(shopHint("\"bestätigen\" eingeben, um zu speichern, oder \"abbrechen\" zum Verwerfen."));
    }

    public static Component shopConfirmInvalidInput() {
        return Component.text("Bitte \"bestätigen\" oder \"abbrechen\" eingeben.", NamedTextColor.RED);
    }

    public static Component shopCreated(String id, String name) {
        return Component.text("Shop \"" + name + "\" (ID: " + id + ") wurde erstellt.", NamedTextColor.GREEN);
    }

    public static Component shopUpdated(String id, String name) {
        return Component.text("Shop \"" + name + "\" (ID: " + id + ") wurde aktualisiert.", NamedTextColor.GREEN);
    }

    public static Component shopRestrictedWorld(String world) {
        return Component.text("In der Welt \"" + world + "\" können keine Shops erstellt werden.", NamedTextColor.RED);
    }

    public static Component shopUnknownWorld(String world) {
        return Component.text("Die Welt \"" + world + "\" existiert nicht.", NamedTextColor.RED);
    }

    public static Component shopNotFound(String id) {
        return Component.text("Kein Shop mit der ID \"" + id + "\" in dieser Welt gefunden.", NamedTextColor.RED);
    }

    public static Component shopNotOwner() {
        return Component.text("Du bist nicht Besitzer dieses Shops.", NamedTextColor.RED);
    }

    public static Component shopTooFarToClose() {
        return Component.text("Du musst dich in der Nähe des Shops befinden, um ihn zu schließen.", NamedTextColor.RED);
    }

    public static Component shopClosed(String id, String name) {
        return Component.text("Shop \"" + name + "\" (ID: " + id + ") wurde geschlossen.", NamedTextColor.GREEN);
    }

    public static Component shopListHeader(int page, int totalPages) {
        return Component.text("--- Deine Shops (Seite " + page + "/" + totalPages + ") ---", NamedTextColor.GOLD);
    }

    public static Component shopListEmpty() {
        return Component.text("Du besitzt noch keine Shops.", NamedTextColor.YELLOW);
    }

    public static Component shopListEntry(String id, String world, String name, String item, int price) {
        Component entry = Component.text(id + " — " + name + " (" + item + " | " + price + " Taler/Slot)", NamedTextColor.AQUA)
                .clickEvent(ClickEvent.runCommand("/tes shop tp " + world + " " + id))
                .hoverEvent(HoverEvent.showText(Component.text("Klicken zum Teleportieren (Welt: " + world + ")", NamedTextColor.GRAY)));
        return entry;
    }

    public static Component shopListNav(int page, int totalPages) {
        Component nav = Component.empty();
        if (page > 1) {
            nav = nav.append(Component.text("« Vorherige Seite ", NamedTextColor.GOLD)
                    .clickEvent(ClickEvent.runCommand("/tes shop liste " + (page - 1))));
        }
        if (page < totalPages) {
            nav = nav.append(Component.text("Nächste Seite »", NamedTextColor.GOLD)
                    .clickEvent(ClickEvent.runCommand("/tes shop liste " + (page + 1))));
        }
        return nav;
    }

    public static Component shopTeleporting(String name) {
        return Component.text("Du wurdest zu Shop \"" + name + "\" teleportiert.", NamedTextColor.GREEN);
    }

}
