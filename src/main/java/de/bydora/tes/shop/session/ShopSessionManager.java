package de.bydora.tes.shop.session;

import de.bydora.tes.shop.ShopRecord;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks each player's in-progress shop creation/edit conversation. Only one session per player
 * at a time; starting a new one replaces any previous one.
 */
public final class ShopSessionManager {

    private final Map<UUID, ShopSession> sessions = new ConcurrentHashMap<>();
    private final Duration timeout;

    public ShopSessionManager(Duration timeout) {
        this.timeout = timeout;
    }

    public ShopSession startCreate(UUID actor, String world) {
        ShopSession session = ShopSession.createNew(actor, world, timeout);
        sessions.put(actor, session);
        return session;
    }

    public ShopSession startEdit(UUID actor, ShopRecord existing) {
        ShopSession session = ShopSession.editing(actor, existing, timeout);
        sessions.put(actor, session);
        return session;
    }

    /**
     * The actor's active session, if any and not yet expired. An expired session is discarded.
     */
    public Optional<ShopSession> active(UUID actor) {
        ShopSession session = sessions.get(actor);
        if (session == null) {
            return Optional.empty();
        }
        if (session.isExpired()) {
            sessions.remove(actor);
            return Optional.empty();
        }
        return Optional.of(session);
    }

    public void cancel(UUID actor) {
        sessions.remove(actor);
    }
}
