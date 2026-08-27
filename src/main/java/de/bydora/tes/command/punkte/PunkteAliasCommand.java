package de.bydora.tes.command.punkte;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;

/**
 * Implements the {@code /punkte} alias for the Treuepunkteshop (spec §3.2): a bare invocation
 * opens the GUI, and {@code übertragen} shares {@link TreuepunkteCommand}'s subtree so
 * {@code /punkte übertragen} behaves identically to {@code /treuepunkte übertragen}, per spec.
 * No admin subtree — {@link TreuepunkteCommand} stays canonical for {@code add|remove|set}.
 */
public final class PunkteAliasCommand {

    private PunkteAliasCommand() {
    }

    public static LiteralArgumentBuilder<CommandSourceStack> build() {
        return Commands.literal("punkte")
                .requires(source -> source.getSender().hasPermission("tes.punkte"))
                .executes(TreuepunkteCommand::open)
                .then(TreuepunkteCommand.uebertragen());
    }
}
