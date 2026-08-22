package de.bydora.tes.shop;

/**
 * Lifecycle of a shop purchase (UC4): starts {@link #PENDING} for the 60-second refund window,
 * then transitions exactly once to either {@link #REFUNDED} (buyer cancelled in time) or
 * {@link #COMPLETED} (window elapsed, TP/EP credited to the buyer).
 */
public enum TransactionState {
    PENDING,
    REFUNDED,
    COMPLETED
}
