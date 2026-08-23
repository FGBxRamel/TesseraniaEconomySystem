package de.bydora.tes.reward;

import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Persistent storage for the Belohnungsinventar (spec §1.3) — a queue of {@link ItemStack}s owed
 * to a player, one row per stack. Not accessed directly outside {@link RewardInventoryService},
 * which is the intended entry point for every reward-producing system.
 */
public interface RewardInventoryRepository {

    void insert(UUID uuid, ItemStack item, long grantedAt);

    /**
     * All items queued for {@code uuid}, oldest first.
     */
    List<RewardInventoryItemRecord> findAllByUuid(UUID uuid);

    Optional<RewardInventoryItemRecord> findById(long id);

    void delete(long id);
}
