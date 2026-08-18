package de.bydora.tes;

import de.bydora.tes.command.TesCommand;
import de.bydora.tes.command.confirm.ConfirmationManager;
import de.bydora.tes.command.spieler.SpielerCommand;
import de.bydora.tes.config.TesConfig;
import de.bydora.tes.data.Database;
import de.bydora.tes.data.PlayerRepository;
import de.bydora.tes.data.SqlitePlayerRepository;
import org.bukkit.plugin.java.JavaPlugin;

import java.time.Duration;
import java.util.UUID;

public final class TesseraniaEconomySystem extends JavaPlugin {

    private Database database;

    @Override
    public void onEnable() {
        TesConfig tesConfig = new TesConfig(this);
        tesConfig.load();

        database = new Database(this);
        database.open();
        PlayerRepository playerRepository = new SqlitePlayerRepository(database);

        TesCommand tesCommand = new TesCommand();
        tesCommand.register(new SpielerCommand(playerRepository, new ConfirmationManager<UUID>(Duration.ofSeconds(30))));

        var command = getCommand("tes");
        command.setExecutor(tesCommand);
        command.setTabCompleter(tesCommand);
    }

    @Override
    public void onDisable() {
        if (database != null) {
            database.close();
        }
    }
}
