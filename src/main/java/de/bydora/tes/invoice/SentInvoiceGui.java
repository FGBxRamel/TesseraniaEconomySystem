package de.bydora.tes.invoice;

import de.bydora.tes.TesseraniaEconomySystem;
import de.bydora.tes.data.PlayerRecord;
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
 * The "Versendete Rechnungen" sub-screen (spec §3.1.1.3 v1.2), reachable from
 * {@link InvoiceGui}'s "Offene Rechnungen": the viewer's own still-open invoices as creator,
 * click-to-retract. Retraction has no time limit and only the creator can trigger it — unlike
 * Stage 1 shop purchases' buyer-side 60s refund window.
 */
public final class SentInvoiceGui {

    private SentInvoiceGui() {
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

        Item openInvoicesItem = Item.builder()
                .setItemProvider(new ItemBuilder(Material.BOOK)
                        .setName(Component.text("Offene Rechnungen", NamedTextColor.WHITE, TextDecoration.BOLD)))
                .addClickHandler((item, click) -> InvoiceGui.open(plugin, click.player()))
                .build();

        PagedGui<Item> gui = PagedGui.itemsBuilder()
                .setStructure(
                        "x x x x x x w w w",
                        "w w w w w w w w w",
                        "g g g g g g g g g",
                        "c g g g d g o g n",
                        "r r r r r r r r r",
                        "r r r r r r r r r")
                .addIngredient('w', Item.simple(new ItemBuilder(Material.WHITE_STAINED_GLASS_PANE).setName(Component.text(" "))))
                .addIngredient('g', Item.simple(new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).setName(Component.text(" "))))
                .addIngredient('r', Item.simple(new ItemBuilder(Material.RED_STAINED_GLASS_PANE).setName(Component.text(" "))))
                .addIngredient('x', Markers.CONTENT_LIST_SLOT_HORIZONTAL)
                .addIngredient('c', PaginationControls.closeOrPreviousPageItem())
                .addIngredient('d', diamondItem)
                .addIngredient('o', openInvoicesItem)
                .addIngredient('n', PaginationControls.nextPageItem())
                .setContent(content(plugin, player))
                .build();

        Window.builder()
                .setViewer(player)
                .setTitle("Versendete Rechnungen")
                .setUpperGui(gui)
                .build()
                .open();
    }

    private static int currentBalance(TesseraniaEconomySystem plugin, Player viewer) {
        return plugin.playerRepository().findByUuid(viewer.getUniqueId()).map(PlayerRecord::invoiceBalance).orElse(0);
    }

    private static List<Item> content(TesseraniaEconomySystem plugin, Player player) {
        return plugin.invoiceRepository().findOpenByCreator(player.getUniqueId()).stream()
                .map(record -> invoiceItem(plugin, record))
                .toList();
    }

    private static Item invoiceItem(TesseraniaEconomySystem plugin, InvoiceRecord record) {
        OfflinePlayer target = Bukkit.getOfflinePlayer(record.targetUuid());
        String targetName = target.getName() != null ? target.getName() : record.targetUuid().toString();
        return Item.builder()
                .setItemProvider(new ItemBuilder(Material.BOOK)
                        .setName(Component.text("Rechnung - Ausstehend", NamedTextColor.WHITE))
                        .addLoreLines(
                                Component.text(record.price() + " Taler | " + targetName, NamedTextColor.GRAY),
                                Component.text(record.reason(), NamedTextColor.GRAY),
                                Component.text(" "),
                                Component.text("Linksklick zum Zurückziehen", NamedTextColor.GRAY, TextDecoration.ITALIC)))
                .addClickHandler((item, click) -> {
                    InvoiceEconomy.RetractResult result = InvoiceEconomy.retract(plugin.invoiceRepository(), record.id());
                    if (result == InvoiceEconomy.RetractResult.RETRACTED) {
                        click.player().sendMessage(Messages.invoiceRetracted(targetName, record.price()));
                        notifyTargetOfRetraction(plugin, record.targetUuid(), click.player().getName(), record.price());
                    } else {
                        click.player().sendMessage(Messages.invoiceRetractAlreadyResolved());
                    }
                    open(plugin, click.player());
                })
                .build();
    }

    private static void notifyTargetOfRetraction(TesseraniaEconomySystem plugin, UUID targetUuid, String creatorName, int price) {
        Player targetPlayer = Bukkit.getOfflinePlayer(targetUuid).getPlayer();
        if (targetPlayer != null) {
            targetPlayer.sendMessage(Messages.invoiceRetractedForTarget(creatorName, price));
        } else {
            plugin.pendingNotificationRepository().enqueue(targetUuid, Messages.invoiceRetractedForTargetText(creatorName, price));
        }
    }
}
