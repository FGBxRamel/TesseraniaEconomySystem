package de.bydora.tes.shop;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Container;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Optional;

/**
 * Shared economics helpers used both by {@code /shop schließen} and by
 * {@link ShopMaintenanceTask}'s orphan cleanup — both need to force-refund any purchase still
 * within its 60-second window when a shop unexpectedly stops existing.
 */
public final class ShopEconomy {

    private ShopEconomy() {
    }

    public static Optional<Inventory> resolveInventory(World world, ShopRecord shop) {
        Block block = world.getBlockAt(shop.position().x(), shop.position().y(), shop.position().z());
        BlockState state = block.getState();
        return state instanceof Container container ? Optional.of(container.getInventory()) : Optional.empty();
    }

    /**
     * Force-refunds every still-{@code PENDING} transaction for a shop that is being closed or
     * was found orphaned: returns the original item to the shop's slot (if the block is still
     * resolvable) and the paid diamonds to the buyer (if online), then marks the transaction
     * {@code REFUNDED}. A shop shouldn't vanish while a buyer's refund right is still open.
     */
    public static void forceRefundPending(ShopTransactionRepository transactionRepository, ShopRecord shop, World world) {
        List<ShopTransactionRecord> pending = transactionRepository.findPendingForShop(shop.world(), shop.id());
        if (pending.isEmpty()) {
            return;
        }
        Optional<Inventory> inventory = world == null ? Optional.empty() : resolveInventory(world, shop);
        for (ShopTransactionRecord transaction : pending) {
            inventory.ifPresent(inv -> {
                if (transaction.slot() < inv.getSize()) {
                    inv.setItem(transaction.slot(), transaction.item().clone());
                }
            });
            Player buyer = Bukkit.getPlayer(transaction.buyerUuid());
            if (buyer != null) {
                buyer.getInventory().addItem(new ItemStack(Material.DIAMOND, transaction.price()));
            }
            transactionRepository.markRefunded(transaction.id(), System.currentTimeMillis());
        }
    }
}
