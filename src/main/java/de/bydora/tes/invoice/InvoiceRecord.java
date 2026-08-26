package de.bydora.tes.invoice;

import java.util.UUID;

/**
 * A single invoice (spec §3.1.1.3) — created by one player against another for a service or
 * flea-market transaction, settled by the target paying the creator's virtual balance.
 *
 * @param id          auto-generated primary key
 * @param creatorUuid the player who created (is owed by) this invoice
 * @param targetUuid  the player who owes the price — need not be registered
 * @param price       diamonds owed
 * @param reason      free-text reason, at most 50 chars (spec §3.1.1.3's only numeric limit)
 * @param state       current lifecycle state
 * @param createdAt   epoch millis of creation
 * @param settledAt   epoch millis the invoice was settled, or {@code null} while still open
 */
public record InvoiceRecord(
        long id,
        UUID creatorUuid,
        UUID targetUuid,
        int price,
        String reason,
        InvoiceState state,
        long createdAt,
        Long settledAt
) {
}
