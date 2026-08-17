# TES Development Roadmap

Development proceeds in stages. Each stage adds one or more significant features and leaves the plugin in a working, buildable state (`mvn clean package` succeeds — enforced by CI from Stage 0 onward). Stages are ordered primarily by the dependency structure in `docs/Tesserania-Economy-System.pdf` ("the spec"): transaction capture is the foundation both reward systems (Treuepunkte, Level) build on, so it comes first; the spec's own explicit priority call-outs (item shops before redstone shops, redstone shops lowest priority overall) are respected.

**How to use this doc**: this is the source of truth for what stage development is currently on, across sessions. When a stage's feature(s) ship (PR(s) merged to `main`), tick its checkbox and add a one-line note (date + release version if one was cut). Don't rewrite history here — append notes, don't delete completed stage descriptions.

## External dependencies / integration notes

- **Permissions**: [LuckPerms](https://luckperms.net/) is a hard requirement on the target server. TES does not implement its own permission system — it only needs to define and check sufficient permission nodes (e.g. `tes.admin`, `tes.shop.create`, `tes.rechnung.erstellen`, ...). Granting/assigning nodes to players/groups is server-side LuckPerms configuration, out of scope for this plugin.
- **Worlds**: a separate plugin already manages worlds on the server, using only standard Bukkit world APIs. Farm-world creation/reset (Stage 5) should stick to standard Bukkit world-management calls, which should coexist without conflict.
- **GUI implementation**: whether to hand-roll inventory-GUI handling or pull in a third-party GUI library is an open implementation choice — revisit it at the start of Stage 3, the first GUI-heavy stage (multi-level chest interfaces with sub-interfaces, pagination, back-navigation).

## Branching & versioning convention

- Each significant feature within a stage gets its own `feat/...` (or `fix/...`) branch and PR into `main`, using Conventional Commits (see root `CLAUDE.md`). A stage is a planning/tracking unit, not necessarily a single PR.
- `main` must always build — enforced mechanically by the Stage 0 CI build workflow.
- Versioning starts at `0.x.y` (semver "initial development" range) since the plugin is pre-feature-complete; bump to `1.0.0` when Stage 6 (full spec coverage) ships. [release-please](https://github.com/googleapis/release-please) derives each bump automatically from Conventional Commit types (`fix:` → patch, `feat:` → minor, `feat!:`/`BREAKING CHANGE:` → major).
- Release mechanics: release-please runs on every push to `main` and maintains a standing "release PR" (version bump + changelog). Merging that PR is the only manual step — it triggers tag + GitHub Release creation, and a second workflow then builds the shaded jar and attaches it to the release automatically. Cut a release whenever there's something worth testing on the server (typically ~once per completed stage, not required to be strictly 1:1).

## Stages

### Stage 0 — Foundation & Release CI
Status: `[ ]` not started
Implements: §1.2, §1.4 (partial)

- Persistent player data layer (registration record, TP/EP/level counters, pause/sanction flag) — storage approach (flat per-player YAML vs. embedded SQLite) to be decided during implementation.
- Central config system (ratios Taler:TP / Taler:EP, spec defaults 1:5 / 1:3).
- `/tes` command with subcommand dispatch and a defined set of LuckPerms permission nodes per subcommand (admin vs. player-facing).
- `/tes spieler add|remove|pause|unpause <Name>`, including the required re-confirmation step for `remove` and full data wipe on confirmed removal.
- `.github/workflows/build.yml`: `mvn clean package` on push/PR to `main`.
- `.github/workflows/release-please.yml`: release PR management (release-type `simple` + `extra-files` XML updater targeting `pom.xml`'s `<version>`). Note: branch protection may require a PAT instead of the default `GITHUB_TOKEN` for the action to push the release PR branch.
- `.github/workflows/release-build.yml`: on tag/release creation — checkout tag, `mvn versions:set`, `mvn clean package`, upload shaded jar to the GitHub Release.
- Deliverable: plugin builds and loads; admins can register/pause players; nothing player-facing yet, but every future merge to `main` produces a downloadable, correctly versioned jar automatically.

### Stage 1 — Item Shops & Transaction Capture
Status: `[ ]` not started
Implements: §3.1.1.1

- Shop container conversion (chest, double chest, redstone chest/double, barrel, shulker + colors) with required attributes (ID, name, owner(s), position, item, price/slot) and optional teleport point.
- `/tes shop erstellen|bearbeiten|schließen|liste <world> [id]`, chat-driven UX modeled on the BlueMap Marker plugin flow, including the >10-shops pagination behavior in `liste`.
- Purchase flow (UC4): slot click → diamond deduction/item exchange, 60s cancellable window with cooldown-overlay UX (ender-pearl-style), owner-only withdraw (post-cooldown) and restock.
- Orphaned-shop-object cleanup + one-time player notification (UC5).
- Transaction-completion event wired to TP/EP accrual using Stage 0's configured ratios.
- `/tes treuepunkte add|remove|set` and `/tes erfahrungspunkte add|remove|set` admin commands (counters exist now; no spend UI yet).
- Deliverable: players can trade through shops end-to-end; points silently accrue; nothing to spend them on yet.

### Stage 2 — Dienstleistungen / Trödelmarkt + Reward Inventory
Status: `[ ]` not started
Implements: §3.1.1.3, §1.3 (Belohnungsinventar)

- Belohnungsinventar core: generic virtual per-player inventory + `/tes belohnung` command, pagination.
- `/tes rechnung erstellen <Ziel> <Preis> <Grund>` with next-login/activity notification.
- `/tes rechnung anzeigen` interface: open invoices list, click-to-settle (buyer → creator's virtual balance), creator cash-out (payout lands in reward inventory, hover-to-see-balance).
- Deliverable: full invoice/flea-market flow works; reward inventory exists and is reusable by later stages.

### Stage 3 — Treuepunktesystem / Loyalty Shop
Status: `[ ]` not started
Implements: §3.2

First GUI-heavy stage — decide hand-rolled vs. third-party GUI library here before building the interfaces below.

- `/tes punkte` / `/tes treuepunkte` main interface (9x4, sunflower balance, back arrow, level-switch button) at the exact grid/costs from §3.2.1.1.
- All ~12 top-level rewards and sub-interfaces (XP-Terminal + 4 XP boosts, Mo1–Mo4 mob-egg bundles, spawner) per the reward table.
- Effect implementations: Prozessverstärker (furnace/beehive boost), Segen der Zwerge (haste), Kraftelixier (potion bundle), Handelsbonus (2-player-max, staatskasse-funded, cooldown-replacement custom-model diamond), Erntewelt/Glutzone reward items (grant the teleport item now; full farm-world mechanics land in Stage 5 — known temporary gap).
- Deliverable: loyalty shop fully spendable except for the farm-world destination itself.

### Stage 4 — Levelsystem + Backpack
Status: `[ ]` not started
Implements: §3.3

- EP accrual (wired in Stage 1) → level-up processing via `f(x) = 30·sqrt(x/30000)`, max-level cutoff (configurable, default 30).
- Level rewards config file (`#Level; Typ; Name; {attrs}` format per §3.3.1.1) covering the full §3.3.1.2 table for reward types 1 (item grant), 3 (backpack expansion), 4 (passive XP), 5 (death protection) now; type 2 (resource-world access) grants the item, teleport completed in Stage 5 (same documented gap as Stage 3).
- `/tes level add|remove|set` admin command.
- Backpack: virtual, location-independent, unlocked level 3 (9 slots), expansions at 9/14/20/25/30 (second page), requires registered + unpaused, `/bp` and `/backpack`.
- Level interface (`/tes level`): current level, total/remaining EP, total spend/income with drill-down (clickable barrels per shop, heads per player, sort toggle, >5-entry pagination), links to reward inventory/backpack/loyalty interface.
- Deliverable: full level progression, backpack, and stats drill-down all work.

### Stage 5 — Farm Worlds
Status: `[ ]` not started
Implements: §3.2.1.3

- Erntewelt (Overworld-type) / Glutzone (Nether-type) world lifecycle: 48h reset+reseed, largest-possible biome size, `/locate biome` open to everyone in these worlds.
- `/tes farmwelt <erntewelt|glutzone>` entry; `/world farmwelt-erntewelt` / `/world farmwelt-glutzone` self-return; 3h stay timer with in-world dashboard (remaining stay time + time to next reset), auto-return to last overworld position on expiry.
- Per-block drop-rate multipliers: `/tes farmwelt multi <Item> <Droprate>`, persisted in config across resets, natural-generation-only (exclude player-placed blocks).
- Wire up TP rewards 5/6 and Level reward type 2 to actually teleport now.
- Deliverable: farm worlds fully functional; earlier "grants item only" gap closed.

### Stage 6 — Item-Redstone-Shops
Status: `[ ]` not started
Implements: §3.1.1.2

Explicitly lowest-priority transaction type per the spec, saved for last among core features.

- Redstone-Kasse pairing with a shop's container attributes (owner(s), item, price/slot, name, ID — no position/teleport per spec).
- `/transaktion abgeschlossen <@p|player> <location> <price>` command-block-driven completion signal.
- No refund right (unlike Stage 1 shops).
- Deliverable: all three transaction types from §3.1 now implemented; plugin is feature-complete against the spec → good point to cut `1.0.0`.

### Stage 7 — Polish / Quality-of-Life
Status: `[ ]` not started
Implements: none directly (cross-cutting)

- Automatic update/new-version check against GitHub Releases (uses the Stage 0 release pipeline as its data source), with an in-game/admin-console notice.
- Edge-case/error-handling hardening pass across all commands and GUIs.
- Localization/wording pass over all player-facing German text for consistency.
- Performance pass (list/GUI pagination, storage access patterns) once real usage patterns are visible.
- Final consolidated user-facing German docs pass and any remaining dev docs.
