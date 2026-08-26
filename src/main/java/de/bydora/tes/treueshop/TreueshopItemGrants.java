package de.bydora.tes.treueshop;

import de.bydora.tes.TesseraniaEconomySystem;
import de.bydora.tes.prozessverstaerker.ProzessverstaerkerItems;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

/**
 * Item-grant Treueshop rewards (spec §3.2.1.1): unlike {@link TreueshopEffects}'s direct potion
 * effects, these deposit an {@link ItemStack} into the buyer's Belohnungsinventar via
 * {@link de.bydora.tes.reward.RewardInventoryService#grant}, never a live inventory placement.
 */
public final class TreueshopItemGrants {

    private TreueshopItemGrants() {
    }

    /**
     * Belohnung 12, "Spawner": a plain {@link Material#SPAWNER} (a working monster spawner,
     * fillable with spawn eggs per the spec) — distinct from the shop button's own
     * {@link Material#TRIAL_SPAWNER} icon, which was an icon-only reference-build choice, not a
     * statement about the granted item (see {@code docs/treueshop-system.md}).
     */
    public static void grantSpawner(TesseraniaEconomySystem plugin, Player player) {
        plugin.rewardInventoryService().grant(player.getUniqueId(), new ItemStack(Material.SPAWNER));
    }

    /**
     * Belohnung 5, "Erntewelt": a stubbed modified Chorus Fruit granting Overworld-type farm-world
     * access. Eating it is meant to teleport the player there (spec §3.2.1.1/§3.2.1.3), but that
     * behavior — and the farm worlds themselves — isn't built until Stage 5; for now the item is
     * inert, a documented gap matching the same "grants item only" pattern planned for Level
     * reward type 2.
     */
    public static void grantErntewelt(TesseraniaEconomySystem plugin, Player player) {
        plugin.rewardInventoryService().grant(player.getUniqueId(), chorusFruit("Erntewelt-Zugang", NamedTextColor.GREEN));
    }

    /**
     * Belohnung 6, "Glutzone": the same stub as {@link #grantErntewelt}, but for the Nether-type
     * farm world.
     */
    public static void grantGlutzone(TesseraniaEconomySystem plugin, Player player) {
        plugin.rewardInventoryService().grant(player.getUniqueId(), chorusFruit("Glutzone-Zugang", NamedTextColor.GOLD));
    }

    /**
     * Belohnung 1, "Prozessverstärker": the tagged Glowstone Dust
     * {@link ProzessverstaerkerItems#create} builds — see
     * {@code de.bydora.tes.prozessverstaerker} for what using it actually does.
     */
    public static void grantProzessverstaerker(TesseraniaEconomySystem plugin, Player player) {
        plugin.rewardInventoryService().grant(player.getUniqueId(), ProzessverstaerkerItems.create(plugin));
    }

    /**
     * Belohnungen 8.1/9.1/10.1/11.1, the bundled mob-egg rewards: one {@link ItemStack} per egg
     * species in {@code bundle}, each stacked to its full grant amount.
     */
    public static void grantMobBundle(TesseraniaEconomySystem plugin, Player player, TreueshopMobBundle bundle) {
        for (TreueshopMobBundle.EggGrant egg : bundle.eggs()) {
            plugin.rewardInventoryService().grant(player.getUniqueId(), new ItemStack(egg.eggMaterial(), egg.amount()));
        }
    }

    private static ItemStack chorusFruit(String name, NamedTextColor color) {
        ItemStack item = new ItemStack(Material.CHORUS_FRUIT);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(name, color, TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(
                Component.text("Isst du diese Chorusfrucht, wirst du in", NamedTextColor.GRAY, TextDecoration.ITALIC),
                Component.text("eine spezielle Ressourcenwelt teleportiert.", NamedTextColor.GRAY, TextDecoration.ITALIC),
                Component.empty(),
                Component.text("(Noch nicht funktionsfähig — folgt mit den Farmwelten.)", NamedTextColor.DARK_GRAY, TextDecoration.ITALIC)));
        item.setItemMeta(meta);
        return item;
    }
}
