package de.bydora.tes.shop;

import org.bukkit.inventory.ItemStack;

import java.util.UUID;

/**
 * One purchase made at a shop (UC4).
 *
 * @param id           auto-generated primary key
 * @param shopWorld    the shop's world
 * @param shopId       the shop's id
 * @param slot         the shop inventory slot the purchase occupies (holds the buyer's diamonds while {@link TransactionState#PENDING})
 * @param buyerUuid    the purchasing player
 * @param item         the exact stack that was sold, including its amount, enchantments, potion
 *                     data, display name, etc. — restored as-is to the buyer or the shop's slot on refund
 * @param price             diamonds paid; the shop owner's full earning, and the amount restored
 *                          to the shop slot on refund
 * @param staatskasseFunded how much of {@code price} was covered by the Handelsbonus Staatskasse
 *                          (spec §3.2.1.1, Belohnung 4) rather than the buyer - 0 unless the buyer
 *                          held an active Handelsbonus discount at purchase time. Subtracted from
 *                          {@code price} both for TP/EP accrual and for what the buyer gets back
 *                          on refund, so neither the buyer nor the state can profit from a
 *                          cancelled purchase; the Staatskasse's own contribution is treated as
 *                          spent regardless (not restored on refund - see
 *                          {@code docs/treueshop-system.md}).
 * @param state             current lifecycle state
 * @param purchasedAt       epoch millis of purchase; the 60s refund window is measured from here
 * @param resolvedAt        epoch millis the transaction left {@link TransactionState#PENDING}, or {@code null} while still pending
 */
public record ShopTransactionRecord(
        long id,
        String shopWorld,
        String shopId,
        int slot,
        UUID buyerUuid,
        ItemStack item,
        int price,
        int staatskasseFunded,
        TransactionState state,
        long purchasedAt,
        Long resolvedAt
) {

    /**
     * Defensively clones {@link #item} so a {@code ShopTransactionRecord} can never be mutated
     * through an {@link ItemStack} reference it was built from.
     */
    public ShopTransactionRecord {
        item = item.clone();
    }

    /**
     * What the buyer actually paid out of pocket — {@link #price} minus
     * {@link #staatskasseFunded} — used for TP/EP accrual and refund amounts.
     */
    public int buyerPaid() {
        return price - staatskasseFunded;
    }
}
