package de.bydora.tes.data;

import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Level;

/**
 * Owns the plugin's single SQLite connection and a single-threaded executor that serializes
 * all access to it, avoiding "database is locked" errors from concurrent access. SQLite only
 * supports one writer at a time regardless of pool size, so a connection pool would add
 * complexity without benefit here.
 */
public final class Database {

    private final JavaPlugin plugin;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private Connection connection;

    public Database(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Opens the connection to {@code <dataFolder>/tes.db} and applies pending schema migrations.
     */
    public void open() {
        File dataFolder = plugin.getDataFolder();
        if (!dataFolder.exists() && !dataFolder.mkdirs()) {
            throw new IllegalStateException("Could not create plugin data folder: " + dataFolder);
        }
        File dbFile = new File(dataFolder, "tes.db");
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());
            try (Statement statement = connection.createStatement()) {
                statement.execute("PRAGMA journal_mode = WAL");
                statement.execute("PRAGMA foreign_keys = ON");
            }
            SchemaMigrator.migrate(connection);
        } catch (SQLException e) {
            throw new IllegalStateException("Could not open TES database", e);
        }
    }

    /**
     * Runs {@code task} on the database's single-threaded executor and blocks for its result,
     * serializing it against all other database access.
     */
    public <T> T execute(Callable<T> task) {
        Future<T> future = executor.submit(task);
        try {
            return future.get();
        } catch (Exception e) {
            throw new IllegalStateException("Database operation failed", e);
        }
    }

    public Connection connection() {
        return connection;
    }

    public void close() {
        executor.shutdown();
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.WARNING, "Failed to close TES database connection", e);
            }
        }
    }
}
