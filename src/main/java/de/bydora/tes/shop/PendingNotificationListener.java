package de.bydora.tes.shop;

import de.bydora.tes.util.Messages;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

/**
 * Delivers any messages queued for a player while they were offline (UC5's orphaned-shop notice
 * chief among them) the next time they join.
 */
public final class PendingNotificationListener implements Listener {

    private final PendingNotificationRepository repository;

    public PendingNotificationListener(PendingNotificationRepository repository) {
        this.repository = repository;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        for (String message : repository.drain(event.getPlayer().getUniqueId())) {
            event.getPlayer().sendMessage(Messages.pendingNotification(message));
        }
    }
}
