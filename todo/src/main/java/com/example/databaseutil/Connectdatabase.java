package com.example.databaseutil;

import java.sql.Connection;
import java.sql.SQLException;

import com.example.config.ConfigurationLoader;
import com.example.config.DatabaseProperties;
import com.example.data.ConnectionFactory;
import com.example.data.JdbcConnectionFactory;
import com.example.data.SchemaInitializer;

public class Connectdatabase {
    private static final DatabaseProperties DATABASE_PROPERTIES = ConfigurationLoader.loadDatabaseProperties();
    private static final ConnectionFactory CONNECTION_FACTORY = new JdbcConnectionFactory(DATABASE_PROPERTIES);
    private static final SchemaInitializer SCHEMA_INITIALIZER = new SchemaInitializer();

    public static Connection getConnection() throws SQLException {
        Connection connection = CONNECTION_FACTORY.getConnection();
        SCHEMA_INITIALIZER.ensureSchema(connection);
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
}
