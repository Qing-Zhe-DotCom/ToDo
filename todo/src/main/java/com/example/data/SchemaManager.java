package com.example.data;

import java.sql.Connection;
import java.sql.SQLException;

public interface SchemaManager {
    void ensureSchema(Connection connection) throws SQLException;
}
