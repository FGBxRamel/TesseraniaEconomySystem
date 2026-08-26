package de.bydora.tes.command.punkte;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;

/**
 * Implements the bare {@code /punkte} alias for opening the Treuepunkteshop (spec §3.2) — a thin
 * top-level command with no admin subtree. {@link TreuepunkteCommand} is the canonical command
 * carrying the {@code add|remove|set} admin actions and TP transfer.
 */
public final class PunkteAliasCommand {

    private PunkteAliasCommand() {
    }

    public static LiteralArgumentBuilder<CommandSourceStack> build() {
        return Commands.literal("punkte")
                .requires(source -> source.getSender().hasPermission("tes.punkte"))
                .executes(TreuepunkteCommand::open);
    }
}
