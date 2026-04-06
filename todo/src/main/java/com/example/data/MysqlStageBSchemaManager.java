package com.example.data;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

public final class MysqlStageBSchemaManager implements SchemaManager {
    private static final List<String> SCHEMA_STATEMENTS = List.of(
        """
        CREATE TABLE IF NOT EXISTS schedule_item (
            id VARCHAR(64) PRIMARY KEY,
            title VARCHAR(255) NOT NULL,
            description TEXT,
            notes TEXT,
            status VARCHAR(32) NOT NULL,
            priority VARCHAR(32) NOT NULL,
            category VARCHAR(128) NOT NULL,
            start_at_utc VARCHAR(32),
            end_at_utc VARCHAR(32),
            due_at_utc VARCHAR(32),
            completed_at_utc VARCHAR(32),
            is_all_day TINYINT NOT NULL DEFAULT 0,
            time_precision VARCHAR(16) NOT NULL DEFAULT 'minute',
            timezone VARCHAR(64) NOT NULL,
            color VARCHAR(32),
            created_at_utc VARCHAR(32) NOT NULL,
            updated_at_utc VARCHAR(32) NOT NULL,
            deleted_at_utc VARCHAR(32),
            version INT NOT NULL DEFAULT 1,
            sync_status VARCHAR(32) NOT NULL DEFAULT 'local_only',
            last_synced_at_utc VARCHAR(32),
            device_id VARCHAR(128),
            metadata_json TEXT
        )
        """,
        """
        CREATE INDEX idx_schedule_item_active
        ON schedule_item (deleted_at_utc, due_at_utc, start_at_utc, updated_at_utc, id)
        """,
        """
        CREATE TABLE IF NOT EXISTS tag (
            id VARCHAR(64) PRIMARY KEY,
            name VARCHAR(191) NOT NULL UNIQUE,
            color VARCHAR(32),
            created_at_utc VARCHAR(32) NOT NULL,
            updated_at_utc VARCHAR(32) NOT NULL
        )
        """,
        """
        CREATE TABLE IF NOT EXISTS schedule_item_tag (
            schedule_item_id VARCHAR(64) NOT NULL,
            tag_id VARCHAR(64) NOT NULL,
            PRIMARY KEY (schedule_item_id, tag_id),
            CONSTRAINT fk_schedule_item_tag_item
                FOREIGN KEY (schedule_item_id) REFERENCES schedule_item(id) ON DELETE CASCADE,
            CONSTRAINT fk_schedule_item_tag_tag
                FOREIGN KEY (tag_id) REFERENCES tag(id) ON DELETE CASCADE
        )
        """,
        """
        CREATE TABLE IF NOT EXISTS reminder (
            id VARCHAR(64) PRIMARY KEY,
            schedule_item_id VARCHAR(64) NOT NULL,
            remind_at_utc VARCHAR(32),
            offset_minutes INT,
            channel VARCHAR(32) NOT NULL,
            status VARCHAR(32) NOT NULL,
            created_at_utc VARCHAR(32) NOT NULL,
            updated_at_utc VARCHAR(32) NOT NULL,
            CONSTRAINT fk_reminder_schedule_item
                FOREIGN KEY (schedule_item_id) REFERENCES schedule_item(id) ON DELETE CASCADE
        )
        """,
        """
        CREATE INDEX idx_reminder_schedule_item
        ON reminder(schedule_item_id, remind_at_utc)
        """,
        """
        CREATE TABLE IF NOT EXISTS recurrence_rule (
            id VARCHAR(64) PRIMARY KEY,
            schedule_item_id VARCHAR(64) NOT NULL UNIQUE,
            freq VARCHAR(16) NOT NULL,
            interval INT NOT NULL,
            by_day_csv VARCHAR(128),
            by_month_day INT,
            until_at_utc VARCHAR(32),
            occurrence_count INT,
            timezone VARCHAR(64),
            is_active TINYINT NOT NULL DEFAULT 1,
            created_at_utc VARCHAR(32) NOT NULL,
            updated_at_utc VARCHAR(32) NOT NULL,
            CONSTRAINT fk_recurrence_schedule_item
                FOREIGN KEY (schedule_item_id) REFERENCES schedule_item(id) ON DELETE CASCADE
        )
        """,
        """
        CREATE TABLE IF NOT EXISTS sync_outbox (
            id VARCHAR(64) PRIMARY KEY,
            entity_type VARCHAR(64) NOT NULL,
            entity_id VARCHAR(64) NOT NULL,
            operation VARCHAR(32) NOT NULL,
            payload_json TEXT,
            local_version INT NOT NULL,
            device_id VARCHAR(128),
            created_at_utc VARCHAR(32) NOT NULL,
            retry_count INT NOT NULL DEFAULT 0,
            status VARCHAR(32) NOT NULL DEFAULT 'pending'
        )
        """,
        """
        CREATE INDEX idx_sync_outbox_status
        ON sync_outbox(status, created_at_utc)
        """,
        """
        CREATE TABLE IF NOT EXISTS sync_checkpoint (
            scope VARCHAR(128) PRIMARY KEY,
            cursor TEXT,
            updated_at_utc VARCHAR(32) NOT NULL
        )
        """,
        """
        CREATE TABLE IF NOT EXISTS device_registry (
            device_id VARCHAR(128) PRIMARY KEY,
            device_name VARCHAR(255) NOT NULL,
            platform VARCHAR(128),
            app_version VARCHAR(64),
            created_at_utc VARCHAR(32) NOT NULL,
            last_seen_at_utc VARCHAR(32) NOT NULL
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
            schemaInitialized = true;
        }
    }

    private void detectLegacySchema(Connection connection) throws SQLException {
        if (tableExists(connection, "schedules")) {
            throw new LegacySchemaException(
                "检测到旧版 MySQL schedules 单表结构。阶段 B 不支持旧库自动升级，请使用空库或手动迁移到新 schema。"
            );
        }
    }

    private boolean tableExists(Connection connection, String tableName) throws SQLException {
        String catalog = resolveCatalog(connection);
        if (catalog == null || catalog.isBlank()) {
            try (PreparedStatement statement = connection.prepareStatement(
                "SHOW TABLES LIKE ?"
            )) {
                statement.setString(1, tableName);
                try (ResultSet resultSet = statement.executeQuery()) {
                    return resultSet.next();
                }
            }
        }

        try (PreparedStatement statement = connection.prepareStatement(
            """
            SELECT 1
            FROM information_schema.tables
            WHERE table_schema = ? AND table_name = ?
            LIMIT 1
            """
        )) {
            statement.setString(1, catalog);
            statement.setString(2, tableName);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    private String resolveCatalog(Connection connection) throws SQLException {
        String catalog = connection.getCatalog();
        if (catalog != null && !catalog.isBlank()) {
            return catalog;
        }
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("SELECT DATABASE()")) {
            if (resultSet.next()) {
                return resultSet.getString(1);
            }
        }
        return null;
    }

    private void initializeSchema(Connection connection) throws SQLException {
        boolean originalAutoCommit = connection.getAutoCommit();
        connection.setAutoCommit(false);
        try {
            for (String statementText : SCHEMA_STATEMENTS) {
                try (Statement statement = connection.createStatement()) {
                    statement.execute(statementText);
                } catch (SQLException exception) {
                    if (!isDuplicateIndexError(exception)) {
                        throw exception;
                    }
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

    private boolean isDuplicateIndexError(SQLException exception) {
        String sqlState = exception.getSQLState();
        return "42000".equals(sqlState) || exception.getErrorCode() == 1061;
    }
}
