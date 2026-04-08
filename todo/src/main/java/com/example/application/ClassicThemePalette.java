package com.example.application;

import java.util.List;

public enum ClassicThemePalette {
    LIGHT("light", "theme.light", null, "#cfd8dc"),
    MINT("mint", "theme.mint", "/styles/mint-theme.css", "#98d8c8"),
    OCEAN("ocean", "theme.ocean", "/styles/ocean-theme.css", "#64b5f6"),
    SUNSET("sunset", "theme.sunset", "/styles/sunset-theme.css", "#ffab91"),
    LAVENDER("lavender", "theme.lavender", "/styles/lavender-theme.css", "#ce93d8"),
    FOREST("forest", "theme.forest", "/styles/forest-theme.css", "#81c784"),
    SLATE("slate", "theme.slate", "/styles/slate-theme.css", "#90a4ae"),
    MACARON("macaron", "theme.macaron", "/styles/macaron-theme.css", "#f8bbd0");

    private final String id;
    private final String labelKey;
    private final String overlayStylesheetPath;
    private final String previewColor;

    ClassicThemePalette(String id, String labelKey, String overlayStylesheetPath, String previewColor) {
        this.id = id;
        this.labelKey = labelKey;
        this.overlayStylesheetPath = overlayStylesheetPath;
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
