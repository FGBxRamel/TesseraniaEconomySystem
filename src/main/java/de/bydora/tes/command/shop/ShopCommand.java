package de.bydora.tes.command.shop;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import de.bydora.tes.TesseraniaEconomySystem;
import de.bydora.tes.config.TesConfig;
import de.bydora.tes.shop.ShopConversion;
import de.bydora.tes.shop.ShopEconomy;
import de.bydora.tes.shop.ShopRecord;
import de.bydora.tes.shop.ShopRegistry;
import de.bydora.tes.shop.ShopRepository;
import de.bydora.tes.shop.ShopTransactionRepository;
import de.bydora.tes.shop.session.ShopSessionManager;
import de.bydora.tes.util.Messages;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Implements {@code /tes shop erstellen|bearbeiten|schließen} (spec §3.1.1.1, UC1–UC3).
 * {@code liste} is added separately once pagination lands.
 */
public final class ShopCommand {

    private ShopCommand() {
    }

    public static LiteralArgumentBuilder<CommandSourceStack> build() {
        return Commands.literal("shop")
                .executes(ctx -> {
                    ctx.getSource().getSender().sendMessage(Messages.usage("/tes shop <erstellen|bearbeiten|schließen> <world> [id]"));
                    return Command.SINGLE_SUCCESS;
                })
                .then(Commands.literal("erstellen")
                        .requires(source -> source.getSender().hasPermission("tes.shop.create"))
                        .then(Commands.argument("world", StringArgumentType.word())
                                .suggests(ShopCommand::suggestWorlds)
                                .executes(ShopCommand::erstellen)))
                .then(Commands.literal("bearbeiten")
                        .requires(source -> source.getSender().hasPermission("tes.shop.edit"))
                        .then(Commands.argument("world", StringArgumentType.word())
                                .suggests(ShopCommand::suggestWorlds)
                                .then(Commands.argument("id", StringArgumentType.word())
                                        .executes(ShopCommand::bearbeiten))))
                .then(Commands.literal("schließen")
                        .requires(source -> source.getSender().hasPermission("tes.shop.close"))
                        .then(Commands.argument("world", StringArgumentType.word())
                                .suggests(ShopCommand::suggestWorlds)
                                .then(Commands.argument("id", StringArgumentType.word())
                                        .executes(ShopCommand::schliessen))));
    }

    private static CompletableFuture<Suggestions> suggestWorlds(CommandContext<CommandSourceStack> ctx, SuggestionsBuilder builder) {
        String remaining = builder.getRemaining().toLowerCase(Locale.ROOT);
        Bukkit.getWorlds().stream()
                .map(World::getName)
                .filter(name -> name.toLowerCase(Locale.ROOT).startsWith(remaining))
                .forEach(builder::suggest);
        return builder.buildFuture();
    }

    private static int erstellen(CommandContext<CommandSourceStack> ctx) {
        CommandSender sender = ctx.getSource().getSender();
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Messages.usage("/tes shop erstellen ist nur für Spieler verfügbar."));
            return Command.SINGLE_SUCCESS;
        }
        String worldName = StringArgumentType.getString(ctx, "world");
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            sender.sendMessage(Messages.shopUnknownWorld(worldName));
            return Command.SINGLE_SUCCESS;
        }
        TesConfig config = plugin().tesConfig();
        String lowerName = world.getName().toLowerCase(Locale.ROOT);
        if (config.shopRestrictedWorlds().contains(lowerName) || lowerName.startsWith("farmwelt-")) {
            sender.sendMessage(Messages.shopRestrictedWorld(world.getName()));
            return Command.SINGLE_SUCCESS;
        }
        ShopSessionManager sessions = plugin().shopSessionManager();
        if (sessions.active(player.getUniqueId()).isPresent()) {
            sender.sendMessage(Messages.shopSessionAlreadyActive());
            return Command.SINGLE_SUCCESS;
        }
        sessions.startCreate(player.getUniqueId(), world.getName());
        sender.sendMessage(Messages.shopCreateStart(world.getName()));
        return Command.SINGLE_SUCCESS;
    }

    private static int bearbeiten(CommandContext<CommandSourceStack> ctx) {
        CommandSender sender = ctx.getSource().getSender();
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Messages.usage("/tes shop bearbeiten ist nur für Spieler verfügbar."));
            return Command.SINGLE_SUCCESS;
        }
        Optional<ShopRecord> maybeShop = resolveOwnedShop(ctx, player);
        if (maybeShop.isEmpty()) {
            return Command.SINGLE_SUCCESS;
        }
        ShopRecord shop = maybeShop.get();
        ShopSessionManager sessions = plugin().shopSessionManager();
        if (sessions.active(player.getUniqueId()).isPresent()) {
            sender.sendMessage(Messages.shopSessionAlreadyActive());
            return Command.SINGLE_SUCCESS;
        }
        sessions.startEdit(player.getUniqueId(), shop);
        sender.sendMessage(Messages.shopEditStart(shop.world(), shop.id(), shop.name()));
        return Command.SINGLE_SUCCESS;
    }

    private static int schliessen(CommandContext<CommandSourceStack> ctx) {
        CommandSender sender = ctx.getSource().getSender();
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Messages.usage("/tes shop schließen ist nur für Spieler verfügbar."));
            return Command.SINGLE_SUCCESS;
        }
        Optional<ShopRecord> maybeShop = resolveOwnedShop(ctx, player);
        if (maybeShop.isEmpty()) {
            return Command.SINGLE_SUCCESS;
        }
        ShopRecord shop = maybeShop.get();
        World world = Bukkit.getWorld(shop.world());
        if (world == null || !player.getWorld().equals(world) || !isNear(player.getLocation(), shop)) {
            sender.sendMessage(Messages.shopTooFarToClose());
            return Command.SINGLE_SUCCESS;
        }

        ShopTransactionRepository transactionRepository = plugin().shopTransactionRepository();
        ShopEconomy.forceRefundPending(transactionRepository, shop, world);

        ShopRepository shopRepository = plugin().shopRepository();
        long now = System.currentTimeMillis();
        shopRepository.close(shop.world(), shop.id(), now);
        plugin().shopRegistry().unregister(shop.world(), shop.id());
        ShopConversion.removeFromShop(plugin(), world, shop.position());
        if (shop.secondaryPosition() != null) {
            ShopConversion.removeFromShop(plugin(), world, shop.secondaryPosition());
        }
        sender.sendMessage(Messages.shopClosed(shop.id(), shop.name()));
        return Command.SINGLE_SUCCESS;
    }

    private static boolean isNear(Location location, ShopRecord shop) {
        double proximity = plugin().tesConfig().shopProximityBlocks();
        double dx = location.getX() - (shop.position().x() + 0.5);
        double dy = location.getY() - (shop.position().y() + 0.5);
        double dz = location.getZ() - (shop.position().z() + 0.5);
        return (dx * dx + dy * dy + dz * dz) <= proximity * proximity;
    }

    private static Optional<ShopRecord> resolveOwnedShop(CommandContext<CommandSourceStack> ctx, Player player) {
        CommandSender sender = ctx.getSource().getSender();
        String worldName = StringArgumentType.getString(ctx, "world");
        String id = StringArgumentType.getString(ctx, "id");
        ShopRegistry registry = plugin().shopRegistry();
        Optional<ShopRecord> shop = registry.findById(worldName, id);
        if (shop.isEmpty()) {
            sender.sendMessage(Messages.shopNotFound(id));
            return Optional.empty();
        }
        if (!shop.get().isOwner(player.getUniqueId())) {
            sender.sendMessage(Messages.shopNotOwner());
            return Optional.empty();
        }
        return shop;
    }

    private static TesseraniaEconomySystem plugin() {
        return TesseraniaEconomySystem.getPlugin(TesseraniaEconomySystem.class);
    }
}
