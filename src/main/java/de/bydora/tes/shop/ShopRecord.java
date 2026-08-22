package de.bydora.tes.shop;

import org.bukkit.Material;

import java.util.Set;
import java.util.UUID;

/**
 * A registered item shop (spec §3.1.1.1).
 *
 * @param id                the shop's admin-assigned, immutable identifier, unique per world
 * @param world             the world the shop's container is in
 * @param name               the shop's display name
 * @param item              the item this shop sells
 * @param price             price in diamonds per slot
 * @param containerType     the converted container's block material
 * @param position          the container's block position
 * @param secondaryPosition the second half of a double chest, if applicable
 * @param teleportPoint     an optional explicit teleport destination
 * @param owners            the shop's owner(s); at least one
 * @param createdAt         epoch millis of creation
 * @param updatedAt         epoch millis of the last attribute change
 */
public record ShopRecord(
        String id,
        String world,
        String name,
        Material item,
        int price,
        Material containerType,
        BlockPos position,
        BlockPos secondaryPosition,
        TeleportPoint teleportPoint,
        Set<UUID> owners,
        long createdAt,
        long updatedAt
) {

    public boolean isOwner(UUID uuid) {
        return owners.contains(uuid);
    }
}
