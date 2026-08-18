package de.bydora.tes.command;

import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Top-level {@code /tes} command. Dispatches to registered {@link TesSubCommand}s by their
 * first argument. Implemented as a Paper {@link BasicCommand} (not a YAML-declared command via
 * {@code plugin.yml}/{@code paper-plugin.yml}), since Paper plugins register commands
 * programmatically.
 */
public final class TesCommand implements BasicCommand {

    private final Map<String, TesSubCommand> subCommands = new LinkedHashMap<>();

    public void register(TesSubCommand subCommand) {
        subCommands.put(subCommand.name().toLowerCase(), subCommand);
    }

    @Override
    public void execute(CommandSourceStack source, String[] args) {
        CommandSender sender = source.getSender();
        if (args.length == 0) {
            sender.sendMessage(Component.text("Verwendung: /tes <" + String.join("|", subCommands.keySet()) + "> ...", NamedTextColor.RED));
            return;
        }
        TesSubCommand subCommand = subCommands.get(args[0].toLowerCase());
        if (subCommand == null) {
            sender.sendMessage(Component.text("Unbekannter Unterbefehl: " + args[0], NamedTextColor.RED));
            return;
        }
        subCommand.execute(sender, Arrays.copyOfRange(args, 1, args.length));
    }

    @Override
    public Collection<String> suggest(CommandSourceStack source, String[] args) {
        if (args.length == 1) {
            List<String> matches = new ArrayList<>();
            for (String name : subCommands.keySet()) {
                if (name.startsWith(args[0].toLowerCase())) {
                    matches.add(name);
                }
            }
            return matches;
        }
        if (args.length > 1) {
            TesSubCommand subCommand = subCommands.get(args[0].toLowerCase());
            if (subCommand != null) {
                return subCommand.tabComplete(source.getSender(), Arrays.copyOfRange(args, 1, args.length));
            }
        }
        return List.of();
    }
}
