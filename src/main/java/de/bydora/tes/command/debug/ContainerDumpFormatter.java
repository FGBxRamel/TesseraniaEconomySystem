package de.bydora.tes.command.debug;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Container;
import org.bukkit.block.Sign;
import org.bukkit.block.sign.Side;
import org.bukkit.block.sign.SignSide;
import org.bukkit.Nameable;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;
import java.util.Optional;

/**
 * Formats a block's exact contents for {@code /debug dump} (see
 * {@code docs/gui-reference-capture.md}): containers get a per-slot listing using the spec's own
 * {@code <col>|<row>} position notation, signs get their text per side. Any other block is
 * unsupported (notably item frames, which are entities rather than blocks and aren't covered).
 *
 * <p>Names/lore/sign text are serialized to MiniMessage ({@code <gold><bold>Text</bold></gold>})
 * rather than stripped to plain text, so color and formatting (bold, italic, ...) survive the
 * dump instead of being silently lost.
 */
public final class ContainerDumpFormatter {

    private ContainerDumpFormatter() {
    }

    public static Optional<ContainerDump> format(Block block) {
        BlockState state = block.getState();
        if (state instanceof Container container) {
            return Optional.of(formatContainer(block, container));
        }
        if (state instanceof Sign sign) {
            return Optional.of(formatSign(block, sign));
        }
        return Optional.empty();
    }

    private static ContainerDump formatContainer(Block block, Container container) {
        Inventory inventory = container.getInventory();
        int rows = inventory.getSize() / 9;
        String title = container.customName() != null
                ? serialize(container.customName())
                : block.getType().name();

        StringBuilder text = new StringBuilder();
        appendHeader(text, block);
        text.append(" | Titel: \"").append(title).append('"')
                .append(" | Größe: 9x").append(rows).append('\n');

        ItemStack[] contents = inventory.getContents();
        int itemCount = 0;
        for (int slot = 0; slot < contents.length; slot++) {
            ItemStack item = contents[slot];
            if (item == null || item.getType() == Material.AIR) {
                continue;
            }
            itemCount++;
            appendSlot(text, slot, item);
        }

        return new ContainerDump("9x" + rows + ", " + itemCount + " Items", text.toString());
    }

    private static void appendSlot(StringBuilder text, int slot, ItemStack item) {
        int col = (slot % 9) + 1;
        int row = (slot / 9) + 1;
        text.append(col).append('|').append(row).append(" — ")
                .append(item.getAmount()).append("x ").append(item.getType().name());

        ItemMeta meta = item.getItemMeta();
        if (meta != null && meta.hasDisplayName()) {
            text.append(" \"").append(serialize(meta.displayName())).append('"');
        }
        if (meta != null && meta.hasLore()) {
            List<Component> lore = meta.lore();
            text.append(" [Lore: ");
            for (int i = 0; i < lore.size(); i++) {
                if (i > 0) {
                    text.append(" / ");
                }
                text.append(serialize(lore.get(i)));
            }
            text.append(']');
        }
        text.append('\n');
    }

    private static ContainerDump formatSign(Block block, Sign sign) {
        StringBuilder text = new StringBuilder();
        appendHeader(text, block);
        text.append(" | Schild\n");

        boolean front = appendSignSide(text, "Vorderseite", sign.getSide(Side.FRONT));
        boolean back = appendSignSide(text, "Rückseite", sign.getSide(Side.BACK));
        if (!front && !back) {
            text.append("(kein Text)\n");
        }

        return new ContainerDump("Schild", text.toString());
    }

    private static boolean appendSignSide(StringBuilder text, String label, SignSide side) {
        List<String> lines = side.lines().stream().map(ContainerDumpFormatter::serialize).toList();
        boolean hasText = lines.stream().anyMatch(line -> !line.isBlank());
        if (!hasText) {
            return false;
        }
        text.append(label).append(":\n");
        for (String line : lines) {
            if (!line.isBlank()) {
                text.append("  ").append(line).append('\n');
            }
        }
        return true;
    }

    private static void appendHeader(StringBuilder text, Block block) {
        text.append("Welt: ").append(block.getWorld().getName())
                .append(" | Pos: ").append(block.getX()).append(' ').append(block.getY()).append(' ').append(block.getZ());
    }

    private static String serialize(Component component) {
        return MiniMessage.miniMessage().serialize(component);
    }
}
