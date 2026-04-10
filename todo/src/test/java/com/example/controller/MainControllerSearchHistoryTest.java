package com.example.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.config.UserPreferencesStore;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class MainControllerSearchHistoryTest {

    @Test
    void persistsNormalizedHistoryEntry() {
        Map<String, String> preferences = new HashMap<>();
        MapPreferencesStore store = new MapPreferencesStore(preferences);
        List<String> buffer = new ArrayList<>();

        assertNull(MainController.normalizeSearchHistoryEntry("   "));

        MainController.rememberSearchHistory(store, buffer, "  plan \n  A  ");

        assertEquals(List.of("plan A"), buffer);
        assertEquals("plan A", store.get("todo.search.history", ""));
    }

    @Test
    void dedupesCaseInsensitiveAndMovesToFront() {
        MapPreferencesStore store = new MapPreferencesStore(new HashMap<>());
        List<String> buffer = new ArrayList<>();

        MainController.rememberSearchHistory(store, buffer, "Study plan");
        MainController.rememberSearchHistory(store, buffer, "read");
        MainController.rememberSearchHistory(store, buffer, "study   PLAN");

        assertEquals(List.of("study PLAN", "read"), buffer);
        assertEquals("study PLAN\nread", store.get("todo.search.history", ""));
    }

    @Test
    void capsHistoryAtTwentyEntries() {
        MapPreferencesStore store = new MapPreferencesStore(new HashMap<>());
        List<String> buffer = new ArrayList<>();

        for (int i = 1; i <= 25; i++) {
            MainController.rememberSearchHistory(store, buffer, "k" + i);
        }

        assertEquals(20, buffer.size());
        assertEquals("k25", buffer.get(0));
        assertEquals("k6", buffer.get(19));

        String stored = store.get("todo.search.history", "");
        assertEquals(20, stored.split("\n").length);
        assertTrue(stored.startsWith("k25\n"));
    }

    private static final class MapPreferencesStore implements UserPreferencesStore {
        private final Map<String, String> preferences;

        private MapPreferencesStore(Map<String, String> seed) {
            preferences = new HashMap<>(seed);
        }

        @Override
        public String get(String key, String fallback) {
            return preferences.getOrDefault(key, fallback);
        }

        @Override
        public void put(String key, String value) {
            preferences.put(key, value);
        }

        @Override
        public void remove(String key) {
            preferences.remove(key);
        }
    }
}

