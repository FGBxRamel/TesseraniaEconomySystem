package de.bydora.tes.command;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import de.bydora.tes.data.PlayerRecord;
import de.bydora.tes.data.PlayerRepository;
import de.bydora.tes.util.Messages;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Shared player-name resolution used by every {@code /tes} subcommand that takes a target player
 * name (spieler, treuepunkte, erfahrungspunkte, and shop owner assignment).
 */
public final class PlayerLookup {

    private PlayerLookup() {
    }

    public static CompletableFuture<Suggestions> suggestOnlinePlayerNames(CommandContext<CommandSourceStack> ctx, SuggestionsBuilder builder) {
        String remaining = builder.getRemaining().toLowerCase(Locale.ROOT);
        Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .filter(name -> name.toLowerCase(Locale.ROOT).startsWith(remaining))
                .forEach(builder::suggest);
        return builder.buildFuture();
    }

    /**
     * Resolves {@code name} to a player the server has seen before (online or previously played);
     * sends an error and returns empty otherwise.
     */
    public static Optional<OfflinePlayer> resolveKnownPlayer(CommandSender sender, String name) {
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(name);
        if (!offlinePlayer.hasPlayedBefore() && !offlinePlayer.isOnline()) {
            sender.sendMessage(Messages.playerNeverSeen(name));
            return Optional.empty();
        }
        return Optional.of(offlinePlayer);
    }

    /**
     * Resolves {@code name} to a known player who is also registered in the TES reward system;
     * sends an error and returns empty otherwise.
     */
    public static Optional<PlayerRecord> requireRegistered(CommandSender sender, PlayerRepository repository, String name) {
        Optional<OfflinePlayer> target = resolveKnownPlayer(sender, name);
        if (target.isEmpty()) {
            return Optional.empty();
        }
        Optional<PlayerRecord> record = repository.findByUuid(target.get().getUniqueId());
        if (record.isEmpty()) {
            sender.sendMessage(Messages.notRegistered(name));
        }
        return record;
    }
}
