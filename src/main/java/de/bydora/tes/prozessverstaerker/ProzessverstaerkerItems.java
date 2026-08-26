package de.bydora.tes.prozessverstaerker;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.List;

/**
 * Builds and identifies the Prozessverstärker's granted item (spec §3.2.1.1, Belohnung 1):
 * "Der Spieler erhält ein Glowstone Dust (vgl. Firecharge) mit dem er einen Funktionsblock
 * boosten kann." Identified by a {@link PersistentDataType#BOOLEAN} tag rather than its name/lore
 * (a player renaming their own Glowstone Dust must never make it act as one), same convention as
 * {@code ShopConversion}'s block-side tagging.
 */
public final class ProzessverstaerkerItems {

    private ProzessverstaerkerItems() {
    }

    public static NamespacedKey key(Plugin plugin) {
        return new NamespacedKey(plugin, "prozessverstaerker");
    }

    public static ItemStack create(Plugin plugin) {
        ItemStack item = new ItemStack(Material.GLOWSTONE_DUST);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("Prozessverstärker", NamedTextColor.YELLOW, TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(
                Component.text("Rechtsklick auf einen Ofen oder", NamedTextColor.GRAY, TextDecoration.ITALIC),
                Component.text("Bienenstock, um ihn zu boosten.", NamedTextColor.GRAY, TextDecoration.ITALIC),
                Component.empty(),
                Component.text("Ofen: 2x Geschwindigkeit", NamedTextColor.GRAY, TextDecoration.ITALIC),
                Component.text("Bienenstock: 2x Honigproduktion", NamedTextColor.GRAY, TextDecoration.ITALIC),
                Component.empty(),
                Component.text("Effekte mehrerer Nutzungen addieren sich.", NamedTextColor.DARK_GRAY, TextDecoration.ITALIC)));
        meta.getPersistentDataContainer().set(key(plugin), PersistentDataType.BOOLEAN, true);
        item.setItemMeta(meta);
        return item;
    }

    public static boolean isProzessverstaerker(Plugin plugin, ItemStack item) {
        if (item == null || item.isEmpty() || !item.hasItemMeta()) {
            return false;
        }
        Boolean tag = item.getItemMeta().getPersistentDataContainer().get(key(plugin), PersistentDataType.BOOLEAN);
        return Boolean.TRUE.equals(tag);
    }
}
