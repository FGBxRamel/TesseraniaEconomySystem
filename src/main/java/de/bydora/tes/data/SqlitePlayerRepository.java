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
            return new PlayerRecord(uuid, username, 0, 0, 0, false, now, now, 0);
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
    public void addTreuepunkte(UUID uuid, int delta) {
        applyCounterUpdate("UPDATE players SET treuepunkte = MAX(0, treuepunkte + ?), updated_at = ? WHERE uuid = ?", delta, uuid);
    }

    @Override
    public void setTreuepunkte(UUID uuid, int value) {
        applyCounterUpdate("UPDATE players SET treuepunkte = MAX(0, ?), updated_at = ? WHERE uuid = ?", value, uuid);
    }

    @Override
    public void addErfahrungspunkte(UUID uuid, int delta) {
        applyCounterUpdate("UPDATE players SET erfahrungspunkte = MAX(0, erfahrungspunkte + ?), updated_at = ? WHERE uuid = ?", delta, uuid);
    }

    @Override
    public void setErfahrungspunkte(UUID uuid, int value) {
        applyCounterUpdate("UPDATE players SET erfahrungspunkte = MAX(0, ?), updated_at = ? WHERE uuid = ?", value, uuid);
    }

    private void applyCounterUpdate(String sql, int amount, UUID uuid) {
        database.execute(() -> {
            try (PreparedStatement statement = database.connection().prepareStatement(sql)) {
                statement.setInt(1, amount);
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

    @Override
    public void addInvoiceBalance(UUID uuid, int delta) {
        applyCounterUpdate("UPDATE players SET invoice_balance = MAX(0, invoice_balance + ?), updated_at = ? WHERE uuid = ?", delta, uuid);
    }

    @Override
    public int cashOutInvoiceBalance(UUID uuid) {
        return database.execute(() -> {
            var connection = database.connection();
            connection.setAutoCommit(false);
            try {
                int balance;
                try (PreparedStatement select = connection.prepareStatement(
                        "SELECT invoice_balance FROM players WHERE uuid = ?")) {
                    select.setString(1, uuid.toString());
                    try (ResultSet resultSet = select.executeQuery()) {
                        balance = resultSet.next() ? resultSet.getInt("invoice_balance") : 0;
                    }
                }
                if (balance > 0) {
                    try (PreparedStatement update = connection.prepareStatement(
                            "UPDATE players SET invoice_balance = 0, updated_at = ? WHERE uuid = ?")) {
                        update.setLong(1, System.currentTimeMillis());
                        update.setString(2, uuid.toString());
                        update.executeUpdate();
                    }
                }
                connection.commit();
                return balance;
            } catch (Exception e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }
        });
    }

    @Override
    public SpendResult spendTreuepunkte(UUID uuid, int cost) {
        return database.execute(() -> {
            var connection = database.connection();
            connection.setAutoCommit(false);
            try {
                int balance = currentTreuepunkte(connection, uuid);
                if (balance < cost) {
                    connection.rollback();
                    return SpendResult.INSUFFICIENT;
                }
                try (PreparedStatement update = connection.prepareStatement(
                        "UPDATE players SET treuepunkte = treuepunkte - ?, updated_at = ? WHERE uuid = ?")) {
                    update.setInt(1, cost);
                    update.setLong(2, System.currentTimeMillis());
                    update.setString(3, uuid.toString());
                    update.executeUpdate();
                }
                connection.commit();
                return SpendResult.SPENT;
            } catch (Exception e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }
        });
    }

    @Override
    public TransferResult transferTreuepunkte(UUID from, UUID to, int amount) {
        return database.execute(() -> {
            var connection = database.connection();
            connection.setAutoCommit(false);
            try {
                int balance = currentTreuepunkte(connection, from);
                if (balance < amount) {
                    connection.rollback();
                    return TransferResult.INSUFFICIENT;
                }
                long now = System.currentTimeMillis();
                try (PreparedStatement debit = connection.prepareStatement(
                        "UPDATE players SET treuepunkte = treuepunkte - ?, updated_at = ? WHERE uuid = ?")) {
                    debit.setInt(1, amount);
                    debit.setLong(2, now);
                    debit.setString(3, from.toString());
                    debit.executeUpdate();
                }
                try (PreparedStatement credit = connection.prepareStatement(
                        "UPDATE players SET treuepunkte = treuepunkte + ?, updated_at = ? WHERE uuid = ?")) {
                    credit.setInt(1, amount);
                    credit.setLong(2, now);
                    credit.setString(3, to.toString());
                    credit.executeUpdate();
                }
                connection.commit();
                return TransferResult.TRANSFERRED;
            } catch (Exception e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }
        });
    }

    private static int currentTreuepunkte(java.sql.Connection connection, UUID uuid) throws SQLException {
        try (PreparedStatement select = connection.prepareStatement(
                "SELECT treuepunkte FROM players WHERE uuid = ?")) {
            select.setString(1, uuid.toString());
            try (ResultSet resultSet = select.executeQuery()) {
                return resultSet.next() ? resultSet.getInt("treuepunkte") : 0;
            }
        }
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
                resultSet.getLong("updated_at"),
                resultSet.getInt("invoice_balance")
        );
    }
}
