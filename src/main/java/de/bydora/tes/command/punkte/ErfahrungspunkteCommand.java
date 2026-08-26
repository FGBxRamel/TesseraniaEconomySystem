package de.bydora.tes.command.punkte;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import de.bydora.tes.util.Messages;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;

/**
 * Implements {@code /erfahrungspunkte add|remove|set <Name> <Anzahl>} (spec §1.4).
 */
public final class ErfahrungspunkteCommand {

    private ErfahrungspunkteCommand() {
    }

    public static LiteralArgumentBuilder<CommandSourceStack> build() {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("erfahrungspunkte")
                .executes(ctx -> {
                    ctx.getSource().getSender().sendMessage(Messages.usage("/erfahrungspunkte <add|remove|set> <Name> <Anzahl>"));
                    return Command.SINGLE_SUCCESS;
                });
        return PunkteCommandFactory.attachAdminActions(root, "tes.admin.erfahrungspunkte", "Erfahrungspunkte",
                new PunkteCommandFactory.Counter(
                        (repository, uuid, amount) -> repository.addErfahrungspunkte(uuid, amount),
                        (repository, uuid, amount) -> repository.setErfahrungspunkte(uuid, amount),
                        record -> record.erfahrungspunkte()
                ));
    }
}
