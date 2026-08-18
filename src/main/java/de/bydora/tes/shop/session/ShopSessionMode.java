package de.bydora.tes.shop.session;

/**
 * Whether a {@link ShopSession} is collecting attributes for a brand-new shop (UC1) or editing
 * an existing one's mutable attributes (UC2).
 */
public enum ShopSessionMode {
    CREATE,
    EDIT
}
