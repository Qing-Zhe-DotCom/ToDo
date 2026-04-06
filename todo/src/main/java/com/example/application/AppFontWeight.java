package com.example.application;

import java.util.List;

public enum AppFontWeight {
    THIN("thin", "100", "Thin", "settings.font.option.thin"),
    REGULAR("regular", "400", "Regular", "settings.font.option.regular"),
    BOLD("bold", "700", "Bold", "settings.font.option.bold");

    private final String id;
    private final String cssWeight;
    private final String resourceSuffix;
    private final String labelKey;

    AppFontWeight(String id, String cssWeight, String resourceSuffix, String labelKey) {
        this.id = id;
        this.cssWeight = cssWeight;
        this.resourceSuffix = resourceSuffix;
        this.labelKey = labelKey;
    }

    public String getId() {
        return id;
    }

    public String getCssWeight() {
        return cssWeight;
    }

    public String getLabelKey() {
        return labelKey;
    }

    public String resolveResourcePath(AppLanguage language) {
        boolean traditional = language != null && language.usesTraditionalChineseFont();
        String folder = traditional ? "HarmonyOS_Sans_TC" : "HarmonyOS_Sans_SC";
        String prefix = traditional ? "HarmonyOS_Sans_TC_" : "HarmonyOS_Sans_SC_";
        return "/font/" + folder + "/" + prefix + resourceSuffix + ".ttf";
    }

    public static List<AppFontWeight> supportedValues() {
        return List.of(values());
    }

    public static AppFontWeight fromPreference(String value) {
        if (value == null || value.isBlank()) {
            return REGULAR;
        }
        for (AppFontWeight fontWeight : values()) {
            if (fontWeight.id.equalsIgnoreCase(value)) {
                return fontWeight;
            }
        }
        return REGULAR;
    }
}
