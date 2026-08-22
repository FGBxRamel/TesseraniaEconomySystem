package de.bydora.tes.shop;

import de.bydora.tes.data.Database;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * SQLite-backed {@link ShopRepository}. All access goes through {@link Database#execute},
 * serializing it against the plugin's single connection.
 */
public final class SqliteShopRepository implements ShopRepository {

    private final Database database;

    public SqliteShopRepository(Database database) {
        this.database = database;
    }

    @Override
    public Optional<ShopRecord> findByWorldAndId(String world, String id) {
        return database.execute(() -> {
            try (PreparedStatement statement = database.connection().prepareStatement(
                    "SELECT * FROM shops WHERE world = ? AND id = ?")) {
                statement.setString(1, world);
                statement.setString(2, id);
                try (ResultSet resultSet = statement.executeQuery()) {
                    return resultSet.next() ? Optional.of(toRecord(resultSet, loadOwners(world, id))) : Optional.empty();
                }
            }
        });
    }

    @Override
    public List<ShopRecord> findAllByOwner(UUID owner) {
        return database.execute(() -> {
            List<ShopRecord> shops = new ArrayList<>();
            try (PreparedStatement statement = database.connection().prepareStatement(
                    "SELECT s.* FROM shops s JOIN shop_owners o ON s.world = o.shop_world AND s.id = o.shop_id "
                            + "WHERE o.uuid = ? ORDER BY s.created_at")) {
                statement.setString(1, owner.toString());
                try (ResultSet resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        shops.add(toRecord(resultSet, loadOwners(resultSet.getString("world"), resultSet.getString("id"))));
                    }
                }
            }
            return shops;
        });
    }

    @Override
    public List<ShopRecord> findAllActive() {
        return database.execute(() -> {
            List<ShopRecord> shops = new ArrayList<>();
            try (PreparedStatement statement = database.connection().prepareStatement(
                    "SELECT * FROM shops");
                 ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    shops.add(toRecord(resultSet, loadOwners(resultSet.getString("world"), resultSet.getString("id"))));
                }
            }
            return shops;
        });
    }

    @Override
    public boolean existsId(String world, String id) {
        return database.execute(() -> {
            try (PreparedStatement statement = database.connection().prepareStatement(
                    "SELECT 1 FROM shops WHERE world = ? AND id = ?")) {
                statement.setString(1, world);
                statement.setString(2, id);
                try (ResultSet resultSet = statement.executeQuery()) {
                    return resultSet.next();
                }
            }
        });
    }

    @Override
    public void insert(ShopRecord shop) {
        database.execute(() -> {
            var connection = database.connection();
            connection.setAutoCommit(false);
            try {
                try (PreparedStatement statement = connection.prepareStatement(
                        "INSERT INTO shops (id, world, name, item, price, container_type, pos_x, pos_y, pos_z, "
                                + "pos2_x, pos2_y, pos2_z, teleport_world, teleport_x, teleport_y, teleport_z, "
                                + "teleport_yaw, teleport_pitch, created_at, updated_at) "
                                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
                    bindInsert(statement, shop);
                    statement.executeUpdate();
                }
                insertOwners(connection, shop.world(), shop.id(), shop.owners());
                connection.commit();
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }
            return null;
        });
    }

    @Override
    public void updateAttributes(ShopRecord shop) {
        database.execute(() -> {
            try (PreparedStatement statement = database.connection().prepareStatement(
                    "UPDATE shops SET name = ?, item = ?, price = ?, teleport_world = ?, teleport_x = ?, "
                            + "teleport_y = ?, teleport_z = ?, teleport_yaw = ?, teleport_pitch = ?, updated_at = ? "
                            + "WHERE world = ? AND id = ?")) {
                statement.setString(1, shop.name());
                statement.setBytes(2, serializeItem(shop.item()));
                statement.setInt(3, shop.price());
                TeleportPoint teleport = shop.teleportPoint();
                if (teleport == null) {
                    statement.setNull(4, java.sql.Types.VARCHAR);
                    statement.setNull(5, java.sql.Types.REAL);
                    statement.setNull(6, java.sql.Types.REAL);
                    statement.setNull(7, java.sql.Types.REAL);
                    statement.setNull(8, java.sql.Types.REAL);
                    statement.setNull(9, java.sql.Types.REAL);
                } else {
                    statement.setString(4, teleport.world());
                    statement.setDouble(5, teleport.x());
                    statement.setDouble(6, teleport.y());
                    statement.setDouble(7, teleport.z());
                    statement.setFloat(8, teleport.yaw());
                    statement.setFloat(9, teleport.pitch());
                }
                statement.setLong(10, shop.updatedAt());
                statement.setString(11, shop.world());
                statement.setString(12, shop.id());
                return statement.executeUpdate();
            }
        });
    }

    @Override
    public void replaceOwners(String world, String id, Set<UUID> owners) {
        database.execute(() -> {
            var connection = database.connection();
            connection.setAutoCommit(false);
            try {
                try (PreparedStatement statement = connection.prepareStatement(
                        "DELETE FROM shop_owners WHERE shop_world = ? AND shop_id = ?")) {
                    statement.setString(1, world);
                    statement.setString(2, id);
                    statement.executeUpdate();
                }
                insertOwners(connection, world, id, owners);
                connection.commit();
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }
            return null;
        });
    }

    @Override
    public void delete(String world, String id) {
        database.execute(() -> {
            try (PreparedStatement statement = database.connection().prepareStatement(
                    "DELETE FROM shops WHERE world = ? AND id = ?")) {
                statement.setString(1, world);
                statement.setString(2, id);
                return statement.executeUpdate();
            }
        });
    }

    private static void insertOwners(java.sql.Connection connection, String world, String id, Set<UUID> owners) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO shop_owners (shop_world, shop_id, uuid) VALUES (?, ?, ?)")) {
            for (UUID owner : owners) {
                statement.setString(1, world);
                statement.setString(2, id);
                statement.setString(3, owner.toString());
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }

    private Set<UUID> loadOwners(String world, String id) throws SQLException {
        Set<UUID> owners = new LinkedHashSet<>();
        try (PreparedStatement statement = database.connection().prepareStatement(
                "SELECT uuid FROM shop_owners WHERE shop_world = ? AND shop_id = ?")) {
            statement.setString(1, world);
            statement.setString(2, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    owners.add(UUID.fromString(resultSet.getString("uuid")));
                }
            }
        }
        return owners;
    }

    private static void bindInsert(PreparedStatement statement, ShopRecord shop) throws SQLException {
        statement.setString(1, shop.id());
        statement.setString(2, shop.world());
        statement.setString(3, shop.name());
        statement.setBytes(4, serializeItem(shop.item()));
        statement.setInt(5, shop.price());
        statement.setString(6, shop.containerType().name());
        statement.setInt(7, shop.position().x());
        statement.setInt(8, shop.position().y());
        statement.setInt(9, shop.position().z());
        BlockPos secondary = shop.secondaryPosition();
        if (secondary == null) {
            statement.setNull(10, java.sql.Types.INTEGER);
            statement.setNull(11, java.sql.Types.INTEGER);
            statement.setNull(12, java.sql.Types.INTEGER);
        } else {
            statement.setInt(10, secondary.x());
            statement.setInt(11, secondary.y());
            statement.setInt(12, secondary.z());
        }
        TeleportPoint teleport = shop.teleportPoint();
        if (teleport == null) {
            statement.setNull(13, java.sql.Types.VARCHAR);
            statement.setNull(14, java.sql.Types.REAL);
            statement.setNull(15, java.sql.Types.REAL);
            statement.setNull(16, java.sql.Types.REAL);
            statement.setNull(17, java.sql.Types.REAL);
            statement.setNull(18, java.sql.Types.REAL);
        } else {
            statement.setString(13, teleport.world());
            statement.setDouble(14, teleport.x());
            statement.setDouble(15, teleport.y());
            statement.setDouble(16, teleport.z());
            statement.setFloat(17, teleport.yaw());
            statement.setFloat(18, teleport.pitch());
        }
        statement.setLong(19, shop.createdAt());
        statement.setLong(20, shop.updatedAt());
    }

    private static ShopRecord toRecord(ResultSet resultSet, Set<UUID> owners) throws SQLException {
        Integer pos2X = getNullableInt(resultSet, "pos2_x");
        BlockPos secondary = pos2X == null ? null : new BlockPos(pos2X, resultSet.getInt("pos2_y"), resultSet.getInt("pos2_z"));

        String teleportWorld = resultSet.getString("teleport_world");
        TeleportPoint teleport = teleportWorld == null ? null : new TeleportPoint(
                teleportWorld,
                resultSet.getDouble("teleport_x"),
                resultSet.getDouble("teleport_y"),
                resultSet.getDouble("teleport_z"),
                resultSet.getFloat("teleport_yaw"),
                resultSet.getFloat("teleport_pitch")
        );

        return new ShopRecord(
                resultSet.getString("id"),
                resultSet.getString("world"),
                resultSet.getString("name"),
                deserializeItem(resultSet.getBytes("item")),
                resultSet.getInt("price"),
                Material.valueOf(resultSet.getString("container_type")),
                new BlockPos(resultSet.getInt("pos_x"), resultSet.getInt("pos_y"), resultSet.getInt("pos_z")),
                secondary,
                teleport,
                owners,
                resultSet.getLong("created_at"),
                resultSet.getLong("updated_at")
        );
    }

    /**
     * {@link ItemStack#serializeAsBytes()} rejects an empty stack (a shop's
     * {@link ShopRecord#SELL_ALL_SENTINEL}, {@code Material.AIR}), so it's stored as a
     * zero-length marker instead; {@link #deserializeItem} recognizes that marker on the way back.
     */
    private static byte[] serializeItem(ItemStack item) {
        return item.isEmpty() ? new byte[0] : item.serializeAsBytes();
    }

    private static ItemStack deserializeItem(byte[] bytes) {
        return bytes.length == 0 ? ShopRecord.SELL_ALL_SENTINEL.clone() : ItemStack.deserializeBytes(bytes);
    }

    private static Integer getNullableInt(ResultSet resultSet, String column) throws SQLException {
        int value = resultSet.getInt(column);
        return resultSet.wasNull() ? null : value;
    }
}
