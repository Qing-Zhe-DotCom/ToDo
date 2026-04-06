package com.example.data;

import java.sql.SQLException;

public final class LegacySchemaException extends SQLException {
    public LegacySchemaException(String message) {
        super(message);
    }
}
