package de.bydora.tes.invoice;

import de.bydora.tes.data.Database;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * SQLite-backed {@link InvoiceRepository}. All access goes through {@link Database#execute},
 * serializing it against the plugin's single connection.
 */
public final class SqliteInvoiceRepository implements InvoiceRepository {

    private final Database database;

    public SqliteInvoiceRepository(Database database) {
        this.database = database;
    }

    @Override
    public InvoiceRecord insert(UUID creatorUuid, UUID targetUuid, int price, String reason, long createdAt) {
        return database.execute(() -> {
            try (PreparedStatement statement = database.connection().prepareStatement(
                    "INSERT INTO invoices (creator_uuid, target_uuid, price, reason, state, created_at) "
                            + "VALUES (?, ?, ?, ?, ?, ?)",
                    java.sql.Statement.RETURN_GENERATED_KEYS)) {
                statement.setString(1, creatorUuid.toString());
                statement.setString(2, targetUuid.toString());
                statement.setInt(3, price);
                statement.setString(4, reason);
                statement.setString(5, InvoiceState.OPEN.name());
                statement.setLong(6, createdAt);
                statement.executeUpdate();
                try (ResultSet keys = statement.getGeneratedKeys()) {
                    keys.next();
                    long id = keys.getLong(1);
                    return new InvoiceRecord(id, creatorUuid, targetUuid, price, reason, InvoiceState.OPEN, createdAt, null);
                }
            }
        });
    }

    @Override
    public List<InvoiceRecord> findOpenByTarget(UUID targetUuid) {
        return database.execute(() -> {
            List<InvoiceRecord> invoices = new ArrayList<>();
            try (PreparedStatement statement = database.connection().prepareStatement(
                    "SELECT * FROM invoices WHERE target_uuid = ? AND state = ? ORDER BY created_at, id")) {
                statement.setString(1, targetUuid.toString());
                statement.setString(2, InvoiceState.OPEN.name());
                try (ResultSet resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        invoices.add(toRecord(resultSet));
                    }
                }
            }
            return invoices;
        });
    }

    @Override
    public Optional<InvoiceRecord> findById(long id) {
        return database.execute(() -> {
            try (PreparedStatement statement = database.connection().prepareStatement(
                    "SELECT * FROM invoices WHERE id = ?")) {
                statement.setLong(1, id);
                try (ResultSet resultSet = statement.executeQuery()) {
                    return resultSet.next() ? Optional.of(toRecord(resultSet)) : Optional.empty();
                }
            }
        });
    }

    @Override
    public void markSettled(long id, long settledAt) {
        database.execute(() -> {
            try (PreparedStatement statement = database.connection().prepareStatement(
                    "UPDATE invoices SET state = ?, settled_at = ? WHERE id = ?")) {
                statement.setString(1, InvoiceState.SETTLED.name());
                statement.setLong(2, settledAt);
                statement.setLong(3, id);
                return statement.executeUpdate();
            }
        });
    }

    private static InvoiceRecord toRecord(ResultSet resultSet) throws SQLException {
        long settledAtValue = resultSet.getLong("settled_at");
        Long settledAt = resultSet.wasNull() ? null : settledAtValue;
        return new InvoiceRecord(
                resultSet.getLong("id"),
                UUID.fromString(resultSet.getString("creator_uuid")),
                UUID.fromString(resultSet.getString("target_uuid")),
                resultSet.getInt("price"),
                resultSet.getString("reason"),
                InvoiceState.valueOf(resultSet.getString("state")),
                resultSet.getLong("created_at"),
                settledAt
        );
    }
}
