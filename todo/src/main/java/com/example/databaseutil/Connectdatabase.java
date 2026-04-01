package com.example.databaseutil;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class Connectdatabase {
    private static final String URL = "jdbc:mysql://localhost:3306/todo_db";
    private static final String USER = "root";
    private static final String PASSWORD = "@Xj213192";
    private static volatile boolean schemaInitialized = false;
    static {
        try {
            // 使用新的MySQL驱动类名
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("MySQL JDBC Driver not found. Please check if MySQL Connector/J is properly installed.", e);
        }
    }   

    public static Connection getConnection() throws SQLException {
        Connection connection = DriverManager.getConnection(URL, USER, PASSWORD);
        ensureSchema(connection);
        return connection;
    }

    public static void closeConnection(Connection conn) {
        if (conn != null) {
            try {
                conn.close();
            } catch (SQLException e) {
                System.err.println("Error closing connection: " + e.getMessage());
            }
        }
    }

    private static void ensureSchema(Connection connection) throws SQLException {
        if (schemaInitialized) {
            return;
        }

        synchronized (Connectdatabase.class) {
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

    private static void ensureColumn(Connection connection, String columnName, String ddl) throws SQLException {
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
