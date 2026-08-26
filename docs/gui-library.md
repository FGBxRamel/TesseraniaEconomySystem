# GUI library (adopted in Stage 2)

`ROADMAP.md` originally deferred choosing a GUI framework to Stage 3 ("the first GUI-heavy
stage"). Stage 2 needed one first — both the Belohnungsinventar (`/tes belohnung`) and the
invoice list (`/tes rechnung anzeigen`) are paginated virtual chest screens with per-item click
handlers — so the decision was pulled forward here instead.

## Chosen: InvUI

[`xyz.xenondevs.invui:invui`](https://github.com/NichtStudioCode/InvUI), version `2.3.0`, MIT
licensed, from the `xenondevs` Maven repository (`https://repo.xenondevs.xyz/releases`, added to
`pom.xml` alongside the PaperMC repo). It was picked over the verified runner-up,
[InventoryFramework](https://github.com/stefvanschie/IF) (Maven Central, broader legacy-version
support), because InvUI 2.x explicitly targets the latest Paper build only and requires Java 25
minimum — matching this project's `paper-api [26.2.build,)`/Java 25 baseline more precisely than
a library optimized for broad backwards compatibility we don't need.

## pom.xml wiring

InvUI is shaded into TES's own jar (not a separate server plugin, so no `depend`/`softdepend`
entry in `paper-plugin.yml`) and relocated from `xyz.xenondevs.invui` to
`de.bydora.tes.libs.invui` via the existing shade execution's `<relocations>` block — standard
practice to avoid classpath clashes with another plugin on the same server bundling a different
InvUI version. Contrast with `org.sqlite`, which is deliberately *not* relocated (see the comment
next to it in `pom.xml`) because sqlite-jdbc's native-library loading doesn't survive relocation;
InvUI has no such constraint.

No explicit initialization call is needed. `InvUI.getInstance().getPlugin()` auto-infers the
owning plugin from the classloader (`ConfiguredPluginClassLoader`, Paper-specific) the first time
any InvUI class is touched, which works out of the box once InvUI is shaded into TES's own jar.

## Conventions for future GUIs (Stage 3/4)

- **54-slot ("Large Chest") layout**: 5 content rows (45 slots) + 1 control row at the bottom,
  matching the spec's own mockups for both the Belohnungsinventar and the invoice list. Follow
  this same shape for new paginated screens unless a spec mockup says otherwise.
- **Page size**: not fixed in code — read from `TesConfig` (e.g.
  `rewardInventoryItemsPerPage()`), since the spec gives no numbers for these two interfaces.
  New GUIs needing a page size should add their own `TesConfig` accessor the same way.
- **Pagination navigation**: build next/previous-page buttons via `BoundItem.pagedBuilder()`
  rather than a plain `Item.builder()` — its three-argument click handler
  `(item, gui, click)` receives the actual bound `PagedGui` instance directly, avoiding a
  forward-reference to a `gui` variable that doesn't exist yet while the `Structure` ingredients
  are still being built.
- **Refreshing after a mutating click**: rather than fight `PagedGui<?>`'s generic wildcard when
  trying to call `setContent(...)` from inside a click handler, the simplest correct approach —
  used by `RewardInventoryGui` — is to just rebuild and reopen a fresh `Window` for the same
  player. It's not the most visually seamless (resets to page 1), but it's simple, type-safe, and
  the spec doesn't require preserving page position across an action.
- **Static vs. dynamic item content**: `Item.simple(...)` for content that never changes (filler
  panes); `Item.builder().setItemProvider(...)` (or the paged-bound variant) for anything that
  needs to be recomputed per render (e.g. a balance shown in an item's lore).
