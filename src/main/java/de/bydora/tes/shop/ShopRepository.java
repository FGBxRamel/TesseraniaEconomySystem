package de.bydora.tes.shop;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Persistent storage for registered item shops (spec §3.1.1.1).
 */
public interface ShopRepository {

    Optional<ShopRecord> findById(String id);

    /**
     * All shops owned by {@code owner}, across every world, ordered by creation time — backs
     * {@code /tes shop liste}.
     */
    List<ShopRecord> findAllByOwner(UUID owner);

    /**
     * Every registered shop, across every world — used to warm {@link ShopRegistry} on enable.
     */
    List<ShopRecord> findAllActive();

    boolean existsId(String id);

    void insert(ShopRecord shop);

    /**
     * Updates the mutable attributes (name, item, price, teleport point) of an existing shop.
     * ID, world and position are immutable once set (UC2) and are not touched here.
     */
    void updateAttributes(ShopRecord shop);

    void replaceOwners(String world, String id, Set<UUID> owners);

    /**
     * Hard-deletes a shop, cascading to its owner and transaction rows, so its ID becomes
     * reusable. Used both for owner-initiated close ({@code /tes shop schließen}) and orphan
     * cleanup (UC5).
     */
    void delete(String id);
}
