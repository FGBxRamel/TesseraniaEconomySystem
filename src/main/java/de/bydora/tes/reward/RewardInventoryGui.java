package de.bydora.tes.reward;

import de.bydora.tes.TesseraniaEconomySystem;
import de.bydora.tes.shop.ShopRecord;
import de.bydora.tes.util.Messages;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import xyz.xenondevs.invui.gui.Markers;
import xyz.xenondevs.invui.gui.PagedGui;
import xyz.xenondevs.invui.item.BoundItem;
import xyz.xenondevs.invui.item.Item;
import xyz.xenondevs.invui.item.ItemBuilder;
import xyz.xenondevs.invui.window.Window;

import java.util.List;

/**
 * The Belohnungsinventar screen (spec §3.3.1.4): a paginated list of items queued for the
 * viewer via {@link RewardInventoryService}. Left-clicking an item takes it into the viewer's
 * real inventory, one at a time (design decision — the spec doesn't describe a claim mechanic).
 * The gold-arrow "back to Level Interface" button is a documented no-op until Stage 4 builds
 * that interface.
 */
public final class RewardInventoryGui {

    private RewardInventoryGui() {
    }

    public static void open(TesseraniaEconomySystem plugin, Player player) {
        Item backItem = Item.builder()
                .setItemProvider(new ItemBuilder(Material.ARROW)
                        .setName("§7Zurück")
                        .addLoreLines("§7Das Levelinterface folgt in einer späteren Stage."))
                .addClickHandler(click -> {
                })
                .build();

        Item nextPageItem = BoundItem.pagedBuilder()
                .setItemProvider((viewer, gui) -> new ItemBuilder(Material.ARROW).setName("§eNächste Seite"))
                .addClickHandler((item, gui, click) -> gui.setPage(gui.getPage() + 1))
                .build();

        PagedGui<Item> gui = PagedGui.itemsBuilder()
                .setStructure(
                        "x x x x x x x x x",
                        "x x x x x x x x x",
                        "x x x x x x x x x",
                        "x x x x x x x x x",
                        "x x x x x x x x x",
                        "b # # # # # # # n")
                .addIngredient('#', Item.simple(new ItemBuilder(Material.BLACK_STAINED_GLASS_PANE).setName(" ")))
                .addIngredient('x', Markers.CONTENT_LIST_SLOT_HORIZONTAL)
                .addIngredient('b', backItem)
                .addIngredient('n', nextPageItem)
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
