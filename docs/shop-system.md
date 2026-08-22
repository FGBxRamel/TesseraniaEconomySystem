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
`"<Name> | <Preis>"`. That tag is a *defensive secondary signal*, not the source of truth for
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
plain left-click on the sold item swaps it for diamonds in that slot. Those diamonds carry a
`UseCooldown` data component (`DataComponentTypes.USE_COOLDOWN`) scoped to a per-shop-and-slot
`NamespacedKey` (`shop-pending-<id>-<slot>`), and `buyer.setCooldown(group, 1200)` starts the
swirl overlay for that group — vanilla's ender-pearl-cooldown mechanism, but keyed to a custom
cooldown group instead of the `DIAMOND` material. That matters for two reasons a plain
`Material.DIAMOND` cooldown can't handle: it doesn't bleed onto the buyer's own currency diamonds
sitting elsewhere in their inventory (they don't carry the component, so they never render the
overlay), and a second, concurrent purchase in a different slot gets its own group and doesn't
reset an already-running overlay in another slot. Clicking that same still-`PENDING` stack again
within the 60-second window refunds it (swap back, `REFUNDED`); the plain diamonds handed back on
refund carry no cooldown component. Owners can't touch a slot's diamonds (by normal click or
shift-click) until it's no longer pending — after that, both are allowed, so `Shift` works as a
quick withdraw. Restocking only accepts the shop's configured item, and non-owner shift-clicks and
all drags on the shop side stay blocked outright to keep the interaction to the single-slot-click
model UC4 describes.

### Sell-all-items shops

Typing `alle`/`all` for the Item attribute instead of a material name creates a shop that buys any
non-diamond item at the configured flat price per slot, rather than one fixed material.
`ShopRecord.SELL_ALL_SENTINEL` (an `ItemStack` of `Material.AIR`) represents this: AIR can never be
a legitimately configured single item (it's not `Material#isItem()`, and the setup flow's material
lookup already rejects it), so it doubles as the "sell all" flag without a new field, DB column, or
migration — `shop.item()` round-trips through SQLite as a serialized `ItemStack` exactly like any
other configured item, it just happens to be an empty AIR stack.
`ShopRecord.sellsAllItems()`/`itemDisplayName(ItemStack)` are the two call sites that need to know
about the sentinel; everywhere else (refund, withdraw, persistence, the orphan scan) is already
item-agnostic since it operates on the *purchased* item recorded per transaction, not the shop's
configured one. `ShopTradeListener` only special-cases the buy-side match check (skip it entirely
for a sell-all shop) and the owner restock check (block diamonds specifically, since there's no
single configured item left to compare against).

## Item identity is a full `ItemStack`, not just a `Material`

`ShopRecord.item` and `ShopTransactionRecord.item` are `ItemStack`s, not `Material`s — a shop's
configured item and a recorded purchase carry their full NBT (enchantments, potion data, custom
display name/lore, custom model data, etc.), not just a type enum. This matters because the shop's
own container inventory *is* the sale slot (see above): whatever `ItemStack` an owner physically
places there — an enchanted book, a named sword, a brewed potion — is exactly what gets sold, and
losing that data on purchase would hand the buyer a plain vanilla item instead. Both records
defensively `clone()` their `item` in a compact constructor, since `ItemStack` is mutable and these
are otherwise-immutable records.

Matching now uses `ItemStack#isSimilar` (type + meta, ignoring stack size) instead of `Material`
equality, both for the owner's restock check and the buyer's "is this the shop's configured item"
check. A purchase clones the *actual clicked stack* (`clicked.clone()`) rather than reconstructing
one from `shop.item()`, so what the buyer receives — and what a refund puts back — is byte-for-byte
what was in the slot, not a freshly-built copy of the shop's template item.

Persistence stores the `ItemStack` via `ItemStack#serializeAsBytes()`/`ItemStack.deserializeBytes`
into a `BLOB` column (`shops.item`, `shop_transactions.item`), replacing the earlier `TEXT` column
that only held `Material.name()`. `shop_transactions` also dropped its separate `amount` column —
the stored `ItemStack` already carries its own amount via `getAmount()`, so keeping both would have
been two sources of truth for the same value.

`ItemStack#serializeAsBytes()` throws on an empty stack, so `SqliteShopRepository` can't call it
directly on `shop.item()` for a sell-all shop (`SELL_ALL_SENTINEL` is an empty `Material.AIR`
stack) — `SqliteShopRepository.serializeItem`/`deserializeItem` special-case that with a
zero-length byte array marker instead of delegating straight to `serializeAsBytes`/
`deserializeBytes`. `ShopTransactionRecord.item` never needs this: it's always a real purchased
item, never the sentinel.

The migration that switches these columns to `BLOB` drops and recreates `shops`,
`shop_owners`, and `shop_transactions` (all emptied first, see above) rather than the usual
rename-copy-drop dance used elsewhere in `SchemaMigrator` — renaming a table SQLite is currently
referencing as an FK target auto-rewrites the *other* tables' FK clauses to the new name (so
`shops` → `shops_old` would leave `shop_owners`/`shop_transactions` referencing a table that gets
dropped a few statements later), so the parent is recreated before its children instead.

## `ShopMaintenanceTask`: two jobs sharing a cause

Both of the task's responsibilities exist because a shop's on-disk state and its real-world block
can drift apart:

- **Completion**: every `PENDING` transaction past its 60s window gets marked `COMPLETED`, and
  the **buyer** (not the shop owner — TP/EP are earned per Taler *spent*, confirmed against the
  spec's own "für einen ausgegebenen Taler" wording) is credited at the configured ratios, unless
  they're unregistered or paused.
- **Orphan scan (UC5)**: re-checks every registered shop's block(s) against the expected material
  and PDC tag; a mismatch means the block was destroyed or altered outside the plugin. The shop is
  hard-deleted, any pending purchase is force-refunded via the shared `ShopEconomy` helper, and
  every owner is notified — immediately if online, otherwise queued in `pending_notifications` and
  delivered on next join by `PendingNotificationListener`.

`pending_notifications` is deliberately shop-agnostic (just a UUID and a message string) so
Stage 2's invoice notifications can reuse it without a schema change.

## Closing always hard-deletes

Both `schließen` (owner-initiated close) and orphan cleanup hard-delete the `shops` row via
`ShopRepository.delete`. `shop_owners` and `shop_transactions` reference `shops(world, id)` with
`ON DELETE CASCADE`, so their rows for that shop are removed automatically — a single
`DELETE FROM shops` is enough. This means a shop's transaction history does not survive its
closing, but its ID becomes immediately reusable in that world, which is the point: an earlier
soft-delete design (a nullable `closed_at` column) kept history around but permanently reserved
the ID, which turned out to matter more in practice.
