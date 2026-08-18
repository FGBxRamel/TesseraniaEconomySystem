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
