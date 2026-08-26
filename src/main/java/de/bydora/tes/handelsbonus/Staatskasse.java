package de.bydora.tes.handelsbonus;

import de.bydora.tes.config.TesConfig;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Container;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;

/**
 * The Handelsbonus's funding source (spec §3.2.1.1, Belohnung 4): a real chest whose coordinates
 * are set in {@code config.yml}, not a virtual balance — its diamonds are a genuinely finite
 * resource an admin has to keep topped up.
 */
public final class Staatskasse {

    private Staatskasse() {
    }

    /**
     * Removes up to {@code amount} diamonds from the configured Staatskasse chest and returns how
     * much was actually removed — capped at whatever's really in there, and 0 if no chest is
     * configured or it can't be resolved (wrong coordinates, chunk not a container, etc.).
     */
    public static int withdraw(TesConfig config, int amount) {
        if (amount <= 0) {
            return 0;
        }
        Optional<Inventory> inventory = resolveInventory(config);
        if (inventory.isEmpty()) {
            return 0;
        }
        int available = countDiamonds(inventory.get());
        int toRemove = Math.min(amount, available);
        if (toRemove > 0) {
            removeDiamonds(inventory.get(), toRemove);
        }
        return toRemove;
    }

    private static Optional<Inventory> resolveInventory(TesConfig config) {
        String worldName = config.treueshopHandelsbonusStaatskasseWorld();
        if (worldName.isBlank()) {
            return Optional.empty();
        }
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            return Optional.empty();
        }
        Block block = world.getBlockAt(config.treueshopHandelsbonusStaatskasseX(),
                config.treueshopHandelsbonusStaatskasseY(), config.treueshopHandelsbonusStaatskasseZ());
        BlockState state = block.getState();
        return state instanceof Container container ? Optional.of(container.getInventory()) : Optional.empty();
    }

    private static int countDiamonds(Inventory inventory) {
        int total = 0;
        for (ItemStack stack : inventory.getContents()) {
            if (stack != null && stack.getType() == Material.DIAMOND) {
                total += stack.getAmount();
            }
        }
        return total;
    }

    private static void removeDiamonds(Inventory inventory, int amount) {
        ItemStack[] contents = inventory.getContents();
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
        inventory.setContents(contents);
    }
}
