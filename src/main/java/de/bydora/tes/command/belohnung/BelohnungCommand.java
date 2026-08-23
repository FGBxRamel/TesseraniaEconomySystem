package de.bydora.tes.command.belohnung;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import de.bydora.tes.TesseraniaEconomySystem;
import de.bydora.tes.data.PlayerRecord;
import de.bydora.tes.reward.RewardInventoryGui;
import de.bydora.tes.util.Messages;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Optional;

/**
 * Implements {@code /tes belohnung} (spec §3.3.1.4): opens the caller's Belohnungsinventar.
 */
public final class BelohnungCommand {

    private BelohnungCommand() {
    }

    public static LiteralArgumentBuilder<CommandSourceStack> build() {
        return Commands.literal("belohnung")
                .requires(source -> source.getSender().hasPermission("tes.belohnung"))
                .executes(ctx -> {
                    CommandSender sender = ctx.getSource().getSender();
                    if (!(sender instanceof Player player)) {
                        sender.sendMessage(Messages.usage("/tes belohnung ist nur für Spieler verfügbar."));
                        return Command.SINGLE_SUCCESS;
                    }
                    TesseraniaEconomySystem plugin = TesseraniaEconomySystem.getPlugin(TesseraniaEconomySystem.class);
                    Optional<PlayerRecord> record = plugin.playerRepository().findByUuid(player.getUniqueId());
                    if (record.isEmpty() || record.get().paused()) {
                        player.sendMessage(Messages.rewardInventoryNotEligible());
                        return Command.SINGLE_SUCCESS;
                    }
                    RewardInventoryGui.open(plugin, player);
                    return Command.SINGLE_SUCCESS;
                });
    }
}
