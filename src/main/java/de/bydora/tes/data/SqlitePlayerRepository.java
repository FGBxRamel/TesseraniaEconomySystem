package de.bydora.tes.data;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;

/**
 * SQLite-backed {@link PlayerRepository}. All access goes through {@link Database#execute},
 * serializing it against the plugin's single connection.
 */
public final class SqlitePlayerRepository implements PlayerRepository {

    private final Database database;

    public SqlitePlayerRepository(Database database) {
        this.database = database;
    }

    @Override
    public Optional<PlayerRecord> findByUuid(UUID uuid) {
        return database.execute(() -> {
            try (PreparedStatement statement = database.connection().prepareStatement(
                    "SELECT * FROM players WHERE uuid = ?")) {
                statement.setString(1, uuid.toString());
                try (ResultSet resultSet = statement.executeQuery()) {
                    return resultSet.next() ? Optional.of(toRecord(resultSet)) : Optional.empty();
                }
            }
        });
    }

    @Override
    public Optional<PlayerRecord> findByUsername(String username) {
        return database.execute(() -> {
            try (PreparedStatement statement = database.connection().prepareStatement(
                    "SELECT * FROM players WHERE username = ? COLLATE NOCASE")) {
                statement.setString(1, username);
                try (ResultSet resultSet = statement.executeQuery()) {
                    return resultSet.next() ? Optional.of(toRecord(resultSet)) : Optional.empty();
                }
            }
        });
    }

    @Override
    public boolean isRegistered(UUID uuid) {
        return findByUuid(uuid).isPresent();
    }

    @Override
    public PlayerRecord register(UUID uuid, String username) {
        return database.execute(() -> {
            long now = System.currentTimeMillis();
            try (PreparedStatement statement = database.connection().prepareStatement(
                    "INSERT INTO players (uuid, username, treuepunkte, erfahrungspunkte, level, paused, registered_at, updated_at) "
                            + "VALUES (?, ?, 0, 0, 0, 0, ?, ?)")) {
                statement.setString(1, uuid.toString());
                statement.setString(2, username);
                statement.setLong(3, now);
                statement.setLong(4, now);
                statement.executeUpdate();
            }
            return new PlayerRecord(uuid, username, 0, 0, 0, false, now, now);
        });
    }

    @Override
    public void setPaused(UUID uuid, boolean paused) {
        database.execute(() -> {
            try (PreparedStatement statement = database.connection().prepareStatement(
                    "UPDATE players SET paused = ?, updated_at = ? WHERE uuid = ?")) {
                statement.setInt(1, paused ? 1 : 0);
                statement.setLong(2, System.currentTimeMillis());
                statement.setString(3, uuid.toString());
                return statement.executeUpdate();
            }
        });
    }

    @Override
    public void delete(UUID uuid) {
        database.execute(() -> {
            try (PreparedStatement statement = database.connection().prepareStatement(
                    "DELETE FROM players WHERE uuid = ?")) {
                statement.setString(1, uuid.toString());
                return statement.executeUpdate();
            }
        });
    }

    private static PlayerRecord toRecord(ResultSet resultSet) throws SQLException {
        return new PlayerRecord(
                UUID.fromString(resultSet.getString("uuid")),
                resultSet.getString("username"),
                resultSet.getInt("treuepunkte"),
                resultSet.getInt("erfahrungspunkte"),
                resultSet.getInt("level"),
                resultSet.getInt("paused") != 0,
                resultSet.getLong("registered_at"),
                resultSet.getLong("updated_at")
        );
    }
}
