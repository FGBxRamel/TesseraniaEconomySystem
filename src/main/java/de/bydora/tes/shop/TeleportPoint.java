package de.bydora.tes.shop;

/**
 * An optional teleport destination for a shop, set either explicitly during creation/editing or
 * derived from the shop's own position.
 */
public record TeleportPoint(String world, double x, double y, double z, float yaw, float pitch) {
}
