package de.bydora.tes.reward;

import de.bydora.tes.TesseraniaEconomySystem;
import de.bydora.tes.gui.PaginationControls;
import de.bydora.tes.shop.ShopRecord;
import de.bydora.tes.util.Messages;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import xyz.xenondevs.invui.gui.Markers;
import xyz.xenondevs.invui.gui.PagedGui;
import xyz.xenondevs.invui.item.Item;
import xyz.xenondevs.invui.item.ItemBuilder;
import xyz.xenondevs.invui.window.Window;

import java.util.List;

/**
 * The Belohnungsinventar screen (spec §1.3/§3.3.1.4), laid out per the reference build at
 * -424 -12 -3382: a paginated list of items queued for the viewer via
 * {@link RewardInventoryService}, bordered by gray filler and two unused, red-paned rows.
 * Left-clicking an item takes it into the viewer's real inventory, one at a time (design
 * decision — the spec doesn't describe a claim mechanic). The "⮜ Levelinterface" button is a
 * documented no-op until Stage 4 builds that interface.
 */
public final class RewardInventoryGui {

    private RewardInventoryGui() {
    }

    public static void open(TesseraniaEconomySystem plugin, Player player) {
        Item levelInterfaceItem = Item.builder()
                .setItemProvider(new ItemBuilder(Material.SPECTRAL_ARROW)
                        .setName(Component.text("⮜ Levelinterface", NamedTextColor.WHITE, TextDecoration.BOLD)))
                .addClickHandler(click -> {
                })
                .build();

        PagedGui<Item> gui = PagedGui.itemsBuilder()
                .setStructure(
                        "g g g g g g g g g",
                        "g x x x x x x x g",
                        "g x x x x x x x g",
                        "g g c g l g n g g",
                        "r r r r r r r r r",
                        "r r r r r r r r r")
                .addIngredient('g', Item.simple(new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).setName(Component.text(" "))))
                .addIngredient('r', Item.simple(new ItemBuilder(Material.RED_STAINED_GLASS_PANE).setName(Component.text(" "))))
                .addIngredient('x', Markers.CONTENT_LIST_SLOT_HORIZONTAL)
                .addIngredient('c', PaginationControls.closeOrPreviousPageItem())
                .addIngredient('l', levelInterfaceItem)
                .addIngredient('n', PaginationControls.nextPageItem())
                .setContent(content(plugin, player))
                .build();

        Window.builder()
                .setViewer(player)
                .setTitle("Belohnungsinventar")
                .setUpperGui(gui)
                .build()
                .open();
    }

    private static List<Item> content(TesseraniaEconomySystem plugin, Player player) {
        return plugin.rewardInventoryService().items(player.getUniqueId()).stream()
                .map(record -> rewardItem(plugin, record))
                .toList();
    }

    private static Item rewardItem(TesseraniaEconomySystem plugin, RewardInventoryItemRecord record) {
        return Item.builder()
                .setItemProvider(new ItemBuilder(record.item()))
                .addClickHandler((item, click) -> {
                    RewardInventoryService.TakeResult result = plugin.rewardInventoryService().take(click.player(), record.id());
                    switch (result) {
                        case TAKEN -> {
                            click.player().sendMessage(Messages.rewardInventoryTaken(ShopRecord.itemDisplayName(record.item())));
                            open(plugin, click.player());
                        }
                        case INVENTORY_FULL -> click.player().sendMessage(Messages.rewardInventoryFull());
                        case NOT_FOUND -> {
                        }
                    }
                })
                .build();
    }
}
