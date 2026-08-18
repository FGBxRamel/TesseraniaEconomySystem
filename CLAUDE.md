# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

TES (Tesserania Economy System) is a PaperMC plugin (`de.bydora.tes.TesseraniaEconomySystem`) for the Minecraft server "Tesserania". The codebase is currently a bare plugin skeleton (`onEnable`/`onDisable` only) — the actual economy features described below have not been implemented yet. `docs/Tesserania-Economy-System.pdf` is the authoritative (German-language) requirements/design spec; consult it before implementing any of the systems below, since it defines exact command syntax, inventory layouts, coordinates for reference inventories in the creative world, and reward tables.

## Build

- `mvn clean package` — the POM's `<defaultGoal>` is already `clean package`, so plain `mvn` runs it.
- Requires Java 25 (`java.version` property) and the PaperMC repo (`repo.papermc.io`) for `paper-api` (version range `[26.2.build,)`, `provided` scope).
- The shade plugin runs on `package`, producing the deployable shaded jar in `target/`.
- `src/main/resources/paper-plugin.yml` is resource-filtered by Maven (`${version}`, `$description` placeholders) — edit `pom.xml`/plugin description to change these, not the yml directly for those fields.
- Local test server: `mvn run-paper:install verify exec:exec@download-luckperms run-paper:run-server` (or the shared IntelliJ "Run Test Server" configuration in `.run/`) boots a real Paper server with the plugin and LuckPerms auto-installed under `target/run/` — see `docs/dev-server.md`. Useful for a first-pass check that a feature actually loads/enables before handing off for interactive testing.
- No test framework or tests are set up yet.

## Working conventions

- **Staged development**: development proceeds in stages, tracked in `docs/ROADMAP.md` — check it before starting new feature work, and update its status checkboxes as stages ship.
- **Ground truth**: `docs/Tesserania-Economy-System.pdf` is authoritative for feature behavior, commands, values, and layouts. Implement to the spec; if it's ambiguous or silent on something, ask rather than guessing.
- **Language split**: all code (identifiers, code comments, commit messages, dev-facing docs) is in English. Anything player/user-facing — in-game messages, GUI item names/lore, user documentation/guides — is in German, matching the spec.
- **Comments**: keep code comments minimal. Favor a proper Javadoc (`/** ... */`) on public/API-facing classes and methods over inline comments; add an inline comment only where the logic itself is genuinely hard to follow. Don't narrate obvious code.
- **Docs per feature**: every implemented feature needs German user-facing docs (how to use it in-game), kept alongside the code as it's built, not deferred. Separate English dev docs (as opposed to Javadoc/code comments) are only worth writing for the bigger picture or genuinely complex features/subsystems — not for individual functions or lines, and not just for the sake of having an overview. If a feature is small or self-explanatory from well-documented code, skip the separate dev doc.
- **Git**: commits and pushes may be made autonomously. Use Conventional Commits for both commit messages and branch names (e.g. `feat/loyalty-point-shop`, `fix(shop): correct refund window`). `main` must always build (`mvn clean package` succeeds) — runtime crashes/incomplete functionality on `main` are acceptable, a broken build is not.
- **Persistence**: player data lives in an embedded SQLite database (`tes.db` in the plugin data folder, via `org.xerial:sqlite-jdbc`), not YAML. Schema changes are additive migrations keyed off `PRAGMA user_version` (see `SchemaMigrator`) — add a new migration entry for new tables/columns, don't edit existing ones in place.
- **Commands**: implement `/tes` subcommands as real Brigadier trees (`Commands.literal(...).then(...)`), registered via `TesBootstrap` (a `PluginBootstrap`, using `LifecycleEvents.COMMANDS`) during the bootstrap phase — not Paper's `BasicCommand` shim, and not YAML-declared commands (Paper plugins don't support those). `BasicCommand` shipped in Stage 0 first and had broken tab-completion; follow the shape already established in `TesCommand`/`SpielerCommand` for new subcommands, with per-action permission checks via `.requires(...)`.
- **Testing**: no automated test framework is set up. The local test server (see Build) lets a first pass be verified non-interactively — plugin loads/enables, commands register, admin flows work as OP — but actual in-game interaction (GUIs, item drops, multiplayer scenarios) still needs the user; ask them to test on the server and report back rather than assuming it works from a successful build alone.
- **Emojis**: never use emojis in code (identifiers, comments, commit messages, code-facing docs). In user-facing docs, only use them where they genuinely add value (e.g. clarifying a GUI icon), not decoratively.

## Domain model (from the design spec)

The plugin replaces a purchase-obligation trading system with an incentive/reward-driven one, built on two complementary reward systems layered on top of raw transactions: a **loyalty point system** (Treuepunkte, TP) and a **level system** (Erfahrungspunkte/EP). Currency unit is the Diamond ("Taler").

- Conversion (configurable via config file): 1 Taler spent → 5 TP, and 1 Taler spent → 3 EP. Only *completed* transactions count (see cancellation window below).
- **Belohnungsinventar** ("reward inventory") — whenever a player earns rewards from the TP or level systems, items land in a virtual inventory opened via `/tes belohnung`, never inside a live shop chest. Purchases in shops do NOT go through the reward inventory.
- **Rucksack** ("backpack") — a virtual, location-independent inventory unlocked at level 3, expanded at later level milestones (9, 14, 20, 25, 30 → second page). Opened via `/bp`, `/backpack`, or the level interface. Requires the player to be registered and not paused/sanctioned in the system.
- A player must first be registered via `/tes spieler add <Name>` before the reward system is active for them; unregistered players can still use shops/invoices but earn nothing. `/tes spieler remove` requires re-confirmation and permanently deletes all of that player's system data.

### Transaction capture (3.1)

Three transaction kinds, all of which feed the reward systems:
1. **Item shops** (default, no redstone) — modified vanilla containers (chest, double chest, "redstone chest"/hopper-adjacent variants, barrel, shulker box + colors) turned into sell points. Required attributes: ID, name, owner(s), position, item, price-per-slot. Optional: teleport point. Managed via `/tes shop erstellen|bearbeiten|schließen|liste <world> [id]`, modeled UX-wise after the BlueMap Marker plugin's chat-based setup flow. Purchases are refundable for 60 seconds (cooldown-overlay UX like an ender pearl cooldown) before counting as a completed transaction; only the shop owner can withdraw earned diamonds (after the cooldown) or restock the sold item.
2. **Item-Redstone shops** (lowest implementation priority) — same base attributes, but paired with a physical "Redstone-Kasse" that signals completed transactions to the plugin via a command block running `/transaktion abgeschlossen <@p|player> <location> <price>`. No refund right.
3. **Dienstleistungen / Trödelmarkt** (services / flea market) — modeled as invoices: `/tes rechnung erstellen <Ziel> <Preis> <Grund>` notifies the target on next login/activity; `/tes rechnung anzeigen` opens an interface listing open invoices, lets the payer settle by clicking (transfers diamonds to the invoice creator's virtual balance), and lets the creator cash out that balance (payout lands in the reward inventory, not directly in their real inventory).

### Loyalty points (3.2) — `/tes punkte` or `/tes treuepunkte`

Opens the "Treuepunkteshop", a 9x4 chest-GUI with a fixed grid of ~12 numbered top-level rewards (each with a TP cost, e.g. furnace-boost "Prozessverstärker" for 25 TP, XP-Terminal opening a sub-GUI of XP boosts, "Segen der Zwerge" haste effect, "Handelsbonus" trade-discount funded from a state treasury chest, farm-world access items "Erntewelt"/"Glutzone", friendly/hostile mob egg bundles opening further sub-GUIs, a mob spawner for 1000 TP). A sunflower item shows the player's current TP balance; a golden arrow navigates back; a wooden-head-with-arrow switches to the level interface. All finished reference GUIs (fully laid out, labeled) live in the creative world at coordinates **-406 -11 -3390**; individual reward description signage is scattered near **-411 -7 -3390** through **-3453** (see spec for exact per-reward coordinates). Reward costs, slot positions, and descriptions are enumerated exhaustively in the PDF (§3.2.1.1–3.2.1.3) — treat that table as the source of truth for exact numbers rather than re-deriving them.

Farm worlds ("Erntewelt" = Overworld-type, "Glutzone" = Nether-type) are reached via `/tes farmwelt <erntewelt|glutzone>`, reset every 48h with a new seed, expose `/locate biome` to everyone, and support per-block drop-rate multipliers configurable via `/tes farmwelt multi <Item> <Droprate>` (persists across resets; only applies to naturally generated blocks, not player-placed ones).

### Level system (3.3) — `/tes level`

Level progression follows `f(x) = 30 * sqrt(x / 30000)` where x is accumulated EP. 30 levels are planned initially (extensible later); beyond max level, no further EP is awarded. Rewards per level fall into 6 configurable types: 1=item grant, 2=resource-world access, 3=backpack slot expansion, 4=passive XP (vanilla) bonus, 5=XP/level-death-protection, 6=custom. The full level 1–30 reward table (items, durations, slot counts) is specified in §3.3.1.2 of the PDF — do not invent reward values, look them up there. Intended config file format is a simple delimited line per level: `#Level; Typ; Name; {Typ-specific attributes}`. The level interface (`/tes level`, reference GUI at **-418 -12 -3384**) shows current level, total/remaining EP, total spend/income (drill-down into per-shop/per-player income via clickable barrels and player heads), and links to the reward inventory, backpack, and the loyalty-point interface.

### Admin commands (all admin-only)

```
/tes treuepunkte add|remove|set <Anzahl>
/tes erfahrungspunkte add|remove|set <Anzahl>
/tes level add|remove|set <Anzahl>
/tes spieler add|remove|pause|unpause <Name>
/tes farmwelt multi <Item> <Droprate>
```

Ratios (Taler:TP, Taler:EP), per-level rewards, and farm-world drop multipliers should all be adjustable via config files, not hard-coded, per the spec's explicit configurability requirements (§1.4).
