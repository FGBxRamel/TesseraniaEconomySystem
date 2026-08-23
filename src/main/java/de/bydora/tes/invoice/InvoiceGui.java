package de.bydora.tes.invoice;

import de.bydora.tes.TesseraniaEconomySystem;
import de.bydora.tes.data.PlayerRecord;
import de.bydora.tes.util.Messages;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import xyz.xenondevs.invui.gui.Markers;
import xyz.xenondevs.invui.gui.PagedGui;
import xyz.xenondevs.invui.item.BoundItem;
import xyz.xenondevs.invui.item.Item;
import xyz.xenondevs.invui.item.ItemBuilder;
import xyz.xenondevs.invui.window.Window;

import java.util.List;

/**
 * The {@code /tes rechnung anzeigen} screen (spec §3.1.1.3): a paginated list of the viewer's
 * own open invoices (as target), click-to-settle, plus a diamond icon to cash out their virtual
 * balance into the Belohnungsinventar and a barrier icon to close the screen.
 */
public final class InvoiceGui {

    private InvoiceGui() {
    }

    public static void open(TesseraniaEconomySystem plugin, Player player) {
        Item closeItem = Item.builder()
                .setItemProvider(new ItemBuilder(Material.BARRIER).setName(Component.text("Schließen", NamedTextColor.RED)))
                .addClickHandler(click -> click.player().closeInventory())
                .build();

        Item diamondItem = Item.builder()
                .setItemProvider(viewer -> new ItemBuilder(Material.DIAMOND)
                        .setName(Component.text("Auszahlen", NamedTextColor.YELLOW))
                        .addLoreLines(
                                Component.text("Aktueller Kontostand: ", NamedTextColor.GRAY)
                                        .append(Component.text(currentBalance(plugin, viewer) + " Taler", NamedTextColor.WHITE)),
                                Component.text("Klicken zum Auszahlen in dein Belohnungsinventar.", NamedTextColor.GRAY)))
                .addClickHandler((item, click) -> {
                    InvoiceEconomy.CashOutResult result = InvoiceEconomy.cashOut(
                            plugin.playerRepository(), plugin.rewardInventoryService(), click.player());
                    if (result == InvoiceEconomy.CashOutResult.CASHED_OUT) {
                        click.player().sendMessage(Messages.invoiceCashedOut());
                    } else {
                        click.player().sendMessage(Messages.invoiceNothingToCashOut());
                    }
                    open(plugin, click.player());
                })
                .build();

        Item nextPageItem = BoundItem.pagedBuilder()
                .setItemProvider((viewer, gui) -> new ItemBuilder(Material.ARROW).setName(Component.text("Nächste Seite", NamedTextColor.YELLOW)))
                .addClickHandler((item, gui, click) -> gui.setPage(gui.getPage() + 1))
                .build();

        PagedGui<Item> gui = PagedGui.itemsBuilder()
                .setStructure(
                        "x x x x x x x x x",
                        "x x x x x x x x x",
                        "x x x x x x x x x",
                        "x x x x x x x x x",
                        "# # # # # # # # #",
                        "c # # # d # # # n")
                .addIngredient('#', Item.simple(new ItemBuilder(Material.BLACK_STAINED_GLASS_PANE).setName(Component.text(" "))))
                .addIngredient('x', Markers.CONTENT_LIST_SLOT_HORIZONTAL)
                .addIngredient('c', closeItem)
                .addIngredient('d', diamondItem)
                .addIngredient('n', nextPageItem)
                .setBackground(new ItemBuilder(Material.WHITE_STAINED_GLASS_PANE).setName(Component.text(" ")))
                .setContent(content(plugin, player))
                .build();

        Window.builder()
                .setViewer(player)
                .setTitle("Offene Rechnungen")
                .setUpperGui(gui)
                .build()
                .open();
    }

    private static int currentBalance(TesseraniaEconomySystem plugin, Player viewer) {
        return plugin.playerRepository().findByUuid(viewer.getUniqueId()).map(PlayerRecord::invoiceBalance).orElse(0);
    }

    private static List<Item> content(TesseraniaEconomySystem plugin, Player player) {
        return plugin.invoiceRepository().findOpenByTarget(player.getUniqueId()).stream()
                .map(record -> invoiceItem(plugin, record))
                .toList();
    }

    private static Item invoiceItem(TesseraniaEconomySystem plugin, InvoiceRecord record) {
        OfflinePlayer creator = Bukkit.getOfflinePlayer(record.creatorUuid());
        String creatorName = creator.getName() != null ? creator.getName() : record.creatorUuid().toString();
        return Item.builder()
                .setItemProvider(new ItemBuilder(Material.PAPER)
                        .setName(Component.text(record.price() + " Taler von " + creatorName, NamedTextColor.YELLOW))
                        .addLoreLines(
                                Component.text("Grund: ", NamedTextColor.GRAY).append(Component.text(record.reason(), NamedTextColor.WHITE)),
                                Component.text("Linksklick zum Bezahlen.", NamedTextColor.GRAY)))
                .addClickHandler((item, click) -> {
                    InvoiceEconomy.SettleResult result = InvoiceEconomy.settle(
                            plugin.invoiceRepository(), plugin.playerRepository(), click.player(), record.id());
                    switch (result) {
                        case SETTLED -> click.player().sendMessage(Messages.invoiceSettled(creatorName, record.price()));
                        case NOT_ENOUGH_DIAMONDS -> click.player().sendMessage(Messages.notEnoughTaler());
                        case ALREADY_SETTLED -> click.player().sendMessage(Messages.invoiceAlreadySettled());
                    }
                    if (result != InvoiceEconomy.SettleResult.NOT_ENOUGH_DIAMONDS) {
                        open(plugin, click.player());
                    }
                })
                .build();
    }
}
