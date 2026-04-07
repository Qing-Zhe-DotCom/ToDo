package com.example.application;

import static org.junit.jupiter.api.Assertions.assertFalse;
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
    void fontServiceFallsBackToRegularForInvalidPreferenceAndBuildsInlineStyle() {
        FontService service = new FontService(new MapPreferencesStore(Map.of("todo.font.weight", "invalid")));

        String simplifiedStyle = service.getInlineStyle(AppLanguage.SIMPLIFIED_CHINESE);
        String traditionalStyle = service.getInlineStyle(AppLanguage.TRADITIONAL_CHINESE);

        assertTrue(simplifiedStyle.contains("-fx-font-weight: 400"));
        assertTrue(traditionalStyle.contains("-fx-font-weight: 400"));
        assertTrue(simplifiedStyle.contains("-fx-font-family"));
        assertTrue(traditionalStyle.contains("-fx-font-family"));
        assertFalse(service.resolveFamily(AppLanguage.SIMPLIFIED_CHINESE).isBlank());
        assertFalse(service.resolveFamily(AppLanguage.TRADITIONAL_CHINESE).isBlank());
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
