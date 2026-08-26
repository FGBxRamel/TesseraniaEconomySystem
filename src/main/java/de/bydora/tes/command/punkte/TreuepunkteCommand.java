package de.bydora.tes.command.punkte;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import de.bydora.tes.TesseraniaEconomySystem;
import de.bydora.tes.command.PlayerLookup;
import de.bydora.tes.data.PlayerRecord;
import de.bydora.tes.data.PlayerRepository;
import de.bydora.tes.treueshop.TreueshopGui;
import de.bydora.tes.util.Messages;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Optional;

/**
 * Implements {@code /treuepunkte} (spec §3.2: a bare invocation opens the Treuepunkteshop),
 * {@code /treuepunkte add|remove|set <Name> <Anzahl>} (spec §1.4, admin), and
 * {@code /treuepunkte übertragen <Zielspieler> <Anzahl>} (spec §2, player-facing TP transfer,
 * capped at the sender's own balance — no fee or cooldown, per spec).
 */
public final class TreuepunkteCommand {

    private TreuepunkteCommand() {
    }

    public static LiteralArgumentBuilder<CommandSourceStack> build() {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("treuepunkte")
                .requires(source -> source.getSender().hasPermission("tes.punkte"))
                .executes(TreuepunkteCommand::open)
                .then(uebertragen());
        return PunkteCommandFactory.attachAdminActions(root, "tes.admin.treuepunkte", "Treuepunkte",
                new PunkteCommandFactory.Counter(
                        (repository, uuid, amount) -> repository.addTreuepunkte(uuid, amount),
                        (repository, uuid, amount) -> repository.setTreuepunkte(uuid, amount),
                        record -> record.treuepunkte()
                ));
    }

    static int open(CommandContext<CommandSourceStack> ctx) {
        CommandSender sender = ctx.getSource().getSender();
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Messages.usage("/treuepunkte ist nur für Spieler verfügbar."));
            return Command.SINGLE_SUCCESS;
        }
        Optional<PlayerRecord> record = PunkteCommandFactory.repository().findByUuid(player.getUniqueId());
        if (record.isEmpty() || record.get().paused()) {
            player.sendMessage(Messages.treueshopNotEligible());
            return Command.SINGLE_SUCCESS;
        }
        TreueshopGui.open(TesseraniaEconomySystem.getPlugin(TesseraniaEconomySystem.class), player);
        return Command.SINGLE_SUCCESS;
    }

    private static LiteralArgumentBuilder<CommandSourceStack> uebertragen() {
        return Commands.literal("übertragen")
                .requires(source -> source.getSender().hasPermission("tes.treuepunkte.uebertragen"))
                .then(Commands.argument("name", StringArgumentType.word())
                        .suggests(PlayerLookup::suggestOnlinePlayerNames)
                        .then(Commands.argument("anzahl", IntegerArgumentType.integer(1))
                                .executes(TreuepunkteCommand::uebertragen)));
    }

    private static int uebertragen(CommandContext<CommandSourceStack> ctx) {
        CommandSender sender = ctx.getSource().getSender();
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Messages.usage("/treuepunkte übertragen ist nur für Spieler verfügbar."));
            return Command.SINGLE_SUCCESS;
        }
        PlayerRepository repository = PunkteCommandFactory.repository();
        Optional<PlayerRecord> senderRecord = repository.findByUuid(player.getUniqueId());
        if (senderRecord.isEmpty() || senderRecord.get().paused()) {
            player.sendMessage(Messages.treueshopNotEligible());
            return Command.SINGLE_SUCCESS;
        }

        String name = StringArgumentType.getString(ctx, "name");
        Optional<PlayerRecord> targetRecord = PlayerLookup.requireRegistered(player, repository, name);
        if (targetRecord.isEmpty()) {
            return Command.SINGLE_SUCCESS;
        }
        if (targetRecord.get().uuid().equals(player.getUniqueId())) {
            player.sendMessage(Messages.treueshopTransferSelfTarget());
            return Command.SINGLE_SUCCESS;
        }

        int amount = IntegerArgumentType.getInteger(ctx, "anzahl");
        PlayerRepository.TransferResult result = repository.transferTreuepunkte(player.getUniqueId(), targetRecord.get().uuid(), amount);
        if (result == PlayerRepository.TransferResult.INSUFFICIENT) {
            player.sendMessage(Messages.treueshopInsufficientTp());
            return Command.SINGLE_SUCCESS;
        }

        int newBalance = repository.findByUuid(player.getUniqueId()).orElseThrow().treuepunkte();
        player.sendMessage(Messages.treueshopTransferSent(targetRecord.get().username(), amount, newBalance));
        Player targetPlayer = Bukkit.getPlayer(targetRecord.get().uuid());
        if (targetPlayer != null) {
            targetPlayer.sendMessage(Messages.treueshopTransferReceived(player.getName(), amount));
        }
        return Command.SINGLE_SUCCESS;
    }
}
