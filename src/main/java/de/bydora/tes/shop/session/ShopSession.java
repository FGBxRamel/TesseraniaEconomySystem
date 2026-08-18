package de.bydora.tes.shop.session;

import de.bydora.tes.shop.BlockPos;
import de.bydora.tes.shop.ShopRecord;
import de.bydora.tes.shop.TeleportPoint;
import org.bukkit.Material;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Mutable, per-player state for one in-progress {@code /tes shop erstellen|bearbeiten}
 * conversation. Fields are filled in over the course of several chat messages; the session is
 * only turned into a {@link ShopRecord} once {@link ShopSessionStep#CONFIRM} is accepted.
 */
public final class ShopSession {

    private final UUID actor;
    private final String world;
    private final ShopSessionMode mode;
    private final String editingId;
    private final Duration timeout;

    private ShopSessionStep step;
    private Instant expiresAt;

    private String id;
    private String name;
    private final Set<UUID> owners = new LinkedHashSet<>();
    private BlockPos position;
    private BlockPos secondaryPosition;
    private Material containerType;
    private Material item;
    private Integer price;
    private TeleportPoint teleportPoint;

    private ShopSession(UUID actor, String world, ShopSessionMode mode, String editingId, ShopSessionStep firstStep, Duration timeout) {
        this.actor = actor;
        this.world = world;
        this.mode = mode;
        this.editingId = editingId;
        this.step = firstStep;
        this.timeout = timeout;
        touch();
    }

    public static ShopSession createNew(UUID actor, String world, Duration timeout) {
        ShopSession session = new ShopSession(actor, world, ShopSessionMode.CREATE, null, ShopSessionStep.ID, timeout);
        session.owners.add(actor);
        return session;
    }

    public static ShopSession editing(UUID actor, ShopRecord existing, Duration timeout) {
        ShopSession session = new ShopSession(actor, existing.world(), ShopSessionMode.EDIT, existing.id(), ShopSessionStep.NAME, timeout);
        session.id = existing.id();
        session.name = existing.name();
        session.owners.addAll(existing.owners());
        session.position = existing.position();
        session.secondaryPosition = existing.secondaryPosition();
        session.containerType = existing.containerType();
        session.item = existing.item();
        session.price = existing.price();
        session.teleportPoint = existing.teleportPoint();
        return session;
    }

    public void touch() {
        expiresAt = Instant.now().plus(timeout);
    }

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    public UUID actor() {
        return actor;
    }

    public String world() {
        return world;
    }

    public ShopSessionMode mode() {
        return mode;
    }

    public String editingId() {
        return editingId;
    }

    public ShopSessionStep step() {
        return step;
    }

    public void advanceTo(ShopSessionStep next) {
        this.step = next;
        touch();
    }

    public String id() {
        return id;
    }

    public void id(String id) {
        this.id = id;
    }

    public String name() {
        return name;
    }

    public void name(String name) {
        this.name = name;
    }

    public Set<UUID> owners() {
        return owners;
    }

    public BlockPos position() {
        return position;
    }

    public BlockPos secondaryPosition() {
        return secondaryPosition;
    }

    public void position(BlockPos position, BlockPos secondaryPosition, Material containerType) {
        this.position = position;
        this.secondaryPosition = secondaryPosition;
        this.containerType = containerType;
    }

    public Material containerType() {
        return containerType;
    }

    public Material item() {
        return item;
    }

    public void item(Material item) {
        this.item = item;
    }

    public Integer price() {
        return price;
    }

    public void price(Integer price) {
        this.price = price;
    }

    public TeleportPoint teleportPoint() {
        return teleportPoint;
    }

    public void teleportPoint(TeleportPoint teleportPoint) {
        this.teleportPoint = teleportPoint;
    }
}
