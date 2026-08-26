package de.bydora.tes.treueshop;

import de.bydora.tes.TesseraniaEconomySystem;
import de.bydora.tes.handelsbonus.HandelsbonusRepository;
import de.bydora.tes.util.Messages;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import xyz.xenondevs.invui.item.Item;
import xyz.xenondevs.invui.item.ItemBuilder;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/**
 * Belohnung 4, "Handelsbonus" (spec §3.2.1.1) — the one Treueshop reward whose main-interface
 * button needs handling different from every other reward's:
 * <ul>
 *     <li>Purchasing it is gated on a precondition beyond cost — at most 2 players may hold an
 *     active Handelsbonus at once, and a player already holding one can't re-trigger it — so it
 *     can't go through {@link TreueshopGui}'s generic "always spend, then apply" dispatch.</li>
 *     <li>Its icon is stateful: once both concurrent slots are taken, the button swaps to a plain
 *     re-lored {@code DIAMOND} ("ausgegrauter Diamant") rather than its normal cost/lore — a
 *     reference-build-confirmed icon choice, not a custom-model-data texture (see
 *     {@code docs/treueshop-system.md}).</li>
 * </ul>
 * What holding a Handelsbonus actually does to shop purchases lives in
 * {@link de.bydora.tes.shop.ShopTradeListener}, not here — this class only covers the
 * purchase/render side of the Treueshop button itself.
 */
final class TreueshopHandelsbonus {

    private TreueshopHandelsbonus() {
    }

    static Item item(TesseraniaEconomySystem plugin, TreueshopReward reward) {
        long now = System.currentTimeMillis();
        boolean full = plugin.handelsbonusRepository().countOnCooldown(now) >= 2;
        return Item.builder()
                .setItemProvider(full ? exhaustedIcon() : TreueshopComponents.rewardIcon(plugin, reward))
                .addClickHandler((item, click) -> purchase(plugin, click.player(), reward))
                .build();
    }

    private static void purchase(TesseraniaEconomySystem plugin, Player player, TreueshopReward reward) {
        HandelsbonusRepository repository = plugin.handelsbonusRepository();
        long now = System.currentTimeMillis();

        if (repository.find(player.getUniqueId()).map(holder -> holder.cooldownActive(now)).orElse(false)) {
            player.sendMessage(Messages.handelsbonusAlreadyActive());
            return;
        }
        if (repository.countOnCooldown(now) >= 2) {
            player.sendMessage(Messages.handelsbonusSlotsFull());
            return;
        }

        int cost = plugin.tesConfig().treueshopRewardCost(reward.costConfigId(), reward.defaultCost());
        TreueshopRewardService.PurchaseResult result = TreueshopRewardService.purchase(plugin, player, cost,
                () -> activate(plugin, player, now));
        if (result == TreueshopRewardService.PurchaseResult.INSUFFICIENT_TP) {
            player.sendMessage(Messages.treueshopInsufficientTp());
            return;
        }
        player.sendMessage(Messages.treueshopRewardPurchased("Handelsbonus"));
        TreueshopGui.open(plugin, player);
    }

    private static void activate(TesseraniaEconomySystem plugin, Player player, long now) {
        int discount = plugin.tesConfig().treueshopHandelsbonusDiscountDiamonds();
        int minDays = plugin.tesConfig().treueshopHandelsbonusCooldownMinDays();
        int maxDays = plugin.tesConfig().treueshopHandelsbonusCooldownMaxDays();
        long cooldownDays = ThreadLocalRandom.current().nextLong(minDays, maxDays + 1);
        long cooldownUntil = now + TimeUnit.DAYS.toMillis(cooldownDays);
        plugin.handelsbonusRepository().activate(player.getUniqueId(), discount, cooldownUntil);
    }

    /**
     * The "ausgegrauter Diamant" shown once both concurrent Handelsbonus slots are taken —
     * transcribed verbatim from {@code GUI_References/Greyed_Diamond.txt}.
     */
    private static ItemBuilder exhaustedIcon() {
        return new ItemBuilder(new ItemStack(Material.DIAMOND))
                .setName(Component.text("Handelsbonus", NamedTextColor.WHITE, TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false))
                .addLoreLines(List.of(
                        Component.text("Belohnung aufgebraucht", NamedTextColor.RED).decoration(TextDecoration.ITALIC, false),
                        Component.empty(),
                        Component.text("Die Belohnung wird in spätestens", NamedTextColor.GRAY, TextDecoration.ITALIC),
                        Component.text("3-4 Wochen erneut freigeschaltet.", NamedTextColor.GRAY, TextDecoration.ITALIC),
                        Component.empty(),
                        Component.text("Schaue gern später wieder vorbei.", NamedTextColor.GRAY, TextDecoration.ITALIC)));
    }
}
