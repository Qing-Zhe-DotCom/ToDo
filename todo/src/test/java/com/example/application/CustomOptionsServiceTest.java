package com.example.application;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.example.config.UserPreferencesStore;

class CustomOptionsServiceTest {

    @Test
    void timeTextInputDefaultsToDisabled() {
        CustomOptionsService service = new CustomOptionsService(new MapPreferencesStore(Map.of()));

        assertFalse(service.isTimeTextInputEnabled());
    }

    @Test
    void timeTextInputPreferenceRoundTrips() {
        CustomOptionsService service = new CustomOptionsService(new MapPreferencesStore(Map.of()));

        service.setTimeTextInputEnabled(true);
        assertTrue(service.isTimeTextInputEnabled());

        service.setTimeTextInputEnabled(false);
        assertFalse(service.isTimeTextInputEnabled());
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

