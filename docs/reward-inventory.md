# Belohnungsinventar (reward inventory)

Implements spec §1.3 / §3.3.1.4. This doc covers the implementation approach for future
maintainers; see `docs/commands.md` for the player-facing `/tes belohnung` usage guide (German),
and `docs/gui-library.md` for the GUI framework it's built on.

## The contract

Per §1.3: whenever a player receives items through the loyalty-point or level system — or, per
§3.1.1.3, an invoice cash-out — those items land here, never in a live shop or the player's real
inventory directly. Shop purchases explicitly never land here. Stage 2 only has one producer
(invoice cash-outs), but the reward inventory itself is deliberately generic so Stage 3's
loyalty-point shop and Stage 4's level rewards can reuse it without any changes here.

**`RewardInventoryService` is the only intended entry point.** Any reward-producing system should
call `grant(UUID, ItemStack)` — never insert into `RewardInventoryRepository` directly. This
mirrors the same "generic queue, single producer API" shape `PendingNotificationRepository` used
for UC5's notifications, which Stage 2's invoice notifications reused unchanged (see
`docs/invoice-system.md`).

## Storage: a queue, not fixed slots

`reward_inventory_items` is one row per stored `ItemStack` (`uuid`, `item` BLOB, `granted_at`),
not a fixed-size slot table — so it never needs a "how many slots" migration, and pagination is
just "however many rows exist, however many fit per page" rather than a capacity limit. `ON
DELETE CASCADE` to `players(uuid)` means `/tes spieler remove` wipes a player's queued rewards
along with everything else, matching the removal confirmation prompt's promise ("virtuelle
Inventare" are deleted).

`ItemStack`s persist via `serializeAsBytes()`/`deserializeBytes()`, the same pattern
`shops.item`/`shop_transactions.item` already use — full fidelity for enchantments, potion data,
display names, etc.

## Click-to-take-one

The spec describes the Belohnungsinventar's navigation (back arrow, next-page arrow) but not how
a player actually collects an item — this was a design decision, not a spec requirement: **left-
click an item to take that single stack into your real inventory.** If it doesn't fully fit
(checked via `RewardInventoryService.take()`'s all-or-nothing merge simulation — see its Javadoc),
nothing is removed from the queue and nothing is added to the inventory; the player is told to
free up space and try again. This avoids ever silently dropping part of a reward on the ground or
partially crediting a stack.

## Gating

Both `/tes belohnung` and (once Stage 2's invoices ship) invoice cash-out require the player be
registered and not paused — the reward inventory is squarely part of the "Belohnungssystem" the
spec gates behind registration (§1.4), unlike raw shop/invoice transactions which unregistered
players can still perform.
