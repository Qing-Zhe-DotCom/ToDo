package com.example.application;

import java.util.Objects;

import com.example.config.UserPreferencesStore;

/**
 * A small preference-backed bucket for "custom options" that may grow over time.
 * Keep this separate from Theme/Icon services to avoid coupling unrelated preferences.
 */
public final class CustomOptionsService {
    public static final String PREF_TIME_TEXT_INPUT_ENABLED_KEY = "todo.custom.time-text-input.enabled";

    private final UserPreferencesStore preferencesStore;

    public CustomOptionsService(UserPreferencesStore preferencesStore) {
        this.preferencesStore = Objects.requireNonNull(preferencesStore, "preferencesStore");
    }

    public boolean isTimeTextInputEnabled() {
        return Boolean.parseBoolean(preferencesStore.get(PREF_TIME_TEXT_INPUT_ENABLED_KEY, Boolean.FALSE.toString()));
    }

    public void setTimeTextInputEnabled(boolean enabled) {
        preferencesStore.put(PREF_TIME_TEXT_INPUT_ENABLED_KEY, Boolean.toString(enabled));
    }
}

