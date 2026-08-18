package de.bydora.tes.command.confirm;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Generic re-confirmation flow for destructive admin actions: a first invocation creates a
 * short-lived token tied to the issuing actor and an arbitrary payload; a second invocation
 * supplying that token within the TTL consumes it. Kept generic (not tied to any one command)
 * since this "confirm via chat button" pattern is expected to recur for other destructive
 * actions in later stages.
 *
 * @param <T> the type of payload being confirmed, e.g. the UUID of a player about to be removed
 */
public final class ConfirmationManager<T> {

    private record Pending<T>(String token, T payload, Instant expiresAt) {
    }

    private final Duration ttl;
    private final Map<UUID, Pending<T>> pending = new ConcurrentHashMap<>();

    public ConfirmationManager(Duration ttl) {
        this.ttl = ttl;
    }

    /**
     * Registers a new pending confirmation for {@code actor}, replacing any previous one, and
     * returns the token that must be supplied to {@link #consume(UUID, String)} within the TTL.
     */
    public String create(UUID actor, T payload) {
        String token = Long.toHexString(ThreadLocalRandom.current().nextLong()).substring(0, 6);
        pending.put(actor, new Pending<>(token, payload, Instant.now().plus(ttl)));
        return token;
    }

    /**
     * Consumes the pending confirmation for {@code actor} if {@code token} matches and it has
     * not yet expired. Either way, the pending entry is removed.
     */
    public Optional<T> consume(UUID actor, String token) {
        Pending<T> entry = pending.remove(actor);
        if (entry == null || Instant.now().isAfter(entry.expiresAt())) {
            return Optional.empty();
        }
        return entry.token().equals(token) ? Optional.of(entry.payload()) : Optional.empty();
    }

    public Duration ttl() {
        return ttl;
    }
}
