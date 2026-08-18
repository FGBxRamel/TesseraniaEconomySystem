package de.bydora.tes.command;

import org.bukkit.command.CommandSender;

import java.util.List;

/**
 * A subcommand of {@code /tes}, e.g. {@code /tes spieler ...}. Implementations register with
 * {@link TesCommand#register(TesSubCommand)}; this is the extensibility seam later stages
 * (shop, treuepunkte, erfahrungspunkte, level, rechnung, farmwelt, ...) plug into.
 */
public interface TesSubCommand {

    /**
     * The literal that selects this subcommand, e.g. {@code "spieler"}.
     */
    String name();

    /**
     * Handles {@code args} with the subcommand's own name already stripped. Implementations are
     * responsible for their own, finer-grained permission checks (e.g. per action) since a
     * subcommand's actions commonly map to distinct permission nodes.
     */
    void execute(CommandSender sender, String label, String[] args);

    /**
     * Tab-completion for this subcommand's arguments, with the subcommand's own name already
     * stripped from {@code args}.
     */
    default List<String> tabComplete(CommandSender sender, String[] args) {
        return List.of();
    }
}
