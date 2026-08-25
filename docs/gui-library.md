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

- **"Large Chest" (9x6) window, but the content grid is smaller than that**: every reference
  build so far (Belohnungsinterface at -424 -12 -3382, both invoice interfaces at -412/-414 -12
  -3392/-3393) opens as a 9x6 container, but only part of it is actual paginated content — the
  rest is fixed border/control chrome, confirmed via `/tes debug dump` (see
  `docs/gui-reference-capture.md`) rather than assumed:
  - Belohnungsinventar: content is rows 2–3, columns 2–8 (2×7 = **14 slots/page**), bordered by a
    full gray-pane top row and gray side columns; row 4 is the control row; rows 5–6 are a
    RED_STAINED_GLASS_PANE-filled dead zone (present in every reference build — replicated as-is
    rather than trimmed, since it's what the reference GUI actually renders as, not merely a
    creative-world building artifact).
  - Both invoice interfaces: content is row 1, columns 1–6 (**6 slots/page**), with row 1's
    remaining 3 columns and all of row 2 as white-pane filler, row 3 gray-pane filler, row 4 the
    control row, rows 5–6 the same red dead zone.
  - These page sizes are baked directly into each GUI's `Structure` (fixed layout, matching the
    spec's own exact mockup) rather than configurable via `TesConfig` — unlike, say,
    `shopSessionTimeoutSeconds()`, there's no meaningful "different number" a server operator
    would want here without also redesigning the layout around it.
- **Pagination navigation**: build next/previous-page buttons via `BoundItem.pagedBuilder()`
  rather than a plain `Item.builder()` — its three-argument click handler
  `(item, gui, click)` receives the actual bound `PagedGui` instance directly, avoiding a
  forward-reference to a `gui` variable that doesn't exist yet while the `Structure` ingredients
  are still being built. `de.bydora.tes.gui.PaginationControls` centralizes this (next-page item,
  and the close/previous-page item below) so every paginated screen shares one implementation.
- **Refreshing after a mutating click**: rather than fight `PagedGui<?>`'s generic wildcard when
  trying to call `setContent(...)` from inside a click handler, the simplest correct approach —
  used by `RewardInventoryGui` — is to just rebuild and reopen a fresh `Window` for the same
  player. It's not the most visually seamless (resets to page 1), but it's simple, type-safe, and
  the spec doesn't require preserving page position across an action.
- **Static vs. dynamic item content**: `Item.simple(...)` for content that never changes (filler
  panes); `Item.builder().setItemProvider(...)` (or the paged-bound variant) for anything that
  needs to be recomputed per render (e.g. a balance shown in an item's lore).
- **Close button doubles as previous-page on page 2+**: confirmed against the in-game reference
  builds (invoice interfaces at -414 -12 -3393 / -412 -12 -3392, and — per user confirmation, since
  its reference capture shows an unlabeled "-" placeholder rather than literal "Schließen" text —
  the Belohnungsinventar too) — the same control-row slot that shows the "Schließen" barrier on
  page 1 automatically swaps to a "⮜ Zurück" previous-page item from page 2 onward, i.e. it's the
  mirror-image counterpart of the "➤ Weiter" next-page item, not a separate always-present slot. A
  screen only ever needs one or the other in that slot (first page → Close, later page → Back),
  never both. `de.bydora.tes.gui.PaginationControls#closeOrPreviousPageItem()` implements this
  once for all three current paginated screens (`InvoiceGui`, `SentInvoiceGui`,
  `RewardInventoryGui`); apply it to any future paginated GUI with a close button too.

## Custom head icons

The reference builds use custom-textured `PLAYER_HEAD`s (not vanilla player skulls) for two
pagination icons, reused across every paginated screen — `de.bydora.tes.gui.CustomHeads` builds
these programmatically from a base64 `textures` profile property (via
`Bukkit.createProfile(...)` + `SkullMeta#setPlayerProfile(...)`), obtained in-game with:

```
/give @p player_head[custom_name={"bold":true,"color":"white","italic":false,"text":"➤ Weiter"},profile={"properties":[{"name":"textures","value":"eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMTliZjMyOTJlMTI2YTEwNWI1NGViYTcxM2FhMWIxNTJkNTQxYTFkODkzODgyOWM1NjM2NGQxNzhlZDIyYmYifX19"}]}] 1
```

```
/give @p player_head[profile={"properties":[{"name":"textures","value":"eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYmQ2OWUwNmU1ZGFkZmQ4NGU1ZjNkMWMyMTA2M2YyNTUzYjJmYTk0NWVlMWQ0ZDcxNTJmZGM1NDI1YmMxMmE5In19fQ=="}]},custom_name={"bold":true,"color":"white","italic":false,"text":"⮜ Zurück"}] 1
```

The first is the "➤ Weiter" (next page) head, the second the "⮜ Zurück" (previous page) head used
by `PaginationControls`. `InvoiceGui`'s "Versendete Rechnungen" link head is a third, one-off
texture (not shared elsewhere, so not documented here) inlined directly in that class.
