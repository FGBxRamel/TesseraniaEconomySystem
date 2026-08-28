package de.bydora.tes.handelsbonus;

import de.bydora.tes.data.Database;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * SQLite-backed {@link HandelsbonusRepository}. All access goes through {@link Database#execute},
 * serializing it against the plugin's single connection — {@link #consumeDiscount}'s
 * read-then-update relies on that serialization to stay atomic.
 */
public final class SqliteHandelsbonusRepository implements HandelsbonusRepository {

    private final Database database;

    public SqliteHandelsbonusRepository(Database database) {
        this.database = database;
    }

    @Override
    public Optional<HandelsbonusHolderRecord> find(UUID uuid) {
        return database.execute(() -> {
            try (PreparedStatement statement = database.connection().prepareStatement(
                    "SELECT * FROM handelsbonus_holders WHERE uuid = ?")) {
                statement.setString(1, uuid.toString());
                try (ResultSet resultSet = statement.executeQuery()) {
                    return resultSet.next() ? Optional.of(toRecord(resultSet)) : Optional.empty();
                }
            }
        });
    }

    @Override
    public int countOnCooldown(long now) {
        return database.execute(() -> {
            try (PreparedStatement statement = database.connection().prepareStatement(
                    "SELECT COUNT(*) FROM handelsbonus_holders WHERE cooldown_until > ?")) {
                statement.setLong(1, now);
                try (ResultSet resultSet = statement.executeQuery()) {
                    resultSet.next();
                    return resultSet.getInt(1);
                }
            }
        });
    }

    @Override
    public List<UUID> onCooldown(long now) {
        return database.execute(() -> {
            try (PreparedStatement statement = database.connection().prepareStatement(
                    "SELECT uuid FROM handelsbonus_holders WHERE cooldown_until > ?")) {
                statement.setLong(1, now);
                try (ResultSet resultSet = statement.executeQuery()) {
                    List<UUID> uuids = new ArrayList<>();
                    while (resultSet.next()) {
                        uuids.add(UUID.fromString(resultSet.getString("uuid")));
                    }
                    return uuids;
                }
            }
        });
    }

    @Override
    public void activate(UUID uuid, int discountRemaining, long cooldownUntil) {
        database.execute(() -> {
            try (PreparedStatement statement = database.connection().prepareStatement("""
                    INSERT INTO handelsbonus_holders (uuid, discount_remaining, cooldown_until) VALUES (?, ?, ?)
                    ON CONFLICT(uuid) DO UPDATE SET discount_remaining = excluded.discount_remaining, cooldown_until = excluded.cooldown_until
                    """)) {
                statement.setString(1, uuid.toString());
                statement.setInt(2, discountRemaining);
                statement.setLong(3, cooldownUntil);
                return statement.executeUpdate();
            }
        });
    }

    @Override
    public int consumeDiscount(UUID uuid, int amount) {
        return database.execute(() -> {
            int current = currentDiscount(uuid);
            int applied = Math.min(current, amount);
            if (applied <= 0) {
                return 0;
            }
            try (PreparedStatement statement = database.connection().prepareStatement(
                    "UPDATE handelsbonus_holders SET discount_remaining = discount_remaining - ? WHERE uuid = ?")) {
                statement.setInt(1, applied);
                statement.setString(2, uuid.toString());
                statement.executeUpdate();
            }
            return applied;
        });
    }

    @Override
    public boolean resetCooldown(UUID uuid, long now) {
        return database.execute(() -> {
            try (PreparedStatement statement = database.connection().prepareStatement(
                    "UPDATE handelsbonus_holders SET cooldown_until = 0 WHERE uuid = ? AND cooldown_until > ?")) {
                statement.setString(1, uuid.toString());
                statement.setLong(2, now);
                return statement.executeUpdate() > 0;
            }
        });
    }

    private int currentDiscount(UUID uuid) throws java.sql.SQLException {
        try (PreparedStatement statement = database.connection().prepareStatement(
                "SELECT discount_remaining FROM handelsbonus_holders WHERE uuid = ?")) {
            statement.setString(1, uuid.toString());
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? resultSet.getInt(1) : 0;
            }
        }
    }

    private static HandelsbonusHolderRecord toRecord(ResultSet resultSet) throws java.sql.SQLException {
        return new HandelsbonusHolderRecord(
                UUID.fromString(resultSet.getString("uuid")),
                resultSet.getInt("discount_remaining"),
                resultSet.getLong("cooldown_until"));
    }
}
