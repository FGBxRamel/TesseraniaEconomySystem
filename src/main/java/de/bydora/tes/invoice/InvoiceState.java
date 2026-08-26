package de.bydora.tes.invoice;

/**
 * Lifecycle of an invoice (spec §3.1.1.3): starts {@link #OPEN}, then transitions exactly once,
 * either to {@link #SETTLED} when the target pays it via {@code /rechnung anzeigen}, or to
 * {@link #RETRACTED} when the creator withdraws it via "Versendete Rechnungen" — unlike Stage 1
 * shop purchases' time-limited buyer-side refund window, retraction has no time limit and only
 * the creator can trigger it.
 */
public enum InvoiceState {
    OPEN,
    SETTLED,
    RETRACTED
}
