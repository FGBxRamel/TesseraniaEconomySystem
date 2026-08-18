# TES – Befehlsübersicht

Diese Datei dokumentiert alle im Spiel verfügbaren `/tes`-Befehle für Admins und Spieler.
Sie wird mit jeder Stage um die neu hinzugekommenen Befehle ergänzt.

## `/tes spieler` (Admin)

Verwaltet die Registrierung eines Spielers im Belohnungssystem (Treuepunkte- und Levelsystem).

### Voraussetzung: Registrierung

Bevor ein Spieler vom Belohnungssystem berücksichtigt wird, muss er zunächst von einem Admin
registriert werden. Bis dahin kann der Spieler zwar bereits in Shops einkaufen und Rechnungen
bezahlen, erhält dafür aber **keine** Treuepunkte oder Erfahrungspunkte – die Belohnungsmechanik
ist für ihn schlicht inaktiv.

### Befehle

| Befehl | Berechtigung | Wirkung |
|---|---|---|
| `/tes spieler add <Name>` | `tes.admin.spieler.add` | Registriert den Spieler neu im Belohnungssystem (TP/EP/Level starten bei 0). |
| `/tes spieler pause <Name>` | `tes.admin.spieler.pause` | Pausiert einen registrierten Spieler: Er sammelt vorübergehend keine TP/EP mehr. |
| `/tes spieler unpause <Name>` | `tes.admin.spieler.unpause` | Hebt die Pausierung wieder auf. |
| `/tes spieler remove <Name>` | `tes.admin.spieler.remove` | Entfernt den Spieler **endgültig** aus dem System (siehe unten). |

### `/tes spieler remove` – Bestätigungsablauf

Da das Entfernen eines Spielers **unwiderruflich alle** mit ihm verbundenen Daten löscht
(Treuepunkte, Erfahrungspunkte, Level, virtuelle Inventare, Statistiken), muss der ausführende
Admin die Aktion erneut bestätigen:

1. `/tes spieler remove <Name>` ausführen.
2. Im Chat erscheint eine Warnung mit einem anklickbaren `»» BESTÄTIGEN ««`-Button.
3. Innerhalb von **30 Sekunden** den Button anklicken (oder den vorgeschlagenen Befehl inkl.
   Bestätigungscode erneut eingeben). Danach verfällt die Bestätigung und der Befehl muss neu
   gestartet werden.

**Achtung:** Nach dem Bestätigen sind alle Daten des Spielers unwiderruflich gelöscht. Es gibt
keine Möglichkeit, den Vorgang rückgängig zu machen.

> Hinweis: Die Spezifikation (§1.4) beschreibt den Bestätigungsschritt wörtlich als erneute
> Bestätigung durch "den Spieler" – ausgeführt wird der Befehl aber ausschließlich von Admins,
> daher bestätigt hier der ausführende Admin selbst. Sollte eine Bestätigung durch den
> betroffenen Spieler gemeint gewesen sein, bitte Rücksprache halten.

## `/tes treuepunkte` und `/tes erfahrungspunkte` (Admin)

Passt die Treuepunkte (TP) bzw. Erfahrungspunkte (EP) eines registrierten Spielers direkt an,
analog zu den Vanilla-`/xp`-Befehlen. Der Zielspieler muss bereits im Belohnungssystem
registriert sein (siehe `/tes spieler add`).

| Befehl | Berechtigung | Wirkung |
|---|---|---|
| `/tes treuepunkte add <Name> <Anzahl>` | `tes.admin.treuepunkte.add` | Erhöht die TP des Spielers um `<Anzahl>`. |
| `/tes treuepunkte remove <Name> <Anzahl>` | `tes.admin.treuepunkte.remove` | Verringert die TP des Spielers um `<Anzahl>` (nicht unter 0). |
| `/tes treuepunkte set <Name> <Anzahl>` | `tes.admin.treuepunkte.set` | Setzt die TP des Spielers exakt auf `<Anzahl>`. |
| `/tes erfahrungspunkte add <Name> <Anzahl>` | `tes.admin.erfahrungspunkte.add` | Erhöht die EP des Spielers um `<Anzahl>`. |
| `/tes erfahrungspunkte remove <Name> <Anzahl>` | `tes.admin.erfahrungspunkte.remove` | Verringert die EP des Spielers um `<Anzahl>` (nicht unter 0). |
| `/tes erfahrungspunkte set <Name> <Anzahl>` | `tes.admin.erfahrungspunkte.set` | Setzt die EP des Spielers exakt auf `<Anzahl>`. |

> Hinweis: Die Spezifikation (§1.4) formuliert diese Befehle wörtlich ohne Namensargument
> (`/tes treuepunkte add/remove/set <Anzahl>`). Da das nur für den ausführenden Admin selbst
> Sinn ergäbe, wurde – analog zu `/tes spieler` und Vanilla `/xp` – ein Namensargument ergänzt.

Im Stage-1-Umfang gibt es noch keine Möglichkeit, Treuepunkte auszugeben (Treuepunkteshop folgt
in Stage 3) – Spieler sammeln TP/EP beim Bezahlen in Item-Shops bereits im Hintergrund.

## `/tes shop` (Spieler)

Verwaltet eigene Item-Shops: umgewandelte Truhen, Doppeltruhen, Redstone-Truhen, Fässer und
Shulkerboxen (alle Farben), die andere Spieler direkt aus dem Container heraus bedienen können.
Jeder Spieler darf eigene Shops verwalten – dafür ist keine Admin-Berechtigung nötig.

### Shop erstellen – `/tes shop erstellen <Welt>`

Startet eine Chat-geführte Einrichtung (angelehnt an das BlueMap-Marker-Plugin): der Server
fragt nacheinander nach ID, Name, weiteren Besitzern, der Position (per Rechtsklick auf den
Container), dem verkauften Item, dem Preis pro Slot und optional einem Teleportpunkt. Am Ende
wird eine Zusammenfassung angezeigt; `bestätigen` speichert den Shop, `abbrechen` verwirft ihn.
`abbrechen` funktioniert jederzeit während der Einrichtung.

Shops können nicht in der Kreativwelt oder (sobald verfügbar) in Farmwelten erstellt werden.

Erlaubte Container: Truhe, Redstone-Truhe (Trapped Chest), Fass, alle Shulkerbox-Farben.

### Shop bearbeiten – `/tes shop bearbeiten <Welt> <ID>`

Nur für Besitzer. Startet dieselbe Chat-Einrichtung, aber nur für die veränderbaren Attribute
(Name, Besitzer, Item, Preis, Teleportpunkt) – ID und Position stehen fest.

### Shop schließen – `/tes shop schließen <Welt> <ID>`

Nur für Besitzer, die sich in der Nähe des Shops befinden. Wandelt den Container zurück in einen
normalen Block um. Noch offene (innerhalb der 60-Sekunden-Frist stornierbare) Käufe werden dabei
automatisch rückabgewickelt.

### Kaufen im Shop

Käufer klicken einfach mit Linksklick auf den gewünschten Slot im Container. Bei ausreichend
Talern (Diamanten) wird das Item gegen Diamanten getauscht. Der Kauf ist **60 Sekunden lang**
stornierbar (an der Diamanten-Schwingungsanimation im Slot erkennbar, wie beim
Enderperlen-Cooldown) – ein erneuter Klick auf denselben Slot innerhalb dieser Frist macht den
Kauf rückgängig. Erst nach Ablauf der Frist kann der Shopbesitzer die Diamanten entnehmen; TP/EP
werden dem Käufer zu diesem Zeitpunkt automatisch gutgeschrieben.
