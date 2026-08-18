package de.bydora.tes.shop.session;

import java.util.List;
import java.util.Optional;

/**
 * One clickable attribute in the {@code /tes shop erstellen|bearbeiten} menu (spec §3.1.1.1, UX
 * modeled on the BlueMap-Marker plugin). Unlike a sequential wizard step, fields carry no order —
 * a player may click any of them, in any sequence, as many times as they like, before confirming.
 *
 * @param key        the token used in the internal {@code /tes shop feld <key>} click command
 * @param label      German display label shown in the menu
 * @param hoverText  German hover text explaining what the attribute is
 * @param mandatory  whether this attribute must be set before {@code /tes shop bestaetigen}
 *                   succeeds (spec's red/green/gray color rule, p.8)
 */
public enum ShopSessionField {
    ID("id", "ID", "Eindeutige, unveränderliche Kennung des Shops.", true),
    NAME("name", "Name", "Anzeigename des Shops.", true),
    OWNERS("owners", "Besitzer", "Spieler, die diesen Shop verwalten dürfen.", true),
    POSITION("position", "Position", "Truhe/Doppeltruhe/Redstone-Truhe/Fass/Shulkerbox, die zum Shop wird (Rechtsklick auf den Block).", true),
    ITEM("item", "Item", "Das im Shop verkaufte Item.", true),
    PRICE("price", "Preis", "Preis pro Slot in Talern.", true),
    TELEPORT("teleport", "Teleportpunkt", "Optionaler Zielpunkt für \"/tes shop tp\".", false);

    private final String key;
    private final String label;
    private final String hoverText;
    private final boolean mandatory;

    ShopSessionField(String key, String label, String hoverText, boolean mandatory) {
        this.key = key;
        this.label = label;
        this.hoverText = hoverText;
        this.mandatory = mandatory;
    }

    public String key() {
        return key;
    }

    public String label() {
        return label;
    }

    public String hoverText() {
        return hoverText;
    }

    public boolean mandatory() {
        return mandatory;
    }

    /**
     * The fields shown in the menu for the given mode. ID and position are immutable once a shop
     * is created (UC2), so {@link ShopSessionMode#EDIT} omits them entirely rather than showing
     * them read-only.
     */
    public static List<ShopSessionField> visibleFor(ShopSessionMode mode) {
        return mode == ShopSessionMode.CREATE
                ? List.of(ID, NAME, OWNERS, POSITION, ITEM, PRICE, TELEPORT)
                : List.of(NAME, OWNERS, ITEM, PRICE, TELEPORT);
    }

    public static Optional<ShopSessionField> fromKey(String key) {
        for (ShopSessionField field : values()) {
            if (field.key.equalsIgnoreCase(key)) {
                return Optional.of(field);
            }
        }
        return Optional.empty();
    }
}
