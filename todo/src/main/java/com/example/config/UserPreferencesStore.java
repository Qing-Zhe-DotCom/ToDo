package com.example.config;

public interface UserPreferencesStore {
    String get(String key, String fallback);

    void put(String key, String value);

    void remove(String key);
}
