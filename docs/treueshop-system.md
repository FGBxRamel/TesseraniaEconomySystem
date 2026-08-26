# Treuepunkteshop (loyalty-point shop)

Implements spec §3.2. This doc covers implementation decisions for future maintainers; see
`docs/commands.md` for the player-facing `/punkte` usage guide (German), and `docs/gui-library.md`
for the GUI framework it's built on. Being shipped incrementally (see `docs/ROADMAP.md`'s Stage 3
checklist) — this doc grows with each branch rather than being written all at once at the end.

## Reference build vs. PDF text

The PDF's own reward-table text doesn't always match what's actually built in the creative-world
reference (captured in `GUI_References/`, workflow in `docs/gui-reference-capture.md`). Per that
doc's own rule ("where they disagree, ask rather than guessing which one wins"), these were
resolved with the user rather than assumed — treat them as final, not open TODOs:

- **Mob-tier rewards (Belohnung 8.1/9.1/10.1/11.1) are bundled purchases**, per the PDF's model —
  the reference build's chests (`Mobs_V1.txt`–`V4.txt`) actually show à-la-carte per-egg pricing
  (16/6/4/6 individually-priced eggs), but a generically-designed reward catalog makes either
  shape a data change either way, so the tie-break (asked of the user) defaulted to the PDF.
- **Feindliche Mobs II grants a Guardian, not a Warden** — the reference build's `Mobs_V4` chest
  has no Warden slot at all (Guardian instead, not mentioned in the PDF anywhere). Confirmed by
  the user, flagged as possibly changing again — keep this tier's egg list as one easily-edited
  data table when it's built, not scattered across code.
- **Spawner's icon is `TRIAL_SPAWNER`**, matching the reference build (the PDF just says
  "Mobspawner" generically — not a real conflict, just an icon choice).
- **The Handelsbonus "on cooldown" icon is a plain re-lored `DIAMOND`**, not a custom-model-data
  texture swap as originally assumed from the PDF's "ausgegrauter Diamant (Custom-Modeldata
  benötigt)" wording — `GUI_References/Greyed_Diamond.txt` shows the same `DIAMOND` material with
  different lore ("Belohnung aufgebraucht..."). No `CustomHeads`-style texture asset needed for it.
- **The per-reward description signs (-411 -7 -3453..-3442) were redundant** — the main-interface
  items' own lore (captured via `/debug dump`) already has the full text. Not part of the capture
  checklist going forward.

## Package layout

`de.bydora.tes.treueshop`:

- `TreueshopReward` / `TreueshopRewardCatalog` — static reward metadata (grid position, icon,
  title, flavor lore), transcribed verbatim from the `/debug dump` captures in `GUI_References/`
  rather than re-derived from the PDF (see reconciliation notes above).
- `TreueshopGui` — the main interface (`Gui.builder()`, not `PagedGui` — see `docs/gui-library.md`).
- `TreueshopRewardService` — purchase orchestration.
- `TreueshopEffects` — direct potion-effect appliers.

## Purchase flow

Every wired-up reward purchase goes through `TreueshopRewardService.purchase(plugin, player, cost,
effect)`: an atomic `PlayerRepository.spendTreuepunkte` check-and-deduct, then the effect
`Runnable`. The atomic spend closes the double-click double-spend hole a plain
`addTreuepunkte(uuid, -cost)` would leave open — same select-check-update-commit/rollback shape as
`cashOutInvoiceBalance`. `TreueshopGui` reopens itself after a successful purchase to refresh the
balance display, the same "rebuild and reopen" tradeoff `RewardInventoryGui` already accepted
(resets to the same fixed screen rather than an in-place refresh).

Two reward "shapes" so far:

- **Direct-effect** (Segen der Zwerge, Kraftelixier): a vanilla `PotionEffect` applied immediately
  to the buyer, no persistence — matches how potion effects don't survive a server restart anyway,
  so there's nothing to durably track.
- **Item-grant** (not wired up yet — XP-Terminal's EP grants are a third shape, a direct
  repository call rather than an item or a potion effect): will route through
  `RewardInventoryService.grant`, never a direct inventory placement, per that service's own
  contract.

## Cost configuration

`TesConfig.treueshopRewardCost(rewardId, fallback)` is one generic accessor over a nested
`treueshop.rewards.<id>.cost` `config.yml` section, rather than a Java constant per reward — keeps
the ~16-entry reward table in one readable YAML block instead of scattered getters. Reward id
strings (e.g. `"segen-der-zwerge"`) live wherever they're used (`TreueshopRewardCatalog`, the
click-dispatch in `TreueshopGui`), not centralized as constants — `TesConfig` itself doesn't know
what rewards exist, only how to look one up.

## `/treuepunkte übertragen`

The spec's overview names this command but never specifies its syntax or constraints anywhere
else in the 32-page document. The user supplied the missing semantics out-of-band (2026-08-26):
`/treuepunkte übertragen <Zielspieler> <Anzahl>`, capped at the sender's own balance, no fee or
cooldown. Backed by `PlayerRepository.transferTreuepunkte` — an atomic debit-then-credit on a
single transaction, not two separate `addTreuepunkte` calls that could partially apply on failure.
