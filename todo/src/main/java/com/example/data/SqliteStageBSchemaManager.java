package com.example.data;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

public final class SqliteStageBSchemaManager implements SchemaManager {
    private static final List<String> SCHEMA_STATEMENTS = List.of(
        """
        CREATE TABLE IF NOT EXISTS schedule_item (
            id TEXT PRIMARY KEY,
            title TEXT NOT NULL,
            description TEXT,
            notes TEXT,
            status TEXT NOT NULL,
            priority TEXT NOT NULL,
            category TEXT NOT NULL,
            start_at_utc TEXT,
            end_at_utc TEXT,
            due_at_utc TEXT,
            completed_at_utc TEXT,
            is_all_day INTEGER NOT NULL DEFAULT 0,
            time_precision TEXT NOT NULL DEFAULT 'minute',
            timezone TEXT NOT NULL,
            color TEXT,
            created_at_utc TEXT NOT NULL,
            updated_at_utc TEXT NOT NULL,
            deleted_at_utc TEXT,
            version INTEGER NOT NULL DEFAULT 1,
            sync_status TEXT NOT NULL DEFAULT 'local_only',
            last_synced_at_utc TEXT,
            device_id TEXT,
            metadata_json TEXT,
            is_suspended INTEGER NOT NULL DEFAULT 0
        )
        """,
        """
        CREATE INDEX IF NOT EXISTS idx_schedule_item_active
        ON schedule_item(deleted_at_utc, due_at_utc, start_at_utc, updated_at_utc, id)
        """,
        """
        CREATE TABLE IF NOT EXISTS tag (
            id TEXT PRIMARY KEY,
            name TEXT NOT NULL UNIQUE,
            color TEXT,
            created_at_utc TEXT NOT NULL,
            updated_at_utc TEXT NOT NULL
        )
        """,
        """
        CREATE TABLE IF NOT EXISTS schedule_item_tag (
            schedule_item_id TEXT NOT NULL,
            tag_id TEXT NOT NULL,
            PRIMARY KEY (schedule_item_id, tag_id),
            FOREIGN KEY (schedule_item_id) REFERENCES schedule_item(id) ON DELETE CASCADE,
            FOREIGN KEY (tag_id) REFERENCES tag(id) ON DELETE CASCADE
        )
        """,
        """
        CREATE TABLE IF NOT EXISTS reminder (
            id TEXT PRIMARY KEY,
            schedule_item_id TEXT NOT NULL,
            remind_at_utc TEXT,
            offset_minutes INTEGER,
            channel TEXT NOT NULL,
            status TEXT NOT NULL,
            created_at_utc TEXT NOT NULL,
            updated_at_utc TEXT NOT NULL,
            FOREIGN KEY (schedule_item_id) REFERENCES schedule_item(id) ON DELETE CASCADE
        )
        """,
        """
        CREATE INDEX IF NOT EXISTS idx_reminder_schedule_item
        ON reminder(schedule_item_id, remind_at_utc)
        """,
        """
        CREATE TABLE IF NOT EXISTS recurrence_rule (
            id TEXT PRIMARY KEY,
            schedule_item_id TEXT NOT NULL UNIQUE,
            freq TEXT NOT NULL,
            interval INTEGER NOT NULL,
            by_day_csv TEXT,
            by_month_day INTEGER,
            until_at_utc TEXT,
            occurrence_count INTEGER,
            timezone TEXT,
            is_active INTEGER NOT NULL DEFAULT 1,
            created_at_utc TEXT NOT NULL,
            updated_at_utc TEXT NOT NULL,
            FOREIGN KEY (schedule_item_id) REFERENCES schedule_item(id) ON DELETE CASCADE
        )
        """,
        """
        CREATE TABLE IF NOT EXISTS sync_outbox (
            id TEXT PRIMARY KEY,
            entity_type TEXT NOT NULL,
            entity_id TEXT NOT NULL,
            operation TEXT NOT NULL,
            payload_json TEXT,
            local_version INTEGER NOT NULL,
            device_id TEXT,
            created_at_utc TEXT NOT NULL,
            retry_count INTEGER NOT NULL DEFAULT 0,
            status TEXT NOT NULL DEFAULT 'pending'
        )
        """,
        """
        CREATE INDEX IF NOT EXISTS idx_sync_outbox_status
        ON sync_outbox(status, created_at_utc)
        """,
        """
        CREATE TABLE IF NOT EXISTS sync_checkpoint (
            scope TEXT PRIMARY KEY,
            cursor TEXT,
            updated_at_utc TEXT NOT NULL
        )
        """,
        """
        CREATE TABLE IF NOT EXISTS device_registry (
            device_id TEXT PRIMARY KEY,
            device_name TEXT NOT NULL,
            platform TEXT,
            app_version TEXT,
            created_at_utc TEXT NOT NULL,
            last_seen_at_utc TEXT NOT NULL
        )
        """
    );

    private volatile boolean schemaInitialized;

    @Override
    public void ensureSchema(Connection connection) throws SQLException {
        if (schemaInitialized) {
            return;
        }

        synchronized (this) {
            if (schemaInitialized) {
                return;
            }

            detectLegacySchema(connection);
            initializeSchema(connection);
            runMigrations(connection);
            schemaInitialized = true;
        }
    }

    private void detectLegacySchema(Connection connection) throws SQLException {
        if (tableExists(connection, "schedules") || tableExists(connection, "schema_version")) {
            throw new LegacySchemaException(
                "检测到旧版 SQLite 数据库（schedules / V001-V003 schema_version）。" +
                    "阶段 B 不支持旧库自动升级，请使用空库或手动迁移到新 schema。"
            );
        }
    }

    private boolean tableExists(Connection connection, String tableName) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
            "SELECT 1 FROM sqlite_master WHERE type = 'table' AND name = ? LIMIT 1"
        )) {
            statement.setString(1, tableName);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    private void initializeSchema(Connection connection) throws SQLException {
        boolean originalAutoCommit = connection.getAutoCommit();
        connection.setAutoCommit(false);
        try {
            for (String statementText : SCHEMA_STATEMENTS) {
                try (Statement statement = connection.createStatement()) {
                    statement.execute(statementText);
                }
            }
            connection.commit();
        } catch (SQLException exception) {
            connection.rollback();
            throw exception;
        } finally {
            connection.setAutoCommit(originalAutoCommit);
        }
    }

    private void runMigrations(Connection connection) throws SQLException {
        if (!columnExists(connection, "schedule_item", "is_suspended")) {
            try (Statement statement = connection.createStatement()) {
                statement.execute(
                    "ALTER TABLE schedule_item ADD COLUMN is_suspended INTEGER NOT NULL DEFAULT 0"
                );
            }
        }
    }

    private boolean columnExists(Connection connection, String tableName, String columnName)
        throws SQLException {
        try (ResultSet resultSet = connection.createStatement()
            .executeQuery("PRAGMA table_info(" + tableName + ")")) {
            while (resultSet.next()) {
                if (columnName.equalsIgnoreCase(resultSet.getString("name"))) {
                    return true;
                }
            }
        }
        return false;
    }
}
