package de.bydora.tes.command.punkte;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import de.bydora.tes.TesseraniaEconomySystem;
import de.bydora.tes.command.PlayerLookup;
import de.bydora.tes.data.PlayerRecord;
import de.bydora.tes.data.PlayerRepository;
import de.bydora.tes.util.Messages;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.command.CommandSender;

import java.util.Optional;
import java.util.UUID;
import java.util.function.ToIntFunction;

/**
 * Builds the {@code add|remove|set <Name> <Anzahl>} subtree shared by {@code /treuepunkte}
 * and {@code /erfahrungspunkte} (spec §1.4) — the two commands differ only in which
 * {@link PlayerRepository} counter they read/write and their permission/message labels.
 */
final class PunkteCommandFactory {

    private PunkteCommandFactory() {
    }

    /**
     * @param add          repository mutator for a relative adjustment (may be negative)
     * @param set          repository mutator for an absolute value
     * @param currentValue reads the counter back off a freshly-fetched {@link PlayerRecord}
     */
    record Counter(TriConsumer add, TriConsumer set, ToIntFunction<PlayerRecord> currentValue) {
    }

    @FunctionalInterface
    interface TriConsumer {
        void accept(PlayerRepository repository, UUID uuid, int amount);
    }

    static LiteralArgumentBuilder<CommandSourceStack> build(String literal, String permissionPrefix, String label, Counter counter) {
        return Commands.literal(literal)
                .executes(ctx -> {
                    ctx.getSource().getSender().sendMessage(Messages.usage("/" + literal + " <add|remove|set> <Name> <Anzahl>"));
                    return Command.SINGLE_SUCCESS;
                })
                .then(action("add", literal, permissionPrefix, label, counter))
                .then(action("remove", literal, permissionPrefix, label, counter))
                .then(action("set", literal, permissionPrefix, label, counter));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> action(String action, String literal, String permissionPrefix, String label, Counter counter) {
        int minAmount = action.equals("set") ? 0 : 1;
        return Commands.literal(action)
                .requires(source -> source.getSender().hasPermission(permissionPrefix + "." + action))
                .then(Commands.argument("name", StringArgumentType.word())
                        .suggests(PlayerLookup::suggestOnlinePlayerNames)
                        .then(Commands.argument("amount", IntegerArgumentType.integer(minAmount))
                                .executes(ctx -> execute(ctx, action, label, counter))));
    }

    private static int execute(CommandContext<CommandSourceStack> ctx, String action, String label, Counter counter) {
        CommandSender sender = ctx.getSource().getSender();
        String name = StringArgumentType.getString(ctx, "name");
        int amount = IntegerArgumentType.getInteger(ctx, "amount");
        PlayerRepository repository = repository();
        Optional<PlayerRecord> record = PlayerLookup.requireRegistered(sender, repository, name);
        if (record.isEmpty()) {
            return Command.SINGLE_SUCCESS;
        }
        UUID uuid = record.get().uuid();
        switch (action) {
            case "add" -> counter.add().accept(repository, uuid, amount);
            case "remove" -> counter.add().accept(repository, uuid, -amount);
            case "set" -> counter.set().accept(repository, uuid, amount);
            default -> throw new IllegalStateException("Unknown action: " + action);
        }
        int newValue = counter.currentValue().applyAsInt(repository.findByUuid(uuid).orElseThrow());
        sender.sendMessage(Messages.punkteUpdated(label, name, newValue));
        return Command.SINGLE_SUCCESS;
    }

    private static PlayerRepository repository() {
        return TesseraniaEconomySystem.getPlugin(TesseraniaEconomySystem.class).playerRepository();
    }
}
