package de.bydora.tes.handelsbonus;

import java.util.Optional;
import java.util.UUID;

/**
 * Persists who currently holds a Handelsbonus (spec §3.2.1.1, Belohnung 4).
 */
public interface HandelsbonusRepository {

    Optional<HandelsbonusHolderRecord> find(UUID uuid);

    /**
     * How many players are currently within their post-trigger cooldown — the spec's
     * "maximal durch 2 Spieler gleichzeitig auslösen" cap on new activations.
     */
    int countOnCooldown(long now);

    /**
     * Starts (or restarts) {@code uuid}'s Handelsbonus: a fresh {@code discountRemaining} and a
     * new {@code cooldownUntil}, replacing whatever they had before.
     */
    void activate(UUID uuid, int discountRemaining, long cooldownUntil);

    /**
     * Spends up to {@code amount} of {@code uuid}'s remaining discount, atomically. Returns how
     * much was actually applied (0 if {@code uuid} has no discount left, or never triggered
     * Handelsbonus at all).
     */
    int consumeDiscount(UUID uuid, int amount);

    /**
     * Admin override (no counterpart in the spec): clears {@code uuid}'s post-trigger cooldown so
     * they can immediately trigger Handelsbonus again, without touching any unused discount
     * balance they still hold. Returns whether an active cooldown actually existed to clear
     * (false if {@code uuid} never triggered Handelsbonus, or their cooldown had already expired).
     */
    boolean resetCooldown(UUID uuid, long now);
}
