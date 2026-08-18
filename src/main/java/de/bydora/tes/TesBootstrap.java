package de.bydora.tes;

import de.bydora.tes.command.TesCommand;
import io.papermc.paper.plugin.bootstrap.BootstrapContext;
import io.papermc.paper.plugin.bootstrap.PluginBootstrap;
import io.papermc.paper.plugin.bootstrap.PluginProviderContext;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

/**
 * Registers TES's Brigadier command tree during the plugin bootstrap phase — Paper's preferred
 * registration context for {@code paper-plugin.yml}-based plugins.
 */
public final class TesBootstrap implements PluginBootstrap {

    @Override
    public void bootstrap(@NotNull BootstrapContext context) {
        context.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, commands ->
                commands.registrar().register(TesCommand.createCommand().build(), "Hauptbefehl des Tesserania Economy Systems"));
    }

    @Override
    public @NotNull JavaPlugin createPlugin(@NotNull PluginProviderContext context) {
        return new TesseraniaEconomySystem();
    }
}
