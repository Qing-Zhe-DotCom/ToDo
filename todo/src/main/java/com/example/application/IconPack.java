package com.example.application;

import java.util.List;

public enum IconPack {
    CLASSIC("classic", "icon.pack.classic"),
    FRESH("fresh", "icon.pack.fresh"),
    COZY("cozy", "icon.pack.cozy"),
    MACARON("macaron", "icon.pack.macaron"),
    MODERN("modern", "icon.pack.modern"),
    BRUTALISM("brutalism", "icon.pack.brutalism"),
    MATERIAL("material", "icon.pack.material"),
    NEUMORPHISM("neumorphism", "icon.pack.neumorphism"),
    COOKIE("cookie", "icon.pack.cookie");

    private final String id;
    private final String labelKey;

    IconPack(String id, String labelKey) {
        this.id = id;
        this.labelKey = labelKey;
    }

    public String getId() {
        return id;
    }

    public String getLabelKey() {
        return labelKey;
    }

    public String resolveResourcePath(IconKey iconKey) {
        IconKey resolvedIconKey = iconKey != null ? iconKey : IconKey.CALENDAR;
        return "/icons/" + id + "/" + resolvedIconKey.getFileName();
    }

    public static List<IconPack> supportedValues() {
        return List.of(values());
    }

    public static IconPack fromPreference(String value) {
        if (value == null || value.isBlank()) {
            return CLASSIC;
        }
        for (IconPack pack : values()) {
            if (pack.id.equalsIgnoreCase(value)) {
                return pack;
            }
        }
        return CLASSIC;
    }

    public static IconPack boundToThemeFamily(ThemeFamily family) {
        ThemeFamily resolvedFamily = family != null ? family : ThemeFamily.CLASSIC;
        return switch (resolvedFamily) {
            case CLASSIC -> CLASSIC;
            case FRESH -> FRESH;
            case COZY -> COZY;
            case MACARON -> MACARON;
            case MODERN_MINIMAL -> MODERN;
            case NEO_BRUTALISM -> BRUTALISM;
            case MATERIAL_YOU -> MATERIAL;
            case NEUMORPHISM -> NEUMORPHISM;
        };
    }
}
