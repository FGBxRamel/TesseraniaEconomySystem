package de.bydora.tes.data;

import java.util.Optional;
import java.util.UUID;

/**
 * Persistent storage for registered players' TES data.
 */
public interface PlayerRepository {

    Optional<PlayerRecord> findByUuid(UUID uuid);

    Optional<PlayerRecord> findByUsername(String username);

    boolean isRegistered(UUID uuid);

    /**
     * Registers a new player with all counters at zero and {@code paused = false}.
     */
    PlayerRecord register(UUID uuid, String username);

    void setPaused(UUID uuid, boolean paused);

    /**
     * Permanently and irrecoverably deletes the player's record and all associated TES data.
     */
    void delete(UUID uuid);
}
