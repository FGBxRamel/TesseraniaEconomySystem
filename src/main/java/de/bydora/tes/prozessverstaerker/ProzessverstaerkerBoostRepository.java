package de.bydora.tes.prozessverstaerker;

import java.util.List;

/**
 * Persists which blocks are currently boosted by the Prozessverstärker (spec §3.2.1.1,
 * Belohnung 1), surviving server restarts.
 */
public interface ProzessverstaerkerBoostRepository {

    /**
     * Applies (or extends) a boost at the given position: a fresh/expired boost starts a new
     * {@code durationMillis}-long window from {@code now}; an already-active one has
     * {@code durationMillis} added on top of its remaining time (spec: "Effekt lässt sich
     * addieren, sollte ein Block mehrfach geboostet werden"). Returns the resulting expiry.
     */
    long extend(String world, int x, int y, int z, BoostKind kind, long durationMillis, long now);

    /**
     * Every currently-tracked boost, expired or not — callers decide what to do with expired
     * ones (see {@link ProzessverstaerkerBoostRecord#isExpired}).
     */
    List<ProzessverstaerkerBoostRecord> findAll();

    void delete(String world, int x, int y, int z);
}
