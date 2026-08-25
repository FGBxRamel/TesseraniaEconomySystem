# Invoice system (Stage 2)

Implements spec §3.1.1.3 (Dienstleistungen/Trödelmarkt — services and the flea market, treated
identically by the spec). This doc covers the implementation approach for future maintainers; see
`docs/commands.md` for the player-facing `/rechnung` usage guide (German), and
`docs/reward-inventory.md` for where cash-outs land.

## No time-limited refund window, unlike shops — but creator-side retraction

Stage 1 item-shop purchases have an explicit 60-second cancellable window (§3.1.1.1's UC4). The
original invoice section (§3.1.1.3, spec v1.0) had no equivalent language at all — no
cancellation, expiry, or dispute mechanism was described anywhere for invoices, and that absence
was treated as deliberate: once created, an invoice stayed `OPEN` until its target settled it,
with no built-in way to retract or contest it.

The spec's v1.2 refresh (23.08.2026 PDF) adds one: the **creator** can retract a still-`OPEN`
invoice they sent, at any time, via the "Versendete Rechnungen" interface (`SentInvoiceGui`)
reachable from "Offene Rechnungen" (`InvoiceGui`) — clicking a sent invoice there withdraws it
(`InvoiceEconomy#retract`, `InvoiceState#RETRACTED`), notifying both creator and target. This is
not a symmetric counterpart to Stage 1's 60s window: no time limit, target-side settlement is
still final and irrevocable, and only the creator can act. `RETRACTED` is a third terminal state
alongside `SETTLED` (soft-transitioned like settlement, not deleted, so a retracted invoice's
history survives) — both interfaces simply query by `state = OPEN`, so a retracted or settled row
stops appearing without any extra filtering logic. Reference layout for both interfaces is in the
creative world at -409 -12 -3392; see `docs/gui-library.md` for the exact slot-by-slot rework.

## `invoice_balance`: a column, not a table

The virtual account balance a creator accumulates from settled invoices lives as
`players.invoice_balance`, sitting right next to `treuepunkte`/`erfahrungspunkte` — the same
shape (a single scalar, 1:1 with a player, no independent lifecycle). A separate table would only
add a join for no benefit: every place that reads or writes it already has the player's UUID in
hand.

## Settle and cash-out atomicity

`InvoiceEconomy.settle()` re-fetches the invoice by id and checks it's still `OPEN` before
touching anything — cheap insurance against a stale GUI render (the player clicked an invoice
that was already settled from another session), not a real race-condition fix: InvUI click
handlers run synchronously on the main server thread like any `InventoryClickEvent`, so two
clicks can never physically interleave.

`PlayerRepository.cashOutInvoiceBalance()` reads the current balance and resets it to 0 inside a
single `Database.execute()` call — already atomic relative to any other repository call, since
`Database` serializes all access through one single-threaded executor (the same guarantee
`SqlitePendingNotificationRepository.drain()` relies on for its own select-then-delete).

## Notification delivery gap

Invoice creation reuses `PendingNotificationRepository` unchanged — it was built during Stage 1
specifically anticipating this reuse (see its Javadoc). The spec's wording is "next login **or
activity**"; the only implemented delivery trigger, both here and for Stage 1's orphaned-shop
notice, is `PlayerJoinEvent`. This is a known, accepted gap rather than something worth new
infrastructure for: the only players actually affected are ones offline at invoice-creation time
who don't log out and back in again before wanting to know.

## Other v1.2 additions: amount cap and settle notification

Two more spec v1.2 additions, alongside retraction above:

- `<Preis>` is capped at **2304 Taler** — `RechnungCommand` checks this manually (German error via
  `Messages.rechnungPreisZuHoch`) rather than via `IntegerArgumentType.integer(1, 2304)`'s bound,
  so the rejection message stays in German instead of Brigadier's default English syntax error.
- Settling an invoice (`InvoiceGui`'s click handler) now also notifies the creator
  (`Messages.invoiceSettledForCreator`/`-Text`), reusing the same online/offline delivery split
  (`PendingNotificationRepository`) already used for invoice creation and, now, retraction.

## Gating

| Action | Registered required | Paused blocks |
|---|---|---|
| Creating an invoice (creator side) | Yes | Yes |
| Being invoiced (target side) | No | No |
| Opening `/rechnung anzeigen`, settling an invoice | No | No |
| Opening "Versendete Rechnungen", retracting an invoice | No | No |
| Cashing out (diamond icon) | Yes | Yes |

The target side stays fully open because a debtor must be able to pay off what they owe
regardless of their own registration/pause status (spec §1.4: "interacting with shops and paying
invoices is possible" even before registration). Cash-out is gated because its payout lands in
the Belohnungsinventar, which is squarely part of the reward system the spec ties to
registration. Note the asymmetry: a creator being paused does **not** block someone else from
settling their invoice — pause only suspends the paused player's own accrual, not their
counterparties' ability to pay a debt.
