package com.example.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.example.config.AppProperties;
import com.example.config.UserPreferencesStore;

class ThemeServiceTest {

    @Test
    void legacyClassicStyleMigrationKeepsClassicPalette() {
        ThemeService service = serviceFor(Map.of(
            ThemeService.LEGACY_PREF_THEME_KEY, "mint",
            ThemeService.LEGACY_PREF_SCHEDULE_CARD_STYLE_KEY, "classic"
        ));

        assertEquals(ThemeFamily.CLASSIC, service.getCurrentThemeFamily());
        assertEquals(ThemeAppearance.LIGHT, service.getCurrentAppearance());
        assertEquals(ClassicThemePalette.MINT, service.getCurrentClassicPalette());
        assertEquals("classic", service.getCurrentScheduleCardStyle());
    }

    @Test
    void legacyNonClassicStyleMigrationIgnoresLegacyPalette() {
        ThemeService service = serviceFor(Map.of(
            ThemeService.LEGACY_PREF_THEME_KEY, "forest",
            ThemeService.LEGACY_PREF_SCHEDULE_CARD_STYLE_KEY, "material-you"
        ));

        assertEquals(ThemeFamily.MATERIAL_YOU, service.getCurrentThemeFamily());
        assertEquals(ClassicThemePalette.LIGHT, service.getCurrentClassicPalette());
        assertEquals("material-you", service.getCurrentScheduleCardStyle());
    }

    @Test
    void importedOrInvalidLegacyThemeFallsBackToLightClassicPalette() {
        ThemeService imported = serviceFor(Map.of(
            ThemeService.LEGACY_PREF_THEME_KEY, "imported",
            ThemeService.LEGACY_PREF_SCHEDULE_CARD_STYLE_KEY, "classic"
        ));
        ThemeService invalid = serviceFor(Map.of(
            ThemeService.LEGACY_PREF_THEME_KEY, "not-a-theme",
            ThemeService.LEGACY_PREF_SCHEDULE_CARD_STYLE_KEY, "classic"
        ));

        assertEquals(ClassicThemePalette.LIGHT, imported.getCurrentClassicPalette());
        assertEquals(ClassicThemePalette.LIGHT, invalid.getCurrentClassicPalette());
    }

    @Test
    void invalidNewPreferenceValuesFallBackToDefaults() {
        ThemeService service = serviceFor(Map.of(
            ThemeService.PREF_THEME_FAMILY_KEY, "unknown",
            ThemeService.PREF_THEME_APPEARANCE_KEY, "bad",
            ThemeService.PREF_THEME_CLASSIC_PALETTE_KEY, "bad"
        ));

        assertEquals(ThemeFamily.CLASSIC, service.getCurrentThemeFamily());
        assertEquals(ThemeAppearance.LIGHT, service.getCurrentAppearance());
        assertEquals(ClassicThemePalette.LIGHT, service.getCurrentClassicPalette());
    }

    @Test
    void resolveStylesheetsUsesFamilySkinAndClassicPaletteOverlay() {
        ThemeService classic = serviceFor(Map.of());
        classic.selectTheme(ThemeFamily.CLASSIC, ThemeAppearance.LIGHT, ClassicThemePalette.OCEAN);

        List<String> classicStylesheets = classic.resolveStylesheets(getClass());
        assertContains(classicStylesheets, "base.css");
        assertContains(classicStylesheets, "light-theme.css");
        assertContains(classicStylesheets, "theme-classic-light.css");
        assertContains(classicStylesheets, "ocean-theme.css");

        ThemeService fresh = serviceFor(Map.of(
            ThemeService.PREF_THEME_FAMILY_KEY, "fresh",
            ThemeService.PREF_THEME_APPEARANCE_KEY, "dark",
            ThemeService.PREF_THEME_CLASSIC_PALETTE_KEY, "mint"
        ));

        List<String> freshStylesheets = fresh.resolveStylesheets(getClass());
        assertContains(freshStylesheets, "theme-fresh-light.css");
        assertTrue(freshStylesheets.stream().noneMatch(path -> path.contains("mint-theme.css")));
    }

    private ThemeService serviceFor(Map<String, String> preferences) {
        return new ThemeService(
            new MapPreferencesStore(preferences),
            new AppProperties("test", "classic", "light", "light", null, null)
        );
    }

    private void assertContains(List<String> stylesheets, String expectedFragment) {
        assertTrue(stylesheets.stream().anyMatch(path -> path.contains(expectedFragment)));
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
