package de.bydora.tes.shop;

import org.bukkit.Material;

import java.util.UUID;

/**
 * One purchase made at a shop (UC4).
 *
 * @param id           auto-generated primary key
 * @param shopWorld    the shop's world
 * @param shopId       the shop's id
 * @param slot         the shop inventory slot the purchase occupies (holds the buyer's diamonds while {@link TransactionState#PENDING})
 * @param buyerUuid    the purchasing player
 * @param item         the item that was sold
 * @param amount       how many of {@code item} were sold
 * @param price        diamonds paid; drives the buyer's TP/EP accrual on completion
 * @param state        current lifecycle state
 * @param purchasedAt  epoch millis of purchase; the 60s refund window is measured from here
 * @param resolvedAt   epoch millis the transaction left {@link TransactionState#PENDING}, or {@code null} while still pending
 */
public record ShopTransactionRecord(
        long id,
        String shopWorld,
        String shopId,
        int slot,
        UUID buyerUuid,
        Material item,
        int amount,
        int price,
        TransactionState state,
        long purchasedAt,
        Long resolvedAt
) {
}
