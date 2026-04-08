package com.example.application;

import java.util.Objects;

import com.example.config.UserPreferencesStore;

public final class ExperimentalFeaturesService {
    public static final String PREF_LABS_ENABLED_KEY = "todo.experimental.labs-enabled";

    private final UserPreferencesStore preferencesStore;

    public ExperimentalFeaturesService(UserPreferencesStore preferencesStore) {
        this.preferencesStore = Objects.requireNonNull(preferencesStore, "preferencesStore");
    }

    public boolean isLabsEnabled() {
        return Boolean.parseBoolean(preferencesStore.get(PREF_LABS_ENABLED_KEY, Boolean.FALSE.toString()));
    }

    public void setLabsEnabled(boolean enabled) {
        preferencesStore.put(PREF_LABS_ENABLED_KEY, Boolean.toString(enabled));
    }
}
