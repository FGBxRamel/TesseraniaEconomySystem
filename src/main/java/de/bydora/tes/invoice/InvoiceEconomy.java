package de.bydora.tes.invoice;

import de.bydora.tes.config.TesConfig;
import de.bydora.tes.data.PlayerRecord;
import de.bydora.tes.data.PlayerRepository;
import de.bydora.tes.handelsbonus.HandelsbonusHolderRecord;
import de.bydora.tes.handelsbonus.HandelsbonusRepository;
import de.bydora.tes.handelsbonus.Staatskasse;
import de.bydora.tes.reward.RewardInventoryService;
import de.bydora.tes.util.DiamondEconomy;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;
import java.util.UUID;

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
     * Outcome of {@link #settle}: the {@link SettleResult} plus, on {@link SettleResult#SETTLED},
     * how much of the invoice's price (if any) was covered by the payer's Handelsbonus
     * (spec §3.2.1.1, Belohnung 4) rather than paid out of their own pocket. Zero for every other
     * result.
     */
    public record SettleOutcome(SettleResult result, int discountApplied) {
    }

    /**
     * Settles {@code invoiceId} on behalf of {@code clicker}: removes the invoice's price — minus
     * any active Handelsbonus discount (see {@link #withdrawHandelsbonusDiscount}) — in diamonds
     * from the clicker's real inventory, but credits the invoice creator's {@code invoice_balance}
     * with the full price, funding the gap from the Staatskasse exactly like
     * {@link de.bydora.tes.shop.ShopTradeListener} does for shop purchases. Marks the invoice
     * settled and awards the payer TP/EP only for the amount they actually paid themselves — a
     * Handelsbonus-funded portion earns no TP/EP (spec: "Für die 5 Dias gibt es keine EP / TP!"),
     * mirroring {@link de.bydora.tes.shop.ShopMaintenanceTask#creditBuyer}. Re-fetches the invoice
     * by id first and no-ops with {@link SettleResult#ALREADY_SETTLED} if it's no longer
     * {@link InvoiceState#OPEN} — cheap insurance against a stale GUI render, not a real race
     * condition fix (InvUI click handlers, like {@code InventoryClickEvent}, always run on the
     * single main server thread).
     */
    public static SettleOutcome settle(InvoiceRepository invoiceRepository, PlayerRepository playerRepository,
                                        HandelsbonusRepository handelsbonusRepository, TesConfig config, Player clicker, long invoiceId) {
        Optional<InvoiceRecord> maybeInvoice = invoiceRepository.findById(invoiceId);
        if (maybeInvoice.isEmpty() || maybeInvoice.get().state() != InvoiceState.OPEN) {
            return new SettleOutcome(SettleResult.ALREADY_SETTLED, 0);
        }
        InvoiceRecord invoice = maybeInvoice.get();
        int discount = withdrawHandelsbonusDiscount(handelsbonusRepository, config, clicker.getUniqueId(), invoice.price());
        int amountToPay = invoice.price() - discount;
        if (DiamondEconomy.countDiamonds(clicker) < amountToPay) {
            return new SettleOutcome(SettleResult.NOT_ENOUGH_DIAMONDS, 0);
        }
        DiamondEconomy.removeDiamonds(clicker, amountToPay);
        playerRepository.addInvoiceBalance(invoice.creatorUuid(), invoice.price());
        invoiceRepository.markSettled(invoice.id(), System.currentTimeMillis());
        creditPayer(playerRepository, config, clicker.getUniqueId(), amountToPay);
        return new SettleOutcome(SettleResult.SETTLED, discount);
    }

    /**
     * Applies the payer's Handelsbonus (spec §3.2.1.1, Belohnung 4), if any, to an invoice
     * settlement — same rules as {@link de.bydora.tes.shop.ShopTradeListener#withdrawHandelsbonusDiscount}:
     * withdraws up to their remaining discount from the configured Staatskasse chest (capped at
     * what's actually in there — see {@link Staatskasse#withdraw}) and debits exactly that much
     * from their tracked balance, so the two always stay in sync.
     */
    private static int withdrawHandelsbonusDiscount(HandelsbonusRepository handelsbonusRepository, TesConfig config, UUID payerUuid, int price) {
        Optional<HandelsbonusHolderRecord> holder = handelsbonusRepository.find(payerUuid);
        if (holder.isEmpty() || holder.get().discountRemaining() <= 0) {
            return 0;
        }
        int wanted = Math.min(price, holder.get().discountRemaining());
        int funded = Staatskasse.withdraw(config, wanted);
        if (funded > 0) {
            handelsbonusRepository.consumeDiscount(payerUuid, funded);
        }
        return funded;
    }

    private static void creditPayer(PlayerRepository playerRepository, TesConfig config, UUID payerUuid, int price) {
        Optional<PlayerRecord> payer = playerRepository.findByUuid(payerUuid);
        if (payer.isEmpty() || payer.get().paused()) {
            return;
        }
        playerRepository.addTreuepunkte(payerUuid, price * config.talerToTpRatio());
        playerRepository.addErfahrungspunkte(payerUuid, price * config.talerToEpRatio());
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
