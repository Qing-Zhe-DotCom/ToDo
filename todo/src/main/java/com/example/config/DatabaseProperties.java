package com.example.config;

public final class DatabaseProperties {
    private final String mode;
    private final String driverClassName;
    private final String url;
    private final String username;
    private final String password;
    private final String sqlitePath;

    public DatabaseProperties(
        String mode,
        String driverClassName,
        String url,
        String username,
        String password,
        String sqlitePath
    ) {
        this.mode = mode == null || mode.isBlank() ? "sqlite" : mode.trim();
        this.driverClassName = driverClassName;
        this.url = url;
        this.username = username;
        this.password = password;
        this.sqlitePath = sqlitePath;
    }

    public String getDriverClassName() {
        return driverClassName;
    }

    public String getUrl() {
        return url;
    }

    public String getSqlitePath() {
        return sqlitePath;
    }
}
