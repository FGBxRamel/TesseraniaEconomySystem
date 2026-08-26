package de.bydora.tes;

import de.bydora.tes.command.belohnung.BelohnungCommand;
import de.bydora.tes.command.debug.DebugCommand;
import de.bydora.tes.command.punkte.ErfahrungspunkteCommand;
import de.bydora.tes.command.punkte.PunkteAliasCommand;
import de.bydora.tes.command.punkte.TreuepunkteCommand;
import de.bydora.tes.command.rechnung.RechnungCommand;
import de.bydora.tes.command.shop.ShopCommand;
import de.bydora.tes.command.spieler.SpielerCommand;
import io.papermc.paper.plugin.bootstrap.BootstrapContext;
import io.papermc.paper.plugin.bootstrap.PluginBootstrap;
import io.papermc.paper.plugin.bootstrap.PluginProviderContext;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

/**
 * Registers TES's Brigadier command trees during the plugin bootstrap phase — Paper's preferred
 * registration context for {@code paper-plugin.yml}-based plugins. Each subsystem is its own
 * top-level command ({@code /shop}, {@code /rechnung}, {@code /spieler}, ...) rather than being
 * nested under a shared {@code /tes} root.
 */
public final class TesBootstrap implements PluginBootstrap {

    @Override
    public void bootstrap(@NotNull BootstrapContext context) {
        context.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, commands -> {
            commands.registrar().register(SpielerCommand.build().build(), "Spielerverwaltung des Tesserania Economy Systems");
            commands.registrar().register(ShopCommand.build().build(), "Item-Shop-Verwaltung des Tesserania Economy Systems");
            commands.registrar().register(TreuepunkteCommand.build().build(), "Treuepunkteverwaltung des Tesserania Economy Systems");
            commands.registrar().register(PunkteAliasCommand.build().build(), "Treuepunkteshop des Tesserania Economy Systems");
            commands.registrar().register(ErfahrungspunkteCommand.build().build(), "Erfahrungspunkteverwaltung des Tesserania Economy Systems");
            commands.registrar().register(BelohnungCommand.build().build(), "Belohnungsinventar des Tesserania Economy Systems");
            commands.registrar().register(RechnungCommand.build().build(), "Rechnungsverwaltung des Tesserania Economy Systems");
            commands.registrar().register(DebugCommand.build().build(), "Admin-Debugwerkzeuge des Tesserania Economy Systems");
        });
    }

    @Override
    public @NotNull JavaPlugin createPlugin(@NotNull PluginProviderContext context) {
        return new TesseraniaEconomySystem();
    }
}
