package de.bydora.tes.treueshop;

import de.bydora.tes.TesseraniaEconomySystem;
import de.bydora.tes.data.PlayerRepository;
import org.bukkit.entity.Player;

/**
 * Orchestrates a Treueshop reward purchase (spec §3.2): atomically spends the reward's
 * Treuepunkte cost via {@link PlayerRepository#spendTreuepunkte}, then applies its effect.
 * Callers must already have confirmed the purchasing player is registered and not paused —
 * checked once at {@code /punkte} entry, the same gating point used for the whole shop, not
 * re-checked per click.
 */
public final class TreueshopRewardService {

    public enum PurchaseResult {
        PURCHASED,
        INSUFFICIENT_TP
    }

    private TreueshopRewardService() {
    }

    public static PurchaseResult purchase(TesseraniaEconomySystem plugin, Player player, int cost, Runnable effect) {
        PlayerRepository.SpendResult spend = plugin.playerRepository().spendTreuepunkte(player.getUniqueId(), cost);
        if (spend == PlayerRepository.SpendResult.INSUFFICIENT) {
            return PurchaseResult.INSUFFICIENT_TP;
        }
        effect.run();
        return PurchaseResult.PURCHASED;
    }
}
