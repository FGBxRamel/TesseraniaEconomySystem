package de.bydora.tes.command;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Top-level executor and tab-completer for {@code /tes}. Dispatches to registered
 * {@link TesSubCommand}s by their first argument.
 */
public final class TesCommand implements CommandExecutor, TabCompleter {

    private final Map<String, TesSubCommand> subCommands = new LinkedHashMap<>();

    public void register(TesSubCommand subCommand) {
        subCommands.put(subCommand.name().toLowerCase(), subCommand);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(Component.text("Verwendung: /" + label + " <" + String.join("|", subCommands.keySet()) + "> ...", NamedTextColor.RED));
            return true;
        }
        TesSubCommand subCommand = subCommands.get(args[0].toLowerCase());
        if (subCommand == null) {
            sender.sendMessage(Component.text("Unbekannter Unterbefehl: " + args[0], NamedTextColor.RED));
            return true;
        }
        subCommand.execute(sender, label, Arrays.copyOfRange(args, 1, args.length));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
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
                return subCommand.tabComplete(sender, Arrays.copyOfRange(args, 1, args.length));
            }
        }
        return List.of();
    }
}
