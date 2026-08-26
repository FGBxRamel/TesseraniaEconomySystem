package de.bydora.tes.reward;

import org.bukkit.inventory.ItemStack;

import java.util.UUID;

/**
 * One item queued in a player's Belohnungsinventar (spec §1.3), awaiting collection.
 *
 * @param id        auto-generated primary key
 * @param uuid      the player this item is owed to
 * @param item      the stack itself, including amount/enchantments/display name/etc.
 * @param grantedAt epoch millis the item was granted
 */
public record RewardInventoryItemRecord(long id, UUID uuid, ItemStack item, long grantedAt) {

    /**
     * Defensively clones {@link #item} so a {@code RewardInventoryItemRecord} can never be
     * mutated through an {@link ItemStack} reference it was built from.
     */
    public RewardInventoryItemRecord {
        item = item.clone();
    }
}
