package de.bydora.tes.command.debug;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import de.bydora.tes.util.Messages;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.command.brigadier.argument.resolvers.BlockPositionResolver;
import io.papermc.paper.math.BlockPosition;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;

import java.util.Optional;

/**
 * Implements {@code /debug dump <Position>}: an admin-only tool that reads a container's or
 * sign's exact contents at a given block position and hands them to the tester as a single
 * clipboard-copyable chat message, for cross-referencing against the spec's in-world reference
 * GUIs. See {@code docs/gui-reference-capture.md} for the workflow this feeds.
 */
public final class DebugCommand {

    private DebugCommand() {
    }

    public static LiteralArgumentBuilder<CommandSourceStack> build() {
        return Commands.literal("debug")
                .executes(ctx -> {
                    ctx.getSource().getSender().sendMessage(Messages.usage("/debug dump <Position>"));
                    return Command.SINGLE_SUCCESS;
                })
                .then(Commands.literal("dump")
                        .requires(source -> source.getSender().hasPermission("tes.admin.debug.dump"))
                        .then(Commands.argument("position", ArgumentTypes.blockPosition())
                                .executes(DebugCommand::dump)));
    }

    private static int dump(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        CommandSender sender = ctx.getSource().getSender();
        BlockPositionResolver resolver = ctx.getArgument("position", BlockPositionResolver.class);
        BlockPosition position = resolver.resolve(ctx.getSource());
        World world = ctx.getSource().getLocation().getWorld();
        Block block = world.getBlockAt(position.blockX(), position.blockY(), position.blockZ());

        Optional<ContainerDump> dump = ContainerDumpFormatter.format(block);
        if (dump.isEmpty()) {
            sender.sendMessage(Messages.debugDumpUnsupported());
            return Command.SINGLE_SUCCESS;
        }
        sender.sendMessage(Messages.debugDumpReady(dump.get().summary(), dump.get().plainText()));
        return Command.SINGLE_SUCCESS;
    }
}
