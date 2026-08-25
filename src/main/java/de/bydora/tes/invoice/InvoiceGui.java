package de.bydora.tes.invoice;

import de.bydora.tes.TesseraniaEconomySystem;
import de.bydora.tes.data.PlayerRecord;
import de.bydora.tes.gui.CustomHeads;
import de.bydora.tes.gui.GuiBackgrounds;
import de.bydora.tes.gui.PaginationControls;
import de.bydora.tes.util.Messages;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import xyz.xenondevs.invui.gui.Markers;
import xyz.xenondevs.invui.gui.PagedGui;
import xyz.xenondevs.invui.item.Item;
import xyz.xenondevs.invui.item.ItemBuilder;
import xyz.xenondevs.invui.window.Window;

import java.util.List;
import java.util.UUID;

/**
 * The {@code /rechnung anzeigen} "Offene Rechnungen" screen (spec §3.1.1.3), laid out per
 * the reference build at -412 -12 -3392: a paginated list of the viewer's own open invoices (as
 * target), click-to-settle, plus a diamond icon to cash out their virtual balance into the
 * Belohnungsinventar and a link to the "Versendete Rechnungen" sub-screen ({@link SentInvoiceGui}).
 * Unlike the reference build, rows 1–2 are both live content slots (18 invoices/page, not 6) and
 * the unused dead-zone rows aren't replicated — see the "Dead/placeholder slots" convention in
 * {@code docs/gui-library.md}.
 */
public final class InvoiceGui {

    private static final String SENT_INVOICES_HEAD_TEXTURE =
            "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYjYyYzA4ODA1YmQ5Yzk1N2RhMzQ1MDU1NGEwOWU5OTQwNDJmNTQ2OTVkYjg1NWMxYzJjYjQ3ZWY0NDJlMWJmNiJ9fX0=";

    private InvoiceGui() {
    }

    public static void open(TesseraniaEconomySystem plugin, Player player) {
        Item diamondItem = Item.builder()
                .setItemProvider(viewer -> new ItemBuilder(Material.DIAMOND)
                        .setName(Component.text("Kontostand", NamedTextColor.AQUA, TextDecoration.BOLD))
                        .addLoreLines(
                                Component.text(currentBalance(plugin, viewer) + " Taler", NamedTextColor.WHITE),
                                Component.text(" "),
                                Component.text("Klicken um Betrag auszuzahlen", NamedTextColor.GRAY, TextDecoration.ITALIC)))
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

        Item sentInvoicesItem = Item.builder()
                .setItemProvider(new ItemBuilder(CustomHeads.texturedHead(SENT_INVOICES_HEAD_TEXTURE))
                        .setCustomName(Component.text("Versendete Rechnungen", NamedTextColor.WHITE, TextDecoration.BOLD)))
                .addClickHandler((item, click) -> SentInvoiceGui.open(plugin, click.player()))
                .build();

        PagedGui<Item> gui = PagedGui.itemsBuilder()
                .setStructure(
                        "x x x x x x x x x",
                        "x x x x x x x x x",
                        "g g g g g g g g g",
                        "c g g g d g s g n")
                .setBackground(GuiBackgrounds.emptyContentSlot())
                .addIngredient('g', Item.simple(new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).setName(Component.text(" "))))
                .addIngredient('x', Markers.CONTENT_LIST_SLOT_HORIZONTAL)
                .addIngredient('c', PaginationControls.closeOrPreviousPageItem())
                .addIngredient('d', diamondItem)
                .addIngredient('s', sentInvoicesItem)
                .addIngredient('n', PaginationControls.nextPageItem())
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
                .setItemProvider(new ItemBuilder(Material.BOOK)
                        .setName(Component.text("Rechnung - Begleichen", NamedTextColor.WHITE))
                        .addLoreLines(
                                Component.text(record.price() + " Taler | " + creatorName, NamedTextColor.GRAY),
                                Component.text(record.reason(), NamedTextColor.GRAY),
                                Component.text(" "),
                                Component.text("Linksklick zum Bezahlen", NamedTextColor.GRAY, TextDecoration.ITALIC)))
                .addClickHandler((item, click) -> {
                    InvoiceEconomy.SettleResult result = InvoiceEconomy.settle(
                            plugin.invoiceRepository(), plugin.playerRepository(), click.player(), record.id());
                    switch (result) {
                        case SETTLED -> {
                            click.player().sendMessage(Messages.invoiceSettled(creatorName, record.price()));
                            notifyCreatorOfSettlement(plugin, record.creatorUuid(), click.player().getName(), record.price());
                        }
                        case NOT_ENOUGH_DIAMONDS -> click.player().sendMessage(Messages.notEnoughTaler());
                        case ALREADY_SETTLED -> click.player().sendMessage(Messages.invoiceAlreadySettled());
                    }
                    if (result != InvoiceEconomy.SettleResult.NOT_ENOUGH_DIAMONDS) {
                        open(plugin, click.player());
                    }
                })
                .build();
    }

    private static void notifyCreatorOfSettlement(TesseraniaEconomySystem plugin, UUID creatorUuid, String payerName, int price) {
        Player creatorPlayer = Bukkit.getOfflinePlayer(creatorUuid).getPlayer();
        if (creatorPlayer != null) {
            creatorPlayer.sendMessage(Messages.invoiceSettledForCreator(payerName, price));
        } else {
            plugin.pendingNotificationRepository().enqueue(creatorUuid, Messages.invoiceSettledForCreatorText(payerName, price));
        }
    }
}
