package de.bydora.tes.command.handelsbonus;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import de.bydora.tes.TesseraniaEconomySystem;
import de.bydora.tes.command.PlayerLookup;
import de.bydora.tes.data.PlayerRecord;
import de.bydora.tes.handelsbonus.HandelsbonusRepository;
import de.bydora.tes.util.Messages;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Implements {@code /handelsbonus reset <Name>} (admin-only, no spec counterpart): clears a
 * player's post-trigger Handelsbonus cooldown (spec §3.2.1.1, Belohnung 4) so they can trigger it
 * again immediately, without touching any unused discount balance they still hold — see
 * {@link HandelsbonusRepository#resetCooldown}.
 */
public final class HandelsbonusCommand {

    private HandelsbonusCommand() {
    }

    public static LiteralArgumentBuilder<CommandSourceStack> build() {
        return Commands.literal("handelsbonus")
                .then(reset());
    }

    private static LiteralArgumentBuilder<CommandSourceStack> reset() {
        return Commands.literal("reset")
                .requires(source -> source.getSender().hasPermission("tes.admin.handelsbonus.reset"))
                .then(Commands.argument("name", StringArgumentType.word())
                        .suggests(HandelsbonusCommand::suggestActiveHolderNames)
                        .executes(HandelsbonusCommand::reset));
    }

    /**
     * Only suggests players currently holding the Handelsbonus (an active cooldown per
     * {@link HandelsbonusRepository#onCooldown}) — the only players {@code reset} can act on.
     */
    private static CompletableFuture<Suggestions> suggestActiveHolderNames(CommandContext<CommandSourceStack> ctx, SuggestionsBuilder builder) {
        String remaining = builder.getRemaining().toLowerCase(Locale.ROOT);
        TesseraniaEconomySystem plugin = TesseraniaEconomySystem.getPlugin(TesseraniaEconomySystem.class);
        plugin.handelsbonusRepository().onCooldown(System.currentTimeMillis()).stream()
                .map(Bukkit::getOfflinePlayer)
                .map(OfflinePlayer::getName)
                .filter(name -> name != null && name.toLowerCase(Locale.ROOT).startsWith(remaining))
                .forEach(builder::suggest);
        return builder.buildFuture();
    }

    private static int reset(CommandContext<CommandSourceStack> ctx) {
        CommandSender sender = ctx.getSource().getSender();
        String name = StringArgumentType.getString(ctx, "name");

        TesseraniaEconomySystem plugin = TesseraniaEconomySystem.getPlugin(TesseraniaEconomySystem.class);
        Optional<PlayerRecord> targetRecord = PlayerLookup.requireRegistered(sender, plugin.playerRepository(), name);
        if (targetRecord.isEmpty()) {
            return Command.SINGLE_SUCCESS;
        }

        boolean reset = plugin.handelsbonusRepository().resetCooldown(targetRecord.get().uuid(), System.currentTimeMillis());
        if (!reset) {
            sender.sendMessage(Messages.handelsbonusNoCooldownToReset(name));
            return Command.SINGLE_SUCCESS;
        }

        sender.sendMessage(Messages.handelsbonusCooldownReset(name));
        Player targetPlayer = Bukkit.getPlayer(targetRecord.get().uuid());
        if (targetPlayer != null) {
            targetPlayer.sendMessage(Messages.handelsbonusCooldownResetNotice());
        }
        return Command.SINGLE_SUCCESS;
    }
}
