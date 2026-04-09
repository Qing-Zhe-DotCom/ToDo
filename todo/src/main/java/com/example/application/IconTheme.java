package com.example.application;

import java.util.List;

public enum IconTheme {
    CLASSIC("classic", "icon.theme.classic"),
    FRESH("fresh", "icon.theme.fresh"),
    COZY("cozy", "icon.theme.cozy"),
    MACARON("macaron", "icon.theme.macaron"),
    MODERN_MINIMAL("modern-minimal", "icon.theme.modernMinimal"),
    NEO_BRUTALISM("neo-brutalism", "icon.theme.neoBrutalism"),
    MATERIAL_YOU("material-you", "icon.theme.materialYou"),
    NEUMORPHISM("neumorphism", "icon.theme.neumorphism");

    private final String id;
    private final String labelKey;

    IconTheme(String id, String labelKey) {
        this.id = id;
        this.labelKey = labelKey;
    }

    public String getId() {
        return id;
    }

    public String getLabelKey() {
        return labelKey;
    }

    public static List<IconTheme> supportedValues() {
        return List.of(values());
    }

    public static IconTheme fromPreference(String value) {
        if (value == null || value.isBlank()) {
            return CLASSIC;
        }
        for (IconTheme iconTheme : values()) {
            if (iconTheme.id.equalsIgnoreCase(value)) {
                return iconTheme;
            }
        }
        return CLASSIC;
    }

    public static IconTheme fromThemeFamily(ThemeFamily themeFamily) {
        if (themeFamily == null) {
            return CLASSIC;
        }
        return fromPreference(themeFamily.getId());
    }
}
