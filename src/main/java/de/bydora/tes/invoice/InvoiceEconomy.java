package de.bydora.tes.invoice;

import de.bydora.tes.data.PlayerRepository;
import de.bydora.tes.reward.RewardInventoryService;
import de.bydora.tes.util.DiamondEconomy;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;

/**
 * Settle/cash-out logic for invoices (spec §3.1.1.3), mirroring {@link de.bydora.tes.shop.ShopEconomy}'s
 * static-helper shape.
 */
public final class InvoiceEconomy {

    private static final int DIAMOND_MAX_STACK_SIZE = 64;

    private InvoiceEconomy() {
    }

    public enum SettleResult {
        SETTLED,
        ALREADY_SETTLED,
        NOT_ENOUGH_DIAMONDS
    }

    /**
     * Settles {@code invoiceId} on behalf of {@code clicker}: removes the invoice's price in
     * diamonds from the clicker's real inventory and credits it to the invoice creator's
     * {@code invoice_balance}, then marks the invoice settled. Re-fetches the invoice by id
     * first and no-ops with {@link SettleResult#ALREADY_SETTLED} if it's no longer
     * {@link InvoiceState#OPEN} — cheap insurance against a stale GUI render, not a real race
     * condition fix (InvUI click handlers, like {@code InventoryClickEvent}, always run on the
     * single main server thread).
     */
    public static SettleResult settle(InvoiceRepository invoiceRepository, PlayerRepository playerRepository, Player clicker, long invoiceId) {
        Optional<InvoiceRecord> maybeInvoice = invoiceRepository.findById(invoiceId);
        if (maybeInvoice.isEmpty() || maybeInvoice.get().state() != InvoiceState.OPEN) {
            return SettleResult.ALREADY_SETTLED;
        }
        InvoiceRecord invoice = maybeInvoice.get();
        if (DiamondEconomy.countDiamonds(clicker) < invoice.price()) {
            return SettleResult.NOT_ENOUGH_DIAMONDS;
        }
        DiamondEconomy.removeDiamonds(clicker, invoice.price());
        playerRepository.addInvoiceBalance(invoice.creatorUuid(), invoice.price());
        invoiceRepository.markSettled(invoice.id(), System.currentTimeMillis());
        return SettleResult.SETTLED;
    }

    public enum RetractResult {
        RETRACTED,
        ALREADY_RESOLVED
    }

    /**
     * Retracts {@code invoiceId} on behalf of its creator (spec §3.1.1.3 v1.2's "Versendete
     * Rechnungen"): marks it {@link InvoiceState#RETRACTED} instead of deleting it, mirroring
     * {@link InvoiceState#SETTLED}'s soft-transition shape. No-ops with
     * {@link RetractResult#ALREADY_RESOLVED} if it's no longer {@link InvoiceState#OPEN} — same
     * stale-GUI-render insurance as {@link #settle}.
     */
    public static RetractResult retract(InvoiceRepository invoiceRepository, long invoiceId) {
        Optional<InvoiceRecord> maybeInvoice = invoiceRepository.findById(invoiceId);
        if (maybeInvoice.isEmpty() || maybeInvoice.get().state() != InvoiceState.OPEN) {
            return RetractResult.ALREADY_RESOLVED;
        }
        invoiceRepository.markRetracted(invoiceId);
        return RetractResult.RETRACTED;
    }

    public enum CashOutResult {
        CASHED_OUT,
        NOTHING_TO_CASH_OUT
    }

    /**
     * Cashes out {@code player}'s entire invoice balance into their Belohnungsinventar as one or
     * more Diamond {@link ItemStack}s, split into vanilla-max-stack-sized (64) chunks since the
     * reward inventory stores one row per stack.
     */
    public static CashOutResult cashOut(PlayerRepository playerRepository, RewardInventoryService rewardInventoryService, Player player) {
        int balance = playerRepository.cashOutInvoiceBalance(player.getUniqueId());
        if (balance <= 0) {
            return CashOutResult.NOTHING_TO_CASH_OUT;
        }
        int remaining = balance;
        while (remaining > 0) {
            int stackAmount = Math.min(remaining, DIAMOND_MAX_STACK_SIZE);
            rewardInventoryService.grant(player.getUniqueId(), new ItemStack(Material.DIAMOND, stackAmount));
            remaining -= stackAmount;
        }
        return CashOutResult.CASHED_OUT;
    }
}
