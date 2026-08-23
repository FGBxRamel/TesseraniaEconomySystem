package de.bydora.tes.util;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * Counts and removes diamonds (Taler, the game's currency) from a player's real inventory.
 * Shared by {@link de.bydora.tes.shop.ShopTradeListener} (shop purchases) and
 * {@link de.bydora.tes.invoice.InvoiceEconomy} (invoice settlement).
 */
public final class DiamondEconomy {

    private DiamondEconomy() {
    }

    public static int countDiamonds(Player player) {
        int total = 0;
        for (ItemStack stack : player.getInventory().getStorageContents()) {
            if (stack != null && stack.getType() == Material.DIAMOND) {
                total += stack.getAmount();
            }
        }
        return total;
    }

    /**
     * Removes up to {@code amount} diamonds from the player's storage contents, scanning slots
     * in order. Callers must first confirm sufficient quantity via {@link #countDiamonds}; this
     * method takes whatever is present without erroring if that turns out to be less than
     * {@code amount}.
     */
    public static void removeDiamonds(Player player, int amount) {
        ItemStack[] contents = player.getInventory().getStorageContents();
        int remaining = amount;
        for (int i = 0; i < contents.length && remaining > 0; i++) {
            ItemStack stack = contents[i];
            if (stack == null || stack.getType() != Material.DIAMOND) {
                continue;
            }
            int take = Math.min(stack.getAmount(), remaining);
            stack.setAmount(stack.getAmount() - take);
            remaining -= take;
            if (stack.getAmount() <= 0) {
                contents[i] = null;
            }
        }
        player.getInventory().setStorageContents(contents);
    }
}
