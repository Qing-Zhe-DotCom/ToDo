package com.example.databaseutil;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class Connectdatabase {
    private static final String URL = "jdbc:mysql://localhost:3306/todo_db";
    private static final String USER = "root";
    private static final String PASSWORD = "@Xj213192";
    static {
        try {
            // 使用新的MySQL驱动类名
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("MySQL JDBC Driver not found. Please check if MySQL Connector/J is properly installed.", e);
        }
    }   

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
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
