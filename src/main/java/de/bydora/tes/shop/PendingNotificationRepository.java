package de.bydora.tes.shop;

import java.util.List;
import java.util.UUID;

/**
 * A generic "tell this player something the next time they're active" queue. Introduced for
 * UC5's one-time orphaned-shop notice (delivered immediately if the owner is online, otherwise
 * queued for their next login), kept deliberately shop-agnostic so Stage 2's invoice
 * notifications can reuse it.
 */
public interface PendingNotificationRepository {

    void enqueue(UUID uuid, String message);

    /**
     * Returns and permanently removes all messages queued for {@code uuid}, in the order they
     * were enqueued.
     */
    List<String> drain(UUID uuid);
}
