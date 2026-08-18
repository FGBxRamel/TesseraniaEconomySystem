package de.bydora.tes.shop;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Persistent storage for registered item shops (spec §3.1.1.1).
 */
public interface ShopRepository {

    Optional<ShopRecord> findByWorldAndId(String world, String id);

    /**
     * All open (non-closed) shops owned by {@code owner}, across every world, ordered by
     * creation time — backs {@code /tes shop liste}.
     */
    List<ShopRecord> findAllByOwner(UUID owner);

    /**
     * Every open shop, across every world — used to warm {@link ShopRegistry} on enable.
     */
    List<ShopRecord> findAllActive();

    boolean existsId(String world, String id);

    void insert(ShopRecord shop);

    /**
     * Updates the mutable attributes (name, item, price, teleport point) of an existing shop.
     * ID, world and position are immutable once set (UC2) and are not touched here.
     */
    void updateAttributes(ShopRecord shop);

    void replaceOwners(String world, String id, Set<UUID> owners);

    void close(String world, String id, long closedAt);

    /**
     * Hard-deletes a shop and its owner rows. Used by orphan cleanup (UC5), where the
     * underlying block is already gone and there is no legitimate history worth keeping.
     */
    void delete(String world, String id);
}
