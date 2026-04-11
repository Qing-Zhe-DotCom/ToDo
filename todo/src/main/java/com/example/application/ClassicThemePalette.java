package com.example.application;

import java.util.List;

public enum ClassicThemePalette {
    LIGHT("light", "theme.light", null, null, "#cfd8dc"),
    MINT("mint", "theme.mint", "/styles/mint-theme.css", "/styles/mint-theme-dark.css", "#98d8c8"),
    OCEAN("ocean", "theme.ocean", "/styles/ocean-theme.css", "/styles/ocean-theme-dark.css", "#64b5f6"),
    SUNSET("sunset", "theme.sunset", "/styles/sunset-theme.css", "/styles/sunset-theme-dark.css", "#ffab91"),
    LAVENDER("lavender", "theme.lavender", "/styles/lavender-theme.css", "/styles/lavender-theme-dark.css", "#ce93d8"),
    FOREST("forest", "theme.forest", "/styles/forest-theme.css", "/styles/forest-theme-dark.css", "#81c784"),
    SLATE("slate", "theme.slate", "/styles/slate-theme.css", "/styles/slate-theme-dark.css", "#90a4ae");

    private final String id;
    private final String labelKey;
    private final String overlayStylesheetPath;
    private final String darkOverlayStylesheetPath;
    private final String previewColor;

    ClassicThemePalette(String id, String labelKey, String overlayStylesheetPath, String darkOverlayStylesheetPath, String previewColor) {
        this.id = id;
        this.labelKey = labelKey;
        this.overlayStylesheetPath = overlayStylesheetPath;
        this.darkOverlayStylesheetPath = darkOverlayStylesheetPath;
        this.previewColor = previewColor;
    }

    public String getId() {
        return id;
    }

    public String getLabelKey() {
        return labelKey;
    }

    public String getOverlayStylesheetPath() {
        return overlayStylesheetPath;
    }

    public String resolveOverlayStylesheetPath(ThemeAppearance appearance) {
        ThemeAppearance resolved = appearance != null ? appearance : ThemeAppearance.LIGHT;
        return resolved == ThemeAppearance.DARK ? darkOverlayStylesheetPath : overlayStylesheetPath;
    }

    public String getPreviewColor() {
        return previewColor;
    }

    public static List<ClassicThemePalette> supportedValues() {
        return List.of(values());
    }

    public static ClassicThemePalette fromPreference(String value) {
        if (value == null || value.isBlank()) {
            return LIGHT;
        }
        for (ClassicThemePalette palette : values()) {
            if (palette.id.equalsIgnoreCase(value)) {
                return palette;
            }
        }
        return LIGHT;
    }
}
