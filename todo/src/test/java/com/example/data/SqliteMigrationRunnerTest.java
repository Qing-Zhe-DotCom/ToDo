package com.example.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
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
    void initializesSchemaAndTracksAppliedVersions() throws SQLException {
        AppDataPaths appDataPaths = new AppDataPaths(tempDir.resolve("data").toString(), "db/test.sqlite");
        SqliteConnectionFactory connectionFactory = new SqliteConnectionFactory(sqliteProperties(), appDataPaths);
        SqliteMigrationRunner migrationRunner = new SqliteMigrationRunner();

        try (Connection connection = connectionFactory.getConnection()) {
            migrationRunner.ensureSchema(connection);
        }
        try (Connection connection = connectionFactory.getConnection()) {
            new SqliteMigrationRunner().ensureSchema(connection);

            assertEquals(2, countSchemaVersions(connection));
            assertTrue(loadColumns(connection).contains("deleted_at"));
            assertTrue(loadColumns(connection).contains("sync_status"));
            assertTrue(loadColumns(connection).contains("metadata_json"));
        }
    }

    private DatabaseProperties sqliteProperties() {
        return new DatabaseProperties("sqlite", "org.sqlite.JDBC", "", "", "", null);
    }

    private int countSchemaVersions(Connection connection) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
            "SELECT COUNT(*) FROM schema_version"
        ); ResultSet resultSet = statement.executeQuery()) {
            resultSet.next();
            return resultSet.getInt(1);
        }
    }

    private Set<String> loadColumns(Connection connection) throws SQLException {
        Set<String> columns = new HashSet<>();
        try (PreparedStatement statement = connection.prepareStatement(
            "PRAGMA table_info(schedules)"
        ); ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                columns.add(resultSet.getString("name"));
            }
        }
        return columns;
    }
}
