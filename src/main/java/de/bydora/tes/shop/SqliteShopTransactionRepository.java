package de.bydora.tes.shop;

import de.bydora.tes.data.Database;
import org.bukkit.inventory.ItemStack;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * SQLite-backed {@link ShopTransactionRepository}. All access goes through
 * {@link Database#execute}, serializing it against the plugin's single connection.
 */
public final class SqliteShopTransactionRepository implements ShopTransactionRepository {

    private final Database database;

    public SqliteShopTransactionRepository(Database database) {
        this.database = database;
    }

    @Override
    public ShopTransactionRecord insertPending(String shopWorld, String shopId, int slot, UUID buyer, ItemStack item, int price, int staatskasseFunded, long purchasedAt) {
        return database.execute(() -> {
            try (PreparedStatement statement = database.connection().prepareStatement(
                    "INSERT INTO shop_transactions (shop_world, shop_id, slot, buyer_uuid, item, price, staatskasse_funded, state, purchased_at, resolved_at) "
                            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, NULL)",
                    Statement.RETURN_GENERATED_KEYS)) {
                statement.setString(1, shopWorld);
                statement.setString(2, shopId);
                statement.setInt(3, slot);
                statement.setString(4, buyer.toString());
                statement.setBytes(5, item.serializeAsBytes());
                statement.setInt(6, price);
                statement.setInt(7, staatskasseFunded);
                statement.setString(8, TransactionState.PENDING.name());
                statement.setLong(9, purchasedAt);
                statement.executeUpdate();
                try (ResultSet keys = statement.getGeneratedKeys()) {
                    keys.next();
                    long id = keys.getLong(1);
                    return new ShopTransactionRecord(id, shopWorld, shopId, slot, buyer, item, price, staatskasseFunded, TransactionState.PENDING, purchasedAt, null);
                }
            }
        });
    }

    @Override
    public Optional<ShopTransactionRecord> findPendingBySlot(String shopWorld, String shopId, int slot, UUID buyer) {
        return database.execute(() -> {
            try (PreparedStatement statement = database.connection().prepareStatement(
                    "SELECT * FROM shop_transactions WHERE shop_world = ? AND shop_id = ? AND slot = ? AND buyer_uuid = ? AND state = ?")) {
                statement.setString(1, shopWorld);
                statement.setString(2, shopId);
                statement.setInt(3, slot);
                statement.setString(4, buyer.toString());
                statement.setString(5, TransactionState.PENDING.name());
                try (ResultSet resultSet = statement.executeQuery()) {
                    return resultSet.next() ? Optional.of(toRecord(resultSet)) : Optional.empty();
                }
            }
        });
    }

    @Override
    public Optional<ShopTransactionRecord> findPendingBySlot(String shopWorld, String shopId, int slot) {
        return database.execute(() -> {
            try (PreparedStatement statement = database.connection().prepareStatement(
                    "SELECT * FROM shop_transactions WHERE shop_world = ? AND shop_id = ? AND slot = ? AND state = ?")) {
                statement.setString(1, shopWorld);
                statement.setString(2, shopId);
                statement.setInt(3, slot);
                statement.setString(4, TransactionState.PENDING.name());
                try (ResultSet resultSet = statement.executeQuery()) {
                    return resultSet.next() ? Optional.of(toRecord(resultSet)) : Optional.empty();
                }
            }
        });
    }

    @Override
    public List<ShopTransactionRecord> findPendingForShop(String shopWorld, String shopId) {
        return database.execute(() -> {
            List<ShopTransactionRecord> transactions = new ArrayList<>();
            try (PreparedStatement statement = database.connection().prepareStatement(
                    "SELECT * FROM shop_transactions WHERE shop_world = ? AND shop_id = ? AND state = ?")) {
                statement.setString(1, shopWorld);
                statement.setString(2, shopId);
                statement.setString(3, TransactionState.PENDING.name());
                try (ResultSet resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        transactions.add(toRecord(resultSet));
                    }
                }
            }
            return transactions;
        });
    }

    @Override
    public List<ShopTransactionRecord> findPendingDueBefore(long cutoff) {
        return database.execute(() -> {
            List<ShopTransactionRecord> transactions = new ArrayList<>();
            try (PreparedStatement statement = database.connection().prepareStatement(
                    "SELECT * FROM shop_transactions WHERE state = ? AND purchased_at <= ?")) {
                statement.setString(1, TransactionState.PENDING.name());
                statement.setLong(2, cutoff);
                try (ResultSet resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        transactions.add(toRecord(resultSet));
                    }
                }
            }
            return transactions;
        });
    }

    @Override
    public void markRefunded(long id, long resolvedAt) {
        markResolved(id, TransactionState.REFUNDED, resolvedAt);
    }

    @Override
    public void markCompleted(long id, long resolvedAt) {
        markResolved(id, TransactionState.COMPLETED, resolvedAt);
    }

    private void markResolved(long id, TransactionState state, long resolvedAt) {
        database.execute(() -> {
            try (PreparedStatement statement = database.connection().prepareStatement(
                    "UPDATE shop_transactions SET state = ?, resolved_at = ? WHERE id = ?")) {
                statement.setString(1, state.name());
                statement.setLong(2, resolvedAt);
                statement.setLong(3, id);
                return statement.executeUpdate();
            }
        });
    }

    private static ShopTransactionRecord toRecord(ResultSet resultSet) throws SQLException {
        long resolvedAtValue = resultSet.getLong("resolved_at");
        Long resolvedAt = resultSet.wasNull() ? null : resolvedAtValue;
        return new ShopTransactionRecord(
                resultSet.getLong("id"),
                resultSet.getString("shop_world"),
                resultSet.getString("shop_id"),
                resultSet.getInt("slot"),
                UUID.fromString(resultSet.getString("buyer_uuid")),
                ItemStack.deserializeBytes(resultSet.getBytes("item")),
                resultSet.getInt("price"),
                resultSet.getInt("staatskasse_funded"),
                TransactionState.valueOf(resultSet.getString("state")),
                resultSet.getLong("purchased_at"),
                resolvedAt
        );
    }
}
