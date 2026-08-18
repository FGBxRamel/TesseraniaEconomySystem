# Item shop system (Stage 1)

Implements spec §3.1.1.1 (the default, non-Redstone item shop). This doc covers the
implementation approach for future maintainers; see `docs/commands.md` for the player-facing
`/tes shop` usage guide (German).

## No GUI, on purpose

A shop is not a custom inventory screen — it *is* the converted container's own inventory. UC4's
worked example makes this explicit: buying 64 Dirt at 3 diamonds/slot turns that exact slot into
a 3-diamond stack, in place. This means Stage 1 needed zero GUI framework; the open question in
`ROADMAP.md` about pulling in a third-party GUI library only applies from Stage 3 onward.

## Conversion and the registry

`ShopConversion` tags a converted container's `PersistentDataContainer` with `"<world>:<id>"`
under the `NamespacedKey(plugin, "shop")` key, and sets its `Nameable` custom name to
`"<Item> | <Preis>"`. That tag is a *defensive secondary signal*, not the source of truth for
anything on the hot path — `ShopRegistry` is. It's an in-memory index (block position → shop,
and world+id → shop), warmed once from `ShopRepository` on enable, and every listener that needs
"is this a shop" (protection, trading) queries it directly rather than touching the database or
re-reading NBT. The PDC tag only gets read back by `ShopMaintenanceTask`'s orphan scan, comparing
it against what the registry expects for that position.

Double chests are stored as two positions (`ShopRecord.position()` /
`.secondaryPosition()`) resolved from Bukkit's `DoubleChestInventory#getLeftSide()/getRightSide()`
at creation time; both halves get tagged and both are registered to the same `ShopRecord`.

## The chat menu (order-independent, per the BlueMap-Marker reference)

`/tes shop erstellen|bearbeiten` open a per-player `ShopSession` and immediately render a
re-usable chat menu (`ShopChatListener.renderMenu` / `Messages.shopMenu`): one line per
`ShopSessionField` visible for the session's mode (`CREATE` shows ID/Name/Besitzer/Position/
Item/Preis/Teleport; `EDIT` omits ID and Position entirely, since both are immutable per UC2 —
not shown read-only, just absent), color-coded green once set, red if mandatory and unset, gray
if optional and unset, each clickable and hover-annotated, ending in `»» BESTÄTIGEN ««`/
`»» ABBRECHEN ««` buttons. This mirrors the spec's own screenshot of BlueMap Marker's builder
interface (§3.1.1.1) rather than a sequential wizard — attributes can be filled, and later
changed, in any order.

Clicking a menu line runs `/tes shop feld <key>`, which "arms" that field
(`ShopSession.pendingField`); the next chat message is routed to that field's handler instead of
being broadcast (`ShopChatListener.onChat`), and on success the field is cleared and the menu is
re-rendered so the player can pick anything else next. `POSITION` is the one field never settable
via typed chat — it's captured via a right-click on the target container
(`PlayerInteractEvent`, gated on `pendingField() == POSITION`) rather than typed coordinates,
since teleporting a player into a shop later would risk suffocation (the spec calls this out
explicitly), and typing coordinates by hand is exactly the kind of friction the BlueMap-Marker-
style flow avoids.

`/tes shop bestaetigen`/`abbrechen` (clickable, or typed as `bestätigen`/`abbrechen` — both work)
finalize or discard the session. Confirming re-checks `ShopSession.missingMandatory()` and, if
anything mandatory is still unset, reports what's missing and re-shows the menu rather than
failing silently. There's no separate confirm/summary screen the way the old linear flow had one
— the always-visible menu already shows every current value. Confirmation is handled entirely
within the session rather than layering on `ConfirmationManager` — the session itself already
carries actor identity and a TTL (which arming a field also refreshes), so a second token layer
would be redundant. The three click-driven leaves (`feld`/`bestaetigen`/`abbrechen`) carry no
permission node of their own: they're only reachable through a runtime-checked active session,
itself gated behind the already permission-checked `erstellen`/`bearbeiten`.

## The purchase mechanic

`ShopTradeListener` hooks `InventoryClickEvent` on the shop's own (top) inventory. A buyer's
plain left-click on the sold item swaps it for diamonds in that slot and calls
`buyer.setCooldown(Material.DIAMOND, 1200)` — vanilla's ender-pearl-cooldown mechanism, applied to
`DIAMOND` instead. Because the purchase just turned that slot into a diamond stack, the swirl
overlay renders on exactly that slot for the buyer, which is a precise match for the spec's own
"like the ender pearl cooldown" description rather than an approximation. Clicking that same
still-`PENDING` stack again within the 60-second window refunds it (swap back, `REFUNDED`);
owners can't touch it until it's no longer pending, and restocking only accepts the shop's
configured item. Shift-clicks and drags on the shop side are blocked outright to keep the whole
interaction to the single-slot-click model UC4 describes.

## `ShopMaintenanceTask`: two jobs sharing a cause

Both of the task's responsibilities exist because a shop's on-disk state and its real-world block
can drift apart:

- **Completion**: every `PENDING` transaction past its 60s window gets marked `COMPLETED`, and
  the **buyer** (not the shop owner — TP/EP are earned per Taler *spent*, confirmed against the
  spec's own "für einen ausgegebenen Taler" wording) is credited at the configured ratios, unless
  they're unregistered or paused.
- **Orphan scan (UC5)**: re-checks every registered shop's block(s) against the expected material
  and PDC tag; a mismatch means the block was destroyed or altered outside the plugin. The shop is
  hard-deleted (no legitimate transaction history is worth keeping for a shop whose block no
  longer exists — contrast with `schließen`'s soft delete, which does keep history), any pending
  purchase is force-refunded via the shared `ShopEconomy` helper, and every owner is notified —
  immediately if online, otherwise queued in `pending_notifications` and delivered on next join by
  `PendingNotificationListener`.

`pending_notifications` is deliberately shop-agnostic (just a UUID and a message string) so
Stage 2's invoice notifications can reuse it without a schema change.

## Soft delete vs. hard delete

`schließen` (a deliberate, owner-initiated close) soft-deletes: the `shops` row gets a
`closed_at` timestamp but stays, along with its transaction history — Stage 4's planned
per-shop income drill-down needs that history to survive a shop being closed and later
recreated under a new ID. Orphan cleanup hard-deletes, since there's nothing legitimate to keep
a record of once the block itself is gone.
