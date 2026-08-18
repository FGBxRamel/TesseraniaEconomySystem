package de.bydora.tes.shop.session;

import de.bydora.tes.command.PlayerLookup;
import de.bydora.tes.shop.BlockPos;
import de.bydora.tes.shop.ShopConversion;
import de.bydora.tes.shop.ShopRecord;
import de.bydora.tes.shop.ShopRegistry;
import de.bydora.tes.shop.ShopRepository;
import de.bydora.tes.shop.TeleportPoint;
import de.bydora.tes.util.Messages;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.DoubleChestInventory;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.Optional;

/**
 * Drives the chat-driven {@code /tes shop erstellen|bearbeiten} conversation (spec §3.1.1.1, UX
 * modeled on the BlueMap-Marker plugin): captures chat messages while a {@link ShopSession} is
 * active instead of broadcasting them, and captures the shop's block position via a right-click
 * while {@link ShopSessionStep#POSITION} is active.
 */
public final class ShopChatListener implements Listener {

    private final Plugin plugin;
    private final ShopSessionManager sessionManager;
    private final ShopRepository shopRepository;
    private final ShopRegistry shopRegistry;

    public ShopChatListener(Plugin plugin, ShopSessionManager sessionManager, ShopRepository shopRepository, ShopRegistry shopRegistry) {
        this.plugin = plugin;
        this.sessionManager = sessionManager;
        this.shopRepository = shopRepository;
        this.shopRegistry = shopRegistry;
    }

    @EventHandler(ignoreCancelled = true)
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        if (sessionManager.active(player.getUniqueId()).isEmpty()) {
            return;
        }
        event.setCancelled(true);
        String text = PlainTextComponentSerializer.plainText().serialize(event.message());
        Bukkit.getScheduler().runTask(plugin, () -> handleInput(player, text));
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        Player player = event.getPlayer();
        Optional<ShopSession> maybeSession = sessionManager.active(player.getUniqueId());
        if (maybeSession.isEmpty() || maybeSession.get().step() != ShopSessionStep.POSITION) {
            return;
        }
        ShopSession session = maybeSession.get();
        Block block = event.getClickedBlock();
        if (block == null) {
            return;
        }
        event.setCancelled(true);
        handlePosition(player, session, block);
    }

    private void handleInput(Player player, String rawText) {
        Optional<ShopSession> maybeSession = sessionManager.active(player.getUniqueId());
        if (maybeSession.isEmpty()) {
            player.sendMessage(Messages.shopSessionExpired());
            return;
        }
        ShopSession session = maybeSession.get();
        String text = rawText.trim();
        if (text.equalsIgnoreCase("abbrechen")) {
            sessionManager.cancel(player.getUniqueId());
            player.sendMessage(Messages.shopSessionCancelled());
            return;
        }
        switch (session.step()) {
            case ID -> handleId(player, session, text);
            case NAME -> handleName(player, session, text);
            case OWNERS -> handleOwners(player, session, text);
            case POSITION -> player.sendMessage(Messages.shopPromptPosition());
            case ITEM -> handleItem(player, session, text);
            case PRICE -> handlePrice(player, session, text);
            case TELEPORT -> handleTeleport(player, session, text);
            case CONFIRM -> handleConfirm(player, session, text);
        }
    }

    private void handleId(Player player, ShopSession session, String text) {
        if (!text.matches("[A-Za-z0-9_-]{1,32}")) {
            player.sendMessage(Messages.shopIdInvalid());
            return;
        }
        if (shopRepository.existsId(session.world(), text) || shopRegistry.existsId(session.world(), text)) {
            player.sendMessage(Messages.shopIdTaken(text));
            return;
        }
        session.id(text);
        session.advanceTo(ShopSessionStep.NAME);
        player.sendMessage(Messages.shopPromptName());
    }

    private void handleName(Player player, ShopSession session, String text) {
        if (text.isBlank() || text.length() > 48) {
            player.sendMessage(Messages.shopNameInvalid());
            return;
        }
        session.name(text);
        session.advanceTo(ShopSessionStep.OWNERS);
        player.sendMessage(Messages.shopPromptOwners());
    }

    private void handleOwners(Player player, ShopSession session, String text) {
        if (!text.equalsIgnoreCase("keine")) {
            for (String token : text.split("\\s+")) {
                Optional<OfflinePlayer> resolved = PlayerLookup.resolveKnownPlayer(player, token);
                if (resolved.isEmpty()) {
                    return;
                }
                session.owners().add(resolved.get().getUniqueId());
            }
        }
        if (session.mode() == ShopSessionMode.CREATE) {
            session.advanceTo(ShopSessionStep.POSITION);
            player.sendMessage(Messages.shopPromptPosition());
        } else {
            session.advanceTo(ShopSessionStep.ITEM);
            player.sendMessage(Messages.shopPromptItem());
        }
    }

    private void handlePosition(Player player, ShopSession session, Block block) {
        if (!block.getWorld().getName().equals(session.world())) {
            player.sendMessage(Messages.shopWrongWorldForPosition());
            return;
        }
        Material type = block.getType();
        if (!ShopConversion.isAllowedContainer(type)) {
            player.sendMessage(Messages.shopInvalidContainer());
            return;
        }
        BlockPos position = new BlockPos(block.getX(), block.getY(), block.getZ());
        if (shopRegistry.findByPosition(session.world(), position).isPresent()) {
            player.sendMessage(Messages.shopPositionAlreadyUsed());
            return;
        }
        BlockPos secondary = resolveSecondaryPosition(block);
        if (secondary != null && shopRegistry.findByPosition(session.world(), secondary).isPresent()) {
            player.sendMessage(Messages.shopPositionAlreadyUsed());
            return;
        }
        session.position(position, secondary, type);
        session.advanceTo(ShopSessionStep.ITEM);
        player.sendMessage(Messages.shopPromptItem());
    }

    private BlockPos resolveSecondaryPosition(Block block) {
        if (!(block.getState() instanceof Chest chest)) {
            return null;
        }
        if (!(chest.getInventory() instanceof DoubleChestInventory doubleChestInventory)) {
            return null;
        }
        Block leftBlock = blockOf(doubleChestInventory.getLeftSide().getHolder());
        Block rightBlock = blockOf(doubleChestInventory.getRightSide().getHolder());
        if (leftBlock == null || rightBlock == null) {
            return null;
        }
        Block other = leftBlock.equals(block) ? rightBlock : leftBlock;
        return new BlockPos(other.getX(), other.getY(), other.getZ());
    }

    private Block blockOf(InventoryHolder holder) {
        return holder instanceof Chest chest ? chest.getBlock() : null;
    }

    private void handleItem(Player player, ShopSession session, String text) {
        Material material;
        if (text.equalsIgnoreCase("hand")) {
            ItemStack inHand = player.getInventory().getItemInMainHand();
            if (inHand.getType() == Material.AIR) {
                player.sendMessage(Messages.shopItemInvalid(text));
                return;
            }
            material = inHand.getType();
        } else {
            material = Material.matchMaterial(text);
            if (material == null || !material.isItem()) {
                player.sendMessage(Messages.shopItemInvalid(text));
                return;
            }
        }
        session.item(material);
        session.advanceTo(ShopSessionStep.PRICE);
        player.sendMessage(Messages.shopPromptPrice());
    }

    private void handlePrice(Player player, ShopSession session, String text) {
        int price;
        try {
            price = Integer.parseInt(text);
        } catch (NumberFormatException e) {
            player.sendMessage(Messages.shopPriceInvalid());
            return;
        }
        if (price <= 0) {
            player.sendMessage(Messages.shopPriceInvalid());
            return;
        }
        session.price(price);
        session.advanceTo(ShopSessionStep.TELEPORT);
        player.sendMessage(Messages.shopPromptTeleport());
    }

    private void handleTeleport(Player player, ShopSession session, String text) {
        if (text.equalsIgnoreCase("hier")) {
            Location location = player.getLocation();
            session.teleportPoint(new TeleportPoint(
                    location.getWorld().getName(), location.getX(), location.getY(), location.getZ(),
                    location.getYaw(), location.getPitch()));
        } else if (!text.equalsIgnoreCase("nein")) {
            player.sendMessage(Messages.shopPromptTeleport());
            return;
        }
        session.advanceTo(ShopSessionStep.CONFIRM);
        player.sendMessage(Messages.shopPromptConfirm(summarize(session)));
    }

    private void handleConfirm(Player player, ShopSession session, String text) {
        if (text.equalsIgnoreCase("bestätigen")) {
            finalizeSession(player, session);
            sessionManager.cancel(player.getUniqueId());
        } else if (text.equalsIgnoreCase("abbrechen")) {
            sessionManager.cancel(player.getUniqueId());
            player.sendMessage(Messages.shopSessionCancelled());
        } else {
            player.sendMessage(Messages.shopConfirmInvalidInput());
        }
    }

    private void finalizeSession(Player player, ShopSession session) {
        long now = System.currentTimeMillis();
        if (session.mode() == ShopSessionMode.CREATE) {
            ShopRecord record = new ShopRecord(
                    session.id(), session.world(), session.name(), session.item(), session.price(),
                    session.containerType(), session.position(), session.secondaryPosition(),
                    session.teleportPoint(), session.owners(), now, now, null);
            shopRepository.insert(record);
            shopRegistry.register(record);
            ShopConversion.applyToShop(plugin, record);
            player.sendMessage(Messages.shopCreated(record.id(), record.name()));
        } else {
            ShopRecord existing = shopRegistry.findById(session.world(), session.editingId()).orElseThrow();
            ShopRecord updated = new ShopRecord(
                    existing.id(), existing.world(), session.name(), session.item(), session.price(),
                    existing.containerType(), existing.position(), existing.secondaryPosition(),
                    session.teleportPoint(), session.owners(), existing.createdAt(), now, existing.closedAt());
            shopRepository.updateAttributes(updated);
            shopRepository.replaceOwners(updated.world(), updated.id(), updated.owners());
            shopRegistry.register(updated);
            ShopConversion.applyToShop(plugin, updated);
            player.sendMessage(Messages.shopUpdated(updated.id(), updated.name()));
        }
    }

    private Component summarize(ShopSession session) {
        StringBuilder owners = new StringBuilder();
        for (var owner : session.owners()) {
            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(owner);
            if (!owners.isEmpty()) {
                owners.append(", ");
            }
            owners.append(offlinePlayer.getName());
        }
        Component summary = Component.text("--- Zusammenfassung ---", NamedTextColor.GOLD)
                .append(Component.newline()).append(Component.text("ID: " + session.id(), NamedTextColor.WHITE))
                .append(Component.newline()).append(Component.text("Name: " + session.name(), NamedTextColor.WHITE))
                .append(Component.newline()).append(Component.text("Besitzer: " + owners, NamedTextColor.WHITE))
                .append(Component.newline()).append(Component.text("Item: " + session.item(), NamedTextColor.WHITE))
                .append(Component.newline()).append(Component.text("Preis: " + session.price() + " Taler/Slot", NamedTextColor.WHITE));
        if (session.teleportPoint() != null) {
            summary = summary.append(Component.newline()).append(Component.text("Teleportpunkt: gesetzt", NamedTextColor.WHITE));
        }
        return summary;
    }
}
