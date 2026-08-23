# Invoice system (Stage 2)

Implements spec §3.1.1.3 (Dienstleistungen/Trödelmarkt — services and the flea market, treated
identically by the spec). This doc covers the implementation approach for future maintainers; see
`docs/commands.md` for the player-facing `/tes rechnung` usage guide (German), and
`docs/reward-inventory.md` for where cash-outs land.

## No refund window, unlike shops

Stage 1 item-shop purchases have an explicit 60-second cancellable window (§3.1.1.1's UC4). The
spec's invoice section (§3.1.1.3) has no equivalent language at all — no cancellation, expiry, or
dispute mechanism is described anywhere for invoices. This isn't an oversight to fix; it's
respected as-is: once created, an invoice stays `OPEN` until its target settles it, with no
built-in way to retract or contest it.

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

## Gating

| Action | Registered required | Paused blocks |
|---|---|---|
| Creating an invoice (creator side) | Yes | Yes |
| Being invoiced (target side) | No | No |
| Opening `/tes rechnung anzeigen`, settling an invoice | No | No |
| Cashing out (diamond icon) | Yes | Yes |

The target side stays fully open because a debtor must be able to pay off what they owe
regardless of their own registration/pause status (spec §1.4: "interacting with shops and paying
invoices is possible" even before registration). Cash-out is gated because its payout lands in
the Belohnungsinventar, which is squarely part of the reward system the spec ties to
registration. Note the asymmetry: a creator being paused does **not** block someone else from
settling their invoice — pause only suspends the paused player's own accrual, not their
counterparties' ability to pay a debt.
