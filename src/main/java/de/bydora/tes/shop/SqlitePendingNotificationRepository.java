package de.bydora.tes.shop;

import de.bydora.tes.data.Database;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * SQLite-backed {@link PendingNotificationRepository}. All access goes through
 * {@link Database#execute}, serializing it against the plugin's single connection.
 */
public final class SqlitePendingNotificationRepository implements PendingNotificationRepository {

    private final Database database;

    public SqlitePendingNotificationRepository(Database database) {
        this.database = database;
    }

    @Override
    public void enqueue(UUID uuid, String message) {
        database.execute(() -> {
            try (PreparedStatement statement = database.connection().prepareStatement(
                    "INSERT INTO pending_notifications (uuid, message, created_at) VALUES (?, ?, ?)")) {
                statement.setString(1, uuid.toString());
                statement.setString(2, message);
                statement.setLong(3, System.currentTimeMillis());
                return statement.executeUpdate();
            }
        });
    }

    @Override
    public List<String> drain(UUID uuid) {
        return database.execute(() -> {
            List<String> messages = new ArrayList<>();
            var connection = database.connection();
            connection.setAutoCommit(false);
            try {
                try (PreparedStatement select = connection.prepareStatement(
                        "SELECT message FROM pending_notifications WHERE uuid = ? ORDER BY created_at, id")) {
                    select.setString(1, uuid.toString());
                    try (ResultSet resultSet = select.executeQuery()) {
                        while (resultSet.next()) {
                            messages.add(resultSet.getString("message"));
                        }
                    }
                }
                try (PreparedStatement delete = connection.prepareStatement(
                        "DELETE FROM pending_notifications WHERE uuid = ?")) {
                    delete.setString(1, uuid.toString());
                    delete.executeUpdate();
                }
                connection.commit();
            } catch (Exception e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }
            return messages;
        });
    }
}
