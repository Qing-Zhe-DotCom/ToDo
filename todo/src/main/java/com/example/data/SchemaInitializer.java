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
                        "completed BOOLEAN DEFAULT FALSE, " +
                        "priority VARCHAR(10) DEFAULT '中', " +
                        "category VARCHAR(50) DEFAULT '默认', " +
                        "tags VARCHAR(255), " +
                        "reminder_time DATETIME, " +
                        "color VARCHAR(20) DEFAULT '#2196F3', " +
                        "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                        "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP" +
                        ")"
                );
            }

            ensureColumn(connection, "start_date", "ALTER TABLE schedules ADD COLUMN start_date DATE AFTER description");
            ensureColumn(connection, "priority", "ALTER TABLE schedules ADD COLUMN priority VARCHAR(10) DEFAULT '中' AFTER completed");
            ensureColumn(connection, "category", "ALTER TABLE schedules ADD COLUMN category VARCHAR(50) DEFAULT '默认' AFTER priority");
            ensureColumn(connection, "tags", "ALTER TABLE schedules ADD COLUMN tags VARCHAR(255) AFTER category");
            ensureColumn(connection, "reminder_time", "ALTER TABLE schedules ADD COLUMN reminder_time DATETIME AFTER tags");
            ensureColumn(connection, "color", "ALTER TABLE schedules ADD COLUMN color VARCHAR(20) DEFAULT '#2196F3' AFTER reminder_time");

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
