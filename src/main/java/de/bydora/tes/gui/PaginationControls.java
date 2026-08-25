package de.bydora.tes.gui;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import xyz.xenondevs.invui.item.BoundItem;
import xyz.xenondevs.invui.item.Item;
import xyz.xenondevs.invui.item.ItemBuilder;

/**
 * Shared pagination control items for TES's paginated InvUI screens, matching the layout
 * confirmed against the invoice interface reference builds (see {@code docs/gui-library.md}):
 * a next-page item, and a close item that swaps to a previous-page item from page 2 onward
 * instead of being a separate, always-present slot.
 */
public final class PaginationControls {

    private PaginationControls() {
    }

    public static Item nextPageItem() {
        return BoundItem.pagedBuilder()
                .setItemProvider((viewer, gui) -> new ItemBuilder(CustomHeads.nextPageHead())
                        .setName(Component.text("➤ Weiter", NamedTextColor.WHITE, TextDecoration.BOLD)))
                .addClickHandler((item, gui, click) -> gui.setPage(gui.getPage() + 1))
                .build();
    }

    /**
     * Closes the GUI on page 1; from page 2 onward, shows and acts as a previous-page control
     * instead.
     */
    public static Item closeOrPreviousPageItem() {
        return BoundItem.pagedBuilder()
                .setItemProvider((viewer, gui) -> gui.getPage() > 0
                        ? new ItemBuilder(CustomHeads.previousPageHead())
                                .setName(Component.text("⮜ Zurück", NamedTextColor.WHITE, TextDecoration.BOLD))
                        : new ItemBuilder(Material.BARRIER)
                                .setName(Component.text("Schließen", NamedTextColor.RED, TextDecoration.BOLD)))
                .addClickHandler((item, gui, click) -> {
                    if (gui.getPage() > 0) {
                        gui.setPage(gui.getPage() - 1);
                    } else {
                        click.player().closeInventory();
                    }
                })
                .build();
    }
}
