package de.bydora.tes.command.rechnung;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import de.bydora.tes.TesseraniaEconomySystem;
import de.bydora.tes.command.PlayerLookup;
import de.bydora.tes.data.PlayerRecord;
import de.bydora.tes.invoice.InvoiceGui;
import de.bydora.tes.util.Messages;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Optional;

/**
 * Implements {@code /tes rechnung erstellen|anzeigen} (spec §3.1.1.3): the Dienstleistungen/
 * Trödelmarkt invoice flow.
 */
public final class RechnungCommand {

    private static final int GRUND_MAX_LENGTH = 50;
    private static final int PREIS_MAX = 2304;

    private RechnungCommand() {
    }

    public static LiteralArgumentBuilder<CommandSourceStack> build() {
        return Commands.literal("rechnung")
                .executes(ctx -> {
                    ctx.getSource().getSender().sendMessage(Messages.usage("/tes rechnung <erstellen|anzeigen>"));
                    return Command.SINGLE_SUCCESS;
                })
                .then(Commands.literal("erstellen")
                        .requires(source -> source.getSender().hasPermission("tes.rechnung.erstellen"))
                        .then(Commands.argument("ziel", StringArgumentType.word())
                                .suggests(PlayerLookup::suggestOnlinePlayerNames)
                                .then(Commands.argument("preis", IntegerArgumentType.integer(1))
                                        .then(Commands.argument("grund", StringArgumentType.greedyString())
                                                .executes(RechnungCommand::erstellen)))))
                .then(Commands.literal("anzeigen")
                        .requires(source -> source.getSender().hasPermission("tes.rechnung.anzeigen"))
                        .executes(RechnungCommand::anzeigen));
    }

    private static int erstellen(CommandContext<CommandSourceStack> ctx) {
        CommandSender sender = ctx.getSource().getSender();
        if (!(sender instanceof Player creator)) {
            sender.sendMessage(Messages.usage("/tes rechnung erstellen ist nur für Spieler verfügbar."));
            return Command.SINGLE_SUCCESS;
        }

        TesseraniaEconomySystem plugin = plugin();
        Optional<PlayerRecord> creatorRecord = plugin.playerRepository().findByUuid(creator.getUniqueId());
        if (creatorRecord.isEmpty()) {
            creator.sendMessage(Messages.notRegistered(creator.getName()));
            return Command.SINGLE_SUCCESS;
        }
        if (creatorRecord.get().paused()) {
            creator.sendMessage(Messages.senderPaused());
            return Command.SINGLE_SUCCESS;
        }

        String targetName = StringArgumentType.getString(ctx, "ziel");
        Optional<OfflinePlayer> target = PlayerLookup.resolveKnownPlayer(creator, targetName);
        if (target.isEmpty()) {
            return Command.SINGLE_SUCCESS;
        }
        if (target.get().getUniqueId().equals(creator.getUniqueId())) {
            creator.sendMessage(Messages.invoiceSelfTarget());
            return Command.SINGLE_SUCCESS;
        }

        String grund = StringArgumentType.getString(ctx, "grund").trim();
        if (grund.isEmpty() || grund.length() > GRUND_MAX_LENGTH) {
            creator.sendMessage(Messages.rechnungGrundInvalid(GRUND_MAX_LENGTH));
            return Command.SINGLE_SUCCESS;
        }

        int preis = IntegerArgumentType.getInteger(ctx, "preis");
        if (preis > PREIS_MAX) {
            creator.sendMessage(Messages.rechnungPreisZuHoch(PREIS_MAX));
            return Command.SINGLE_SUCCESS;
        }
        plugin.invoiceRepository().insert(creator.getUniqueId(), target.get().getUniqueId(), preis, grund, System.currentTimeMillis());
        creator.sendMessage(Messages.invoiceCreated(targetName, preis, grund));
        notifyTarget(plugin, target.get(), creator.getName(), preis, grund);
        return Command.SINGLE_SUCCESS;
    }

    private static void notifyTarget(TesseraniaEconomySystem plugin, OfflinePlayer target, String creatorName, int price, String reason) {
        Player targetPlayer = target.getPlayer();
        if (targetPlayer != null) {
            targetPlayer.sendMessage(Messages.invoiceCreatedNotification(creatorName, price, reason));
        } else {
            plugin.pendingNotificationRepository().enqueue(target.getUniqueId(), Messages.invoiceCreatedText(creatorName, price, reason));
        }
    }

    private static int anzeigen(CommandContext<CommandSourceStack> ctx) {
        CommandSender sender = ctx.getSource().getSender();
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Messages.usage("/tes rechnung anzeigen ist nur für Spieler verfügbar."));
            return Command.SINGLE_SUCCESS;
        }
        InvoiceGui.open(plugin(), player);
        return Command.SINGLE_SUCCESS;
    }

    private static TesseraniaEconomySystem plugin() {
        return TesseraniaEconomySystem.getPlugin(TesseraniaEconomySystem.class);
    }
}
