package com.example.application;

import java.io.InputStream;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

import com.example.config.UserPreferencesStore;

import javafx.scene.Node;
import javafx.scene.text.Font;

public final class FontService {
    private static final String PREF_FONT_WEIGHT_KEY = "todo.font.weight";
    private static final String DEFAULT_SC_REGULAR_FACE = "HarmonyOS Sans SC";
    private static final String DEFAULT_SC_THIN_FACE = "HarmonyOS Sans SC Thin";
    private static final String DEFAULT_SC_BOLD_FACE = "HarmonyOS Sans SC Bold";
    private static final String DEFAULT_TC_REGULAR_FACE = "HarmonyOS Sans TC";
    private static final String DEFAULT_TC_THIN_FACE = "HarmonyOS Sans TC Thin";
    private static final String DEFAULT_TC_BOLD_FACE = "HarmonyOS Sans TC Bold";
    private static final double DEFAULT_FONT_SIZE = Font.getDefault().getSize();

    private final UserPreferencesStore preferencesStore;
    private final Map<AppFontWeight, String> simplifiedFaceNames = new EnumMap<>(AppFontWeight.class);
    private final Map<AppFontWeight, String> traditionalFaceNames = new EnumMap<>(AppFontWeight.class);

    private AppFontWeight currentFontWeight;

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
        String fontName = resolveFontName(language, currentFontWeight);
        return "-fx-font: " + formatFontSize(DEFAULT_FONT_SIZE) + "px \"" + escape(fontName) + "\";";
    }

    public void applyTo(Node node, AppLanguage language) {
        if (node == null) {
            return;
        }
        node.setStyle(getInlineStyle(language));
    }

    public String resolveFamily(AppLanguage language) {
        return resolveFontName(language, currentFontWeight);
    }

    String resolveFontName(AppLanguage language, AppFontWeight fontWeight) {
        AppFontWeight resolvedWeight = fontWeight != null ? fontWeight : AppFontWeight.REGULAR;
        Map<AppFontWeight, String> faceNames = language != null && language.usesTraditionalChineseFont()
            ? traditionalFaceNames
            : simplifiedFaceNames;
        return faceNames.getOrDefault(resolvedWeight, defaultFaceName(language, resolvedWeight));
    }

    private void loadFonts() {
        for (AppFontWeight weight : AppFontWeight.supportedValues()) {
            simplifiedFaceNames.put(weight, loadFaceName(AppLanguage.SIMPLIFIED_CHINESE, weight));
            traditionalFaceNames.put(weight, loadFaceName(AppLanguage.TRADITIONAL_CHINESE, weight));
        }
    }

    private String loadFaceName(AppLanguage language, AppFontWeight weight) {
        String resourcePath = weight.resolveResourcePath(language);
        try (InputStream inputStream = getClass().getResourceAsStream(resourcePath)) {
            if (inputStream == null) {
                return defaultFaceName(language, weight);
            }
            Font font = Font.loadFont(inputStream, DEFAULT_FONT_SIZE);
            return font != null && font.getName() != null && !font.getName().isBlank()
                ? font.getName()
                : defaultFaceName(language, weight);
        } catch (Exception ignored) {
            return defaultFaceName(language, weight);
        }
    }

    private String defaultFaceName(AppLanguage language, AppFontWeight weight) {
        boolean traditional = language != null && language.usesTraditionalChineseFont();
        AppFontWeight resolvedWeight = weight != null ? weight : AppFontWeight.REGULAR;
        if (traditional) {
            return switch (resolvedWeight) {
                case THIN -> DEFAULT_TC_THIN_FACE;
                case BOLD -> DEFAULT_TC_BOLD_FACE;
                case REGULAR -> DEFAULT_TC_REGULAR_FACE;
            };
        }
        return switch (resolvedWeight) {
            case THIN -> DEFAULT_SC_THIN_FACE;
            case BOLD -> DEFAULT_SC_BOLD_FACE;
            case REGULAR -> DEFAULT_SC_REGULAR_FACE;
        };
    }

    private String formatFontSize(double size) {
        return size == Math.rint(size) ? Integer.toString((int) size) : Double.toString(size);
    }

    private String escape(String fontName) {
        return fontName.replace("\"", "\\\"");
    }
}
