package de.bydora.tes.shop;

import de.bydora.tes.util.Messages;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;

/**
 * Handles UC4 (the purchase flow) directly on a shop's own container inventory: a single
 * left-click by a non-owner either buys the clicked slot's stock (diamonds ⇄ item, in place) or,
 * if it's the buyer's own still-{@link TransactionState#PENDING} diamonds, refunds it. Owners may
 * withdraw diamonds once their 60-second window has elapsed and restock with the configured item;
 * everything else on the shop's side of the inventory is blocked to keep the interaction to the
 * single-slot-click model the spec describes.
 */
public final class ShopTradeListener implements Listener {

    private final ShopRegistry shopRegistry;
    private final ShopTransactionRepository transactionRepository;

    public ShopTradeListener(ShopRegistry shopRegistry, ShopTransactionRepository transactionRepository) {
        this.shopRegistry = shopRegistry;
        this.transactionRepository = transactionRepository;
    }

    @EventHandler(ignoreCancelled = true)
    public void onClick(InventoryClickEvent event) {
        Inventory topInventory = event.getView().getTopInventory();
        Optional<ShopRecord> maybeShop = ShopBlocks.resolveShop(shopRegistry, topInventory.getHolder());
        if (maybeShop.isEmpty() || !(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        ShopRecord shop = maybeShop.get();

        if (event.getClick().isShiftClick() || event.getAction() == InventoryAction.COLLECT_TO_CURSOR) {
            event.setCancelled(true);
            return;
        }
        if (event.getClickedInventory() == null || !event.getClickedInventory().equals(topInventory)) {
            return;
        }

        if (shop.isOwner(player.getUniqueId())) {
            handleOwnerClick(event, shop, player);
        } else {
            handleBuyerClick(event, shop, player);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onDrag(InventoryDragEvent event) {
        Inventory topInventory = event.getView().getTopInventory();
        if (ShopBlocks.resolveShop(shopRegistry, topInventory.getHolder()).isEmpty()) {
            return;
        }
        if (event.getRawSlots().stream().anyMatch(slot -> slot < topInventory.getSize())) {
            event.setCancelled(true);
        }
    }

    private void handleBuyerClick(InventoryClickEvent event, ShopRecord shop, Player buyer) {
        event.setCancelled(true);
        if (event.getClick() != ClickType.LEFT) {
            return;
        }
        int slot = event.getSlot();
        Inventory shopInventory = event.getClickedInventory();
        ItemStack clicked = shopInventory.getItem(slot);
        if (clicked == null || clicked.getType() == Material.AIR) {
            return;
        }

        if (clicked.getType() == Material.DIAMOND) {
            transactionRepository.findPendingBySlot(shop.world(), shop.id(), slot, buyer.getUniqueId())
                    .ifPresent(pending -> refund(shopInventory, pending, buyer));
            return;
        }

        if (clicked.getType() != shop.item()) {
            return;
        }
        int price = shop.price();
        if (countDiamonds(buyer) < price) {
            buyer.sendMessage(Messages.notEnoughTaler());
            return;
        }

        int amount = clicked.getAmount();
        Material item = clicked.getType();
        removeDiamonds(buyer, price);
        shopInventory.setItem(slot, new ItemStack(Material.DIAMOND, price));
        giveItem(buyer, new ItemStack(item, amount));
        transactionRepository.insertPending(shop.world(), shop.id(), slot, buyer.getUniqueId(), item, amount, price, System.currentTimeMillis());
        buyer.setCooldown(Material.DIAMOND, 1200);
    }

    private void handleOwnerClick(InventoryClickEvent event, ShopRecord shop, Player owner) {
        int slot = event.getSlot();
        ItemStack clicked = event.getClickedInventory().getItem(slot);

        if (clicked != null && clicked.getType() == Material.DIAMOND) {
            if (transactionRepository.findPendingBySlot(shop.world(), shop.id(), slot).isPresent()) {
                event.setCancelled(true);
                owner.sendMessage(Messages.shopWithdrawCooldownActive());
            }
            return;
        }

        ItemStack cursor = event.getCursor();
        boolean placingItem = cursor.getType() != Material.AIR && switch (event.getAction()) {
            case PLACE_ALL, PLACE_SOME, PLACE_ONE, SWAP_WITH_CURSOR -> true;
            default -> false;
        };
        if (placingItem && cursor.getType() != shop.item()) {
            event.setCancelled(true);
            owner.sendMessage(Messages.shopWrongItemForShop(shop.item().name()));
        }
    }

    private void refund(Inventory shopInventory, ShopTransactionRecord transaction, Player buyer) {
        shopInventory.setItem(transaction.slot(), new ItemStack(transaction.item(), transaction.amount()));
        giveItem(buyer, new ItemStack(Material.DIAMOND, transaction.price()));
        transactionRepository.markRefunded(transaction.id(), System.currentTimeMillis());
    }

    private static int countDiamonds(Player player) {
        int total = 0;
        for (ItemStack stack : player.getInventory().getStorageContents()) {
            if (stack != null && stack.getType() == Material.DIAMOND) {
                total += stack.getAmount();
            }
        }
        return total;
    }

    private static void removeDiamonds(Player player, int amount) {
        ItemStack[] contents = player.getInventory().getStorageContents();
        int remaining = amount;
        for (int i = 0; i < contents.length && remaining > 0; i++) {
            ItemStack stack = contents[i];
            if (stack == null || stack.getType() != Material.DIAMOND) {
                continue;
            }
            int take = Math.min(stack.getAmount(), remaining);
            stack.setAmount(stack.getAmount() - take);
            remaining -= take;
            if (stack.getAmount() <= 0) {
                contents[i] = null;
            }
        }
        player.getInventory().setStorageContents(contents);
    }

    private static void giveItem(Player player, ItemStack item) {
        player.getInventory().addItem(item).values()
                .forEach(leftover -> player.getWorld().dropItemNaturally(player.getLocation(), leftover));
    }
}
