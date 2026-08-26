package de.bydora.tes.prozessverstaerker;

import de.bydora.tes.TesseraniaEconomySystem;
import de.bydora.tes.util.Messages;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Furnace;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.concurrent.TimeUnit;

/**
 * Applies the Prozessverstärker (spec §3.2.1.1, Belohnung 1): right-clicking a boostable block
 * (see {@link BoostKind}) while holding {@link ProzessverstaerkerItems}'s tagged Glowstone Dust
 * consumes one and starts/extends that block's boost via
 * {@link ProzessverstaerkerBoostRepository#extend}. The furnace speedup is applied here
 * immediately (via {@link Furnace#setCookSpeedMultiplier}, a Paper API that persists on the block
 * itself); the beehive honey-doubling and boost expiry are both handled by
 * {@link ProzessverstaerkerSweepTask} instead, since neither has a single "apply once" moment.
 */
public final class ProzessverstaerkerListener implements Listener {

    private final TesseraniaEconomySystem plugin;
    private final ProzessverstaerkerBoostRepository repository;

    public ProzessverstaerkerListener(TesseraniaEconomySystem plugin, ProzessverstaerkerBoostRepository repository) {
        this.plugin = plugin;
        this.repository = repository;
    }

    @EventHandler(ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        ItemStack item = event.getItem();
        if (!ProzessverstaerkerItems.isProzessverstaerker(plugin, item)) {
            return;
        }
        Block block = event.getClickedBlock();
        BoostKind kind = block == null ? null : BoostKind.forMaterial(block.getType());
        if (kind == null) {
            return;
        }

        event.setCancelled(true);
        item.setAmount(item.getAmount() - 1);

        long durationMillis = TimeUnit.MINUTES.toMillis(plugin.tesConfig().treueshopProzessverstaerkerBoostMinutes());
        long expiresAt = repository.extend(block.getWorld().getName(), block.getX(), block.getY(), block.getZ(),
                kind, durationMillis, System.currentTimeMillis());

        if (kind == BoostKind.FURNACE) {
            applyFurnaceMultiplier(block, 2.0);
        }

        long remainingMinutes = TimeUnit.MILLISECONDS.toMinutes(expiresAt - System.currentTimeMillis());
        event.getPlayer().sendMessage(Messages.prozessverstaerkerApplied(remainingMinutes));
    }

    static void applyFurnaceMultiplier(Block block, double multiplier) {
        if (BoostKind.forMaterial(block.getType()) != BoostKind.FURNACE) {
            return;
        }
        BlockState state = block.getState();
        if (!(state instanceof Furnace furnace)) {
            return;
        }
        furnace.setCookSpeedMultiplier(multiplier);
        furnace.update();
    }
}
