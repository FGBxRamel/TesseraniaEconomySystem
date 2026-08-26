package de.bydora.tes.shop;

import de.bydora.tes.config.TesConfig;
import de.bydora.tes.data.PlayerRecord;
import de.bydora.tes.data.PlayerRepository;
import de.bydora.tes.util.Messages;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Periodic background task with two responsibilities that both revolve around a shop's block
 * potentially no longer matching what the database expects:
 * <ol>
 *     <li>Completing purchases whose 60-second refund window has elapsed, crediting the buyer's
 *     TP/EP (spec §3.1.1.1 UC4, §1.4 ratios).</li>
 *     <li>Detecting orphaned shops — the underlying block was destroyed or altered outside the
 *     plugin — deleting them and notifying their owners (UC5).</li>
 * </ol>
 */
public final class ShopMaintenanceTask extends BukkitRunnable {

    private static final long REFUND_WINDOW_MILLIS = 60_000L;

    private final Plugin plugin;
    private final ShopRegistry shopRegistry;
    private final ShopRepository shopRepository;
    private final ShopTransactionRepository transactionRepository;
    private final PlayerRepository playerRepository;
    private final PendingNotificationRepository pendingNotificationRepository;
    private final TesConfig config;

    public ShopMaintenanceTask(Plugin plugin, ShopRegistry shopRegistry, ShopRepository shopRepository,
                                ShopTransactionRepository transactionRepository, PlayerRepository playerRepository,
                                PendingNotificationRepository pendingNotificationRepository, TesConfig config) {
        this.plugin = plugin;
        this.shopRegistry = shopRegistry;
        this.shopRepository = shopRepository;
        this.transactionRepository = transactionRepository;
        this.playerRepository = playerRepository;
        this.pendingNotificationRepository = pendingNotificationRepository;
        this.config = config;
    }

    @Override
    public void run() {
        completePendingTransactions();
        scanForOrphans();
    }

    private void completePendingTransactions() {
        long cutoff = System.currentTimeMillis() - REFUND_WINDOW_MILLIS;
        for (ShopTransactionRecord transaction : transactionRepository.findPendingDueBefore(cutoff)) {
            transactionRepository.markCompleted(transaction.id(), System.currentTimeMillis());
            creditBuyer(transaction);
        }
    }

    private void creditBuyer(ShopTransactionRecord transaction) {
        Optional<PlayerRecord> buyer = playerRepository.findByUuid(transaction.buyerUuid());
        if (buyer.isEmpty() || buyer.get().paused()) {
            return;
        }
        // Handelsbonus-funded diamonds don't earn TP/EP (spec: "Für die 5 Dias gibt es keine EP / TP!").
        int creditablePrice = transaction.buyerPaid();
        playerRepository.addTreuepunkte(transaction.buyerUuid(), creditablePrice * config.talerToTpRatio());
        playerRepository.addErfahrungspunkte(transaction.buyerUuid(), creditablePrice * config.talerToEpRatio());
    }

    private void scanForOrphans() {
        for (ShopRecord shop : List.copyOf(shopRegistry.all())) {
            World world = Bukkit.getWorld(shop.world());
            if (world == null) {
                continue;
            }
            if (!blockStillMatches(world, shop)) {
                handleOrphan(world, shop);
            }
        }
    }

    private boolean blockStillMatches(World world, ShopRecord shop) {
        return blockMatches(world, shop.position(), shop) && (shop.secondaryPosition() == null || blockMatches(world, shop.secondaryPosition(), shop));
    }

    private boolean blockMatches(World world, BlockPos position, ShopRecord shop) {
        Block block = world.getBlockAt(position.x(), position.y(), position.z());
        if (block.getType() != shop.containerType()) {
            return false;
        }
        return ShopConversion.readShopTag(plugin, block.getState())
                .map(tag -> tag.equals(shop.world() + ":" + shop.id()))
                .orElse(false);
    }

    private void handleOrphan(World world, ShopRecord shop) {
        ShopEconomy.forceRefundPending(transactionRepository, shop, world);
        shopRepository.delete(shop.id());
        shopRegistry.unregister(shop.id());
        notifyOwners(shop);
    }

    private void notifyOwners(ShopRecord shop) {
        String text = Messages.shopOrphanedText(shop.name(), shop.id());
        for (UUID owner : shop.owners()) {
            Player player = Bukkit.getPlayer(owner);
            if (player != null) {
                player.sendMessage(Messages.shopOrphaned(shop.name(), shop.id()));
            } else {
                pendingNotificationRepository.enqueue(owner, text);
            }
        }
    }
}
