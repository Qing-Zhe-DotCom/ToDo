package com.example.config;

public final class AppProperties {
    private final String defaultTheme;
    private final String defaultScheduleCardStyle;

    public AppProperties(String defaultTheme, String defaultScheduleCardStyle) {
        this.defaultTheme = defaultTheme;
        this.defaultScheduleCardStyle = defaultScheduleCardStyle;
    }

    public String getDefaultTheme() {
        return defaultTheme;
    }

    public String getDefaultScheduleCardStyle() {
        return defaultScheduleCardStyle;
    }
}
