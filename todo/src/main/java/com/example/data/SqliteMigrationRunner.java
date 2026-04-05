package com.example.data;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class SqliteMigrationRunner {
    private static final List<Migration> MIGRATIONS = List.of(
        new Migration(1, "/db/sqlite/V001__create_schedules.sql"),
        new Migration(2, "/db/sqlite/V002__add_local_sync_columns.sql")
    );

    private volatile boolean schemaInitialized;

    public void ensureSchema(Connection connection) throws SQLException {
        if (schemaInitialized) {
            return;
        }

        synchronized (this) {
            if (schemaInitialized) {
                return;
            }

            ensureSchemaVersionTable(connection);
            Set<Integer> appliedVersions = loadAppliedVersions(connection);
            for (Migration migration : MIGRATIONS) {
                if (appliedVersions.contains(migration.version())) {
                    continue;
                }
                applyMigration(connection, migration);
            }
            schemaInitialized = true;
        }
    }

    private void ensureSchemaVersionTable(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute(
                "CREATE TABLE IF NOT EXISTS schema_version (" +
                    "version INTEGER PRIMARY KEY, " +
                    "script_name TEXT NOT NULL, " +
                    "applied_at TEXT NOT NULL" +
                ")"
            );
        }
    }

    private Set<Integer> loadAppliedVersions(Connection connection) throws SQLException {
        Set<Integer> versions = new HashSet<>();
        try (PreparedStatement statement = connection.prepareStatement(
            "SELECT version FROM schema_version"
        ); ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                versions.add(resultSet.getInt("version"));
            }
        }
        return versions;
    }

    private void applyMigration(Connection connection, Migration migration) throws SQLException {
        boolean originalAutoCommit = connection.getAutoCommit();
        connection.setAutoCommit(false);
        try {
            for (String statementText : loadStatements(migration.resourcePath())) {
                try (Statement statement = connection.createStatement()) {
                    statement.execute(statementText);
                }
            }

            try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO schema_version(version, script_name, applied_at) VALUES (?, ?, ?)"
            )) {
                statement.setInt(1, migration.version());
                statement.setString(2, migration.resourcePath());
                statement.setString(3, LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS).toString());
                statement.executeUpdate();
            }

            connection.commit();
        } catch (SQLException exception) {
            connection.rollback();
            throw new SQLException("SQLite schema 初始化失败：" + migration.resourcePath(), exception);
        } finally {
            connection.setAutoCommit(originalAutoCommit);
        }
    }

    private List<String> loadStatements(String resourcePath) throws SQLException {
        try (InputStream inputStream = SqliteMigrationRunner.class.getResourceAsStream(resourcePath)) {
            if (inputStream == null) {
                throw new SQLException("找不到 SQLite migration 资源：" + resourcePath);
            }

            String script = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            return splitStatements(script);
        } catch (IOException exception) {
            throw new SQLException("读取 SQLite migration 资源失败：" + resourcePath, exception);
        }
    }

    private List<String> splitStatements(String script) {
        StringBuilder filteredScript = new StringBuilder();
        for (String line : script.split("\\R")) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("--")) {
                continue;
            }
            filteredScript.append(line).append('\n');
        }

        List<String> statements = new ArrayList<>();
        for (String statement : filteredScript.toString().split(";")) {
            String trimmed = statement.trim();
            if (!trimmed.isEmpty()) {
                statements.add(trimmed);
            }
        }
        return statements;
    }

    private record Migration(int version, String resourcePath) {
    }
}
