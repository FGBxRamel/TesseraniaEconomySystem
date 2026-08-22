package de.bydora.tes.data;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

/**
 * Applies schema migrations tracked via SQLite's {@code PRAGMA user_version}. Each entry in
 * {@link #MIGRATIONS} is the DDL for one migration step; later stages append new entries here
 * rather than editing existing ones, so a fresh database and an upgraded one converge on the
 * same schema.
 */
final class SchemaMigrator {

    private static final List<String> MIGRATIONS = List.of(
            """
            CREATE TABLE IF NOT EXISTS players (
                uuid              TEXT PRIMARY KEY,
                username          TEXT NOT NULL,
                treuepunkte       INTEGER NOT NULL DEFAULT 0,
                erfahrungspunkte  INTEGER NOT NULL DEFAULT 0,
                level             INTEGER NOT NULL DEFAULT 0,
                paused            INTEGER NOT NULL DEFAULT 0,
                registered_at     INTEGER NOT NULL,
                updated_at        INTEGER NOT NULL
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS shops (
                id              TEXT NOT NULL,
                world           TEXT NOT NULL,
                name            TEXT NOT NULL,
                item            TEXT NOT NULL,
                price           INTEGER NOT NULL,
                container_type  TEXT NOT NULL,
                pos_x           INTEGER NOT NULL,
                pos_y           INTEGER NOT NULL,
                pos_z           INTEGER NOT NULL,
                pos2_x          INTEGER,
                pos2_y          INTEGER,
                pos2_z          INTEGER,
                teleport_world  TEXT,
                teleport_x      REAL,
                teleport_y      REAL,
                teleport_z      REAL,
                teleport_yaw    REAL,
                teleport_pitch  REAL,
                created_at      INTEGER NOT NULL,
                updated_at      INTEGER NOT NULL,
                closed_at       INTEGER,
                PRIMARY KEY (world, id)
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS shop_owners (
                shop_world TEXT NOT NULL,
                shop_id    TEXT NOT NULL,
                uuid       TEXT NOT NULL,
                PRIMARY KEY (shop_world, shop_id, uuid),
                FOREIGN KEY (shop_world, shop_id) REFERENCES shops(world, id)
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS shop_transactions (
                id           INTEGER PRIMARY KEY AUTOINCREMENT,
                shop_world   TEXT NOT NULL,
                shop_id      TEXT NOT NULL,
                slot         INTEGER NOT NULL,
                buyer_uuid   TEXT NOT NULL,
                item         TEXT NOT NULL,
                amount       INTEGER NOT NULL,
                price        INTEGER NOT NULL,
                state        TEXT NOT NULL,
                purchased_at INTEGER NOT NULL,
                resolved_at  INTEGER,
                FOREIGN KEY (shop_world, shop_id) REFERENCES shops(world, id)
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS pending_notifications (
                id         INTEGER PRIMARY KEY AUTOINCREMENT,
                uuid       TEXT NOT NULL,
                message    TEXT NOT NULL,
                created_at INTEGER NOT NULL
            )
            """,
            "CREATE INDEX IF NOT EXISTS idx_shop_transactions_pending ON shop_transactions(state, purchased_at)",
            // Closing a shop now hard-deletes its row (so the ID becomes reusable) instead of
            // soft-deleting via closed_at, so the column is no longer needed.
            "ALTER TABLE shops DROP COLUMN closed_at",
            // Rebuild shop_owners/shop_transactions with ON DELETE CASCADE so deleting a shop
            // (on close or as an orphan) cleans up its owner and transaction rows automatically,
            // instead of failing the FK check or requiring manual multi-statement deletes.
            "ALTER TABLE shop_owners RENAME TO shop_owners_old",
            """
            CREATE TABLE shop_owners (
                shop_world TEXT NOT NULL,
                shop_id    TEXT NOT NULL,
                uuid       TEXT NOT NULL,
                PRIMARY KEY (shop_world, shop_id, uuid),
                FOREIGN KEY (shop_world, shop_id) REFERENCES shops(world, id) ON DELETE CASCADE
            )
            """,
            "INSERT INTO shop_owners SELECT * FROM shop_owners_old",
            "DROP TABLE shop_owners_old",
            "ALTER TABLE shop_transactions RENAME TO shop_transactions_old",
            """
            CREATE TABLE shop_transactions (
                id           INTEGER PRIMARY KEY AUTOINCREMENT,
                shop_world   TEXT NOT NULL,
                shop_id      TEXT NOT NULL,
                slot         INTEGER NOT NULL,
                buyer_uuid   TEXT NOT NULL,
                item         TEXT NOT NULL,
                amount       INTEGER NOT NULL,
                price        INTEGER NOT NULL,
                state        TEXT NOT NULL,
                purchased_at INTEGER NOT NULL,
                resolved_at  INTEGER,
                FOREIGN KEY (shop_world, shop_id) REFERENCES shops(world, id) ON DELETE CASCADE
            )
            """,
            "INSERT INTO shop_transactions SELECT * FROM shop_transactions_old",
            "DROP TABLE shop_transactions_old",
            "CREATE INDEX IF NOT EXISTS idx_shop_transactions_pending ON shop_transactions(state, purchased_at)",
            // Item shops previously stored only the sold item's Material name, silently dropping
            // enchantments/potion data/display names/etc. on purchase, restock and refund. The
            // item column now holds a full serialized ItemStack (ItemStack#serializeAsBytes)
            // instead, and shop_transactions no longer needs a separate amount column since the
            // stored ItemStack already carries its own amount. Existing rows predate any release
            // and only ever held a bare Material with no metadata to recover, so they're cleared
            // rather than migrated.
            // Dropped and recreated rather than the usual rename-copy-drop dance: SQLite
            // auto-rewrites *other* tables' FK clauses to follow a renamed parent table (so
            // renaming shops -> shops_old leaves shop_owners/shop_transactions referencing
            // "shops_old"), and since all three tables are already emptied above there's no data
            // to preserve across the rebuild anyway — drop children before parent, recreate parent
            // before children so the new FK targets resolve correctly from the start.
            "DELETE FROM shop_transactions",
            "DELETE FROM shop_owners",
            "DELETE FROM shops",
            "DROP TABLE shop_transactions",
            "DROP TABLE shop_owners",
            "DROP TABLE shops",
            """
            CREATE TABLE shops (
                id              TEXT NOT NULL,
                world           TEXT NOT NULL,
                name            TEXT NOT NULL,
                item            BLOB NOT NULL,
                price           INTEGER NOT NULL,
                container_type  TEXT NOT NULL,
                pos_x           INTEGER NOT NULL,
                pos_y           INTEGER NOT NULL,
                pos_z           INTEGER NOT NULL,
                pos2_x          INTEGER,
                pos2_y          INTEGER,
                pos2_z          INTEGER,
                teleport_world  TEXT,
                teleport_x      REAL,
                teleport_y      REAL,
                teleport_z      REAL,
                teleport_yaw    REAL,
                teleport_pitch  REAL,
                created_at      INTEGER NOT NULL,
                updated_at      INTEGER NOT NULL,
                PRIMARY KEY (world, id)
            )
            """,
            """
            CREATE TABLE shop_owners (
                shop_world TEXT NOT NULL,
                shop_id    TEXT NOT NULL,
                uuid       TEXT NOT NULL,
                PRIMARY KEY (shop_world, shop_id, uuid),
                FOREIGN KEY (shop_world, shop_id) REFERENCES shops(world, id) ON DELETE CASCADE
            )
            """,
            """
            CREATE TABLE shop_transactions (
                id           INTEGER PRIMARY KEY AUTOINCREMENT,
                shop_world   TEXT NOT NULL,
                shop_id      TEXT NOT NULL,
                slot         INTEGER NOT NULL,
                buyer_uuid   TEXT NOT NULL,
                item         BLOB NOT NULL,
                price        INTEGER NOT NULL,
                state        TEXT NOT NULL,
                purchased_at INTEGER NOT NULL,
                resolved_at  INTEGER,
                FOREIGN KEY (shop_world, shop_id) REFERENCES shops(world, id) ON DELETE CASCADE
            )
            """,
            "CREATE INDEX IF NOT EXISTS idx_shop_transactions_pending ON shop_transactions(state, purchased_at)"
    );

    private SchemaMigrator() {
    }

    static void migrate(Connection connection) throws SQLException {
        int currentVersion = readUserVersion(connection);
        try (Statement statement = connection.createStatement()) {
            for (int i = currentVersion; i < MIGRATIONS.size(); i++) {
                statement.execute(MIGRATIONS.get(i));
            }
            statement.execute("PRAGMA user_version = " + MIGRATIONS.size());
        }
    }

    private static int readUserVersion(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            var resultSet = statement.executeQuery("PRAGMA user_version");
            return resultSet.next() ? resultSet.getInt(1) : 0;
        }
    }
}
