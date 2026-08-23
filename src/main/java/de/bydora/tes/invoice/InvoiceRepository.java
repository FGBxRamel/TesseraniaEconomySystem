package de.bydora.tes.invoice;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Persistent storage for invoices (spec §3.1.1.3).
 */
public interface InvoiceRepository {

    InvoiceRecord insert(UUID creatorUuid, UUID targetUuid, int price, String reason, long createdAt);

    /**
     * All open invoices where {@code targetUuid} is the payer, oldest first — backs
     * {@code /tes rechnung anzeigen}.
     */
    List<InvoiceRecord> findOpenByTarget(UUID targetUuid);

    Optional<InvoiceRecord> findById(long id);

    void markSettled(long id, long settledAt);
}
