# TES – Befehlsübersicht

Diese Datei dokumentiert alle im Spiel verfügbaren Befehle des Tesserania Economy Systems für
Admins und Spieler. Jedes Subsystem ist ein eigener Top-Level-Befehl (`/shop`, `/rechnung`,
`/spieler`, ...) statt unter einem gemeinsamen `/tes`-Präfix zusammengefasst zu sein.
Sie wird mit jeder Stage um die neu hinzugekommenen Befehle ergänzt.

## `/spieler` (Admin)

Verwaltet die Registrierung eines Spielers im Belohnungssystem (Treuepunkte- und Levelsystem).

### Voraussetzung: Registrierung

Bevor ein Spieler vom Belohnungssystem berücksichtigt wird, muss er zunächst von einem Admin
registriert werden. Bis dahin kann der Spieler zwar bereits in Shops einkaufen und Rechnungen
bezahlen, erhält dafür aber **keine** Treuepunkte oder Erfahrungspunkte – die Belohnungsmechanik
ist für ihn schlicht inaktiv.

### Befehle

| Befehl | Berechtigung | Wirkung |
|---|---|---|
| `/spieler add <Name>` | `tes.admin.spieler.add` | Registriert den Spieler neu im Belohnungssystem (TP/EP/Level starten bei 0). |
| `/spieler pause <Name>` | `tes.admin.spieler.pause` | Pausiert einen registrierten Spieler: Er sammelt vorübergehend keine TP/EP mehr. |
| `/spieler unpause <Name>` | `tes.admin.spieler.unpause` | Hebt die Pausierung wieder auf. |
| `/spieler remove <Name>` | `tes.admin.spieler.remove` | Entfernt den Spieler **endgültig** aus dem System (siehe unten). |

### `/spieler remove` – Bestätigungsablauf

Da das Entfernen eines Spielers **unwiderruflich alle** mit ihm verbundenen Daten löscht
(Treuepunkte, Erfahrungspunkte, Level, virtuelle Inventare, Statistiken), muss der ausführende
Admin die Aktion erneut bestätigen:

1. `/spieler remove <Name>` ausführen.
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

## `/treuepunkte` und `/erfahrungspunkte` (Admin)

Passt die Treuepunkte (TP) bzw. Erfahrungspunkte (EP) eines registrierten Spielers direkt an,
analog zu den Vanilla-`/xp`-Befehlen. Der Zielspieler muss bereits im Belohnungssystem
registriert sein (siehe `/spieler add`).

| Befehl | Berechtigung | Wirkung |
|---|---|---|
| `/treuepunkte add <Name> <Anzahl>` | `tes.admin.treuepunkte.add` | Erhöht die TP des Spielers um `<Anzahl>`. |
| `/treuepunkte remove <Name> <Anzahl>` | `tes.admin.treuepunkte.remove` | Verringert die TP des Spielers um `<Anzahl>` (nicht unter 0). |
| `/treuepunkte set <Name> <Anzahl>` | `tes.admin.treuepunkte.set` | Setzt die TP des Spielers exakt auf `<Anzahl>`. |
| `/erfahrungspunkte add <Name> <Anzahl>` | `tes.admin.erfahrungspunkte.add` | Erhöht die EP des Spielers um `<Anzahl>`. |
| `/erfahrungspunkte remove <Name> <Anzahl>` | `tes.admin.erfahrungspunkte.remove` | Verringert die EP des Spielers um `<Anzahl>` (nicht unter 0). |
| `/erfahrungspunkte set <Name> <Anzahl>` | `tes.admin.erfahrungspunkte.set` | Setzt die EP des Spielers exakt auf `<Anzahl>`. |

> Hinweis: Die Spezifikation (§1.4) formuliert diese Befehle wörtlich ohne Namensargument
> (`/treuepunkte add/remove/set <Anzahl>`). Da das nur für den ausführenden Admin selbst
> Sinn ergäbe, wurde – analog zu `/spieler` und Vanilla `/xp` – ein Namensargument ergänzt.

## `/punkte` bzw. `/treuepunkte` (Spieler)

Ohne weitere Argumente öffnet `/punkte` (oder gleichbedeutend `/treuepunkte`) den
**Treuepunkteshop**: die Sonnenblume oben links zeigt deinen aktuellen Treuepunktestand, die
übrigen Felder sind die einzelnen Belohnungen mit ihren TP-Kosten und Beschreibungen.

| Berechtigung | Voraussetzung |
|---|---|
| `tes.punkte` | Du musst im Belohnungssystem registriert (`/spieler add`) und nicht pausiert sein. |

Direkt im Hauptinterface einlösbar sind:

- **Segen der Zwerge** (Eile II für 30 Minuten)
- **Kraftelixier** (Regeneration II, Resistenz II, Stärke und Held des Dorfes für 30 Minuten)
- **Spawner** (ein normaler Monsterspawner, den du mit Spawneiern bestücken kannst)
- **Erntewelt** / **Glutzone** (schaltet die jeweilige Farmwelt frei – landet als Chorusfrucht in
  deinem Belohnungsinventar; das Essen der Frucht funktioniert erst, sobald die Farmwelten selbst
  fertig sind)
- **Prozessverstärker** (ein Glowstone-Staub, der in dein Belohnungsinventar wandert – siehe unten)

Ein Linksklick zieht die angezeigten Treuepunkte ab und wendet den Effekt sofort an bzw. legt das
Item in dein Belohnungsinventar (`/belohnung`).

Vier weitere Felder öffnen ein Untermenü statt direkt etwas zu verkaufen:

- **XP-Terminal** – vier XP-Boosts (6.000/12.500/30.000/50.000 Erfahrungspunkte für 50/75/100/125
  TP), die dir sofort echte Vanilla-Minecraft-XP gutschreiben.
- **Freundliche Mobs I/II** und **Feindliche Mobs I/II** – je ein Sammelkauf, der dir auf einmal
  alle Spawneier dieser Stufe ins Belohnungsinventar legt (z. B. Freundliche Mobs I: je 1x
  Hilfsgeist/Gürteltier/Ozelot/Panda/Eisbär/Katze/Wolf/Fuchs/Dromedar/Biene plus je 2x
  Kuh/Esel/Huhn/Schwein/Pferd/Kaninchen).

Ein Pfeil ("⮜ Zurück") führt aus jedem Untermenü zurück zum Hauptinterface. Nur
**Handelsbonus** ist noch nicht einlösbar; das folgt in einer weiteren Ausbaustufe von Stage 3.

### Prozessverstärker benutzen

Der Prozessverstärker landet als Item in deinem Belohnungsinventar (`/belohnung`). Rechtsklicke
damit auf einen **Ofen** (Ofen, Hochofen oder Räucherofen) oder einen **Bienenstock**
(Bienenstock oder Bienennest), um ihn für 15 Minuten zu boosten – der Ofen brennt danach doppelt
so schnell, der Bienenstock produziert doppelt so viel Honig. Das Item wird dabei verbraucht.
Boostest du denselben Block erneut, während der Boost noch läuft, addiert sich die Dauer auf die
verbleibende Zeit.

### Treuepunkte übertragen – `/treuepunkte übertragen <Spieler> <Anzahl>`

| Berechtigung | Voraussetzung |
|---|---|
| `tes.treuepunkte.uebertragen` | Du musst im Belohnungssystem registriert und nicht pausiert sein; das Ziel muss ebenfalls registriert sein. |

Überträgt `<Anzahl>` deiner eigenen Treuepunkte an `<Spieler>` – du kannst nicht mehr übertragen,
als du besitzt, und nicht an dich selbst. Es fällt keine Gebühr an und es gibt keine Wartezeit.
Ist das Ziel online, erhält es sofort eine Chat-Benachrichtigung.

## `/shop` (Spieler)

Verwaltet eigene Item-Shops: umgewandelte Truhen, Doppeltruhen, Redstone-Truhen, Fässer und
Shulkerboxen (alle Farben), die andere Spieler direkt aus dem Container heraus bedienen können.
Jeder Spieler darf eigene Shops verwalten – dafür ist keine Admin-Berechtigung nötig.

### Shop erstellen – `/shop erstellen`

Der Shop wird in der Welt erstellt, in der der Spieler gerade steht. Startet eine Chat-geführte
Einrichtung, angelehnt an das BlueMap-Marker-Plugin: der Server zeigt
ein Menü mit allen Attributen (ID, Name, Besitzer, Position, Item, Preis, optional
Teleportpunkt) – jede Zeile ist anklickbar und kann in **beliebiger Reihenfolge** ausgefüllt
werden. Rote Zeilen sind noch offene Pflichtattribute, grüne Zeilen sind bereits gesetzt, graue
Zeilen sind optional. Ein Klick auf eine Zeile fragt nach dem jeweiligen Wert (die Position wird
per Rechtsklick auf den gewünschten Container festgelegt, nicht per Chat-Eingabe); danach
erscheint das Menü mit dem aktualisierten Stand erneut. Bereits gesetzte Attribute können jederzeit
erneut angeklickt werden, um sie zu ändern.

Beim Item-Attribut kann statt eines Materialnamens auch `alle` eingegeben werden: Der Shop
verkauft dann jedes Item außer Diamanten (Diamanten bleiben die Währung) zum festgelegten
Preis pro Slot – nützlich für Sammel-/Ankaufsshops, die nicht auf ein einzelnes Item beschränkt
sein sollen.

Der Preis pro Slot ist eine ganze Zahl zwischen **1 und 64** Talern (ein Slot fasst maximal
einen vollen Stack, daher die Obergrenze).

Am Ende des Menüs stehen zwei Schaltflächen: `»» BESTÄTIGEN ««` speichert den Shop (nur möglich,
wenn alle Pflichtattribute gesetzt sind – fehlt noch etwas, wird das gemeldet und das Menü bleibt
offen), `»» ABBRECHEN ««` verwirft die Einrichtung. Beide funktionieren auch als Chat-Eingabe
(`bestätigen`/`abbrechen`), jederzeit während der Einrichtung.

Shops können nicht in der Kreativwelt oder (sobald verfügbar) in Farmwelten erstellt werden.

Erlaubte Container: Truhe, Redstone-Truhe (Trapped Chest), Fass, alle Shulkerbox-Farben.

### Shop bearbeiten – `/shop bearbeiten <ID>`

Nur für Besitzer. Startet dasselbe Menü, aber nur für die veränderbaren Attribute (Name,
Besitzer, Item, Preis, Teleportpunkt) – ID und Position werden gar nicht erst angezeigt, da sie
nach der Erstellung feststehen. Shop-IDs sind weltweit eindeutig, daher genügt die ID allein zur
Identifikation. Bist du im Belohnungssystem pausiert, kannst du deine Shops nicht bearbeiten.

### Shop schließen – `/shop schließen <ID>`

Nur für Besitzer, die sich in der Nähe des Shops befinden. Wandelt den Container zurück in einen
normalen Block um. Noch offene (innerhalb der 60-Sekunden-Frist stornierbare) Käufe werden dabei
automatisch rückabgewickelt.

### Shops auflisten – `/shop liste [Seite]`

Listet alle eigenen Shops (über alle Welten hinweg), 10 pro Seite. Jeder Eintrag ist anklickbar
und teleportiert zum jeweiligen Shop (zum gesetzten Teleportpunkt, sonst auf den Container).
Bei mehr als einer Seite erscheinen anklickbare Navigationsbuttons; eine Seite kann auch direkt
per `/shop liste <Seite>` angesprungen werden.

### Zu einem Shop teleportieren – `/shop tp <ID>`

Nur für Besitzer. Teleportiert direkt zum angegebenen Shop (zum gesetzten Teleportpunkt, sonst
sicher oberhalb des Containers, um ein Ersticken im Block zu vermeiden) – derselbe Sprung, den
auch ein Klick auf einen Eintrag in `/shop liste` auslöst.

### Kaufen im Shop

Käufer klicken einfach mit Linksklick auf den gewünschten Slot im Container. Bei ausreichend
Talern (Diamanten) wird das Item gegen Diamanten getauscht. Der Kauf ist **60 Sekunden lang**
stornierbar (an der Diamanten-Schwingungsanimation im Slot erkennbar, wie beim
Enderperlen-Cooldown) – ein erneuter Klick auf denselben Slot innerhalb dieser Frist macht den
Kauf rückgängig. Erst nach Ablauf der Frist kann der Shopbesitzer die Diamanten entnehmen; TP/EP
werden dem Käufer zu diesem Zeitpunkt automatisch gutgeschrieben. Ist ein Besitzer im
Belohnungssystem pausiert, kann er seine entnahmebereiten Diamanten nicht abheben, bis die Pause
aufgehoben wird – bei einem Shop mit mehreren Besitzern gilt das nur für den pausierten Besitzer
selbst, nicht für die anderen.

Shopbesitzer füllen einen Shop nach, indem sie das konfigurierte Item einfach in einen freien
Slot legen – andere Items werden abgelehnt (bei einem "alle"-Shop ist jedes Item außer Diamanten
erlaubt). Diamanten selbst können nicht als Shop-Item verkauft werden, da sie die Währung (Taler)
sind. Shift-Klicks und Drag-Aktionen sind im Shop-Inventar für
Käufer sowie für Nicht-Diamanten-Slots deaktiviert, um beim Kauf/Nachfüllen ausschließlich beim
beschriebenen Einzelklick zu bleiben – Besitzer dürfen entnahmebereite (nicht mehr stornierbare)
Diamanten aber auch per Shift-Klick entnehmen.

## `/belohnung` (Spieler)

Öffnet dein **Belohnungsinventar**: Immer wenn du Items über das Treuepunkte- oder Levelsystem
erhältst (und, sobald verfügbar, beim Auszahlen einer Rechnung über `/rechnung anzeigen`),
landen diese hier – niemals direkt in deinem Inventar oder in einem Shop. Käufe in Shops landen
**nicht** im Belohnungsinventar.

| Berechtigung | Voraussetzung |
|---|---|
| `tes.belohnung` | Du musst im Belohnungssystem registriert (`/spieler add`) und nicht pausiert sein. |

Ein Linksklick auf ein Item übernimmt genau diesen einen Stapel in dein reales Inventar. Ist dein
Inventar voll, bleibt das Item im Belohnungsinventar liegen – es geht nichts verloren. Der weiße
Pfeil ("⮜ Levelinterface") führt später zum Levelinterface (folgt in einer späteren Stage); der
Kopf ("➤ Weiter") blättert zur nächsten Seite, sofern mehr Items vorhanden sind, als auf eine Seite
passen (14 pro Seite). Der Barriere-Block schließt das Interface – ab Seite 2 wird er stattdessen
zum "⮜ Zurück"-Kopf, der eine Seite zurückblättert.

## `/rechnung` (Spieler)

Bildet Transaktionen aus Dienstleistungen und dem Trödelmarkt ab – Situationen, die (anders als
Shops) nicht automatisch erfasst werden können, weil kein Container beteiligt ist.

### Rechnung erstellen – `/rechnung erstellen <Ziel> <Preis> <Grund>`

| Berechtigung | Voraussetzung |
|---|---|
| `tes.rechnung.erstellen` | Du musst im Belohnungssystem registriert und nicht pausiert sein. |

`<Ziel>` ist der Name des Spielers, der dir den Betrag schuldet – dieser muss dem Server bekannt
sein, aber **nicht** im Belohnungssystem registriert sein (auch unregistrierte Spieler können
Rechnungen bezahlen). `<Preis>` ist der Betrag in Talern (Diamanten), maximal **2304**. `<Grund>`
ist ein freier Text mit maximal **50 Zeichen** und darf mehrere Wörter enthalten (z. B.
`/rechnung erstellen Beispielspieler 10 Reparatur der Brücke`).

Der Zielspieler erhält sofort eine Nachricht, wenn er online ist, sonst beim nächsten Login. Eine
Rechnung hat keine Stornofrist wie ein Shop-Kauf, kann aber vom Steller jederzeit über
"Versendete Rechnungen" (siehe unten) zurückgezogen werden.

### Rechnungen ansehen und bezahlen – `/rechnung anzeigen`

| Berechtigung | Voraussetzung |
|---|---|
| `tes.rechnung.anzeigen` | Keine – auch unregistrierte oder pausierte Spieler können offene Rechnungen gegen sich einsehen und bezahlen. |

Öffnet das Interface "Offene Rechnungen" mit allen offenen Rechnungen, bei denen du das Ziel bist
(6 pro Seite). Ein Linksklick auf eine Rechnung bezahlt sie: Die Diamanten werden aus deinem
Inventar entfernt und dem virtuellen Kontostand des Rechnungsstellers gutgeschrieben; der Steller
erhält dafür eine Benachrichtigung (sofort, wenn online, sonst beim nächsten Login).

Der Diamant ("Kontostand") zeigt in der Lore deinen eigenen virtuellen Kontostand (aus Rechnungen,
die andere Spieler an dich bezahlt haben) und zahlt ihn per Klick vollständig aus – die
Auszahlung landet als Diamanten in deinem **Belohnungsinventar** (`/belohnung`), nicht direkt
in deinem Inventar. Der Barriere-Block schließt das Interface (ab Seite 2 stattdessen ein
"⮜ Zurück"-Kopf, der eine Seite zurückblättert), der Kopf rechts unten ("➤ Weiter") blättert zur
nächsten Seite.

Der Kopf "Versendete Rechnungen" öffnet ein zweites Interface mit allen offenen Rechnungen, die
**du** gestellt hast (ebenfalls 6 pro Seite, gleiche Navigation). Ein Linksklick auf einen Eintrag
dort zieht die Rechnung zurück – ohne Frist, jederzeit möglich, nur vom Steller selbst auslösbar.
Ziel und Steller erhalten je eine Benachrichtigung. Ein Buch ("Offene Rechnungen") führt von dort
zurück zum ersten Interface.

## `/debug dump <Position>` (Admin/Dev)

Kein Spielerbefehl, sondern ein Werkzeug für die Entwicklung: liest einen Container (Truhe,
Doppeltruhe, Fass, Shulkerbox) oder ein Schild an der angegebenen Position aus (Koordinaten wie
bei `/data get block`, auch relativ mit `~`) und schickt eine kurze Zusammenfassung in den Chat,
mit einem anklickbaren Link, der den vollständigen Inhalt (Slot-Positionen, Items, Namen, Lore)
in die Zwischenablage kopiert. Gedacht, um die in der Kreativwelt gebauten Referenz-GUIs aus der
Spezifikation exakt an eine Entwicklungssession weiterzugeben – siehe
`docs/gui-reference-capture.md`.

| Berechtigung | Voraussetzung |
|---|---|
| `tes.admin.debug.dump` | Admin (`default: op`). |
