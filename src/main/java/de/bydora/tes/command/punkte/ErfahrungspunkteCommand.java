package de.bydora.tes.command.punkte;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;

/**
 * Implements {@code /tes erfahrungspunkte add|remove|set <Name> <Anzahl>} (spec §1.4).
 */
public final class ErfahrungspunkteCommand {

    private ErfahrungspunkteCommand() {
    }

    public static LiteralArgumentBuilder<CommandSourceStack> build() {
        return PunkteCommandFactory.build(
                "erfahrungspunkte",
                "tes.admin.erfahrungspunkte",
                "Erfahrungspunkte",
                new PunkteCommandFactory.Counter(
                        (repository, uuid, amount) -> repository.addErfahrungspunkte(uuid, amount),
                        (repository, uuid, amount) -> repository.setErfahrungspunkte(uuid, amount),
                        record -> record.erfahrungspunkte()
                ));
    }
}
