package de.bydora.tes.shop;

import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.DoubleChest;
import org.bukkit.inventory.InventoryHolder;

import java.util.Optional;

/**
 * Resolves an {@link InventoryHolder} (as seen from an inventory event) back to the {@link Block}
 * it lives at, and from there to its {@link ShopRecord} if any — shared by every listener that
 * needs to recognize "is this inventory a shop".
 */
public final class ShopBlocks {

    private ShopBlocks() {
    }

    public static Block resolveBlock(InventoryHolder holder) {
        if (holder instanceof DoubleChest doubleChest) {
            return resolveBlock(doubleChest.getLeftSide());
        }
        return holder instanceof BlockState state ? state.getBlock() : null;
    }

    public static Optional<ShopRecord> resolveShop(ShopRegistry registry, InventoryHolder holder) {
        Block block = resolveBlock(holder);
        if (block == null) {
            return Optional.empty();
        }
        return registry.findByPosition(block.getWorld().getName(), new BlockPos(block.getX(), block.getY(), block.getZ()));
    }
}
