package com.example.config;

public final class AppProperties {
    private final String appVersion;
    private final String defaultTheme;
    private final String defaultScheduleCardStyle;
    private final String dataDirectoryOverride;

    public AppProperties(
        String appVersion,
        String defaultTheme,
        String defaultScheduleCardStyle,
        String dataDirectoryOverride
    ) {
        this.appVersion = appVersion;
        this.defaultTheme = defaultTheme;
        this.defaultScheduleCardStyle = defaultScheduleCardStyle;
        this.dataDirectoryOverride = dataDirectoryOverride;
    }

    public String getAppVersion() {
        return appVersion;
    }

    public String getDefaultTheme() {
        return defaultTheme;
    }

    public String getDefaultScheduleCardStyle() {
        return defaultScheduleCardStyle;
    }

    public String getDataDirectoryOverride() {
        return dataDirectoryOverride;
    }
}
