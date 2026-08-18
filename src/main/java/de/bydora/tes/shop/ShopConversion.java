package de.bydora.tes.shop;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Nameable;
import org.bukkit.block.BlockState;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataHolder;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.EnumSet;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * Converts a vanilla container into a shop object and back: tags its
 * {@link PersistentDataContainer} with the owning shop's world/id (the defensive signal
 * {@link ShopMaintenanceTask}'s orphan scan checks against {@link ShopRegistry}) and sets its
 * display name to {@code "<Item> | <Preis>"} per spec §3.1.1.1.
 */
public final class ShopConversion {

    /**
     * Container materials a shop may be created on: chest, "Redstone-Truhe" (trapped chest),
     * barrel, and every shulker box colour.
     */
    public static final Set<Material> ALLOWED_CONTAINERS;

    static {
        EnumSet<Material> containers = EnumSet.of(Material.CHEST, Material.TRAPPED_CHEST, Material.BARREL);
        for (Material material : Material.values()) {
            if (material.name().endsWith("SHULKER_BOX")) {
                containers.add(material);
            }
        }
        ALLOWED_CONTAINERS = java.util.Collections.unmodifiableSet(containers);
    }

    private ShopConversion() {
    }

    public static boolean isAllowedContainer(Material material) {
        return ALLOWED_CONTAINERS.contains(material);
    }

    public static NamespacedKey shopKey(Plugin plugin) {
        return new NamespacedKey(plugin, "shop");
    }

    /**
     * Tags {@code state} as belonging to the given shop and sets its label. The caller is
     * responsible for calling this on both halves of a double chest and persisting the state
     * ({@code state.update(true, false)}).
     */
    public static void markAsShop(Plugin plugin, BlockState state, String world, String id, Material item, int price) {
        PersistentDataContainer container = ((PersistentDataHolder) state).getPersistentDataContainer();
        container.set(shopKey(plugin), PersistentDataType.STRING, world + ":" + id);
        if (state instanceof Nameable nameable) {
            nameable.customName(label(item, price));
        }
    }

    /**
     * Reverts a converted container back to a plain vanilla one. The caller is responsible for
     * persisting the state.
     */
    public static void clearShopTag(Plugin plugin, BlockState state) {
        PersistentDataContainer container = ((PersistentDataHolder) state).getPersistentDataContainer();
        container.remove(shopKey(plugin));
        if (state instanceof Nameable nameable) {
            nameable.customName(null);
        }
    }

    /**
     * Reads back the {@code "<world>:<id>"} tag set by {@link #markAsShop}, if present.
     */
    public static Optional<String> readShopTag(Plugin plugin, BlockState state) {
        PersistentDataContainer container = ((PersistentDataHolder) state).getPersistentDataContainer();
        return Optional.ofNullable(container.get(shopKey(plugin), PersistentDataType.STRING));
    }

    public static Component label(Material item, int price) {
        return Component.text(formatMaterialName(item) + " | " + price, NamedTextColor.GOLD);
    }

    private static String formatMaterialName(Material material) {
        String[] words = material.name().toLowerCase(Locale.ROOT).split("_");
        StringBuilder builder = new StringBuilder();
        for (String word : words) {
            if (!builder.isEmpty()) {
                builder.append(' ');
            }
            builder.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return builder.toString();
    }
}
