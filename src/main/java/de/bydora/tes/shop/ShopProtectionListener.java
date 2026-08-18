package de.bydora.tes.shop;

import de.bydora.tes.util.Messages;
import org.bukkit.GameMode;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.DoubleChest;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

/**
 * Enforces that a converted shop container is indestructible in survival/adventure (spec
 * §3.1.1.1: "im Survivalmodus unzerstörbar") and cannot be drained by hoppers before a pending
 * purchase's 60-second refund window elapses — the latter isn't spelled out in the spec, but
 * without it a hopper under a shop chest could extract a buyer's still-refundable diamonds
 * before the refund right expires.
 */
public final class ShopProtectionListener implements Listener {

    private final ShopRegistry registry;

    public ShopProtectionListener(ShopRegistry registry) {
        this.registry = registry;
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        if (!isShopBlock(event.getBlock())) {
            return;
        }
        GameMode gameMode = event.getPlayer().getGameMode();
        if (gameMode == GameMode.SURVIVAL || gameMode == GameMode.ADVENTURE) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(Messages.shopBlockProtected());
        }
    }

    @EventHandler
    public void onBlockExplode(BlockExplodeEvent event) {
        event.blockList().removeIf(this::isShopBlock);
    }

    @EventHandler
    public void onEntityExplode(EntityExplodeEvent event) {
        event.blockList().removeIf(this::isShopBlock);
    }

    @EventHandler
    public void onPistonExtend(BlockPistonExtendEvent event) {
        if (event.getBlocks().stream().anyMatch(this::isShopBlock)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPistonRetract(BlockPistonRetractEvent event) {
        if (event.getBlocks().stream().anyMatch(this::isShopBlock)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryMoveItem(InventoryMoveItemEvent event) {
        if (isShopInventory(event.getSource()) || isShopInventory(event.getDestination())) {
            event.setCancelled(true);
        }
    }

    private boolean isShopBlock(Block block) {
        return registry.findByPosition(block.getWorld().getName(), new BlockPos(block.getX(), block.getY(), block.getZ())).isPresent();
    }

    private boolean isShopInventory(Inventory inventory) {
        InventoryHolder holder = inventory.getHolder();
        if (holder instanceof DoubleChest doubleChest) {
            return isShopHolder(doubleChest.getLeftSide()) || isShopHolder(doubleChest.getRightSide());
        }
        return isShopHolder(holder);
    }

    private boolean isShopHolder(InventoryHolder holder) {
        return holder instanceof BlockState state && isShopBlock(state.getBlock());
    }
}
