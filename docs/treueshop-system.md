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

- **Mob-tier rewards (Belohnung 8.1/9.1/10.1/11.1) are à-la-carte purchases, one egg species per
  purchase** — matching the reference build's chests (`Mobs_V1.txt`–`V4.txt`, 16/6/4/6
  individually-priced eggs) and spec v1.3's revised wording ("Der Spieler kann auswählen, ob er
  ... erwirbt"). An earlier revision of this doc recorded a "bundled, grants every egg at once"
  reconciliation per the pre-v1.3 PDF text — that's been superseded; each species is now its own
  `TreueshopMobBundle.MobEggOption`, purchased and granted independently.
- **Feindliche Mobs II grants a Warden, not a Guardian** — the reference build's `Mobs_V4` chest
  originally had no Warden slot (Guardian instead), but was updated in-world alongside the v1.3
  (27.08.2026) spec revision to swap it for a Warden egg. The earlier Guardian reconciliation
  recorded here no longer applies.
- **Spawner's icon is `TRIAL_SPAWNER`**, matching the reference build (the PDF just says
  "Mobspawner" generically — not a real conflict, just an icon choice).
- **The Handelsbonus "on cooldown" icon is a plain re-lored `DIAMOND`**, not a custom-model-data
  texture swap as originally assumed from the PDF's "ausgegrauter Diamant (Custom-Modeldata
  benötigt)" wording — `GUI_References/Greyed_Diamond.txt` shows the same `DIAMOND` material with
  different lore ("Belohnung aufgebraucht..."). No `CustomHeads`-style texture asset needed for it.
- **The per-reward description signs (-411 -7 -3453..-3442) were redundant** — the main-interface
  items' own lore (captured via `/debug dump`) already has the full text. Not part of the capture
  checklist going forward.
- **XP-Terminal boosts grant real vanilla Minecraft experience, not TES's own Erfahrungspunkte
  counter** — despite the PDF's dev-facing text saying "Der Spieler erhält 6000 Erfahrungspunkte
  (points)", which reuses the same German term as TES's level-system currency. Two signals settle
  it: the "(points)" qualifier only makes sense to disambiguate vanilla XP *points* from vanilla XP
  *levels* (TES's own EP has no such distinction), and the PDF's own "(~ 50/69/100/120 Level)"
  subtitles line up closely with the real vanilla XP-to-level curve, not with the level system's
  `f(x) = 30·sqrt(x/30000)` (which reaches level 30 at just 30,000 total EP — a single 50,000-EP
  "boost" would instantly blow past max level, which the fixed-30-level design clearly doesn't
  intend). Implemented via `Player#giveExp`, corrected from this doc's earlier (pre-implementation)
  speculation that it would be a level-system repository call.
- **The XP-Terminal sub-interface uses plain `EXPERIENCE_BOTTLE` icons**, not the PDF's "Kopf mit
  1/2/3/4" custom-textured heads — the reference build's own Subinterface-(XP) dump already shows
  `EXPERIENCE_BOTTLE`, so no new `CustomHeads` texture capture was needed (reference-build-wins,
  same rule as everywhere else in this doc).
- **The Spawner reward's granted item is a plain `SPAWNER`, not `TRIAL_SPAWNER`** — the earlier
  `TRIAL_SPAWNER` reconciliation was specifically about the shop button's *icon* ("not a real
  conflict, just an icon choice"), not the item handed to the player. A `TRIAL_SPAWNER` can't
  actually be filled with spawn eggs (it's the Trial Chambers structure block, with its own
  built-in mob pool), which would contradict the PDF's "kann mit Spawneiern bestückt werden" —
  so the granted item stays a regular `SPAWNER` while the button keeps its `TRIAL_SPAWNER` icon.

## Package layout

`de.bydora.tes.treueshop`:

- `TreueshopReward` / `TreueshopRewardCatalog` — static reward metadata (grid position, icon,
  title, flavor lore), transcribed verbatim from the `/debug dump` captures in `GUI_References/`
  rather than re-derived from the PDF (see reconciliation notes above). Also holds the XP-Terminal
  sub-interface's four leaf rewards (`xpTerminalRewards()`) — same record shape, since a
  sub-interface's own grid position works the same way.
- `TreueshopMobBundle` / `TreueshopMobBundleCatalog` — the four mob-egg tier sub-interfaces (a
  shared cost plus a `MobEggOption` list, each option independently purchasable), kept as a
  separate record rather than folded into `TreueshopReward` since nothing else needs an egg list.
- `TreueshopComponents` — UI pieces shared by the main interface and every sub-interface: the
  balance sunflower, the "⮜ Zurück" button, filler panes, and the reward-icon-with-cost-lore
  builder (overloaded for both `TreueshopReward` and a `TreueshopMobBundle`/`MobEggOption` pair).
- `TreueshopGui` — the main interface (`Gui.builder()`, not `PagedGui` — see `docs/gui-library.md`).
  Dispatches each reward to either a sub-interface opener or a purchase handler.
- `TreueshopXpTerminalGui` — the XP-Terminal sub-interface (4 XP-Boost buttons).
- `TreueshopMobBundleGui` — one generic sub-interface reused for all four Mo1-Mo4 tiers,
  parameterized by `TreueshopMobBundle`: a grid of its `MobEggOption`s, each independently
  purchasable (buying one doesn't grant the others). Each option carries its own row/column,
  transcribed from the reference build's `Mobs_V1-V4` chest dumps (including that chest's
  recurring column-5 gap splitting each row into two groups) — not auto-packed left-to-right,
  since the reference layout is what a returning player already expects to see.
- `TreueshopRewardService` — purchase orchestration.
- `TreueshopEffects` — direct effects applied straight to the buying player (potion effects, and
  now `applyXpBoost`'s `Player#giveExp`) — nothing persisted, matching how these don't survive a
  server restart anyway.
- `TreueshopItemGrants` — item-grant rewards (Spawner, the Erntewelt/Glutzone stub Chorus Fruits,
  mob-egg bundles, Prozessverstärker), all routed through `RewardInventoryService#grant`, never a
  live inventory placement.
- `TreueshopHandelsbonus` (package-private) — the Handelsbonus button's purchase gating and its
  two icon states; see the dedicated "Handelsbonus" section below for why it's special-cased.

## Purchase flow

Every wired-up reward purchase goes through `TreueshopRewardService.purchase(plugin, player, cost,
effect)`: an atomic `PlayerRepository.spendTreuepunkte` check-and-deduct, then the effect
`Runnable`. The atomic spend closes the double-click double-spend hole a plain
`addTreuepunkte(uuid, -cost)` would leave open — same select-check-update-commit/rollback shape as
`cashOutInvoiceBalance`. `TreueshopGui` reopens itself after a successful purchase to refresh the
balance display, the same "rebuild and reopen" tradeoff `RewardInventoryGui` already accepted
(resets to the same fixed screen rather than an in-place refresh).

Four reward "shapes" now exist:

- **Direct-effect** (Segen der Zwerge, Kraftelixier, the XP-Terminal boosts): applied immediately
  to the buyer — a `PotionEffect`, or `Player#giveExp` for XP — with nothing persisted, matching
  how none of these survive a server restart anyway.
- **Item-grant** (Spawner, Erntewelt, Glutzone, one selected mob egg, Prozessverstärker): routes
  through `RewardInventoryService.grant`, never a direct inventory placement, per that service's
  own contract. Lives in `TreueshopItemGrants`, separate from `TreueshopEffects`, since the two
  shapes take different dependencies (a `RewardInventoryService` vs. nothing beyond the `Player`)
  — both are still dispatched through the same `directEffect` switch in `TreueshopGui` and the same
  `TreueshopRewardService.purchase` call, since from that call's point of view both are just "spend
  TP, then run an effect". `TreueshopMobBundleGui` calls `TreueshopRewardService.purchase` directly
  per option rather than going through `TreueshopGui`'s `directEffect` switch, since each option
  needs its own click handler rather than one fixed effect per reward id.
- **Sub-interface openers** (XP-Terminal, Freundliche/Feindliche Mobs I/II): no cost or effect of
  their own — `TreueshopGui` dispatches on `TreueshopReward#subInterfaceId` to open the
  corresponding screen instead of calling `TreueshopRewardService.purchase`.
- **Custom-gated** (Handelsbonus only): the only reward whose purchase can be *blocked* by
  something other than insufficient TP (the 2-slot cap, or already holding one) — special-cased
  entirely outside the `directEffect`/`subInterfaceId` dispatch, in `TreueshopHandelsbonus`. See
  the "Handelsbonus" section below.

`TreueshopGui.purchase` takes an explicit `onSuccess` callback rather than always reopening the
main interface, so `TreueshopXpTerminalGui` and `TreueshopMobBundleGui` can reopen themselves
(refreshing their own balance display) instead of bouncing the player back to the main screen
after every purchase.

## Cost configuration

`TesConfig.treueshopRewardCost(rewardId, fallback)` is one generic accessor over a nested
`treueshop.rewards.<id>.cost` `config.yml` section, rather than a Java constant per reward — keeps
the ~16-entry reward table in one readable YAML block instead of scattered getters. Reward id
strings (e.g. `"segen-der-zwerge"`) live wherever they're used (`TreueshopRewardCatalog`, the
click-dispatch in `TreueshopGui`), not centralized as constants — `TesConfig` itself doesn't know
what rewards exist, only how to look one up. The same accessor now also serves the four XP-Boost
ids (`xp-boost-1`-`4`) and the four mob-bundle ids (`freundliche-mobs-1`/`2`,
`feindliche-mobs-1`/`2`) — no `TesConfig` changes were needed for this branch at all. The XP boost
amounts (6000/12500/30000/50000) and each bundle's egg list, by contrast, are **not** configurable
— they're baked into lore text taken verbatim from the reference build/PDF, and would go stale if
changed independently via config (same tradeoff the existing haste/Kraftelixier duration lore
already accepts: config governs the actual effect, but the lore shows the shipped default as a
static string).

## Prozessverstärker (`de.bydora.tes.prozessverstaerker`)

Belohnung 1 is the only Treueshop reward that isn't a shop-menu interaction at all past the
purchase itself — it grants a physical item (a re-lored, tag-identified `GLOWSTONE_DUST`,
`ProzessverstaerkerItems.create`) that the player then right-clicks onto a furnace (any of
`FURNACE`/`BLAST_FURNACE`/`SMOKER`) or beehive (`BEEHIVE`/`BEE_NEST`) to consume it and apply a
boost. Kept as its own top-level package rather than folded into `treueshop` — it has its own
persistence, listener and background task, and nothing about it is Treueshop-GUI-shaped once the
item has been granted.

- **Item identification is a PDC tag** (`ProzessverstaerkerItems`), not name/lore matching — same
  convention as `ShopConversion`'s block-side shop tag — so a player can't make their own Glowstone
  Dust act as one by renaming it.
- **`prozessverstaerker_boosts`** (new table) is natural-keyed on `(world, x, y, z)`: a block only
  ever has one active boost, so re-use extends `expires_at` rather than inserting a second row —
  `SqliteProzessverstaerkerBoostRepository#extend` does a read-then-upsert (`MAX(remaining, now) +
  duration`), safe without extra locking since `Database#execute` already serializes all access to
  the single connection.
- **Furnace boost** is a one-shot `Furnace#setCookSpeedMultiplier(2.0)` (a Paper API) applied
  immediately in `ProzessverstaerkerListener`, not something the sweep task drives — the multiplier
  is a genuine block property that persists on its own once set.
- **Beehive boost** has no such single "apply once" hook: honey level increases deep inside a bee's
  return-to-hive tick, with no matching Bukkit event. `ProzessverstaerkerSweepTask` instead tracks
  each boosted beehive's last-observed `Beehive#getHoneyLevel()` (in memory, rebuilt from the live
  block the first time a boost is seen) and adds one more whenever vanilla's own +1 already landed
  — turning every natural increment into +2, capped at `getMaximumHoneyLevel()`.
- **The sweep task** (`ProzessverstaerkerSweepTask`, every 100 ticks, same cadence as
  `ShopMaintenanceTask`) is also what expires boosts (resets the furnace multiplier to `1.0`,
  forgets the beehive's tracked level, deletes the row) and self-heals if a boosted block was
  destroyed/changed outside the plugin, mirroring `ShopMaintenanceTask`'s orphan handling.
- Both furnace and beehive boosts share one duration (`treueshop.prozessverstaerker.boost-minutes`,
  default 15) — the PDF ties the beehive doubling to "die Zeit des Boosts" (the same window a
  furnace boost would run), not a separate value. "Effekt lässt sich addieren, sollte ein Block
  mehrfach geboostet werden" is implemented as duration stacking (each re-use adds the full
  duration on top of any remaining time) — not multiplier stacking (a furnace never exceeds 2x,
  a beehive never exceeds +2 per increment), which the spec's flat "15min doppelt so schnell"
  wording (not "up to Nx") supports.

## Handelsbonus (`de.bydora.tes.handelsbonus`)

Belohnung 4 is the last Stage 3 reward and the one that reaches back into already-shipped Stage 1
and Stage 2 purchase code (`ShopTradeListener`/`ShopEconomy`/`ShopMaintenanceTask` for shops,
`InvoiceEconomy` for invoice settlement) rather than staying self-contained — the reward's own lore
("Rabatt auf alle Einkäufe") means the discount has to apply to every future purchase the holder
makes, not to anything granted at Treueshop-purchase time.

### Model

- **At most 2 players may hold an active Handelsbonus at once** ("maximal durch 2 Spieler
  gleichzeitig auslösen"). A player already holding one (cooldown still running) can't trigger it
  again; once 2 *different* players are both on cooldown, a third can't trigger it either — the
  Treueshop button swaps to the "ausgegrauter Diamant" (transcribed from
  `GUI_References/Greyed_Diamond.txt`: plain re-lored `DIAMOND`, not custom-model-data, same
  reconciliation as branch 1) until a slot frees up.
- **The cooldown and the discount balance are tracked independently**, both in one
  `handelsbonus_holders` row per player. Triggering sets a random 3-4-week `cooldown_until` (spec:
  "einem zufälligen Zeitraum zwischen 3 und 4 Wochen") *and* a fresh 5-diamond
  `discount_remaining` — but the spec's own lore ("Nicht verbrauchte Werte bleiben erhalten") says
  the discount balance itself never expires on a timer, only re-triggering is cooldown-gated. So
  `countOnCooldown` (the concurrency/greying check) filters on `cooldown_until`, while discount
  application (`consumeDiscount`) only cares whether `discount_remaining > 0` — a player whose
  cooldown already lapsed can still have leftover discount sitting unused, and it still applies.
- **The Staatskasse is a real chest**, not a virtual balance — `Staatskasse.withdraw` resolves it
  from `config.yml` coordinates (mirroring `ShopEconomy.resolveInventory`'s pattern) and caps the
  discount at whatever's actually in there. Unconfigured (blank world) or an emptied chest just
  means Handelsbonus purchases still succeed but discount nothing until an admin funds it — no
  error, no blocked purchase.

### Wiring into `ShopTradeListener`

`handleBuyerClick` now computes `discount = withdrawHandelsbonusDiscount(buyer, price)` before the
affordability check (so a buyer only needs `price - discount` diamonds on hand), and the shop
slot's earned diamonds stay at the full nominal `price` regardless — the owner is never aware a
discount happened; only the buyer's own out-of-pocket diamonds and the Staatskasse's stock change.
`ShopTransactionRecord` gained a `staatskasseFunded` field (new `shop_transactions` column,
default 0 for all pre-existing rows) so the rest of the purchase lifecycle can tell how much of a
given sale was state-funded:

- **TP/EP accrual** (`ShopMaintenanceTask.creditBuyer`) uses `transaction.buyerPaid()`
  (`price - staatskasseFunded`), not `price` — spec: "Für die 5 Dias gibt es keine EP / TP!".
- **A within-window refund** (`ShopTradeListener.refund`, and `ShopEconomy.forceRefundPending` for
  a closed/orphaned shop) also gives back only `buyerPaid()`, not the full price — otherwise a
  refunded purchase would hand the buyer free diamonds they never actually spent.
- **Deliberately not unwound on refund**: the Staatskasse's contribution and the holder's consumed
  discount balance. The spec doesn't describe refund semantics for Handelsbonus at all; treating
  the state's spend as sunk once applied is the simpler, lower-risk reading (and arguably the more
  realistic one — a cancelled purchase doesn't reverse the state's own bookkeeping). This kept the
  change from needing to thread `HandelsbonusRepository`/`TesConfig` through both
  `forceRefundPending` call sites (`ShopCommand`, `ShopMaintenanceTask`) on top of the purchase
  path that already needed it.

### Wiring into `InvoiceEconomy`

`InvoiceEconomy.settle` mirrors `ShopTradeListener.handleBuyerClick`'s discount handling exactly:
it withdraws the payer's Handelsbonus discount from the Staatskasse before checking affordability
(so the payer only needs `price - discount` diamonds on hand), but always credits the invoice
creator's `invoice_balance` with the full nominal price — the creator, like a shop owner, is never
aware a discount happened. TP/EP accrual for the payer (`creditPayer`) uses the discounted
`amountToPay`, not the full price, for the same "Für die 5 Dias gibt es keine EP / TP!" reason as
`ShopMaintenanceTask.creditBuyer`. `settle` now returns a `SettleOutcome(SettleResult,
discountApplied)` record instead of a bare `SettleResult` so `InvoiceGui` can show the same
"Handelsbonus aktiv" message shop purchases already show. As with shops, a `NOT_ENOUGH_DIAMONDS`
outcome doesn't unwind an already-withdrawn Staatskasse contribution or consumed discount balance —
same pre-existing sunk-cost tradeoff as the shop path, not something this change revisits.

### Admin setup

The Staatskasse chest doesn't exist by default — an admin must place a real container (chest,
barrel, etc.) somewhere and set `treueshop.handelsbonus.staatskasse.world/x/y/z` in `config.yml`
to its coordinates, then keep it stocked with diamonds. Nothing in-game points an admin at this
requirement yet; it's config-only, matching the spec's own "Koordinaten... in der Configdatei...
angegeben werden müssen".

### Admin cooldown reset

`/handelsbonus reset <Name>` (`tes.admin.handelsbonus.reset`, `HandelsbonusCommand`) clears a
player's `cooldown_until` back to 0 via `HandelsbonusRepository#resetCooldown`, without touching
`discount_remaining` — an admin-only escape hatch with no spec counterpart, for cases like a
misconfigured Staatskasse or an accidental activation. Reports back whether the player actually had
an active cooldown to clear.

### Package layout

`de.bydora.tes.handelsbonus`: `HandelsbonusHolderRecord`/`HandelsbonusRepository`/
`SqliteHandelsbonusRepository` (the holder table) and `Staatskasse` (chest resolution/withdrawal).
`TreueshopHandelsbonus` (in `de.bydora.tes.treueshop`, package-private) owns the Treueshop button's
purchase gating and its two icon states — special-cased directly in `TreueshopGui.rewardItem`
rather than fitting the generic cost-then-effect dispatch every other reward uses, since this is
the only reward whose purchase can be blocked by something other than insufficient TP.

## Erntewelt / Glutzone item stub

Both grant a custom-named, custom-lored `CHORUS_FRUIT` (`TreueshopItemGrants.grantErntewelt` /
`grantGlutzone`) with no functional right-click/eat behavior yet — the actual teleport-into-a-
farm-world mechanic depends on the farm worlds themselves, which are Stage 5 (§3.2.1.3). Documented
gap, matching the same "grants item only, teleport wired up later" pattern already planned for
Level reward type 2. The lore says as much in-game so it isn't a silent no-op from the player's
perspective.

## `/treuepunkte übertragen`

The spec's overview names this command but never specifies its syntax or constraints anywhere
else in the 32-page document. The user supplied the missing semantics out-of-band (2026-08-26):
`/treuepunkte übertragen <Zielspieler> <Anzahl>`, capped at the sender's own balance, no fee or
cooldown. Backed by `PlayerRepository.transferTreuepunkte` — an atomic debit-then-credit on a
single transaction, not two separate `addTreuepunkte` calls that could partially apply on failure.
