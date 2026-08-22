package de.bydora.tes.command.spieler;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import de.bydora.tes.TesseraniaEconomySystem;
import de.bydora.tes.command.PlayerLookup;
import de.bydora.tes.command.confirm.ConfirmationManager;
import de.bydora.tes.data.PlayerRecord;
import de.bydora.tes.data.PlayerRepository;
import de.bydora.tes.util.Messages;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Optional;
import java.util.UUID;

/**
 * Implements {@code /tes spieler add|remove|pause|unpause <Name>} (spec §1.4).
 */
public final class SpielerCommand {

    /** Stand-in actor identity for the console, which has no player UUID of its own. */
    private static final UUID CONSOLE_ACTOR = new UUID(0, 0);

    private SpielerCommand() {
    }

    public static LiteralArgumentBuilder<CommandSourceStack> build() {
        return Commands.literal("spieler")
                .executes(ctx -> {
                    ctx.getSource().getSender().sendMessage(Messages.usage("/tes spieler <add|remove|pause|unpause> <Name>"));
                    return Command.SINGLE_SUCCESS;
                })
                .then(Commands.literal("add")
                        .requires(source -> source.getSender().hasPermission("tes.admin.spieler.add"))
                        .then(Commands.argument("name", StringArgumentType.word())
                                .suggests(PlayerLookup::suggestOnlinePlayerNames)
                                .executes(SpielerCommand::add)))
                .then(Commands.literal("pause")
                        .requires(source -> source.getSender().hasPermission("tes.admin.spieler.pause"))
                        .then(Commands.argument("name", StringArgumentType.word())
                                .suggests(PlayerLookup::suggestOnlinePlayerNames)
                                .executes(SpielerCommand::pause)))
                .then(Commands.literal("unpause")
                        .requires(source -> source.getSender().hasPermission("tes.admin.spieler.unpause"))
                        .then(Commands.argument("name", StringArgumentType.word())
                                .suggests(PlayerLookup::suggestOnlinePlayerNames)
                                .executes(SpielerCommand::unpause)))
                .then(Commands.literal("remove")
                        .requires(source -> source.getSender().hasPermission("tes.admin.spieler.remove"))
                        .then(Commands.argument("name", StringArgumentType.word())
                                .suggests(PlayerLookup::suggestOnlinePlayerNames)
                                .executes(SpielerCommand::removeStart)
                                .then(Commands.argument("token", StringArgumentType.word())
                                        .executes(SpielerCommand::removeConfirm))));
    }

    private static int add(CommandContext<CommandSourceStack> ctx) {
        CommandSender sender = ctx.getSource().getSender();
        String name = StringArgumentType.getString(ctx, "name");
        Optional<OfflinePlayer> target = PlayerLookup.resolveKnownPlayer(sender, name);
        if (target.isEmpty()) {
            return Command.SINGLE_SUCCESS;
        }
        OfflinePlayer offlinePlayer = target.get();
        if (repository().isRegistered(offlinePlayer.getUniqueId())) {
            sender.sendMessage(Messages.alreadyRegistered(name));
            return Command.SINGLE_SUCCESS;
        }
        repository().register(offlinePlayer.getUniqueId(), name);
        sender.sendMessage(Messages.registered(name));
        return Command.SINGLE_SUCCESS;
    }

    private static int pause(CommandContext<CommandSourceStack> ctx) {
        CommandSender sender = ctx.getSource().getSender();
        String name = StringArgumentType.getString(ctx, "name");
        Optional<PlayerRecord> record = PlayerLookup.requireRegistered(sender, repository(), name);
        if (record.isEmpty()) {
            return Command.SINGLE_SUCCESS;
        }
        if (record.get().paused()) {
            sender.sendMessage(Messages.alreadyPaused(name));
            return Command.SINGLE_SUCCESS;
        }
        repository().setPaused(record.get().uuid(), true);
        sender.sendMessage(Messages.paused(name));
        return Command.SINGLE_SUCCESS;
    }

    private static int unpause(CommandContext<CommandSourceStack> ctx) {
        CommandSender sender = ctx.getSource().getSender();
        String name = StringArgumentType.getString(ctx, "name");
        Optional<PlayerRecord> record = PlayerLookup.requireRegistered(sender, repository(), name);
        if (record.isEmpty()) {
            return Command.SINGLE_SUCCESS;
        }
        if (!record.get().paused()) {
            sender.sendMessage(Messages.notPaused(name));
            return Command.SINGLE_SUCCESS;
        }
        repository().setPaused(record.get().uuid(), false);
        sender.sendMessage(Messages.unpaused(name));
        return Command.SINGLE_SUCCESS;
    }

    private static int removeStart(CommandContext<CommandSourceStack> ctx) {
        CommandSender sender = ctx.getSource().getSender();
        String name = StringArgumentType.getString(ctx, "name");
        Optional<PlayerRecord> record = PlayerLookup.requireRegistered(sender, repository(), name);
        if (record.isEmpty()) {
            return Command.SINGLE_SUCCESS;
        }
        ConfirmationManager<UUID> confirmations = removeConfirmations();
        String token = confirmations.create(actorOf(sender), record.get().uuid());
        sender.sendMessage(Messages.removeConfirmPrompt(name, token, confirmations.ttl().toSeconds()));
        return Command.SINGLE_SUCCESS;
    }

    private static int removeConfirm(CommandContext<CommandSourceStack> ctx) {
        CommandSender sender = ctx.getSource().getSender();
        String name = StringArgumentType.getString(ctx, "name");
        String token = StringArgumentType.getString(ctx, "token");
        Optional<UUID> confirmed = removeConfirmations().consume(actorOf(sender), token);
        if (confirmed.isEmpty()) {
            sender.sendMessage(Messages.removeConfirmExpiredOrInvalid());
            return Command.SINGLE_SUCCESS;
        }
        repository().delete(confirmed.get());
        sender.sendMessage(Messages.removed(name));
        return Command.SINGLE_SUCCESS;
    }

    private static UUID actorOf(CommandSender sender) {
        return sender instanceof Player player ? player.getUniqueId() : CONSOLE_ACTOR;
    }

    private static PlayerRepository repository() {
        return TesseraniaEconomySystem.getPlugin(TesseraniaEconomySystem.class).playerRepository();
    }

    private static ConfirmationManager<UUID> removeConfirmations() {
        return TesseraniaEconomySystem.getPlugin(TesseraniaEconomySystem.class).removeConfirmations();
    }
}
