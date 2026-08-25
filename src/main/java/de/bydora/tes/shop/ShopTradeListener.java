package de.bydora.tes.shop;

import de.bydora.tes.data.PlayerRecord;
import de.bydora.tes.data.PlayerRepository;
import de.bydora.tes.util.DiamondEconomy;
import de.bydora.tes.util.Messages;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.UseCooldown;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * Handles UC4 (the purchase flow) directly on a shop's own container inventory: a single
 * left-click by a non-owner either buys the clicked slot's stock (diamonds ⇄ item, in place) or,
 * if it's the buyer's own still-{@link TransactionState#PENDING} diamonds, refunds it. Owners may
 * withdraw diamonds (once their 60-second window has elapsed, by normal or shift click) and
 * restock with the configured item — or, for a {@link ShopRecord#sellsAllItems() sell-all-items}
 * shop, with anything except diamonds; everything else on the shop's side of the inventory is
 * blocked to keep the interaction to the single-slot-click model the spec describes.
 *
 * <p>A purchase is also blocked if the buyer is paused, or if every one of the shop's owners is
 * paused (a shop with at least one active owner keeps selling normally).
 */
public final class ShopTradeListener implements Listener {

    private static final int REFUND_WINDOW_TICKS = 1200;

    private final Plugin plugin;
    private final ShopRegistry shopRegistry;
    private final ShopTransactionRepository transactionRepository;
    private final PlayerRepository playerRepository;

    public ShopTradeListener(Plugin plugin, ShopRegistry shopRegistry, ShopTransactionRepository transactionRepository,
                              PlayerRepository playerRepository) {
        this.plugin = plugin;
        this.shopRegistry = shopRegistry;
        this.transactionRepository = transactionRepository;
        this.playerRepository = playerRepository;
    }

    @EventHandler(ignoreCancelled = true)
    public void onClick(InventoryClickEvent event) {
        Inventory topInventory = event.getView().getTopInventory();
        Optional<ShopRecord> maybeShop = ShopBlocks.resolveShop(shopRegistry, topInventory.getHolder());
        if (maybeShop.isEmpty() || !(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        ShopRecord shop = maybeShop.get();
        boolean clickedTop = topInventory.equals(event.getClickedInventory());

        if (event.getClick().isShiftClick() || event.getAction() == InventoryAction.COLLECT_TO_CURSOR) {
            if (clickedTop && shop.isOwner(player.getUniqueId())) {
                handleOwnerShiftWithdraw(event, shop, player);
            } else {
                event.setCancelled(true);
            }
            return;
        }
        if (!clickedTop) {
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

        if (!shop.sellsAllItems() && !clicked.isSimilar(shop.item())) {
            return;
        }
        if (allOwnersPaused(shop)) {
            buyer.sendMessage(Messages.shopOutOfOrder());
            return;
        }
        if (isPaused(buyer.getUniqueId())) {
            buyer.sendMessage(Messages.senderPaused());
            return;
        }
        int price = shop.price();
        if (DiamondEconomy.countDiamonds(buyer) < price) {
            buyer.sendMessage(Messages.notEnoughTaler());
            return;
        }

        ItemStack sold = clicked.clone();
        DiamondEconomy.removeDiamonds(buyer, price);
        NamespacedKey cooldownGroup = cooldownGroup(shop, slot);
        ItemStack pendingDiamonds = new ItemStack(Material.DIAMOND, price);
        pendingDiamonds.setData(DataComponentTypes.USE_COOLDOWN, UseCooldown.useCooldown(REFUND_WINDOW_TICKS / 20f).cooldownGroup(cooldownGroup));
        shopInventory.setItem(slot, pendingDiamonds);
        giveItem(buyer, sold.clone());
        transactionRepository.insertPending(shop.world(), shop.id(), slot, buyer.getUniqueId(), sold, price, System.currentTimeMillis());
        buyer.setCooldown(cooldownGroup, REFUND_WINDOW_TICKS);
    }

    private boolean isPaused(UUID uuid) {
        return playerRepository.findByUuid(uuid).map(PlayerRecord::paused).orElse(false);
    }

    /**
     * A shop with several co-owners only counts as out of order once every one of them is
     * paused; as long as at least one owner remains active, buyers may still purchase from it.
     */
    private boolean allOwnersPaused(ShopRecord shop) {
        return shop.owners().stream().allMatch(this::isPaused);
    }

    private void handleOwnerClick(InventoryClickEvent event, ShopRecord shop, Player owner) {
        int slot = event.getSlot();
        ItemStack clicked = event.getClickedInventory().getItem(slot);

        if (clicked != null && clicked.getType() == Material.DIAMOND) {
            if (transactionRepository.findPendingBySlot(shop.world(), shop.id(), slot).isPresent()) {
                event.setCancelled(true);
                owner.sendMessage(Messages.shopWithdrawCooldownActive());
            } else if (clicked.hasData(DataComponentTypes.USE_COOLDOWN)) {
                event.getClickedInventory().setItem(slot, withoutCooldown(clicked));
            }
            return;
        }

        ItemStack cursor = event.getCursor();
        boolean placingItem = cursor.getType() != Material.AIR && switch (event.getAction()) {
            case PLACE_ALL, PLACE_SOME, PLACE_ONE, SWAP_WITH_CURSOR -> true;
            default -> false;
        };
        if (!placingItem) {
            return;
        }
        if (shop.sellsAllItems()) {
            if (cursor.getType() == Material.DIAMOND) {
                event.setCancelled(true);
                owner.sendMessage(Messages.shopDiamondsNotStockable());
            }
        } else if (!cursor.isSimilar(shop.item())) {
            event.setCancelled(true);
            owner.sendMessage(Messages.shopWrongItemForShop(ShopRecord.itemDisplayName(shop.item())));
        }
    }

    /**
     * Owner shift-clicking a withdrawable (non-pending) diamond stack is left uncancelled so
     * vanilla move-to-other-inventory handles the transfer; anything else on the shop's side
     * (item stock, or diamonds still within the buyer's refund window) stays blocked.
     */
    private void handleOwnerShiftWithdraw(InventoryClickEvent event, ShopRecord shop, Player owner) {
        int slot = event.getSlot();
        ItemStack clicked = event.getClickedInventory().getItem(slot);
        if (clicked == null || clicked.getType() != Material.DIAMOND) {
            event.setCancelled(true);
            return;
        }
        if (transactionRepository.findPendingBySlot(shop.world(), shop.id(), slot).isPresent()) {
            event.setCancelled(true);
            owner.sendMessage(Messages.shopWithdrawCooldownActive());
            return;
        }
        if (clicked.hasData(DataComponentTypes.USE_COOLDOWN)) {
            event.getClickedInventory().setItem(slot, withoutCooldown(clicked));
        }
    }

    /**
     * Strips the refund-window {@code USE_COOLDOWN} overlay component from a shop-slot diamond
     * stack once it's no longer pending, so withdrawn diamonds are plain stacks again and merge
     * normally with the player's other diamonds instead of being permanently kept apart by the
     * shop/slot-specific cooldown group baked into the component.
     */
    private static ItemStack withoutCooldown(ItemStack stack) {
        ItemStack copy = stack.clone();
        copy.resetData(DataComponentTypes.USE_COOLDOWN);
        return copy;
    }

    private void refund(Inventory shopInventory, ShopTransactionRecord transaction, Player buyer) {
        ItemStack item = transaction.item();
        if (countMatching(buyer, stack -> stack.isSimilar(item)) < item.getAmount()) {
            buyer.sendMessage(Messages.shopRefundItemMissing());
            return;
        }
        removeMatching(buyer, stack -> stack.isSimilar(item), item.getAmount());
        shopInventory.setItem(transaction.slot(), item.clone());
        giveItem(buyer, new ItemStack(Material.DIAMOND, transaction.price()));
        transactionRepository.markRefunded(transaction.id(), System.currentTimeMillis());
    }

    /**
     * A cooldown group scoped to this shop's slot, so the refund-window overlay shows only on the
     * pending diamond stack sitting in that slot — not on the buyer's real currency diamonds
     * elsewhere in their inventory, and without resetting any other slot's still-running overlay.
     */
    private NamespacedKey cooldownGroup(ShopRecord shop, int slot) {
        return new NamespacedKey(plugin, ("shop-pending-" + shop.id() + "-" + slot).toLowerCase(Locale.ROOT));
    }

    private static int countMatching(Player player, Predicate<ItemStack> matcher) {
        int total = 0;
        for (ItemStack stack : player.getInventory().getStorageContents()) {
            if (stack != null && matcher.test(stack)) {
                total += stack.getAmount();
            }
        }
        return total;
    }

    /**
     * Removes up to {@code amount} of items matching {@code matcher} from the player's storage
     * contents, scanning slots in order. Callers must first confirm sufficient quantity via
     * {@link #countMatching(Player, java.util.function.Predicate)}; this method takes whatever is
     * present without erroring if that turns out to be less than {@code amount}.
     */
    private static void removeMatching(Player player, Predicate<ItemStack> matcher, int amount) {
        ItemStack[] contents = player.getInventory().getStorageContents();
        int remaining = amount;
        for (int i = 0; i < contents.length && remaining > 0; i++) {
            ItemStack stack = contents[i];
            if (stack == null || !matcher.test(stack)) {
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
