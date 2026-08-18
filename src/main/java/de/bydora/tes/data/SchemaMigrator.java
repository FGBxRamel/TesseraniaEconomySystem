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
            """
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
