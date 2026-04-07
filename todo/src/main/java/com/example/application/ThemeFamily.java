package com.example.application;

import java.util.List;

import com.example.view.ScheduleCardStyleSupport;

public enum ThemeFamily {
    CLASSIC("classic", "theme.family.classic", ScheduleCardStyleSupport.STYLE_ID_CLASSIC, true),
    FRESH("fresh", "theme.family.fresh", ScheduleCardStyleSupport.STYLE_ID_FRESH, false),
    COZY("cozy", "theme.family.cozy", ScheduleCardStyleSupport.STYLE_ID_COZY, false),
    MODERN_MINIMAL("modern-minimal", "theme.family.modernMinimal", ScheduleCardStyleSupport.STYLE_ID_MODERN_MINIMAL, false),
    NEO_BRUTALISM("neo-brutalism", "theme.family.neoBrutalism", ScheduleCardStyleSupport.STYLE_ID_NEO_BRUTALISM, false),
    MATERIAL_YOU("material-you", "theme.family.materialYou", ScheduleCardStyleSupport.STYLE_ID_MATERIAL_YOU, false),
    NEUMORPHISM("neumorphism", "theme.family.neumorphism", ScheduleCardStyleSupport.STYLE_ID_NEUMORPHISM, false);

    private final String id;
    private final String labelKey;
    private final String boundScheduleCardStyle;
    private final boolean supportsClassicPalette;

    ThemeFamily(String id, String labelKey, String boundScheduleCardStyle, boolean supportsClassicPalette) {
        this.id = id;
        this.labelKey = labelKey;
        this.boundScheduleCardStyle = boundScheduleCardStyle;
        this.supportsClassicPalette = supportsClassicPalette;
    }

    public String getId() {
        return id;
    }

    public String getLabelKey() {
        return labelKey;
    }

    public String getBoundScheduleCardStyle() {
        return boundScheduleCardStyle;
    }

    public boolean supportsClassicPalette() {
        return supportsClassicPalette;
    }

    public String resolveStylesheetPath(ThemeAppearance appearance) {
        ThemeAppearance resolvedAppearance = appearance != null ? appearance : ThemeAppearance.LIGHT;
        return "/styles/theme-" + id + "-" + resolvedAppearance.getId() + ".css";
    }

    public static List<ThemeFamily> supportedValues() {
        return List.of(values());
    }

    public static ThemeFamily fromPreference(String value) {
        if (value == null || value.isBlank()) {
            return CLASSIC;
        }
        for (ThemeFamily family : values()) {
            if (family.id.equalsIgnoreCase(value)) {
                return family;
            }
        }
        return CLASSIC;
    }

    public static ThemeFamily fromBoundCardStyle(String styleId) {
        String normalized = ScheduleCardStyleSupport.normalizeStyleId(styleId);
        for (ThemeFamily family : values()) {
            if (family.boundScheduleCardStyle.equals(normalized)) {
                return family;
            }
        }
        return CLASSIC;
    }
}
