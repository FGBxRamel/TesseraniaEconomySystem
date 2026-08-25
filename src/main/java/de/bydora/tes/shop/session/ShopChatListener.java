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

import java.util.List;
import java.util.Optional;

/**
 * Drives the chat-driven {@code /shop erstellen|bearbeiten} menu (spec §3.1.1.1, UX modeled
 * on the BlueMap-Marker plugin): a persistent, re-rendered chat menu lists every attribute for
 * the session's mode, color-coded red/green/gray, clickable in any order via
 * {@code /shop feld <key>}. Clicking a field "arms" it ({@link ShopSession#pendingField()});
 * the next chat message (or, for {@link ShopSessionField#POSITION}, the next right-click) is fed
 * to that field's handler instead of being broadcast. {@code /shop bestaetigen}/
 * {@code abbrechen} (or the typed equivalents) finalize or discard the session.
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
        if (maybeSession.isEmpty() || maybeSession.get().pendingField() != ShopSessionField.POSITION) {
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

    /**
     * Arms the given field (from clicking its menu line) and prompts the player for its value.
     * A no-op if the field isn't visible for the session's mode (e.g. a stale click on ID/Position
     * during an edit) — those attributes are immutable once a shop exists (UC2).
     */
    public void armField(Player player, ShopSessionField field) {
        Optional<ShopSession> maybeSession = requireActiveSession(player);
        if (maybeSession.isEmpty()) {
            return;
        }
        ShopSession session = maybeSession.get();
        if (!ShopSessionField.visibleFor(session.mode()).contains(field)) {
            return;
        }
        session.pendingField(field);
        player.sendMessage(promptFor(field));
    }

    public void confirmFromClick(Player player) {
        Optional<ShopSession> maybeSession = requireActiveSession(player);
        if (maybeSession.isEmpty()) {
            return;
        }
        handleConfirmAction(player, maybeSession.get());
    }

    public void cancelFromClick(Player player) {
        if (requireActiveSession(player).isEmpty()) {
            return;
        }
        cancel(player);
    }

    private Optional<ShopSession> requireActiveSession(Player player) {
        Optional<ShopSession> maybeSession = sessionManager.active(player.getUniqueId());
        if (maybeSession.isEmpty()) {
            player.sendMessage(Messages.shopSessionExpired());
        }
        return maybeSession;
    }

    private void cancel(Player player) {
        sessionManager.cancel(player.getUniqueId());
        player.sendMessage(Messages.shopSessionCancelled());
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
            cancel(player);
            return;
        }
        if (text.equalsIgnoreCase("bestätigen") || text.equalsIgnoreCase("bestaetigen")) {
            handleConfirmAction(player, session);
            return;
        }
        ShopSessionField field = session.pendingField();
        if (field == null) {
            player.sendMessage(Messages.shopNoFieldArmed());
            player.sendMessage(renderMenu(session));
            return;
        }
        switch (field) {
            case ID -> handleId(player, session, text);
            case NAME -> handleName(player, session, text);
            case OWNERS -> handleOwners(player, session, text);
            case POSITION -> player.sendMessage(Messages.shopPromptPosition());
            case ITEM -> handleItem(player, session, text);
            case PRICE -> handlePrice(player, session, text);
            case TELEPORT -> handleTeleport(player, session, text);
        }
    }

    private void handleId(Player player, ShopSession session, String text) {
        if (!text.matches("[A-Za-z0-9_-]{1,32}")) {
            player.sendMessage(Messages.shopIdInvalid());
            return;
        }
        if (shopRepository.existsId(text) || shopRegistry.existsId(text)) {
            player.sendMessage(Messages.shopIdTaken(text));
            return;
        }
        session.id(text);
        session.pendingField(null);
        player.sendMessage(renderMenu(session));
    }

    private void handleName(Player player, ShopSession session, String text) {
        if (text.isBlank() || text.length() > 48) {
            player.sendMessage(Messages.shopNameInvalid());
            return;
        }
        session.name(text);
        session.pendingField(null);
        player.sendMessage(renderMenu(session));
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
        session.pendingField(null);
        player.sendMessage(renderMenu(session));
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
        session.pendingField(null);
        player.sendMessage(renderMenu(session));
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
        if (text.equalsIgnoreCase("alle") || text.equalsIgnoreCase("all")) {
            session.item(ShopRecord.SELL_ALL_SENTINEL);
            session.pendingField(null);
            player.sendMessage(renderMenu(session));
            return;
        }
        ItemStack item;
        if (text.equalsIgnoreCase("hand")) {
            ItemStack inHand = player.getInventory().getItemInMainHand();
            if (inHand.getType() == Material.AIR) {
                player.sendMessage(Messages.shopItemInvalid(text));
                return;
            }
            item = inHand.clone();
        } else {
            Material material = Material.matchMaterial(text);
            if (material == null || !material.isItem()) {
                player.sendMessage(Messages.shopItemInvalid(text));
                return;
            }
            item = new ItemStack(material);
        }
        if (item.getType() == Material.DIAMOND) {
            player.sendMessage(Messages.shopItemIsCurrency());
            return;
        }
        session.item(item);
        session.pendingField(null);
        player.sendMessage(renderMenu(session));
    }

    private void handlePrice(Player player, ShopSession session, String text) {
        int price;
        try {
            price = Integer.parseInt(text);
        } catch (NumberFormatException e) {
            player.sendMessage(Messages.shopPriceInvalid());
            return;
        }
        if (price <= 0 || price > 64) {
            player.sendMessage(Messages.shopPriceInvalid());
            return;
        }
        session.price(price);
        session.pendingField(null);
        player.sendMessage(renderMenu(session));
    }

    private void handleTeleport(Player player, ShopSession session, String text) {
        if (text.equalsIgnoreCase("hier")) {
            Location location = player.getLocation();
            session.teleportPoint(new TeleportPoint(
                    location.getWorld().getName(), location.getX(), location.getY(), location.getZ(),
                    location.getYaw(), location.getPitch()));
        } else if (text.equalsIgnoreCase("nein")) {
            session.teleportPoint(null);
        } else {
            player.sendMessage(Messages.shopPromptTeleport());
            return;
        }
        session.pendingField(null);
        player.sendMessage(renderMenu(session));
    }

    private void handleConfirmAction(Player player, ShopSession session) {
        List<ShopSessionField> missing = session.missingMandatory();
        if (!missing.isEmpty()) {
            player.sendMessage(Messages.shopConfirmMissingAttributes(missing.stream().map(ShopSessionField::label).toList()));
            player.sendMessage(renderMenu(session));
            return;
        }
        finalizeSession(player, session);
        sessionManager.cancel(player.getUniqueId());
    }

    private void finalizeSession(Player player, ShopSession session) {
        long now = System.currentTimeMillis();
        if (session.mode() == ShopSessionMode.CREATE) {
            ShopRecord record = new ShopRecord(
                    session.id(), session.world(), session.name(), session.item(), session.price(),
                    session.containerType(), session.position(), session.secondaryPosition(),
                    session.teleportPoint(), session.owners(), now, now);
            shopRepository.insert(record);
            shopRegistry.register(record);
            ShopConversion.applyToShop(plugin, record);
            player.sendMessage(Messages.shopCreated(record.id(), record.name()));
        } else {
            ShopRecord existing = shopRegistry.findById(session.editingId()).orElseThrow();
            ShopRecord updated = new ShopRecord(
                    existing.id(), existing.world(), session.name(), session.item(), session.price(),
                    existing.containerType(), existing.position(), existing.secondaryPosition(),
                    session.teleportPoint(), session.owners(), existing.createdAt(), now);
            shopRepository.updateAttributes(updated);
            shopRepository.replaceOwners(updated.world(), updated.id(), updated.owners());
            shopRegistry.register(updated);
            ShopConversion.applyToShop(plugin, updated);
            player.sendMessage(Messages.shopUpdated(updated.id(), updated.name()));
        }
    }

    private Component promptFor(ShopSessionField field) {
        return switch (field) {
            case ID -> Messages.shopPromptId();
            case NAME -> Messages.shopPromptName();
            case OWNERS -> Messages.shopPromptOwners();
            case POSITION -> Messages.shopPromptPosition();
            case ITEM -> Messages.shopPromptItem();
            case PRICE -> Messages.shopPromptPrice();
            case TELEPORT -> Messages.shopPromptTeleport();
        };
    }

    private String displayValue(ShopSession session, ShopSessionField field) {
        return switch (field) {
            case ID -> session.id();
            case NAME -> session.name();
            case OWNERS -> session.owners().isEmpty() ? null : ownerNames(session);
            case POSITION -> session.position() == null ? null
                    : session.position().x() + " " + session.position().y() + " " + session.position().z();
            case ITEM -> session.item() == null ? null : ShopRecord.itemDisplayName(session.item());
            case PRICE -> session.price() == null ? null : session.price() + " Taler/Slot";
            case TELEPORT -> session.teleportPoint() != null ? "gesetzt" : null;
        };
    }

    private String ownerNames(ShopSession session) {
        StringBuilder owners = new StringBuilder();
        for (var owner : session.owners()) {
            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(owner);
            if (!owners.isEmpty()) {
                owners.append(", ");
            }
            owners.append(offlinePlayer.getName());
        }
        return owners.toString();
    }

    public Component renderMenu(ShopSession session) {
        List<Messages.ShopMenuLine> lines = ShopSessionField.visibleFor(session.mode()).stream()
                .map(field -> new Messages.ShopMenuLine(
                        field.key(), field.label(), displayValue(session, field), field.mandatory(),
                        session.isSet(field), field.hoverText()))
                .toList();
        return Messages.shopMenu(lines);
    }
}
