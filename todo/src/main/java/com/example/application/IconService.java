package com.example.application;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

import com.example.config.UserPreferencesStore;

public final class IconService {
    static final String PREF_ICON_PACK_KEY = "todo.icon.pack";
    static final String PREF_ICON_THEME_BOUND_KEY = "todo.icon.theme-bound";

    private final UserPreferencesStore preferencesStore;
    private final List<Runnable> listeners = new CopyOnWriteArrayList<>();

    private ThemeFamily currentThemeFamily;
    private ThemeAppearance currentAppearance = ThemeAppearance.LIGHT;
    private IconPack currentIconPack;
    private IconPack persistedIconPack;
    private boolean themeBindingEnabled;
    private boolean persistedThemeBindingEnabled;

    public IconService(UserPreferencesStore preferencesStore, ThemeFamily initialThemeFamily) {
        this.preferencesStore = Objects.requireNonNull(preferencesStore, "preferencesStore");
        this.currentThemeFamily = initialThemeFamily != null ? initialThemeFamily : ThemeFamily.CLASSIC;
        load();
    }

    public IconPack getCurrentIconPack() {
        return currentIconPack;
    }

    public boolean isThemeBindingEnabled() {
        return themeBindingEnabled;
    }

    public void setThemeBindingEnabled(boolean enabled) {
        commitSelection(currentThemeFamily, enabled, currentIconPack);
    }

    public void selectIconPack(IconPack iconPack) {
        if (themeBindingEnabled) {
            return;
        }
        commitSelection(currentThemeFamily, false, iconPack);
    }

    public void syncThemeFamily(ThemeFamily family) {
        ThemeFamily resolvedFamily = family != null ? family : ThemeFamily.CLASSIC;
        IconPack previousPack = currentIconPack;
        currentThemeFamily = resolvedFamily;
        if (themeBindingEnabled) {
            currentIconPack = IconPack.boundToThemeFamily(resolvedFamily);
            persistedIconPack = currentIconPack;
            save();
        }
        notifyIfPackChanged(previousPack);
    }

    public void syncThemeAppearance(ThemeAppearance appearance) {
        ThemeAppearance resolved = appearance != null ? appearance : ThemeAppearance.LIGHT;
        if (resolved == currentAppearance) {
            return;
        }
        currentAppearance = resolved;
        notifyIconographyChanged();
    }

    public void previewSelection(ThemeFamily family, boolean bindingEnabled, IconPack iconPack) {
        ThemeFamily resolvedFamily = family != null ? family : currentThemeFamily;
        IconPack previousPack = currentIconPack;

        currentThemeFamily = resolvedFamily;
        themeBindingEnabled = bindingEnabled;
        currentIconPack = resolveEffectiveIconPack(resolvedFamily, bindingEnabled, iconPack);

        notifyIfPackChanged(previousPack);
    }

    public void commitSelection(ThemeFamily family, boolean bindingEnabled, IconPack iconPack) {
        previewSelection(family, bindingEnabled, iconPack);
        persistedThemeBindingEnabled = themeBindingEnabled;
        persistedIconPack = currentIconPack;
        save();
    }

    public void restorePersistedSelection() {
        previewSelection(currentThemeFamily, persistedThemeBindingEnabled, persistedIconPack);
    }

    public String resolveResourcePath(IconKey iconKey) {
        return resolveResourcePath(currentIconPack, iconKey);
    }

    public String resolveResourcePath(IconPack iconPack, IconKey iconKey) {
        IconPack resolvedPack = iconPack != null ? iconPack : IconPack.CLASSIC;
        IconKey resolvedKey = iconKey != null ? iconKey : IconKey.CALENDAR;

        for (String candidatePath : resolveCandidatePaths(resolvedPack, resolvedKey)) {
            if (resourceExists(candidatePath)) {
                return candidatePath;
            }
        }

        for (String candidatePath : resolveCandidatePaths(IconPack.CLASSIC, resolvedKey)) {
            if (resourceExists(candidatePath)) {
                return candidatePath;
            }
        }

        // Should always exist, but keep a hard fallback to avoid breaking the UI on packaging mistakes.
        return IconPack.CLASSIC.resolveResourcePath(resolvedKey);
    }

    public Runnable addChangeListener(Runnable listener) {
        Objects.requireNonNull(listener, "listener");
        listeners.add(listener);
        return () -> listeners.remove(listener);
    }

    public List<IconPack> getAvailableIconPacks() {
        return new ArrayList<>(IconPack.supportedValues());
    }

    private void load() {
        persistedThemeBindingEnabled = Boolean.parseBoolean(
            preferencesStore.get(PREF_ICON_THEME_BOUND_KEY, Boolean.TRUE.toString())
        );
        persistedIconPack = resolveEffectiveIconPack(
            currentThemeFamily,
            persistedThemeBindingEnabled,
            IconPack.fromPreference(preferencesStore.get(PREF_ICON_PACK_KEY, null))
        );
        themeBindingEnabled = persistedThemeBindingEnabled;
        currentIconPack = persistedIconPack;
    }

    private void save() {
        preferencesStore.put(PREF_ICON_THEME_BOUND_KEY, Boolean.toString(themeBindingEnabled));
        preferencesStore.put(PREF_ICON_PACK_KEY, currentIconPack.getId());
    }

    private IconPack resolveEffectiveIconPack(ThemeFamily family, boolean bindingEnabled, IconPack iconPack) {
        if (bindingEnabled) {
            return IconPack.boundToThemeFamily(family);
        }
        return iconPack != null ? iconPack : IconPack.boundToThemeFamily(family);
    }

    private boolean resourceExists(String resourcePath) {
        URL resource = IconService.class.getResource(resourcePath);
        return resource != null;
    }

    private List<String> resolveCandidatePaths(IconPack pack, IconKey iconKey) {
        List<String> candidates = new ArrayList<>(2);
        if (currentAppearance == ThemeAppearance.DARK) {
            candidates.add("/icons/" + pack.getId() + "_dark/" + iconKey.getFileName());
        }
        candidates.add(pack.resolveResourcePath(iconKey));
        return candidates;
    }

    private void notifyIfPackChanged(IconPack previousPack) {
        if (previousPack == currentIconPack) {
            return;
        }
        notifyIconographyChanged();
    }

    private void notifyIconographyChanged() {
        for (Runnable listener : listeners) {
            listener.run();
        }
    }
}
