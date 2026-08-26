package de.bydora.tes.prozessverstaerker;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Beehive;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;

/**
 * Periodic background task for the Prozessverstärker (spec §3.2.1.1, Belohnung 1) — the two jobs
 * that don't have a single "apply once" moment at boost time:
 * <ol>
 *     <li>Doubling each honey-level increment on a boosted beehive/bee nest. Vanilla exposes no
 *     event for "honey level about to increase" (it happens deep in a bee's return-to-hive tick),
 *     so this compares each active beehive's level against the last value this task observed and
 *     adds one more whenever vanilla's own +1 already landed.</li>
 *     <li>Expiring boosts: resetting a furnace's cook-speed multiplier back to normal and
 *     forgetting a beehive's tracked level, then deleting the DB row.</li>
 * </ol>
 * Also self-heals if a boosted block was destroyed/changed outside the plugin, mirroring
 * {@link de.bydora.tes.shop.ShopMaintenanceTask}'s orphan handling.
 */
public final class ProzessverstaerkerSweepTask extends BukkitRunnable {

    private final ProzessverstaerkerBoostRepository repository;
    private final Map<BlockKey, Integer> lastKnownHoneyLevel = new HashMap<>();

    public ProzessverstaerkerSweepTask(ProzessverstaerkerBoostRepository repository) {
        this.repository = repository;
    }

    @Override
    public void run() {
        long now = System.currentTimeMillis();
        for (ProzessverstaerkerBoostRecord boost : repository.findAll()) {
            World world = Bukkit.getWorld(boost.world());
            if (world == null) {
                continue;
            }
            Block block = world.getBlockAt(boost.x(), boost.y(), boost.z());
            BlockKey key = new BlockKey(boost.world(), boost.x(), boost.y(), boost.z());

            if (BoostKind.forMaterial(block.getType()) != boost.kind()) {
                forget(boost, key);
                continue;
            }
            if (boost.isExpired(now)) {
                expire(boost, block, key);
                continue;
            }
            if (boost.kind() == BoostKind.BEEHIVE) {
                doubleNewHoney(block, key);
            }
        }
    }

    private void forget(ProzessverstaerkerBoostRecord boost, BlockKey key) {
        repository.delete(boost.world(), boost.x(), boost.y(), boost.z());
        lastKnownHoneyLevel.remove(key);
    }

    private void expire(ProzessverstaerkerBoostRecord boost, Block block, BlockKey key) {
        if (boost.kind() == BoostKind.FURNACE) {
            ProzessverstaerkerListener.applyFurnaceMultiplier(block, 1.0);
        }
        repository.delete(boost.world(), boost.x(), boost.y(), boost.z());
        lastKnownHoneyLevel.remove(key);
    }

    private void doubleNewHoney(Block block, BlockKey key) {
        BlockData data = block.getBlockData();
        if (!(data instanceof Beehive beehive)) {
            return;
        }
        int currentLevel = beehive.getHoneyLevel();
        Integer previousLevel = lastKnownHoneyLevel.put(key, currentLevel);
        if (previousLevel == null || currentLevel <= previousLevel) {
            return;
        }
        int boosted = Math.min(beehive.getMaximumHoneyLevel(), currentLevel + (currentLevel - previousLevel));
        beehive.setHoneyLevel(boosted);
        block.setBlockData(beehive);
        lastKnownHoneyLevel.put(key, boosted);
    }

    private record BlockKey(String world, int x, int y, int z) {
    }
}
