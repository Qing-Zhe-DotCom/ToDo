package com.example.data;

import java.io.IOException;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

import com.example.config.AppDataPaths;
import com.example.config.DatabaseProperties;

public final class SqliteConnectionFactory implements ConnectionFactory {
    private final DatabaseProperties properties;
    private final AppDataPaths appDataPaths;
    private volatile boolean driverLoaded;

    public SqliteConnectionFactory(DatabaseProperties properties, AppDataPaths appDataPaths) {
        this.properties = properties;
        this.appDataPaths = appDataPaths;
    }

    @Override
    public Connection getConnection() throws SQLException {
        ensureDriverLoaded();
        ensureStorageDirectories();

        String jdbcUrl = resolveJdbcUrl();
        try {
            Connection connection = DriverManager.getConnection(jdbcUrl);
            configureConnection(connection);
            return connection;
        } catch (SQLException exception) {
            throw new SQLException(
                "无法打开本地 SQLite 数据库：" + describeDatabaseLocation(),
                exception
            );
        }
    }

    private void ensureDriverLoaded() throws SQLException {
        if (driverLoaded) {
            return;
        }

        synchronized (this) {
            if (driverLoaded) {
                return;
            }
            try {
                Class.forName(properties.getDriverClassName());
                driverLoaded = true;
            } catch (ClassNotFoundException exception) {
                throw new SQLException("SQLite JDBC 驱动不可用，无法初始化本地数据库。", exception);
            }
        }
    }

    private void ensureStorageDirectories() throws SQLException {
        try {
            appDataPaths.ensureBaseDirectories();
        } catch (IOException exception) {
            throw new SQLException(
                "无法创建本地数据目录，请检查路径是否可写：" + appDataPaths.getDataDirectory(),
                exception
            );
        }
    }

    private String resolveJdbcUrl() {
        String configuredUrl = properties.getUrl();
        if (configuredUrl != null && !configuredUrl.isBlank()) {
            return configuredUrl;
        }
        return "jdbc:sqlite:" + appDataPaths.getSqliteDatabasePath();
    }

    private String describeDatabaseLocation() {
        String configuredUrl = properties.getUrl();
        if (configuredUrl != null && !configuredUrl.isBlank()) {
            return configuredUrl;
        }
        Path databasePath = appDataPaths.getSqliteDatabasePath();
        return databasePath.toString();
    }

    private void configureConnection(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys = ON");
            statement.execute("PRAGMA journal_mode = WAL");
            statement.execute("PRAGMA synchronous = NORMAL");
        }
    }
}
