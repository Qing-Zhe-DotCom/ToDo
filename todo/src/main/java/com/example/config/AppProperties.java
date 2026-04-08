package com.example.config;

public final class AppProperties {
    private final String appVersion;
    private final String defaultThemeFamily;
    private final String defaultThemeAppearance;
    private final String defaultClassicPalette;
    private final String defaultLanguage;
    private final String dataDirectoryOverride;

    public AppProperties(
        String appVersion,
        String defaultThemeFamily,
        String defaultThemeAppearance,
        String defaultClassicPalette,
        String defaultLanguage,
        String dataDirectoryOverride
    ) {
        this.appVersion = appVersion;
        this.defaultThemeFamily = defaultThemeFamily;
        this.defaultThemeAppearance = defaultThemeAppearance;
        this.defaultClassicPalette = defaultClassicPalette;
        this.defaultLanguage = defaultLanguage;
        this.dataDirectoryOverride = dataDirectoryOverride;
    }

    public String getAppVersion() {
        return appVersion;
    }

    public String getDefaultThemeFamily() {
        return defaultThemeFamily;
    }

    public String getDefaultThemeAppearance() {
        return defaultThemeAppearance;
    }

    public String getDefaultClassicPalette() {
        return defaultClassicPalette;
    }

    public String getDefaultLanguage() {
        return defaultLanguage;
    }

    public String getDataDirectoryOverride() {
        return dataDirectoryOverride;
    }
}
