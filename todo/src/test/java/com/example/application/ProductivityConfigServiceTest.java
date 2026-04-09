package com.example.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.example.config.UserPreferencesStore;

class ProductivityConfigServiceTest {

    @Test
    void loadUsesDefaultConfigurationWhenNoOverridesExist() {
        ProductivityConfigService service = new ProductivityConfigService(new MapPreferencesStore(Map.of()));

        ProductivityConfig config = service.load();

        assertEquals(EnumSet.allOf(ProductivityConfig.SearchField.class), config.searchFields());
        assertEquals(ProductivityConfig.SortMode.PRIORITY, config.defaultSortMode());
        assertEquals(ProductivityConfig.DEFAULT_HEATMAP_PRESET, config.heatmapPreset());
        assertIterableEquals(ProductivityConfig.DEFAULT_HEATMAP_THRESHOLDS, config.heatmapThresholds());
        assertIterableEquals(ProductivityConfig.DEFAULT_HEATMAP_COLORS, config.heatmapColors());
        assertTrue(config.clockDisplay().enabled());
        assertTrue(config.clockDisplay().use24Hour());
        assertFalse(config.clockDisplay().showSeconds());
    }

    @Test
    void saveAndLoadRoundTripsProductivityConfiguration() {
        ProductivityConfigService service = new ProductivityConfigService(new MapPreferencesStore(Map.of()));

        ProductivityConfig source = new ProductivityConfig(
            EnumSet.of(ProductivityConfig.SearchField.TITLE, ProductivityConfig.SearchField.TAGS),
            ProductivityConfig.SortMode.REMAINING_TIME,
            "custom-levels",
            List.of(1, 4, 7, 10),
            List.of("#ffffff", "#cce5ff", "#99ccff", "#6699ff", "#003399"),
            List.of(" Work ", "Review", "Work", "Meeting"),
            new ProductivityConfig.ClockDisplay(false, false, true),
            List.of("alpha query", "测试 查询"),
            List.of("project-x", "roadmap")
        );

        service.save(source);
        ProductivityConfig loaded = service.load();

        assertEquals(EnumSet.of(ProductivityConfig.SearchField.TITLE, ProductivityConfig.SearchField.TAGS), loaded.searchFields());
        assertEquals(ProductivityConfig.SortMode.REMAINING_TIME, loaded.defaultSortMode());
        assertEquals("custom-levels", loaded.heatmapPreset());
        assertIterableEquals(List.of(1, 4, 7, 10), loaded.heatmapThresholds());
        assertIterableEquals(List.of("#ffffff", "#cce5ff", "#99ccff", "#6699ff", "#003399"), loaded.heatmapColors());
        assertIterableEquals(List.of("Work", "Review", "Meeting"), loaded.presetTags());
        assertFalse(loaded.clockDisplay().enabled());
        assertFalse(loaded.clockDisplay().use24Hour());
        assertTrue(loaded.clockDisplay().showSeconds());
        assertIterableEquals(List.of("alpha query", "测试 查询"), loaded.searchHistory());
        assertIterableEquals(List.of("project-x", "roadmap"), loaded.suggestionSeeds());
    }

    @Test
    void resetRemovesOverridesAndReturnsDefaults() {
        MapPreferencesStore store = new MapPreferencesStore(Map.of());
        ProductivityConfigService service = new ProductivityConfigService(store);
        service.save(new ProductivityConfig(
            EnumSet.of(ProductivityConfig.SearchField.CATEGORY),
            ProductivityConfig.SortMode.CREATED_TIME,
            "custom",
            List.of(1, 3, 6, 9),
            List.of("#111111", "#222222", "#333333", "#444444", "#555555"),
            List.of("TagA"),
            new ProductivityConfig.ClockDisplay(false, true, true),
            List.of("q"),
            List.of("s")
        ));

        assertTrue(store.containsKey(ProductivityConfigService.PREF_DEFAULT_SORT_MODE_KEY));

        ProductivityConfig resetConfig = service.reset();

        assertEquals(ProductivityConfig.SortMode.PRIORITY, resetConfig.defaultSortMode());
        assertEquals(EnumSet.allOf(ProductivityConfig.SearchField.class), resetConfig.searchFields());
        assertFalse(store.containsKey(ProductivityConfigService.PREF_DEFAULT_SORT_MODE_KEY));
        assertFalse(store.containsKey(ProductivityConfigService.PREF_SEARCH_FIELDS_KEY));
        assertFalse(store.containsKey(ProductivityConfigService.PREF_HEATMAP_PRESET_KEY));
    }

    @Test
    void saveSupportsExplicitEmptyListsWithoutFallingBackToDefaults() {
        ProductivityConfigService service = new ProductivityConfigService(new MapPreferencesStore(Map.of()));

        service.save(new ProductivityConfig(
            EnumSet.allOf(ProductivityConfig.SearchField.class),
            ProductivityConfig.SortMode.PRIORITY,
            ProductivityConfig.DEFAULT_HEATMAP_PRESET,
            ProductivityConfig.DEFAULT_HEATMAP_THRESHOLDS,
            ProductivityConfig.DEFAULT_HEATMAP_COLORS,
            List.of(),
            ProductivityConfig.ClockDisplay.defaultValue(),
            List.of(),
            List.of()
        ));

        ProductivityConfig loaded = service.load();
        assertIterableEquals(List.of(), loaded.presetTags());
        assertIterableEquals(List.of(), loaded.searchHistory());
        assertIterableEquals(List.of(), loaded.suggestionSeeds());
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

        private boolean containsKey(String key) {
            return values.containsKey(key);
        }
    }
}
