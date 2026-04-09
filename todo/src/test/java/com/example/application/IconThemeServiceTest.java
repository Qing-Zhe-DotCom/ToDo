package com.example.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.example.config.UserPreferencesStore;

class IconThemeServiceTest {

    @Test
    void defaultsToBindingIconsToThemeWithClassicManualSelection() {
        IconThemeService service = new IconThemeService(new MapPreferencesStore(Map.of()));

        assertTrue(service.isBoundToTheme());
        assertEquals(IconTheme.CLASSIC, service.getSelectedIconTheme());
        assertEquals(IconTheme.CLASSIC, service.resolveIconTheme(ThemeFamily.CLASSIC));
    }

    @Test
    void boundModeFollowsThemeFamily() {
        IconThemeService service = new IconThemeService(new MapPreferencesStore(Map.of()));

        assertEquals(IconTheme.MATERIAL_YOU, service.resolveIconTheme(ThemeFamily.MATERIAL_YOU));
        assertEquals(IconTheme.NEUMORPHISM, service.resolveIconTheme(ThemeFamily.NEUMORPHISM));
    }

    @Test
    void manualModeUsesSelectedIconTheme() {
        IconThemeService service = new IconThemeService(new MapPreferencesStore(Map.of()));

        service.setBindToTheme(false, ThemeFamily.COZY);
        service.selectIconTheme(IconTheme.NEO_BRUTALISM);

        assertEquals(IconTheme.NEO_BRUTALISM, service.getSelectedIconTheme());
        assertEquals(IconTheme.NEO_BRUTALISM, service.resolveIconTheme(ThemeFamily.MATERIAL_YOU));
    }

    @Test
    void switchingFromBoundToManualInheritsCurrentActiveIconTheme() {
        IconThemeService service = new IconThemeService(new MapPreferencesStore(Map.of()));

        service.setBindToTheme(false, ThemeFamily.MODERN_MINIMAL);

        assertEquals(IconTheme.MODERN_MINIMAL, service.getSelectedIconTheme());
        assertEquals(IconTheme.MODERN_MINIMAL, service.resolveIconTheme(ThemeFamily.CLASSIC));
    }

    @Test
    void macaronIconThemeCanBeManuallySelectedEvenWithoutLabs() {
        IconThemeService service = new IconThemeService(new MapPreferencesStore(Map.of()));

        service.setBindToTheme(false, ThemeFamily.CLASSIC);
        service.selectIconTheme(IconTheme.MACARON);

        assertEquals(IconTheme.MACARON, service.resolveIconTheme(ThemeFamily.CLASSIC));
    }

    @Test
    void invalidPreferenceValuesFallBackToDefaults() {
        IconThemeService service = new IconThemeService(new MapPreferencesStore(Map.of(
            IconThemeService.PREF_ICON_BIND_TO_THEME_KEY, "not-a-boolean",
            IconThemeService.PREF_ICON_THEME_KEY, "unknown-theme"
        )));

        assertEquals(IconTheme.CLASSIC, service.getSelectedIconTheme());
        assertEquals(IconTheme.CLASSIC, service.resolveIconTheme(ThemeFamily.CLASSIC));
    }

    @Test
    void missingThemeResourceFallsBackToClassicPack() {
        IconThemeService service = new IconThemeService(new MapPreferencesStore(Map.of()));

        String resolved = service.resolveIconResource(
            path -> path.equals("/icons/themes/classic/nav-schedule.svg"),
            IconTheme.FRESH,
            IconKey.NAV_SCHEDULE
        );

        assertEquals("/icons/themes/classic/nav-schedule.svg", resolved);
    }

    @Test
    void returnsNullWhenNeitherThemeNorClassicResourceExists() {
        IconThemeService service = new IconThemeService(new MapPreferencesStore(Map.of()));

        String resolved = service.resolveIconResource(path -> false, IconTheme.FRESH, IconKey.NAV_SCHEDULE);

        assertNull(resolved);
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
