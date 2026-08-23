package de.bydora.tes.reward;

import de.bydora.tes.data.Database;
import org.bukkit.inventory.ItemStack;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * SQLite-backed {@link RewardInventoryRepository}. All access goes through
 * {@link Database#execute}, serializing it against the plugin's single connection.
 */
public final class SqliteRewardInventoryRepository implements RewardInventoryRepository {

    private final Database database;

    public SqliteRewardInventoryRepository(Database database) {
        this.database = database;
    }

    @Override
    public void insert(UUID uuid, ItemStack item, long grantedAt) {
        database.execute(() -> {
            try (PreparedStatement statement = database.connection().prepareStatement(
                    "INSERT INTO reward_inventory_items (uuid, item, granted_at) VALUES (?, ?, ?)")) {
                statement.setString(1, uuid.toString());
                statement.setBytes(2, item.serializeAsBytes());
                statement.setLong(3, grantedAt);
                return statement.executeUpdate();
            }
        });
    }

    @Override
    public List<RewardInventoryItemRecord> findAllByUuid(UUID uuid) {
        return database.execute(() -> {
            List<RewardInventoryItemRecord> items = new ArrayList<>();
            try (PreparedStatement statement = database.connection().prepareStatement(
                    "SELECT * FROM reward_inventory_items WHERE uuid = ? ORDER BY granted_at, id")) {
                statement.setString(1, uuid.toString());
                try (ResultSet resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        items.add(toRecord(resultSet));
                    }
                }
            }
            return items;
        });
    }

    @Override
    public Optional<RewardInventoryItemRecord> findById(long id) {
        return database.execute(() -> {
            try (PreparedStatement statement = database.connection().prepareStatement(
                    "SELECT * FROM reward_inventory_items WHERE id = ?")) {
                statement.setLong(1, id);
                try (ResultSet resultSet = statement.executeQuery()) {
                    return resultSet.next() ? Optional.of(toRecord(resultSet)) : Optional.empty();
                }
            }
        });
    }

    @Override
    public void delete(long id) {
        database.execute(() -> {
            try (PreparedStatement statement = database.connection().prepareStatement(
                    "DELETE FROM reward_inventory_items WHERE id = ?")) {
                statement.setLong(1, id);
                return statement.executeUpdate();
            }
        });
    }

    private static RewardInventoryItemRecord toRecord(ResultSet resultSet) throws java.sql.SQLException {
        return new RewardInventoryItemRecord(
                resultSet.getLong("id"),
                UUID.fromString(resultSet.getString("uuid")),
                ItemStack.deserializeBytes(resultSet.getBytes("item")),
                resultSet.getLong("granted_at")
        );
    }
}
