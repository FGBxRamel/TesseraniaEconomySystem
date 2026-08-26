package de.bydora.tes.prozessverstaerker;

import de.bydora.tes.data.Database;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * SQLite-backed {@link ProzessverstaerkerBoostRepository}. All access goes through
 * {@link Database#execute}, serializing it against the plugin's single connection — the
 * read-then-upsert in {@link #extend} relies on that serialization to stay atomic.
 */
public final class SqliteProzessverstaerkerBoostRepository implements ProzessverstaerkerBoostRepository {

    private final Database database;

    public SqliteProzessverstaerkerBoostRepository(Database database) {
        this.database = database;
    }

    @Override
    public long extend(String world, int x, int y, int z, BoostKind kind, long durationMillis, long now) {
        return database.execute(() -> {
            long base = Math.max(now, currentExpiry(world, x, y, z).orElse(now));
            long newExpiresAt = base + durationMillis;
            try (PreparedStatement statement = database.connection().prepareStatement("""
                    INSERT INTO prozessverstaerker_boosts (world, x, y, z, kind, expires_at) VALUES (?, ?, ?, ?, ?, ?)
                    ON CONFLICT(world, x, y, z) DO UPDATE SET kind = excluded.kind, expires_at = excluded.expires_at
                    """)) {
                statement.setString(1, world);
                statement.setInt(2, x);
                statement.setInt(3, y);
                statement.setInt(4, z);
                statement.setString(5, kind.name());
                statement.setLong(6, newExpiresAt);
                statement.executeUpdate();
            }
            return newExpiresAt;
        });
    }

    private Optional<Long> currentExpiry(String world, int x, int y, int z) throws java.sql.SQLException {
        try (PreparedStatement statement = database.connection().prepareStatement(
                "SELECT expires_at FROM prozessverstaerker_boosts WHERE world = ? AND x = ? AND y = ? AND z = ?")) {
            statement.setString(1, world);
            statement.setInt(2, x);
            statement.setInt(3, y);
            statement.setInt(4, z);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? Optional.of(resultSet.getLong("expires_at")) : Optional.empty();
            }
        }
    }

    @Override
    public List<ProzessverstaerkerBoostRecord> findAll() {
        return database.execute(() -> {
            List<ProzessverstaerkerBoostRecord> boosts = new ArrayList<>();
            try (PreparedStatement statement = database.connection().prepareStatement(
                    "SELECT * FROM prozessverstaerker_boosts");
                 ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    boosts.add(new ProzessverstaerkerBoostRecord(
                            resultSet.getString("world"),
                            resultSet.getInt("x"),
                            resultSet.getInt("y"),
                            resultSet.getInt("z"),
                            BoostKind.valueOf(resultSet.getString("kind")),
                            resultSet.getLong("expires_at")));
                }
            }
            return boosts;
        });
    }

    @Override
    public void delete(String world, int x, int y, int z) {
        database.execute(() -> {
            try (PreparedStatement statement = database.connection().prepareStatement(
                    "DELETE FROM prozessverstaerker_boosts WHERE world = ? AND x = ? AND y = ? AND z = ?")) {
                statement.setString(1, world);
                statement.setInt(2, x);
                statement.setInt(3, y);
                statement.setInt(4, z);
                return statement.executeUpdate();
            }
        });
    }
}
