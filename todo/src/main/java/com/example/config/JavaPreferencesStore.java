package com.example.config;

import java.util.prefs.Preferences;

public final class JavaPreferencesStore implements UserPreferencesStore {
    private final Preferences preferences;

    public JavaPreferencesStore(Class<?> anchorClass) {
        this.preferences = Preferences.userNodeForPackage(anchorClass);
    }

    @Override
    public String get(String key, String fallback) {
        return preferences.get(key, fallback);
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
