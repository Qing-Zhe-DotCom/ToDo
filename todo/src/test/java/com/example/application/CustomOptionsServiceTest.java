package com.example.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.example.config.UserPreferencesStore;

class CustomOptionsServiceTest {

    @Test
    void seedsDefaultTasksWhenPreferenceMissing() {
        MapPreferencesStore store = new MapPreferencesStore();

        CustomOptionsService service = new CustomOptionsService(store);

        assertFalse(service.getTasks().isEmpty());
        assertEquals(9, service.getTasks().size());
        assertTrue(service.getTags().isEmpty());

        assertNotNull(store.get(CustomOptionsService.PREF_TASKS_KEY, null));
    }

    @Test
    void ensureTaskExistsAddsWhenUnderLimitAndRejectsWhenAtLimit() {
        MapPreferencesStore store = new MapPreferencesStore();
        CustomOptionsService service = new CustomOptionsService(store);

        assertTrue(service.ensureTaskExists("娴嬭瘯浠诲姟"));
        assertTrue(service.getTasks().contains("娴嬭瘯浠诲姟"));

        List<String> filled = new ArrayList<>();
        for (int i = 0; i < CustomOptionsService.MAX_TASKS; i++) {
            filled.add("T" + i);
        }
        service.replaceTasks(filled);

        assertFalse(service.ensureTaskExists("T-EXTRA"));
    }

    @Test
    void ensureTagsExistDeduplicatesAndRespectsLimit() {
        MapPreferencesStore store = new MapPreferencesStore();
        CustomOptionsService service = new CustomOptionsService(store);

        assertTrue(service.ensureTagsExist(List.of("Work", "work", "Life")));
        assertEquals(List.of("Work", "Life"), service.getTags());

        List<String> filled = new ArrayList<>();
        for (int i = 0; i < CustomOptionsService.MAX_TAGS; i++) {
            filled.add("Tag" + i);
        }
        service.replaceTags(filled);

        assertFalse(service.ensureTagsExist(List.of("Another")));
    }

    @Test
    void replaceTasksRejectsOverMaxUniqueEntries() {
        MapPreferencesStore store = new MapPreferencesStore();
        CustomOptionsService service = new CustomOptionsService(store);

        List<String> tooMany = new ArrayList<>();
        for (int i = 0; i < CustomOptionsService.MAX_TASKS + 1; i++) {
            tooMany.add("Task" + i);
        }

        assertThrows(IllegalArgumentException.class, () -> service.replaceTasks(tooMany));
    }

    @Test
    void timeTextInputDefaultsToDisabled() {
        CustomOptionsService service = new CustomOptionsService(new MapPreferencesStore());

        assertFalse(service.isTimeTextInputEnabled());
    }

    @Test
    void timeTextInputPreferenceRoundTrips() {
        CustomOptionsService service = new CustomOptionsService(new MapPreferencesStore());

        service.setTimeTextInputEnabled(true);
        assertTrue(service.isTimeTextInputEnabled());

        service.setTimeTextInputEnabled(false);
        assertFalse(service.isTimeTextInputEnabled());
    }

    private static final class MapPreferencesStore implements UserPreferencesStore {
        private final Map<String, String> values = new HashMap<>();

        private MapPreferencesStore() {
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
