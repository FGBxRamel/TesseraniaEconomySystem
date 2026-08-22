package de.bydora.tes.shop;

import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Persistent storage for shop purchases (UC4).
 */
public interface ShopTransactionRepository {

    ShopTransactionRecord insertPending(String shopWorld, String shopId, int slot, UUID buyer, ItemStack item, int price, long purchasedAt);

    /**
     * The buyer's own still-{@code PENDING} transaction occupying {@code slot}, if any — used to
     * recognize a refund click.
     */
    Optional<ShopTransactionRecord> findPendingBySlot(String shopWorld, String shopId, int slot, UUID buyer);

    /**
     * Whichever still-{@code PENDING} transaction occupies {@code slot}, regardless of buyer —
     * used by the owner-side withdraw cooldown check.
     */
    Optional<ShopTransactionRecord> findPendingBySlot(String shopWorld, String shopId, int slot);

    /**
     * All {@code PENDING} transactions for a shop — force-refunded when the shop is closed or
     * found orphaned.
     */
    List<ShopTransactionRecord> findPendingForShop(String shopWorld, String shopId);

    /**
     * {@code PENDING} transactions whose 60-second refund window has elapsed by {@code cutoff}
     * (epoch millis) — due for completion and TP/EP accrual.
     */
    List<ShopTransactionRecord> findPendingDueBefore(long cutoff);

    void markRefunded(long id, long resolvedAt);

    void markCompleted(long id, long resolvedAt);
}
