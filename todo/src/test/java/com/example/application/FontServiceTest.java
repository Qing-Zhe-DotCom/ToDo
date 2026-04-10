package com.example.application;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.example.config.UserPreferencesStore;

class FontServiceTest {

    @Test
    void resourcePathSelectionMatchesLanguageAndWeight() {
        assertTrue(AppFontWeight.THIN.resolveResourcePath(AppLanguage.SIMPLIFIED_CHINESE).contains("HarmonyOS_Sans_SC_Thin.ttf"));
        assertTrue(AppFontWeight.BOLD.resolveResourcePath(AppLanguage.TRADITIONAL_CHINESE).contains("HarmonyOS_Sans_TC_Bold.ttf"));
    }

    @Test
    void resolvesBundledFaceNamesForAllWeightsAndScripts() {
        FontService service = new FontService(new MapPreferencesStore(Map.of()));

        assertEquals("HarmonyOS Sans SC Thin", service.resolveFontName(AppLanguage.SIMPLIFIED_CHINESE, AppFontWeight.THIN));
        assertEquals("HarmonyOS Sans SC", service.resolveFontName(AppLanguage.SIMPLIFIED_CHINESE, AppFontWeight.REGULAR));
        assertEquals("HarmonyOS Sans SC Bold", service.resolveFontName(AppLanguage.SIMPLIFIED_CHINESE, AppFontWeight.BOLD));

        assertEquals("HarmonyOS Sans TC Thin", service.resolveFontName(AppLanguage.TRADITIONAL_CHINESE, AppFontWeight.THIN));
        assertEquals("HarmonyOS Sans TC", service.resolveFontName(AppLanguage.TRADITIONAL_CHINESE, AppFontWeight.REGULAR));
        assertEquals("HarmonyOS Sans TC Bold", service.resolveFontName(AppLanguage.TRADITIONAL_CHINESE, AppFontWeight.BOLD));
    }

    @Test
    void fontServiceFallsBackToRegularForInvalidPreferenceAndBuildsInlineStyle() {
        FontService service = new FontService(new MapPreferencesStore(Map.of("todo.font.weight", "invalid")));

        assertEquals(AppFontWeight.REGULAR, service.getCurrentFontWeight());

        String simplifiedStyle = service.getInlineStyle(AppLanguage.SIMPLIFIED_CHINESE);
        String traditionalStyle = service.getInlineStyle(AppLanguage.TRADITIONAL_CHINESE);

        assertTrue(simplifiedStyle.contains("-fx-font-family:"));
        assertTrue(traditionalStyle.contains("-fx-font-family:"));
        assertTrue(simplifiedStyle.contains("\"HarmonyOS Sans SC\""));
        assertTrue(traditionalStyle.contains("\"HarmonyOS Sans TC\""));
        assertFalse(service.resolveFontName(AppLanguage.SIMPLIFIED_CHINESE, AppFontWeight.REGULAR).isBlank());
        assertFalse(service.resolveFontName(AppLanguage.TRADITIONAL_CHINESE, AppFontWeight.REGULAR).isBlank());
    }

    @Test
    void inlineStyleTracksSelectedFontWeight() {
        FontService service = new FontService(new MapPreferencesStore(Map.of()));

        service.selectFontWeight(AppFontWeight.THIN);
        assertTrue(service.getInlineStyle(AppLanguage.SIMPLIFIED_CHINESE).contains("\"HarmonyOS Sans SC Thin\""));

        service.selectFontWeight(AppFontWeight.BOLD);
        assertTrue(service.getInlineStyle(AppLanguage.SIMPLIFIED_CHINESE).contains("\"HarmonyOS Sans SC Bold\""));
    }

    private static final class MapPreferencesStore implements UserPreferencesStore {
        private final Map<String, String> values = new HashMap<>();

        private MapPreferencesStore(Map<String, String> seed) {
            if (seed != null) {
                values.putAll(seed);
            }
        }

        @Override
        public String get(String key, String fallback) {
            return values.getOrDefault(key, fallback);
        }

        @Override
        public void put(String key, String value) {
            values.put(key, value);
        }

        @Override
        public void remove(String key) {
            values.remove(key);
        }
    }
}
