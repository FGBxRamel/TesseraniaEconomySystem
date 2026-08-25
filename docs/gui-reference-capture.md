# Reference GUI capture workflow

The spec (`docs/Tesserania-Economy-System.pdf`) points to fully-built reference GUIs in the
creative world at exact coordinates for most GUI-heavy features (Treuepunkteshop §3.2.1.1, its
sub-interfaces §3.2.1.2, the level interface §3.3.1.1, the invoice interfaces §3.1.1.3, and
scattered per-reward signage near further coordinates). The PDF's own text description of a given
GUI is frequently under-specified relative to what's actually built there (exact wording, slot
arrangement nuances, costs buried in lore) — building from text alone and revising after the fact
costs more round-trips than getting the reference captured up front.

## Dual-capture method

Two complementary captures per reference GUI, both supplied by whoever is testing in-game:

1. **Screenshot(s)** — the GUI open in-game, one overview shot per interface/sub-interface. Gives
   spatial/visual layout, icon shapes, and — for multi-GUI flows — which item opens which
   sub-interface. Claude can read images directly, no extra tooling needed.
2. **Debug dump** (see below) — an exact, textual per-slot listing of the same container. Gives
   unambiguous material, display name, lore, and cost text — the things a screenshot can get wrong
   (German umlauts, exact TP/EP numbers buried in lore, near-identical item icons) and that would
   otherwise need a hover-tooltip screenshot per slot.

Neither replaces the other: the screenshot answers "what does this look like", the dump answers
"what does this exactly say/contain".

## When to capture

Front-load it: get both captures for a stage's relevant reference coordinates *before* starting
implementation of that stage's GUIs, not after. Mid-implementation spot-checks ("what's actually
in slot 3|2 of the Mo3 sub-interface?") are fine on request too, using the same two-capture method
for just that one container.

Reference GUIs are static, known layouts — per the spec, the only per-session variation is content
volume (e.g. how many open invoices someone has), not layout — so a single capture per interface
is normally sufficient; no screen recording needed for click-flow, since which item opens which
sub-GUI is already stated in the spec text.

## The `/tes debug dump` command

Implemented (`de.bydora.tes.command.debug`), landed alongside this doc.

`/tes debug dump <Position>` (admin-only, permission `tes.admin.debug.dump`, following the
existing `tes.admin.<subcommand>.<action>` convention) takes a block position via Paper's
`ArgumentTypes.blockPosition()` — same coordinate syntax as vanilla's `/data get block` (absolute
or `~`-relative, tab-completed), resolved in whatever world the tester is standing in. It reads
the block there and, if it's a supported type, replies with a short chat summary plus a single
clickable line — clicking it copies the *entire* formatted dump straight to the OS clipboard via
Adventure's `ClickEvent.copyToClipboard(...)`, no file write involved (the live server is
remote-hosted for at least one regular tester, so a plugin-data-folder file wouldn't be reachable
for them; the click-to-clipboard chat message only needs the Minecraft client).

Supported blocks and their dump format:

- **Containers** (chest, double chest, barrel, shulker box, trapped chest — anything implementing
  Bukkit's `Container`): a header (world, coordinates, the container's custom name if renamed —
  i.e. the GUI title — and grid size, e.g. `9x4`), then one line per non-empty slot using the
  spec's own `<col>|<row>` position notation (not the raw linear NBT slot index) with material,
  display name, full lore, and stack count. A double chest's merged 54-slot inventory is handled
  transparently, since Bukkit already merges it on `getInventory()`. Empty slots are omitted.
- **Signs**: header, then front/back side text if present ("kein Text" if neither side has any).

Names/lore/sign text are serialized as MiniMessage (`<gold><bold>Prozessverstärker</bold></gold>`)
rather than stripped to plain text — color and formatting (bold, italic, underline, ...) survive
the dump instead of being silently lost, which matters since the spec's reward tables sometimes
use color/bold to distinguish reward tiers or highlight a cost.

Anything else (including item frames — entities, not blocks, so out of scope for a
position-based dump) gets `Messages.debugDumpUnsupported()`.

### How a session uses it

1. Identify the reference coordinates for the GUI in question from the PDF (see the § references
   above, or the specific coordinates cited inline in the relevant spec section).
2. Ask the tester to stand at (or near, using `~` offsets) those coordinates in the creative
   world, run `/tes debug dump <x> <y> <z>`, click the chat message to copy, and paste the result
   here — alongside an overview screenshot of the same container opened in-game (per this
   session's workflow, screenshots are dropped into the git repo rather than sent directly).
3. Treat the dump as the source of truth for exact text/costs/slot positions, and the screenshot
   for layout/flow confirmation. Cross-reference both against the spec's own written description —
   where they disagree, ask rather than guessing which one wins.
