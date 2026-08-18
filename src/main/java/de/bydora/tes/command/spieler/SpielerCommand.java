package de.bydora.tes.command.spieler;

import de.bydora.tes.command.TesSubCommand;
import de.bydora.tes.command.confirm.ConfirmationManager;
import de.bydora.tes.data.PlayerRecord;
import de.bydora.tes.data.PlayerRepository;
import de.bydora.tes.util.Messages;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Implements {@code /tes spieler add|remove|pause|unpause <Name>} (spec §1.4).
 */
public final class SpielerCommand implements TesSubCommand {

    /** Stand-in actor identity for the console, which has no player UUID of its own. */
    private static final UUID CONSOLE_ACTOR = new UUID(0, 0);

    private final PlayerRepository playerRepository;
    private final ConfirmationManager<UUID> removeConfirmations;

    public SpielerCommand(PlayerRepository playerRepository, ConfirmationManager<UUID> removeConfirmations) {
        this.playerRepository = playerRepository;
        this.removeConfirmations = removeConfirmations;
    }

    @Override
    public String name() {
        return "spieler";
    }

    @Override
    public void execute(CommandSender sender, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(Messages.usage("/" + label + " spieler <add|remove|pause|unpause> <Name>"));
            return;
        }
        switch (args[0].toLowerCase()) {
            case "add" -> add(sender, args);
            case "remove" -> remove(sender, label, args);
            case "pause" -> pause(sender, args);
            case "unpause" -> unpause(sender, args);
            default -> sender.sendMessage(Messages.usage("/" + label + " spieler <add|remove|pause|unpause> <Name>"));
        }
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return List.of("add", "remove", "pause", "unpause").stream()
                    .filter(action -> action.startsWith(args[0].toLowerCase()))
                    .toList();
        }
        return List.of();
    }

    private void add(CommandSender sender, String[] args) {
        if (!sender.hasPermission("tes.admin.spieler.add")) {
            sender.sendMessage(Messages.noPermission());
            return;
        }
        if (args.length != 2) {
            sender.sendMessage(Messages.usage("/tes spieler add <Name>"));
            return;
        }
        Optional<OfflinePlayer> target = resolveKnownPlayer(sender, args[1]);
        if (target.isEmpty()) {
            return;
        }
        OfflinePlayer offlinePlayer = target.get();
        if (playerRepository.isRegistered(offlinePlayer.getUniqueId())) {
            sender.sendMessage(Messages.alreadyRegistered(args[1]));
            return;
        }
        playerRepository.register(offlinePlayer.getUniqueId(), args[1]);
        sender.sendMessage(Messages.registered(args[1]));
    }

    private void pause(CommandSender sender, String[] args) {
        if (!sender.hasPermission("tes.admin.spieler.pause")) {
            sender.sendMessage(Messages.noPermission());
            return;
        }
        if (args.length != 2) {
            sender.sendMessage(Messages.usage("/tes spieler pause <Name>"));
            return;
        }
        Optional<PlayerRecord> record = requireRegistered(sender, args[1]);
        if (record.isEmpty()) {
            return;
        }
        if (record.get().paused()) {
            sender.sendMessage(Messages.alreadyPaused(args[1]));
            return;
        }
        playerRepository.setPaused(record.get().uuid(), true);
        sender.sendMessage(Messages.paused(args[1]));
    }

    private void unpause(CommandSender sender, String[] args) {
        if (!sender.hasPermission("tes.admin.spieler.unpause")) {
            sender.sendMessage(Messages.noPermission());
            return;
        }
        if (args.length != 2) {
            sender.sendMessage(Messages.usage("/tes spieler unpause <Name>"));
            return;
        }
        Optional<PlayerRecord> record = requireRegistered(sender, args[1]);
        if (record.isEmpty()) {
            return;
        }
        if (!record.get().paused()) {
            sender.sendMessage(Messages.notPaused(args[1]));
            return;
        }
        playerRepository.setPaused(record.get().uuid(), false);
        sender.sendMessage(Messages.unpaused(args[1]));
    }

    private void remove(CommandSender sender, String label, String[] args) {
        if (!sender.hasPermission("tes.admin.spieler.remove")) {
            sender.sendMessage(Messages.noPermission());
            return;
        }
        UUID actor = sender instanceof Player player ? player.getUniqueId() : CONSOLE_ACTOR;

        if (args.length == 2) {
            Optional<PlayerRecord> record = requireRegistered(sender, args[1]);
            if (record.isEmpty()) {
                return;
            }
            String token = removeConfirmations.create(actor, record.get().uuid());
            sender.sendMessage(Messages.removeConfirmPrompt(args[1], label, token, removeConfirmations.ttl().toSeconds()));
            return;
        }
        if (args.length == 3) {
            String name = args[1];
            String token = args[2];
            Optional<UUID> confirmed = removeConfirmations.consume(actor, token);
            if (confirmed.isEmpty()) {
                sender.sendMessage(Messages.removeConfirmExpiredOrInvalid());
                return;
            }
            playerRepository.delete(confirmed.get());
            sender.sendMessage(Messages.removed(name));
            return;
        }
        sender.sendMessage(Messages.usage("/" + label + " spieler remove <Name>"));
    }

    private Optional<OfflinePlayer> resolveKnownPlayer(CommandSender sender, String name) {
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(name);
        if (!offlinePlayer.hasPlayedBefore() && !offlinePlayer.isOnline()) {
            sender.sendMessage(Messages.playerNeverSeen(name));
            return Optional.empty();
        }
        return Optional.of(offlinePlayer);
    }

    private Optional<PlayerRecord> requireRegistered(CommandSender sender, String name) {
        Optional<OfflinePlayer> target = resolveKnownPlayer(sender, name);
        if (target.isEmpty()) {
            return Optional.empty();
        }
        Optional<PlayerRecord> record = playerRepository.findByUuid(target.get().getUniqueId());
        if (record.isEmpty()) {
            sender.sendMessage(Messages.notRegistered(name));
        }
        return record;
    }
}
