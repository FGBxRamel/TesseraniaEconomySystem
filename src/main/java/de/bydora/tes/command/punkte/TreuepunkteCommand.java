package de.bydora.tes.command.punkte;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;

/**
 * Implements {@code /treuepunkte add|remove|set <Name> <Anzahl>} (spec §1.4).
 */
public final class TreuepunkteCommand {

    private TreuepunkteCommand() {
    }

    public static LiteralArgumentBuilder<CommandSourceStack> build() {
        return PunkteCommandFactory.build(
                "treuepunkte",
                "tes.admin.treuepunkte",
                "Treuepunkte",
                new PunkteCommandFactory.Counter(
                        (repository, uuid, amount) -> repository.addTreuepunkte(uuid, amount),
                        (repository, uuid, amount) -> repository.setTreuepunkte(uuid, amount),
                        record -> record.treuepunkte()
                ));
    }
}
