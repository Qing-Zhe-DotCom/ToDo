package com.example.application;

import java.util.Objects;
import java.util.function.Predicate;

import com.example.config.UserPreferencesStore;

public final class IconThemeService {
    static final String PREF_ICON_THEME_KEY = "todo.icon.theme";
    static final String PREF_ICON_BIND_TO_THEME_KEY = "todo.icon.bind-to-theme";

    private static final String BASE_PATH = "/icons/themes/";

    private final UserPreferencesStore preferencesStore;

    private boolean bindToTheme;
    private IconTheme selectedIconTheme;

    public IconThemeService(UserPreferencesStore preferencesStore) {
        this.preferencesStore = Objects.requireNonNull(preferencesStore, "preferencesStore");
        load();
    }

    public boolean isBoundToTheme() {
        return bindToTheme;
    }

    public IconTheme getSelectedIconTheme() {
        return selectedIconTheme;
    }

    public IconTheme resolveIconTheme(ThemeFamily currentThemeFamily) {
        return resolveIconTheme(bindToTheme, selectedIconTheme, currentThemeFamily);
    }

    public IconTheme resolveIconTheme(boolean bindIconsToTheme, IconTheme manualIconTheme, ThemeFamily currentThemeFamily) {
        if (bindIconsToTheme) {
            return IconTheme.fromThemeFamily(currentThemeFamily);
        }
        return manualIconTheme != null ? manualIconTheme : IconTheme.CLASSIC;
    }

    public void setBindToTheme(boolean bindIconsToTheme, ThemeFamily currentThemeFamily) {
        if (bindToTheme && !bindIconsToTheme) {
            selectedIconTheme = resolveIconTheme(currentThemeFamily);
        }
        bindToTheme = bindIconsToTheme;
        save();
    }

    public void selectIconTheme(IconTheme iconTheme) {
        selectedIconTheme = iconTheme != null ? iconTheme : IconTheme.CLASSIC;
        save();
    }

    public void applySelection(boolean bindIconsToTheme, IconTheme manualIconTheme) {
        bindToTheme = bindIconsToTheme;
        selectedIconTheme = manualIconTheme != null ? manualIconTheme : IconTheme.CLASSIC;
        save();
    }

    public String resolveIconResource(Class<?> resourceAnchor, IconTheme iconTheme, IconKey iconKey) {
        Objects.requireNonNull(resourceAnchor, "resourceAnchor");
        return resolveIconResource(path -> resourceAnchor.getResource(path) != null, iconTheme, iconKey);
    }

    String resolveIconResource(Predicate<String> resourceExists, IconTheme iconTheme, IconKey iconKey) {
        Objects.requireNonNull(resourceExists, "resourceExists");
        Objects.requireNonNull(iconKey, "iconKey");

        IconTheme resolvedTheme = iconTheme != null ? iconTheme : IconTheme.CLASSIC;
        String candidate = themedPath(resolvedTheme, iconKey);
        if (resourceExists.test(candidate)) {
            return candidate;
        }

        String fallback = themedPath(IconTheme.CLASSIC, iconKey);
        if (resourceExists.test(fallback)) {
            return fallback;
        }
        return null;
    }

    private void load() {
        bindToTheme = Boolean.parseBoolean(preferencesStore.get(PREF_ICON_BIND_TO_THEME_KEY, Boolean.TRUE.toString()));
        selectedIconTheme = IconTheme.fromPreference(preferencesStore.get(PREF_ICON_THEME_KEY, IconTheme.CLASSIC.getId()));
    }

    private void save() {
        preferencesStore.put(PREF_ICON_BIND_TO_THEME_KEY, Boolean.toString(bindToTheme));
        preferencesStore.put(PREF_ICON_THEME_KEY, selectedIconTheme.getId());
    }

    private String themedPath(IconTheme iconTheme, IconKey iconKey) {
        return BASE_PATH + iconTheme.getId() + "/" + iconKey.getFileName();
    }
}
