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
     * Adds {@code delta} (which may be negative) to the player's Treuepunkte, clamped at 0.
     */
    void addTreuepunkte(UUID uuid, int delta);

    /**
     * Sets the player's Treuepunkte to {@code value}, clamped at 0.
     */
    void setTreuepunkte(UUID uuid, int value);

    /**
     * Adds {@code delta} (which may be negative) to the player's Erfahrungspunkte, clamped at 0.
     */
    void addErfahrungspunkte(UUID uuid, int delta);

    /**
     * Sets the player's Erfahrungspunkte to {@code value}, clamped at 0.
     */
    void setErfahrungspunkte(UUID uuid, int value);

    /**
     * Permanently and irrecoverably deletes the player's record and all associated TES data.
     */
    void delete(UUID uuid);

    /**
     * Adds {@code delta} (which may be negative) to the player's invoice balance (spec §3.1.1.3
     * — diamonds owed from settled invoices they created), clamped at 0.
     */
    void addInvoiceBalance(UUID uuid, int delta);

    /**
     * Atomically reads the player's current invoice balance and resets it to 0, returning the
     * pre-reset value — the amount to hand out as diamonds on cash-out.
     */
    int cashOutInvoiceBalance(UUID uuid);

    enum SpendResult {
        SPENT,
        INSUFFICIENT
    }

    /**
     * Atomically deducts {@code cost} Treuepunkte from {@code uuid} if they have enough,
     * otherwise leaves their balance untouched (spec §3.2: every loyalty-shop reward purchase
     * must go through this rather than {@link #addTreuepunkte} with a negative delta, which
     * would let two concurrent purchases both succeed against the same balance).
     */
    SpendResult spendTreuepunkte(UUID uuid, int cost);

    enum TransferResult {
        TRANSFERRED,
        INSUFFICIENT
    }

    /**
     * Atomically moves {@code amount} Treuepunkte from {@code from} to {@code to} if {@code from}
     * has enough, otherwise leaves both balances untouched ({@code /treuepunkte übertragen}).
     */
    TransferResult transferTreuepunkte(UUID from, UUID to, int amount);
}
