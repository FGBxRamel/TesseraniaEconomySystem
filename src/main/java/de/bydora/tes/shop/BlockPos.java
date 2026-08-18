package de.bydora.tes.shop;

/**
 * An integer block position within a single world (the world is tracked separately by whatever
 * holds this position, e.g. {@link ShopRecord#world()}).
 */
public record BlockPos(int x, int y, int z) {
}
