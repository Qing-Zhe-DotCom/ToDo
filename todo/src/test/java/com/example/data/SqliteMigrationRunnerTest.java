package com.example.data;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.example.config.AppDataPaths;
import com.example.config.DatabaseProperties;

class SqliteMigrationRunnerTest {

    @TempDir
    Path tempDir;

    @Test
    void initializesStageBSchemaForEmptyDatabase() throws SQLException {
        AppDataPaths appDataPaths = new AppDataPaths(tempDir.resolve("data").toString(), "db/test.sqlite");
        SqliteConnectionFactory connectionFactory = new SqliteConnectionFactory(sqliteProperties(), appDataPaths);
        SqliteStageBSchemaManager schemaManager = new SqliteStageBSchemaManager();

        try (Connection connection = connectionFactory.getConnection()) {
            schemaManager.ensureSchema(connection);
            Set<String> tables = loadTables(connection);
            assertTrue(tables.contains("schedule_item"));
            assertTrue(tables.contains("tag"));
            assertTrue(tables.contains("schedule_item_tag"));
            assertTrue(tables.contains("reminder"));
            assertTrue(tables.contains("recurrence_rule"));
            assertTrue(tables.contains("sync_outbox"));
            assertTrue(tables.contains("sync_checkpoint"));
            assertTrue(tables.contains("device_registry"));
        }
    }

    @Test
    void blocksLegacySchedulesTableInsteadOfMigrating() throws Exception {
        AppDataPaths appDataPaths = new AppDataPaths(tempDir.resolve("legacy").toString(), "db/legacy.sqlite");
        SqliteConnectionFactory connectionFactory = new SqliteConnectionFactory(sqliteProperties(), appDataPaths);

        try (Connection connection = connectionFactory.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE schedules (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL)");
        }

        try (Connection connection = connectionFactory.getConnection()) {
            assertThrows(LegacySchemaException.class, () -> new SqliteStageBSchemaManager().ensureSchema(connection));
        }
    }

    private DatabaseProperties sqliteProperties() {
        return new DatabaseProperties("sqlite", "org.sqlite.JDBC", "", "", "", null);
    }

    private Set<String> loadTables(Connection connection) throws SQLException {
        Set<String> tables = new HashSet<>();
        try (PreparedStatement statement = connection.prepareStatement(
            "SELECT name FROM sqlite_master WHERE type = 'table'"
        ); ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                tables.add(resultSet.getString("name"));
            }
        }
        return tables;
    }
}
