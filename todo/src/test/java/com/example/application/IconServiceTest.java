package com.example.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import com.example.config.UserPreferencesStore;

class IconServiceTest {

    @Test
    void defaultsToThemeBindingAndUsesThemeMappedPack() {
        IconService service = serviceFor(Map.of(), ThemeFamily.MODERN_MINIMAL);

        assertTrue(service.isThemeBindingEnabled());
        assertEquals(IconPack.MODERN, service.getCurrentIconPack());
    }

    @Test
    void manualSelectionRoundTripsWhenUnbound() {
        IconService service = serviceFor(
            Map.of(
                IconService.PREF_ICON_THEME_BOUND_KEY, Boolean.FALSE.toString(),
                IconService.PREF_ICON_PACK_KEY, IconPack.MACARON.getId()
            ),
            ThemeFamily.CLASSIC
        );

        assertFalse(service.isThemeBindingEnabled());
        assertEquals(IconPack.MACARON, service.getCurrentIconPack());

        service.selectIconPack(IconPack.MODERN);
        assertEquals(IconPack.MODERN, service.getCurrentIconPack());
    }

    @Test
    void manualSelectionIsIgnoredWhileThemeBindingIsEnabled() {
        IconService service = serviceFor(Map.of(), ThemeFamily.MACARON);

        service.selectIconPack(IconPack.CLASSIC);

        assertEquals(IconPack.MACARON, service.getCurrentIconPack());
    }

    @Test
    void syncingThemeFamilyRemapsPackWhenBindingIsEnabled() {
        IconService service = serviceFor(Map.of(), ThemeFamily.CLASSIC);

        service.syncThemeFamily(ThemeFamily.MACARON);

        assertEquals(IconPack.MACARON, service.getCurrentIconPack());
    }

    @Test
    void nonMatchingPackCanBeChosenWhenBindingIsDisabled() {
        IconService service = serviceFor(Map.of(), ThemeFamily.CLASSIC);

        service.commitSelection(ThemeFamily.CLASSIC, false, IconPack.MACARON);

        assertFalse(service.isThemeBindingEnabled());
        assertEquals(IconPack.MACARON, service.getCurrentIconPack());
    }

    @Test
    void darkAppearancePrefersDarkIconDirectoriesWhenAvailable() {
        IconService service = serviceFor(Map.of(), ThemeFamily.CLASSIC);
        service.syncThemeAppearance(ThemeAppearance.DARK);

        assertTrue(service.resolveResourcePath(IconKey.CALENDAR).contains("/icons/classic_dark/calendar.svg"));
    }

    @Test
    void darkAppearanceFallsBackToLightPackWhenPackHasNoDarkDirectory() {
        IconService service = serviceFor(Map.of(), ThemeFamily.MACARON);
        service.syncThemeAppearance(ThemeAppearance.DARK);

        assertTrue(service.resolveResourcePath(IconKey.CALENDAR).contains("/icons/macaron/calendar.svg"));
    }

    @Test
    void syncingThemeAppearanceNotifiesListenersOnlyWhenChanged() {
        IconService service = serviceFor(Map.of(), ThemeFamily.MODERN_MINIMAL);
        AtomicInteger calls = new AtomicInteger();
        service.addChangeListener(calls::incrementAndGet);

        service.syncThemeAppearance(ThemeAppearance.DARK);
        service.syncThemeAppearance(ThemeAppearance.DARK);
        service.syncThemeAppearance(ThemeAppearance.LIGHT);

        assertEquals(2, calls.get());
    }

    private IconService serviceFor(Map<String, String> preferences, ThemeFamily themeFamily) {
        return new IconService(new MapPreferencesStore(preferences), themeFamily);
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
