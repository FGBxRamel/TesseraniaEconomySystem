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
            "CREATE INDEX IF NOT EXISTS idx_shop_transactions_pending ON shop_transactions(state, purchased_at)",
            // Stage 2: the Belohnungsinventar (spec §1.3) — a generic, queue-style store of items
            // owed to a player by the loyalty-point/level/invoice systems. One row per stored
            // ItemStack rather than fixed slots, so it never needs a "how many slots" migration.
            """
            CREATE TABLE IF NOT EXISTS reward_inventory_items (
                id         INTEGER PRIMARY KEY AUTOINCREMENT,
                uuid       TEXT NOT NULL,
                item       BLOB NOT NULL,
                granted_at INTEGER NOT NULL,
                FOREIGN KEY (uuid) REFERENCES players(uuid) ON DELETE CASCADE
            )
            """,
            "CREATE INDEX IF NOT EXISTS idx_reward_inventory_items_uuid ON reward_inventory_items(uuid, granted_at)",
            // Stage 2: invoices (spec §3.1.1.3) and the virtual balance they credit their
            // creator with. invoice_balance sits on players like treuepunkte/erfahrungspunkte —
            // a single scalar 1:1 with a player, not worth a separate table. creator_uuid has an
            // FK (a creator must be registered); target_uuid deliberately does not, since
            // unregistered/removed players must remain valid, payable invoice targets.
            "ALTER TABLE players ADD COLUMN invoice_balance INTEGER NOT NULL DEFAULT 0",
            """
            CREATE TABLE IF NOT EXISTS invoices (
                id           INTEGER PRIMARY KEY AUTOINCREMENT,
                creator_uuid TEXT NOT NULL,
                target_uuid  TEXT NOT NULL,
                price        INTEGER NOT NULL,
                reason       TEXT NOT NULL,
                state        TEXT NOT NULL,
                created_at   INTEGER NOT NULL,
                settled_at   INTEGER,
                FOREIGN KEY (creator_uuid) REFERENCES players(uuid) ON DELETE CASCADE
            )
            """,
            "CREATE INDEX IF NOT EXISTS idx_invoices_open_target ON invoices(target_uuid, state, created_at)",
            // Stage 2 v1.2: invoice retraction backs a symmetric "Versendete Rechnungen" list
            // (open invoices by creator), same access pattern as the target-side index above.
            "CREATE INDEX IF NOT EXISTS idx_invoices_open_creator ON invoices(creator_uuid, state, created_at)",
            // Stage 2 v1.3: shop IDs are now globally unique rather than unique-per-world, so
            // /tes shop bearbeiten|schließen|tp no longer need a <world> argument to disambiguate.
            // The (world, id) primary key is left as-is (already covered by this index anyway,
            // and reworking it would mean rebuilding shop_owners/shop_transactions' composite
            // foreign keys for no benefit) — this index is what actually enforces the new
            // invariant.
            "CREATE UNIQUE INDEX IF NOT EXISTS idx_shops_id_unique ON shops(id)"
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
