package de.bydora.tes.command.shop;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
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
import de.bydora.tes.shop.session.ShopSession;
import de.bydora.tes.shop.session.ShopSessionField;
import de.bydora.tes.shop.session.ShopSessionManager;
import de.bydora.tes.util.Messages;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Implements {@code /tes shop erstellen|bearbeiten|schließen|liste} (spec §3.1.1.1, UC1–UC3 plus
 * the shop list). Deviates from the literal spec syntax by dropping the {@code <world>} argument:
 * shop ids are enforced globally unique ({@link ShopRegistry}, {@code idx_shops_id_unique}), so
 * {@code erstellen} uses the player's current world and {@code bearbeiten}/{@code schließen}/
 * {@code tp} resolve a shop by id alone.
 */
public final class ShopCommand {

    private static final int SHOPS_PER_PAGE = 10;

    private ShopCommand() {
    }

    public static LiteralArgumentBuilder<CommandSourceStack> build() {
        return Commands.literal("shop")
                .executes(ctx -> {
                    ctx.getSource().getSender().sendMessage(Messages.usage("/tes shop <erstellen|bearbeiten|schließen|liste>"));
                    return Command.SINGLE_SUCCESS;
                })
                .then(Commands.literal("erstellen")
                        .requires(source -> source.getSender().hasPermission("tes.shop.create"))
                        .executes(ShopCommand::erstellen))
                .then(Commands.literal("bearbeiten")
                        .requires(source -> source.getSender().hasPermission("tes.shop.edit"))
                        .then(Commands.argument("id", StringArgumentType.word())
                                .suggests(ShopCommand::suggestOwnShopIds)
                                .executes(ShopCommand::bearbeiten)))
                .then(Commands.literal("schließen")
                        .requires(source -> source.getSender().hasPermission("tes.shop.close"))
                        .then(Commands.argument("id", StringArgumentType.word())
                                .suggests(ShopCommand::suggestOwnShopIds)
                                .executes(ShopCommand::schliessen)))
                .then(Commands.literal("liste")
                        .requires(source -> source.getSender().hasPermission("tes.shop.list"))
                        .executes(ctx -> liste(ctx, 1))
                        .then(Commands.argument("seite", IntegerArgumentType.integer(1))
                                .executes(ctx -> liste(ctx, IntegerArgumentType.getInteger(ctx, "seite")))))
                .then(Commands.literal("tp")
                        .requires(source -> source.getSender().hasPermission("tes.shop.list"))
                        .then(Commands.argument("id", StringArgumentType.word())
                                .suggests(ShopCommand::suggestOwnShopIds)
                                .executes(ShopCommand::teleport)))
                .then(Commands.literal("feld")
                        .then(Commands.argument("key", StringArgumentType.word())
                                .suggests(ShopCommand::suggestFieldKeys)
                                .executes(ShopCommand::feld)))
                .then(Commands.literal("bestaetigen").executes(ShopCommand::bestaetigen))
                .then(Commands.literal("abbrechen").executes(ShopCommand::abbrechen));
    }

    private static CompletableFuture<Suggestions> suggestOwnShopIds(CommandContext<CommandSourceStack> ctx, SuggestionsBuilder builder) {
        String remaining = builder.getRemaining().toLowerCase(Locale.ROOT);
        if (ctx.getSource().getSender() instanceof Player player) {
            plugin().shopRegistry().allByOwner(player.getUniqueId()).stream()
                    .map(ShopRecord::id)
                    .filter(id -> id.toLowerCase(Locale.ROOT).startsWith(remaining))
                    .forEach(builder::suggest);
        }
        return builder.buildFuture();
    }

    private static CompletableFuture<Suggestions> suggestFieldKeys(CommandContext<CommandSourceStack> ctx, SuggestionsBuilder builder) {
        String remaining = builder.getRemaining().toLowerCase(Locale.ROOT);
        if (ctx.getSource().getSender() instanceof Player player) {
            plugin().shopSessionManager().active(player.getUniqueId())
                    .map(ShopSession::mode)
                    .ifPresent(mode -> ShopSessionField.visibleFor(mode).stream()
                            .map(ShopSessionField::key)
                            .filter(key -> key.startsWith(remaining))
                            .forEach(builder::suggest));
        }
        return builder.buildFuture();
    }

    private static int erstellen(CommandContext<CommandSourceStack> ctx) {
        CommandSender sender = ctx.getSource().getSender();
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Messages.usage("/tes shop erstellen ist nur für Spieler verfügbar."));
            return Command.SINGLE_SUCCESS;
        }
        World world = player.getWorld();
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
        ShopSession session = sessions.startCreate(player.getUniqueId(), world.getName());
        sender.sendMessage(Messages.shopCreateStart(world.getName()));
        sender.sendMessage(plugin().shopChatListener().renderMenu(session));
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
        ShopSession session = sessions.startEdit(player.getUniqueId(), shop);
        sender.sendMessage(Messages.shopEditStart(shop.world(), shop.id(), shop.name()));
        sender.sendMessage(plugin().shopChatListener().renderMenu(session));
        return Command.SINGLE_SUCCESS;
    }

    private static int feld(CommandContext<CommandSourceStack> ctx) {
        CommandSender sender = ctx.getSource().getSender();
        if (!(sender instanceof Player player)) {
            return Command.SINGLE_SUCCESS;
        }
        Optional<ShopSessionField> field = ShopSessionField.fromKey(StringArgumentType.getString(ctx, "key"));
        field.ifPresent(f -> plugin().shopChatListener().armField(player, f));
        return Command.SINGLE_SUCCESS;
    }

    private static int bestaetigen(CommandContext<CommandSourceStack> ctx) {
        CommandSender sender = ctx.getSource().getSender();
        if (sender instanceof Player player) {
            plugin().shopChatListener().confirmFromClick(player);
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int abbrechen(CommandContext<CommandSourceStack> ctx) {
        CommandSender sender = ctx.getSource().getSender();
        if (sender instanceof Player player) {
            plugin().shopChatListener().cancelFromClick(player);
        }
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
        shopRepository.delete(shop.id());
        plugin().shopRegistry().unregister(shop.id());
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
        String id = StringArgumentType.getString(ctx, "id");
        ShopRegistry registry = plugin().shopRegistry();
        Optional<ShopRecord> shop = registry.findById(id);
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

    private static int liste(CommandContext<CommandSourceStack> ctx, int requestedPage) {
        CommandSender sender = ctx.getSource().getSender();
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Messages.usage("/tes shop liste ist nur für Spieler verfügbar."));
            return Command.SINGLE_SUCCESS;
        }
        List<ShopRecord> shops = plugin().shopRegistry().allByOwner(player.getUniqueId()).stream()
                .sorted(Comparator.comparingLong(ShopRecord::createdAt))
                .toList();
        if (shops.isEmpty()) {
            sender.sendMessage(Messages.shopListEmpty());
            return Command.SINGLE_SUCCESS;
        }
        int totalPages = (shops.size() + SHOPS_PER_PAGE - 1) / SHOPS_PER_PAGE;
        int page = Math.max(1, Math.min(requestedPage, totalPages));
        int fromIndex = (page - 1) * SHOPS_PER_PAGE;
        int toIndex = Math.min(fromIndex + SHOPS_PER_PAGE, shops.size());

        sender.sendMessage(Messages.shopListHeader(page, totalPages));
        for (ShopRecord shop : shops.subList(fromIndex, toIndex)) {
            sender.sendMessage(Messages.shopListEntry(shop.id(), shop.world(), shop.name(), ShopRecord.itemDisplayName(shop.item()), shop.price()));
        }
        if (totalPages > 1) {
            sender.sendMessage(Messages.shopListNav(page, totalPages));
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int teleport(CommandContext<CommandSourceStack> ctx) {
        CommandSender sender = ctx.getSource().getSender();
        if (!(sender instanceof Player player)) {
            return Command.SINGLE_SUCCESS;
        }
        Optional<ShopRecord> maybeShop = resolveOwnedShop(ctx, player);
        if (maybeShop.isEmpty()) {
            return Command.SINGLE_SUCCESS;
        }
        ShopRecord shop = maybeShop.get();
        Location destination = shop.teleportPoint() != null
                ? new Location(Bukkit.getWorld(shop.teleportPoint().world()), shop.teleportPoint().x(), shop.teleportPoint().y(),
                        shop.teleportPoint().z(), shop.teleportPoint().yaw(), shop.teleportPoint().pitch())
                : safeLocationAboveShop(shop);
        if (destination == null || destination.getWorld() == null) {
            sender.sendMessage(Messages.shopUnknownWorld(shop.world()));
            return Command.SINGLE_SUCCESS;
        }
        player.teleport(destination);
        sender.sendMessage(Messages.shopTeleporting(shop.name()));
        return Command.SINGLE_SUCCESS;
    }

    /**
     * Standing directly on top of the shop's block avoids teleporting the player into the
     * container itself (suffocation risk, called out explicitly in spec §3.1.1.1).
     */
    private static Location safeLocationAboveShop(ShopRecord shop) {
        World world = Bukkit.getWorld(shop.world());
        if (world == null) {
            return null;
        }
        return new Location(world, shop.position().x() + 0.5, shop.position().y() + 1, shop.position().z() + 0.5);
    }

    private static TesseraniaEconomySystem plugin() {
        return TesseraniaEconomySystem.getPlugin(TesseraniaEconomySystem.class);
    }
}
