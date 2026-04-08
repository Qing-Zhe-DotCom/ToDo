package com.example.application;

import java.util.List;
import java.util.Locale;

public enum AppLanguage {
    SIMPLIFIED_CHINESE("zh-CN", Locale.forLanguageTag("zh-CN"), false, "settings.language.option.zhCn"),
    TRADITIONAL_CHINESE("zh-TW", Locale.forLanguageTag("zh-TW"), true, "settings.language.option.zhTw"),
    ENGLISH("en", Locale.ENGLISH, false, "settings.language.option.en");

    private final String id;
    private final Locale locale;
    private final boolean traditionalChineseFont;
    private final String labelKey;

    AppLanguage(String id, Locale locale, boolean traditionalChineseFont, String labelKey) {
        this.id = id;
        this.locale = locale;
        this.traditionalChineseFont = traditionalChineseFont;
        this.labelKey = labelKey;
    }

    public String getId() {
        return id;
    }

    public Locale getLocale() {
        return locale;
    }

    public boolean usesTraditionalChineseFont() {
        return traditionalChineseFont;
    }

    public String getLabelKey() {
        return labelKey;
    }

    public static List<AppLanguage> supportedValues() {
        return List.of(values());
    }

    public static AppLanguage fromId(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        for (AppLanguage language : values()) {
            if (language.id.equalsIgnoreCase(value)) {
                return language;
            }
        }
        return null;
    }

    public static AppLanguage fromPreference(String value) {
        AppLanguage resolved = fromId(value);
        return resolved != null ? resolved : detectDefault();
    }

    public static AppLanguage detectDefault() {
        Locale locale = Locale.getDefault();
        String tag = locale.toLanguageTag().toLowerCase(Locale.ROOT);
        if (tag.startsWith("zh-tw") || tag.startsWith("zh-hk") || tag.startsWith("zh-mo")) {
            return TRADITIONAL_CHINESE;
        }
        if (tag.startsWith("en")) {
            return ENGLISH;
        }
        return SIMPLIFIED_CHINESE;
    }
}
