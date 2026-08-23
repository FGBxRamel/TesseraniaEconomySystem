package de.bydora.tes.invoice;

/**
 * Lifecycle of an invoice (spec §3.1.1.3): starts {@link #OPEN}, transitions exactly once to
 * {@link #SETTLED} when the target pays it via {@code /tes rechnung anzeigen}. Unlike Stage 1
 * shop purchases, invoices have no refund/cancellation window.
 */
public enum InvoiceState {
    OPEN,
    SETTLED
}
