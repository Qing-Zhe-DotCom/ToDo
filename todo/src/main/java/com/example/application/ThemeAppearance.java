package com.example.application;

import java.util.List;

public enum ThemeAppearance {
    LIGHT("light"),
    DARK("dark");

    private final String id;

    ThemeAppearance(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public static List<ThemeAppearance> supportedValues() {
        return List.of(values());
    }

    public static ThemeAppearance fromPreference(String value) {
        if (value == null || value.isBlank()) {
            return LIGHT;
        }
        for (ThemeAppearance appearance : values()) {
            if (appearance.id.equalsIgnoreCase(value)) {
                return appearance;
            }
        }
        return LIGHT;
    }
}
