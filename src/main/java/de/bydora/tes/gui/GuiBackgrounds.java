package de.bydora.tes.gui;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import xyz.xenondevs.invui.item.ItemBuilder;
import xyz.xenondevs.invui.item.ItemProvider;

/**
 * Shared background fillers for TES's InvUI screens. Passed to {@code Gui.Builder#setBackground},
 * these only render in slots that are otherwise empty (e.g. a {@code CONTENT_LIST_SLOT_HORIZONTAL}
 * with no item on the current page) — the slot stays functional and shows real content the moment
 * there is any.
 */
public final class GuiBackgrounds {

    private GuiBackgrounds() {
    }

    public static ItemProvider emptyContentSlot() {
        return new ItemBuilder(Material.WHITE_STAINED_GLASS_PANE).setName(Component.text(" "));
    }
}
