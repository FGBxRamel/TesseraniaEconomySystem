package de.bydora.tes.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import de.bydora.tes.command.spieler.SpielerCommand;
import de.bydora.tes.util.Messages;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;

/**
 * Builds the {@code /tes} Brigadier command tree. Later stages add their subcommand's tree here
 * (shop, treuepunkte, erfahrungspunkte, level, rechnung, farmwelt, ...) via another
 * {@code .then(...)}.
 */
public final class TesCommand {

    private TesCommand() {
    }

    public static LiteralArgumentBuilder<CommandSourceStack> createCommand() {
        return Commands.literal("tes")
                .executes(ctx -> {
                    ctx.getSource().getSender().sendMessage(Messages.usage("/tes <spieler> ..."));
                    return Command.SINGLE_SUCCESS;
                })
                .then(SpielerCommand.build());
    }
}
