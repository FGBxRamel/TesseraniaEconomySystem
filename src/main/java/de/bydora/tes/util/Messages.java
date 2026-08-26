package de.bydora.tes.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

import java.util.List;

/**
 * Centralized German, admin/player-facing chat messages for TES's commands.
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
     * The clickable re-confirmation prompt shown before {@code /spieler remove} takes effect.
     */
    public static Component removeConfirmPrompt(String name, String token, long ttlSeconds) {
        Component button = Component.text("»» BESTÄTIGEN ««", NamedTextColor.GOLD, TextDecoration.BOLD, TextDecoration.UNDERLINED)
                .clickEvent(ClickEvent.runCommand("/spieler remove " + name + " " + token))
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

    // ---- Shop session (chat-driven /shop erstellen|bearbeiten flow, spec §3.1.1.1) ----

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
        return shopHint("Shop-Erstellung in Welt \"" + world + "\" gestartet. Klicke unten ein Attribut an, um es festzulegen.");
    }

    public static Component shopEditStart(String world, String id, String currentName) {
        return shopHint("Bearbeitung von Shop \"" + id + "\" (\"" + currentName + "\") in Welt \"" + world + "\" gestartet. "
                + "Klicke unten ein Attribut an, um es zu ändern.");
    }

    public static Component shopPromptId() {
        return shopHint("Wie soll die ID des Shops lauten? (Buchstaben, Zahlen, \"_\"/\"-\", max. 32 Zeichen, muss eindeutig sein)");
    }

    public static Component shopIdInvalid() {
        return Component.text("Ungültige ID. Erlaubt sind Buchstaben, Zahlen, \"_\" und \"-\" (max. 32 Zeichen).", NamedTextColor.RED);
    }

    public static Component shopIdTaken(String id) {
        return Component.text("Die ID \"" + id + "\" ist bereits vergeben.", NamedTextColor.RED);
    }

    public static Component shopNoFieldArmed() {
        return Component.text("Bitte klicke zuerst ein Attribut im Menü an.", NamedTextColor.YELLOW);
    }

    public static Component shopConfirmMissingAttributes(List<String> missingLabels) {
        return Component.text("Folgende Pflichtattribute fehlen noch: " + String.join(", ", missingLabels) + ".", NamedTextColor.RED);
    }

    /**
     * One line of the {@code /shop erstellen|bearbeiten} attribute menu (spec §3.1.1.1, UX
     * modeled on the BlueMap-Marker plugin): green once {@code set}, otherwise red if
     * {@code mandatory} or gray if optional.
     */
    public record ShopMenuLine(String key, String label, String valueDisplay, boolean mandatory, boolean set, String hoverText) {
    }

    public static Component shopMenu(List<ShopMenuLine> lines) {
        Component menu = Component.text("--- Shop konfigurieren ---", NamedTextColor.GOLD);
        for (ShopMenuLine line : lines) {
            NamedTextColor color = line.set() ? NamedTextColor.GREEN : line.mandatory() ? NamedTextColor.RED : NamedTextColor.GRAY;
            String value = line.valueDisplay() != null ? line.valueDisplay() : "Nicht gesetzt" + (line.mandatory() ? "" : " (optional)");
            Component entry = Component.text(line.label() + ": " + value, color)
                    .clickEvent(ClickEvent.runCommand("/shop feld " + line.key()))
                    .hoverEvent(HoverEvent.showText(Component.text(line.hoverText(), NamedTextColor.GRAY)));
            menu = menu.append(Component.newline()).append(entry);
        }
        Component confirm = Component.text("»» BESTÄTIGEN ««", NamedTextColor.GREEN, TextDecoration.BOLD)
                .clickEvent(ClickEvent.runCommand("/shop bestaetigen"))
                .hoverEvent(HoverEvent.showText(Component.text("Shop speichern.", NamedTextColor.GRAY)));
        Component cancel = Component.text("»» ABBRECHEN ««", NamedTextColor.RED, TextDecoration.BOLD)
                .clickEvent(ClickEvent.runCommand("/shop abbrechen"))
                .hoverEvent(HoverEvent.showText(Component.text("Vorgang verwerfen.", NamedTextColor.GRAY)));
        return menu.append(Component.newline()).append(confirm).append(Component.text("   ")).append(cancel)
                .append(Component.newline())
                .append(Component.text("(Jederzeit \"abbrechen\" eingeben, um den Vorgang zu beenden.)", NamedTextColor.GRAY));
    }

    public static Component shopPromptName() {
        return shopHint("Wie soll der Shop heißen?");
    }

    public static Component shopNameInvalid() {
        return Component.text("Der Name darf nicht leer sein und maximal 48 Zeichen lang sein.", NamedTextColor.RED);
    }

    public static Component shopPromptOwners() {
        return shopHint("Weitere Besitzer (durch Leerzeichen getrennt) oder \"keine\" eingeben. Du bist bereits automatisch Besitzer. "
                + "Einen bereits eingetragenen Namen erneut eingeben entfernt ihn wieder als Besitzer (mindestens ein Besitzer muss bestehen bleiben).");
    }

    public static Component shopOwnerCannotRemoveLast(String name) {
        return Component.text(name + " kann nicht entfernt werden: Ein Shop muss mindestens einen Besitzer haben.", NamedTextColor.RED);
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
        return shopHint("Welches Item soll verkauft werden? Materialname eingeben (z. B. \"dirt\"), \"hand\" für das "
                + "Item in deiner Hand, oder \"alle\" für einen Shop, der jedes Item außer Diamanten verkauft.");
    }

    public static Component shopItemInvalid(String text) {
        return Component.text("Unbekanntes Item: \"" + text + "\".", NamedTextColor.RED);
    }

    public static Component shopItemIsCurrency() {
        return Component.text("Diamanten sind die Währung (Taler) und können nicht verkauft werden.", NamedTextColor.RED);
    }

    public static Component shopPromptPrice() {
        return shopHint("Preis pro Slot in Talern (ganze Zahl, 1 bis 64)?");
    }

    public static Component shopPriceInvalid() {
        return Component.text("Ungültiger Preis. Bitte eine ganze Zahl zwischen 1 und 64 eingeben.", NamedTextColor.RED);
    }

    public static Component shopPromptTeleport() {
        return shopHint("Teleportpunkt festlegen? \"hier\" für deinen aktuellen Standort, sonst \"nein\".");
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
        return Component.text("Kein Shop mit der ID \"" + id + "\" gefunden.", NamedTextColor.RED);
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
                .clickEvent(ClickEvent.runCommand("/shop tp " + id))
                .hoverEvent(HoverEvent.showText(Component.text("Klicken zum Teleportieren (Welt: " + world + ")", NamedTextColor.GRAY)));
        return entry;
    }

    public static Component shopListNav(int page, int totalPages) {
        Component nav = Component.empty();
        if (page > 1) {
            nav = nav.append(Component.text("« Vorherige Seite ", NamedTextColor.GOLD)
                    .clickEvent(ClickEvent.runCommand("/shop liste " + (page - 1))));
        }
        if (page < totalPages) {
            nav = nav.append(Component.text("Nächste Seite »", NamedTextColor.GOLD)
                    .clickEvent(ClickEvent.runCommand("/shop liste " + (page + 1))));
        }
        return nav;
    }

    public static Component shopTeleporting(String name) {
        return Component.text("Du wurdest zu Shop \"" + name + "\" teleportiert.", NamedTextColor.GREEN);
    }

    public static Component notEnoughTaler() {
        return Component.text("Nicht genügend Taler.", NamedTextColor.RED);
    }

    public static Component shopWithdrawCooldownActive() {
        return Component.text("Diese Diamanten sind noch innerhalb der Stornierungsfrist des Käufers und können nicht entnommen werden.", NamedTextColor.RED);
    }

    public static Component shopWrongItemForShop(String item) {
        return Component.text("Dieser Shop verkauft nur " + item + ".", NamedTextColor.RED);
    }

    public static Component shopDiamondsNotStockable() {
        return Component.text("Diamanten können nicht als Shop-Bestand eingelagert werden.", NamedTextColor.RED);
    }

    public static Component shopRefundItemMissing() {
        return Component.text("Stornierung nicht möglich: Du besitzt die gekauften Items nicht mehr vollständig.", NamedTextColor.RED);
    }

    public static Component shopOutOfOrder() {
        return Component.text("Dieser Shop ist derzeit außer Betrieb, da alle Inhaber pausiert sind.", NamedTextColor.RED);
    }

    /**
     * UC5: the shop's block no longer exists or was tampered with outside the plugin. Returned
     * as plain text since it's also what gets persisted for offline owners via
     * {@link de.bydora.tes.shop.PendingNotificationRepository} and re-rendered on next login.
     */
    public static String shopOrphanedText(String name, String id) {
        return "Dein Shop \"" + name + "\" (ID: " + id + ") wurde entfernt, da der zugehörige Block nicht mehr existiert.";
    }

    public static Component shopOrphaned(String name, String id) {
        return Component.text(shopOrphanedText(name, id), NamedTextColor.RED);
    }

    public static Component pendingNotification(String text) {
        return Component.text(text, NamedTextColor.YELLOW);
    }

    // ---- Belohnungsinventar (/belohnung, spec §1.3/§3.3.1.4) ----

    public static Component rewardInventoryNotEligible() {
        return Component.text("Du musst im Belohnungssystem registriert und nicht pausiert sein, um dein "
                + "Belohnungsinventar zu nutzen.", NamedTextColor.RED);
    }

    public static Component rewardInventoryTaken(String itemName) {
        return Component.text(itemName + " wurde in dein Inventar übernommen.", NamedTextColor.GREEN);
    }

    public static Component rewardInventoryFull() {
        return Component.text("Dein Inventar ist voll. Das Item bleibt im Belohnungsinventar.", NamedTextColor.RED);
    }

    // ---- Rechnungen / Dienstleistungen / Trödelmarkt (/rechnung, spec §3.1.1.3) ----

    public static Component senderPaused() {
        return Component.text("Du bist im Belohnungssystem pausiert und kannst diese Aktion nicht ausführen.", NamedTextColor.RED);
    }

    public static Component rechnungGrundInvalid(int maxLength) {
        return Component.text("Der Grund darf nicht leer sein und maximal " + maxLength + " Zeichen lang sein.", NamedTextColor.RED);
    }

    public static Component rechnungPreisZuHoch(int maxPreis) {
        return Component.text("Der Preis darf maximal " + maxPreis + " Taler betragen.", NamedTextColor.RED);
    }

    public static Component invoiceSelfTarget() {
        return Component.text("Du kannst dir selbst keine Rechnung stellen.", NamedTextColor.RED);
    }

    public static Component invoiceCreated(String targetName, int price, String reason) {
        return Component.text("Rechnung über " + price + " Taler an " + targetName + " (\"" + reason + "\") wurde erstellt.", NamedTextColor.GREEN);
    }

    public static Component invoiceCreatedNotification(String creatorName, int price, String reason) {
        return Component.text(invoiceCreatedText(creatorName, price, reason), NamedTextColor.YELLOW)
                .clickEvent(ClickEvent.runCommand("/rechnung anzeigen"))
                .hoverEvent(HoverEvent.showText(Component.text("Klicken zum Öffnen deiner offenen Rechnungen.", NamedTextColor.GRAY)));
    }

    /**
     * Plain-text variant, used both as the base for {@link #invoiceCreatedNotification} and for
     * offline delivery via {@link de.bydora.tes.shop.PendingNotificationRepository}.
     */
    public static String invoiceCreatedText(String creatorName, int price, String reason) {
        return creatorName + " hat dir eine Rechnung über " + price + " Taler gestellt (\"" + reason + "\"). "
                + "Mit /rechnung anzeigen einsehen und bezahlen.";
    }

    public static Component invoiceSettled(String creatorName, int price) {
        return Component.text("Rechnung über " + price + " Taler an " + creatorName + " wurde bezahlt.", NamedTextColor.GREEN);
    }

    public static Component invoiceAlreadySettled() {
        return Component.text("Diese Rechnung wurde bereits bezahlt.", NamedTextColor.RED);
    }

    /**
     * Plain-text variant, used both as the base for {@link #invoiceSettledForCreator} and for
     * offline delivery via {@link de.bydora.tes.shop.PendingNotificationRepository}.
     */
    public static String invoiceSettledForCreatorText(String payerName, int price) {
        return payerName + " hat deine Rechnung über " + price + " Taler bezahlt.";
    }

    public static Component invoiceSettledForCreator(String payerName, int price) {
        return Component.text(invoiceSettledForCreatorText(payerName, price), NamedTextColor.GREEN);
    }

    public static Component invoiceRetractAlreadyResolved() {
        return Component.text("Diese Rechnung ist nicht mehr offen.", NamedTextColor.RED);
    }

    public static Component invoiceRetracted(String targetName, int price) {
        return Component.text("Rechnung über " + price + " Taler an " + targetName + " wurde zurückgezogen.", NamedTextColor.GREEN);
    }

    /**
     * Plain-text variant, used both as the base for {@link #invoiceRetractedForTarget} and for
     * offline delivery via {@link de.bydora.tes.shop.PendingNotificationRepository}.
     */
    public static String invoiceRetractedForTargetText(String creatorName, int price) {
        return creatorName + " hat die Rechnung über " + price + " Taler gegen dich zurückgezogen.";
    }

    public static Component invoiceRetractedForTarget(String creatorName, int price) {
        return Component.text(invoiceRetractedForTargetText(creatorName, price), NamedTextColor.YELLOW);
    }

    public static Component invoiceCashedOut() {
        return Component.text("Dein Kontostand wurde als Diamanten in dein Belohnungsinventar ausgezahlt.", NamedTextColor.GREEN);
    }

    public static Component invoiceNothingToCashOut() {
        return Component.text("Du hast keinen Kontostand zum Auszahlen.", NamedTextColor.RED);
    }

    // ---- Debug (/debug dump, siehe docs/gui-reference-capture.md) ----

    public static Component debugDumpUnsupported() {
        return Component.text("An dieser Position befindet sich kein unterstützter Container oder Schild.", NamedTextColor.RED);
    }

    public static Component debugDumpReady(String summary, String plainDump) {
        return Component.text("Dump bereit (" + summary + "). ", NamedTextColor.GREEN)
                .append(Component.text("» Klicke hier, um ihn zu kopieren «", NamedTextColor.AQUA, TextDecoration.UNDERLINED)
                        .clickEvent(ClickEvent.copyToClipboard(plainDump))
                        .hoverEvent(HoverEvent.showText(Component.text("Kopiert den vollständigen Dump in deine Zwischenablage.", NamedTextColor.GRAY))));
    }

}
