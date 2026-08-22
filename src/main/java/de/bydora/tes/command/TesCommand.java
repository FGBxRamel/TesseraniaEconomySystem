package de.bydora.tes.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import de.bydora.tes.command.punkte.ErfahrungspunkteCommand;
import de.bydora.tes.command.punkte.TreuepunkteCommand;
import de.bydora.tes.command.shop.ShopCommand;
import de.bydora.tes.command.spieler.SpielerCommand;
import de.bydora.tes.util.Messages;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;

/**
 * Builds the {@code /tes} Brigadier command tree. Later stages add their subcommand's tree here
 * (level, rechnung, farmwelt, ...) via another {@code .then(...)}.
 */
public final class TesCommand {

    private TesCommand() {
    }

    public static LiteralArgumentBuilder<CommandSourceStack> createCommand() {
        return Commands.literal("tes")
                .executes(ctx -> {
                    ctx.getSource().getSender().sendMessage(Messages.usage("/tes <spieler|shop|treuepunkte|erfahrungspunkte> ..."));
                    return Command.SINGLE_SUCCESS;
                })
                .then(SpielerCommand.build())
                .then(ShopCommand.build())
                .then(TreuepunkteCommand.build())
                .then(ErfahrungspunkteCommand.build());
    }
}
