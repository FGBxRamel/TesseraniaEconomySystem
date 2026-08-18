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
