package de.bydora.tes.shop.session;

/**
 * A step in the chat-driven shop creation/edit flow (spec §3.1.1.1, UX modeled on the
 * BlueMap-Marker plugin). {@link ShopSessionMode#CREATE} visits every step in order;
 * {@link ShopSessionMode#EDIT} starts at {@link #NAME} since a shop's ID and position are
 * immutable once set (UC2).
 */
public enum ShopSessionStep {
    ID,
    NAME,
    OWNERS,
    POSITION,
    ITEM,
    PRICE,
    TELEPORT,
    CONFIRM
}
