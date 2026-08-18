package de.bydora.tes.shop;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory index of every open shop, warmed from {@link ShopRepository} on enable. This is the
 * hot-path source of truth for block-position and id lookups (block-protection listeners,
 * purchase handling, command resolution) — the database is only consulted to persist changes,
 * never queried on those paths.
 */
public final class ShopRegistry {

    private final ShopRepository repository;
    private final Map<String, Map<BlockPos, ShopRecord>> byPosition = new ConcurrentHashMap<>();
    private final Map<String, Map<String, ShopRecord>> byId = new ConcurrentHashMap<>();

    public ShopRegistry(ShopRepository repository) {
        this.repository = repository;
    }

    /**
     * Discards the current in-memory index and reloads it from every open shop in the database.
     * Called once on plugin enable.
     */
    public void load() {
        byPosition.clear();
        byId.clear();
        for (ShopRecord shop : repository.findAllActive()) {
            register(shop);
        }
    }

    public void register(ShopRecord shop) {
        byId.computeIfAbsent(shop.world(), w -> new ConcurrentHashMap<>()).put(shop.id(), shop);
        Map<BlockPos, ShopRecord> positions = byPosition.computeIfAbsent(shop.world(), w -> new ConcurrentHashMap<>());
        positions.put(shop.position(), shop);
        if (shop.secondaryPosition() != null) {
            positions.put(shop.secondaryPosition(), shop);
        }
    }

    public void unregister(String world, String id) {
        Map<String, ShopRecord> ids = byId.get(world);
        if (ids == null) {
            return;
        }
        ShopRecord shop = ids.remove(id);
        if (shop == null) {
            return;
        }
        Map<BlockPos, ShopRecord> positions = byPosition.get(world);
        if (positions != null) {
            positions.remove(shop.position());
            if (shop.secondaryPosition() != null) {
                positions.remove(shop.secondaryPosition());
            }
        }
    }

    public Optional<ShopRecord> findByPosition(String world, BlockPos position) {
        Map<BlockPos, ShopRecord> positions = byPosition.get(world);
        return positions == null ? Optional.empty() : Optional.ofNullable(positions.get(position));
    }

    public Optional<ShopRecord> findById(String world, String id) {
        Map<String, ShopRecord> ids = byId.get(world);
        return ids == null ? Optional.empty() : Optional.ofNullable(ids.get(id));
    }

    public boolean existsId(String world, String id) {
        return findById(world, id).isPresent();
    }

    /**
     * Every currently registered shop, across all worlds — used by {@link ShopMaintenanceTask}.
     */
    public Collection<ShopRecord> all() {
        return byId.values().stream().flatMap(m -> m.values().stream()).toList();
    }

    public List<ShopRecord> allByOwner(java.util.UUID owner) {
        return all().stream().filter(shop -> shop.isOwner(owner)).toList();
    }
}
