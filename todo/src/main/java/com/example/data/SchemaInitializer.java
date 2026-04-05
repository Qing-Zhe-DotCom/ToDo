package com.example.data;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public final class SchemaInitializer {
    private volatile boolean schemaInitialized;

    public void ensureSchema(Connection connection) throws SQLException {
        if (schemaInitialized) {
            return;
        }

        synchronized (this) {
            if (schemaInitialized) {
                return;
            }

            try (Statement statement = connection.createStatement()) {
                statement.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS schedules (" +
                        "id INT AUTO_INCREMENT PRIMARY KEY, " +
                        "name VARCHAR(255) NOT NULL, " +
                        "description TEXT, " +
                        "start_date DATE, " +
                        "due_date DATE, " +
                        "start_at DATETIME, " +
                        "due_at DATETIME, " +
                        "completed BOOLEAN DEFAULT FALSE, " +
                        "priority VARCHAR(10) DEFAULT '\u4e2d', " +
                        "category VARCHAR(50) DEFAULT '\u672a\u5206\u7c7b', " +
                        "tags VARCHAR(255), " +
                        "reminder_time DATETIME, " +
                        "color VARCHAR(20) DEFAULT '#2196F3', " +
                        "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                        "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP" +
                        ")"
                );
            }

            ensureColumn(connection, "start_date", "ALTER TABLE schedules ADD COLUMN start_date DATE AFTER description");
            ensureColumn(connection, "due_date", "ALTER TABLE schedules ADD COLUMN due_date DATE AFTER start_date");
            ensureColumn(connection, "start_at", "ALTER TABLE schedules ADD COLUMN start_at DATETIME AFTER due_date");
            ensureColumn(connection, "due_at", "ALTER TABLE schedules ADD COLUMN due_at DATETIME AFTER start_at");
            ensureColumn(connection, "priority", "ALTER TABLE schedules ADD COLUMN priority VARCHAR(10) DEFAULT '\u4e2d' AFTER completed");
            ensureColumn(connection, "category", "ALTER TABLE schedules ADD COLUMN category VARCHAR(50) DEFAULT '\u672a\u5206\u7c7b' AFTER priority");
            ensureColumn(connection, "tags", "ALTER TABLE schedules ADD COLUMN tags VARCHAR(255) AFTER category");
            ensureColumn(connection, "reminder_time", "ALTER TABLE schedules ADD COLUMN reminder_time DATETIME AFTER tags");
            ensureColumn(connection, "color", "ALTER TABLE schedules ADD COLUMN color VARCHAR(20) DEFAULT '#2196F3' AFTER reminder_time");

            try (Statement statement = connection.createStatement()) {
                statement.executeUpdate(
                    "UPDATE schedules SET " +
                        "start_at = COALESCE(start_at, CASE WHEN start_date IS NOT NULL THEN TIMESTAMP(start_date, '00:00:00') END), " +
                        "due_at = COALESCE(due_at, CASE WHEN due_date IS NOT NULL THEN TIMESTAMP(due_date, '23:59:00') END)"
                );
            }

            schemaInitialized = true;
        }
    }

    private void ensureColumn(Connection connection, String columnName, String ddl) throws SQLException {
        DatabaseMetaData metaData = connection.getMetaData();
        try (ResultSet columns = metaData.getColumns(connection.getCatalog(), null, "schedules", columnName)) {
            if (!columns.next()) {
                try (Statement statement = connection.createStatement()) {
                    statement.executeUpdate(ddl);
                }
            }
        }
    }
}
