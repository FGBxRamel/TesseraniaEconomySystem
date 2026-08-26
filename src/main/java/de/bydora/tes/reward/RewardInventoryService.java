package de.bydora.tes.reward;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * The Belohnungsinventar's public API (spec §1.3) and the single entry point every
 * reward-producing system should use: invoice cash-outs (Stage 2), and later the loyalty-point
 * shop (Stage 3) and level rewards (Stage 4). Never place an item reward directly into a live
 * shop chest or a player's real inventory — always go through {@link #grant}.
 */
public final class RewardInventoryService {

    private final RewardInventoryRepository repository;

    public RewardInventoryService(RewardInventoryRepository repository) {
        this.repository = repository;
    }

    /**
     * Deposits {@code item} into {@code uuid}'s reward inventory. Silently ignores an empty/AIR
     * stack — there is nothing meaningful to hand back later.
     */
    public void grant(UUID uuid, ItemStack item) {
        if (item == null || item.isEmpty()) {
            return;
        }
        repository.insert(uuid, item.clone(), System.currentTimeMillis());
    }

    /**
     * All items currently queued for {@code uuid}, oldest first.
     */
    public List<RewardInventoryItemRecord> items(UUID uuid) {
        return repository.findAllByUuid(uuid);
    }

    public enum TakeResult {
        TAKEN,
        NOT_FOUND,
        INVENTORY_FULL
    }

    /**
     * Attempts to move exactly one queued item ({@code rewardItemId}) into {@code player}'s real
     * inventory. All-or-nothing: if the stack does not fully fit, {@code player}'s real inventory
     * is left untouched and the item stays queued (spec/design decision: click-to-take one at a
     * time, never partially consumed on a full inventory).
     */
    public TakeResult take(Player player, long rewardItemId) {
        Optional<RewardInventoryItemRecord> record = repository.findById(rewardItemId);
        if (record.isEmpty() || !record.get().uuid().equals(player.getUniqueId())) {
            return TakeResult.NOT_FOUND;
        }
        ItemStack item = record.get().item();
        ItemStack[] simulated = cloneContents(player.getInventory().getStorageContents());
        if (!merge(simulated, item)) {
            return TakeResult.INVENTORY_FULL;
        }
        player.getInventory().setStorageContents(simulated);
        repository.delete(rewardItemId);
        return TakeResult.TAKEN;
    }

    private static ItemStack[] cloneContents(ItemStack[] contents) {
        ItemStack[] clone = new ItemStack[contents.length];
        for (int i = 0; i < contents.length; i++) {
            clone[i] = contents[i] == null ? null : contents[i].clone();
        }
        return clone;
    }

    /**
     * Simulates merging {@code item} into {@code contents} (mutated in place, on already-cloned
     * stacks): first stacking onto existing similar, non-full stacks, then filling empty slots —
     * mirroring vanilla merge order without {@code Inventory#addItem}'s partial-fill side
     * effects. Returns {@code false}, leaving nothing to commit, if {@code item} does not fully
     * fit; callers must not write {@code contents} back to the inventory in that case.
     */
    private static boolean merge(ItemStack[] contents, ItemStack item) {
        int remaining = item.getAmount();
        int maxStackSize = item.getMaxStackSize();

        for (int i = 0; i < contents.length && remaining > 0; i++) {
            ItemStack stack = contents[i];
            if (stack != null && stack.isSimilar(item) && stack.getAmount() < maxStackSize) {
                int space = maxStackSize - stack.getAmount();
                int add = Math.min(space, remaining);
                stack.setAmount(stack.getAmount() + add);
                remaining -= add;
            }
        }
        for (int i = 0; i < contents.length && remaining > 0; i++) {
            if (contents[i] == null) {
                int add = Math.min(maxStackSize, remaining);
                ItemStack placed = item.clone();
                placed.setAmount(add);
                contents[i] = placed;
                remaining -= add;
            }
        }
        return remaining == 0;
    }
}
