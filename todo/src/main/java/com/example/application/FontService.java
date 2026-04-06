package com.example.application;

import java.io.InputStream;
import java.util.Objects;

import com.example.config.UserPreferencesStore;

import javafx.scene.Node;
import javafx.scene.text.Font;

public final class FontService {
    private static final String PREF_FONT_WEIGHT_KEY = "todo.font.weight";
    private static final String DEFAULT_SC_FAMILY = "HarmonyOS Sans SC";
    private static final String DEFAULT_TC_FAMILY = "HarmonyOS Sans TC";

    private final UserPreferencesStore preferencesStore;

    private AppFontWeight currentFontWeight;
    private String simplifiedFamily = DEFAULT_SC_FAMILY;
    private String traditionalFamily = DEFAULT_TC_FAMILY;

    public FontService(UserPreferencesStore preferencesStore) {
        this.preferencesStore = Objects.requireNonNull(preferencesStore, "preferencesStore");
        this.currentFontWeight = AppFontWeight.fromPreference(preferencesStore.get(PREF_FONT_WEIGHT_KEY, AppFontWeight.REGULAR.getId()));
        loadFonts();
    }

    public AppFontWeight getCurrentFontWeight() {
        return currentFontWeight;
    }

    public void selectFontWeight(AppFontWeight fontWeight) {
        currentFontWeight = fontWeight != null ? fontWeight : AppFontWeight.REGULAR;
        preferencesStore.put(PREF_FONT_WEIGHT_KEY, currentFontWeight.getId());
    }

    public String getInlineStyle(AppLanguage language) {
        String family = resolveFamily(language);
        return "-fx-font-family: \"" + family + "\"; -fx-font-weight: " + currentFontWeight.getCssWeight() + ";";
    }

    public void applyTo(Node node, AppLanguage language) {
        if (node == null) {
            return;
        }
        node.setStyle(getInlineStyle(language));
    }

    public String resolveFamily(AppLanguage language) {
        return language != null && language.usesTraditionalChineseFont() ? traditionalFamily : simplifiedFamily;
    }

    private void loadFonts() {
        for (AppFontWeight weight : AppFontWeight.supportedValues()) {
            simplifiedFamily = loadFamily(AppLanguage.SIMPLIFIED_CHINESE, weight, simplifiedFamily);
            traditionalFamily = loadFamily(AppLanguage.TRADITIONAL_CHINESE, weight, traditionalFamily);
        }
    }

    private String loadFamily(AppLanguage language, AppFontWeight weight, String fallbackFamily) {
        String resourcePath = weight.resolveResourcePath(language);
        try (InputStream inputStream = getClass().getResourceAsStream(resourcePath)) {
            if (inputStream == null) {
                return fallbackFamily;
            }
            Font font = Font.loadFont(inputStream, 12);
            return font != null && font.getFamily() != null && !font.getFamily().isBlank()
                ? font.getFamily()
                : fallbackFamily;
        } catch (Exception ignored) {
            return fallbackFamily;
        }
    }
}
