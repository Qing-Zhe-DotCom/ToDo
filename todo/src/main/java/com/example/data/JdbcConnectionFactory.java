package com.example.data;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import com.example.config.DatabaseProperties;

public final class JdbcConnectionFactory implements ConnectionFactory {
    private final DatabaseProperties properties;

    public JdbcConnectionFactory(DatabaseProperties properties) {
        this.properties = properties;
        try {
            Class.forName(properties.getDriverClassName());
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("MySQL JDBC Driver not found.", e);
        }
    }

    @Override
    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(
            properties.getUrl(),
            properties.getUsername(),
            properties.getPassword()
        );
    }
}
