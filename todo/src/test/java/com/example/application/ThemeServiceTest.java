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
        ThemeService macaron = serviceFor(Map.of(
            ThemeService.LEGACY_PREF_THEME_KEY, "macaron",
            ThemeService.LEGACY_PREF_SCHEDULE_CARD_STYLE_KEY, "classic"
        ));
        ThemeService invalid = serviceFor(Map.of(
            ThemeService.LEGACY_PREF_THEME_KEY, "not-a-theme",
            ThemeService.LEGACY_PREF_SCHEDULE_CARD_STYLE_KEY, "classic"
        ));

        assertEquals(ClassicThemePalette.LIGHT, imported.getCurrentClassicPalette());
        assertEquals(ClassicThemePalette.LIGHT, macaron.getCurrentClassicPalette());
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
    void macaronPreferenceFallsBackToClassicWhenLabsAreDisabled() {
        ThemeService service = serviceFor(
            Map.of(ThemeService.PREF_THEME_FAMILY_KEY, "macaron"),
            false
        );

        assertEquals(ThemeFamily.CLASSIC, service.getCurrentThemeFamily());
        assertEquals(ClassicThemePalette.LIGHT, service.getCurrentClassicPalette());
    }

    @Test
    void neoBrutalismPreferenceFallsBackToClassicWhenLabsAreDisabled() {
        ThemeService service = serviceFor(
            Map.of(ThemeService.PREF_THEME_FAMILY_KEY, "neo-brutalism"),
            false
        );

        assertEquals(ThemeFamily.CLASSIC, service.getCurrentThemeFamily());
        assertEquals(ClassicThemePalette.LIGHT, service.getCurrentClassicPalette());
    }

    @Test
    void macaronPreferenceIsKeptWhenLabsAreEnabled() {
        ThemeService service = serviceFor(
            Map.of(ThemeService.PREF_THEME_FAMILY_KEY, "macaron"),
            true
        );

        assertEquals(ThemeFamily.MACARON, service.getCurrentThemeFamily());
    }

    @Test
    void neoBrutalismPreferenceIsKeptWhenLabsAreEnabled() {
        ThemeService service = serviceFor(
            Map.of(ThemeService.PREF_THEME_FAMILY_KEY, "neo-brutalism"),
            true
        );

        assertEquals(ThemeFamily.NEO_BRUTALISM, service.getCurrentThemeFamily());
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

        classic.selectTheme(ThemeFamily.CLASSIC, ThemeAppearance.DARK, ClassicThemePalette.MINT);
        List<String> classicDarkStylesheets = classic.resolveStylesheets(getClass());
        assertContains(classicDarkStylesheets, "base.css");
        assertContains(classicDarkStylesheets, "dark-theme.css");
        assertContains(classicDarkStylesheets, "theme-classic-dark.css");
        assertContains(classicDarkStylesheets, "mint-theme-dark.css");
        assertTrue(classicDarkStylesheets.stream().noneMatch(path -> path.contains("mint-theme.css")));

        ThemeService fresh = serviceFor(Map.of(
            ThemeService.PREF_THEME_FAMILY_KEY, "fresh",
            ThemeService.PREF_THEME_APPEARANCE_KEY, "dark",
            ThemeService.PREF_THEME_CLASSIC_PALETTE_KEY, "mint"
        ));

        List<String> freshStylesheets = fresh.resolveStylesheets(getClass());
        assertContains(freshStylesheets, "dark-theme.css");
        assertContains(freshStylesheets, "theme-fresh-dark.css");
        assertTrue(freshStylesheets.stream().noneMatch(path -> path.contains("light-theme.css")));
        assertTrue(freshStylesheets.stream().noneMatch(path -> path.contains("mint-theme.css")));
        assertTrue(freshStylesheets.stream().noneMatch(path -> path.contains("mint-theme-dark.css")));

        ThemeService macaron = serviceFor(
            Map.of(
                ThemeService.PREF_THEME_FAMILY_KEY, "macaron",
                ThemeService.PREF_THEME_APPEARANCE_KEY, "dark",
                ThemeService.PREF_THEME_CLASSIC_PALETTE_KEY, "sunset"
            ),
            true
        );

        List<String> macaronStylesheets = macaron.resolveStylesheets(getClass());
        assertContains(macaronStylesheets, "dark-theme.css");
        assertContains(macaronStylesheets, "theme-macaron-dark.css");
        assertTrue(macaronStylesheets.stream().noneMatch(path -> path.contains("sunset-theme.css")));
    }

    private ThemeService serviceFor(Map<String, String> preferences) {
        return serviceFor(preferences, false);
    }

    private ThemeService serviceFor(Map<String, String> preferences, boolean labsEnabled) {
        MapPreferencesStore store = new MapPreferencesStore(preferences);
        ExperimentalFeaturesService experimentalFeaturesService = new ExperimentalFeaturesService(store);
        experimentalFeaturesService.setLabsEnabled(labsEnabled);
        return new ThemeService(
            store,
            new AppProperties("test", "classic", "light", "light", null, null),
            experimentalFeaturesService
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
