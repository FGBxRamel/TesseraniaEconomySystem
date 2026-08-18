package de.bydora.tes;

import de.bydora.tes.command.confirm.ConfirmationManager;
import de.bydora.tes.config.TesConfig;
import de.bydora.tes.data.Database;
import de.bydora.tes.data.PlayerRepository;
import de.bydora.tes.data.SqlitePlayerRepository;
import org.bukkit.plugin.java.JavaPlugin;

import java.time.Duration;
import java.util.UUID;

public final class TesseraniaEconomySystem extends JavaPlugin {

    private final ConfirmationManager<UUID> removeConfirmations = new ConfirmationManager<>(Duration.ofSeconds(30));

    private Database database;
    private PlayerRepository playerRepository;

    @Override
    public void onEnable() {
        TesConfig tesConfig = new TesConfig(this);
        tesConfig.load();

        database = new Database(this);
        database.open();
        playerRepository = new SqlitePlayerRepository(database);
    }

    @Override
    public void onDisable() {
        if (database != null) {
            database.close();
        }
    }

    /**
     * Looked up lazily by command handlers (registered during the bootstrap phase, before
     * {@link #onEnable()} has run) via {@code JavaPlugin.getPlugin(TesseraniaEconomySystem.class)}.
     */
    public PlayerRepository playerRepository() {
        return playerRepository;
    }

    public ConfirmationManager<UUID> removeConfirmations() {
        return removeConfirmations;
    }
}
