package de.bydora.tes.shop;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Set;
import java.util.UUID;

/**
 * A registered item shop (spec §3.1.1.1).
 *
 * @param id                the shop's admin-assigned, immutable identifier, unique per world
 * @param world             the world the shop's container is in
 * @param name               the shop's display name
 * @param item              the item this shop sells, including its enchantments/potion
 *                          data/display name/etc., or {@link #SELL_ALL_SENTINEL} for a shop that
 *                          sells any non-diamond item (see {@link #sellsAllItems()})
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
        ItemStack item,
        int price,
        Material containerType,
        BlockPos position,
        BlockPos secondaryPosition,
        TeleportPoint teleportPoint,
        Set<UUID> owners,
        long createdAt,
        long updatedAt
) {

    /**
     * Reserved value for {@link #item} marking a shop that sells any item except diamonds.
     * {@link Material#AIR} can never be a legitimately configured single-item value (it's not an
     * item at all, and the setup flow's material lookup already rejects it), so it doubles as the
     * "sell all items" flag without needing a separate field, DB column, or migration.
     */
    public static final ItemStack SELL_ALL_SENTINEL = new ItemStack(Material.AIR);

    /**
     * Defensively clones {@link #item} so a {@code ShopRecord} can never be mutated through an
     * {@link ItemStack} reference (e.g. the player's hand item, or another record) it was built
     * from.
     */
    public ShopRecord {
        item = item.clone();
    }

    public boolean isOwner(UUID uuid) {
        return owners.contains(uuid);
    }

    public boolean sellsAllItems() {
        return item.getType() == Material.AIR;
    }

    /**
     * German display text for {@code item}, used everywhere the shop's configured item is shown
     * to a player (setup menu, shop list): its custom display name if set, else its material name.
     */
    public static String itemDisplayName(ItemStack item) {
        if (item.getType() == Material.AIR) {
            return "Alle Items";
        }
        ItemMeta meta = item.getItemMeta();
        Component displayName = meta == null ? null : meta.displayName();
        return displayName == null ? item.getType().name() : PlainTextComponentSerializer.plainText().serialize(displayName);
    }
}
