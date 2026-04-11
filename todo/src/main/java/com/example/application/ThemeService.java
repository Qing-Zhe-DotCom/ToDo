package com.example.application;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.example.config.AppProperties;
import com.example.config.UserPreferencesStore;
import com.example.view.ScheduleCardStyleSupport;

public final class ThemeService {
    static final String PREF_THEME_FAMILY_KEY = "todo.theme.family";
    static final String PREF_THEME_APPEARANCE_KEY = "todo.theme.appearance";
    static final String PREF_THEME_CLASSIC_PALETTE_KEY = "todo.theme.classic.palette";

    static final String LEGACY_PREF_THEME_KEY = "todo.theme";
    static final String LEGACY_PREF_IMPORTED_THEME_KEY = "todo.theme.imported.path";
    static final String LEGACY_PREF_SCHEDULE_CARD_STYLE_KEY = "todo.schedule.card.style";
    static final String LEGACY_PREF_TIMELINE_CARD_STYLE_KEY = "todo.timeline.card.style";

    private final UserPreferencesStore preferencesStore;
    private final ExperimentalFeaturesService experimentalFeaturesService;
    private final ThemeFamily defaultThemeFamily;
    private final ThemeAppearance defaultAppearance;
    private final ClassicThemePalette defaultClassicPalette;

    private ThemeFamily currentThemeFamily;
    private ThemeAppearance currentAppearance;
    private ClassicThemePalette currentClassicPalette;

    public ThemeService(
        UserPreferencesStore preferencesStore,
        AppProperties appProperties,
        ExperimentalFeaturesService experimentalFeaturesService
    ) {
        this.preferencesStore = Objects.requireNonNull(preferencesStore, "preferencesStore");
        Objects.requireNonNull(appProperties, "appProperties");
        this.experimentalFeaturesService = Objects.requireNonNull(experimentalFeaturesService, "experimentalFeaturesService");
        this.defaultThemeFamily = ThemeFamily.fromPreference(appProperties.getDefaultThemeFamily());
        this.defaultAppearance = ThemeAppearance.fromPreference(appProperties.getDefaultThemeAppearance());
        this.defaultClassicPalette = ClassicThemePalette.fromPreference(appProperties.getDefaultClassicPalette());
        load();
    }

    public List<ThemeFamily> getThemeFamilies() {
        return ThemeFamily.supportedValues();
    }

    public List<ClassicThemePalette> getClassicPalettes() {
        return ClassicThemePalette.supportedValues();
    }

    public ThemeFamily getCurrentThemeFamily() {
        return currentThemeFamily;
    }

    public ThemeAppearance getCurrentAppearance() {
        return currentAppearance;
    }

    public ClassicThemePalette getCurrentClassicPalette() {
        return currentClassicPalette;
    }

    public String getCurrentScheduleCardStyle() {
        return currentThemeFamily.getBoundScheduleCardStyle();
    }

    public boolean supportsClassicPalette(ThemeFamily family) {
        ThemeFamily resolved = family != null ? family : currentThemeFamily;
        return resolved.supportsClassicPalette();
    }

    public void selectTheme(ThemeFamily family, ThemeAppearance appearance, ClassicThemePalette classicPalette) {
        currentThemeFamily = family != null ? family : defaultThemeFamily;
        currentAppearance = appearance != null ? appearance : defaultAppearance;
        currentClassicPalette = classicPalette != null ? classicPalette : defaultClassicPalette;
        enforceThemeAvailability();
        save();
    }

    public void selectThemeFamily(ThemeFamily family) {
        selectTheme(family, currentAppearance, currentClassicPalette);
    }

    public void selectClassicPalette(ClassicThemePalette classicPalette) {
        selectTheme(currentThemeFamily, currentAppearance, classicPalette);
    }

    public List<String> resolveStylesheets(Class<?> resourceAnchor) {
        return resolveStylesheets(resourceAnchor, currentThemeFamily, currentAppearance, currentClassicPalette);
    }

    public List<String> resolveStylesheets(
        Class<?> resourceAnchor,
        ThemeFamily family,
        ThemeAppearance appearance,
        ClassicThemePalette classicPalette
    ) {
        ThemeFamily resolvedFamily = family != null ? family : defaultThemeFamily;
        ThemeAppearance resolvedAppearance = appearance != null ? appearance : defaultAppearance;
        ClassicThemePalette resolvedPalette = classicPalette != null ? classicPalette : defaultClassicPalette;

        List<String> stylesheets = new ArrayList<>();
        stylesheets.add(resolveRequiredStylesheet(resourceAnchor, "/styles/base.css"));
        String tokenStylesheet = resolvedAppearance == ThemeAppearance.DARK ? "/styles/dark-theme.css" : "/styles/light-theme.css";
        stylesheets.add(resolveRequiredStylesheet(resourceAnchor, tokenStylesheet));

        String familyStylesheet = resolveOptionalStylesheet(
            resourceAnchor,
            resolvedFamily.resolveStylesheetPath(resolvedAppearance)
        );
        if (familyStylesheet == null && resolvedAppearance != ThemeAppearance.LIGHT) {
            familyStylesheet = resolveOptionalStylesheet(
                resourceAnchor,
                resolvedFamily.resolveStylesheetPath(ThemeAppearance.LIGHT)
            );
        }
        if (familyStylesheet != null) {
            stylesheets.add(familyStylesheet);
        }

        String paletteOverlayPath = resolvedPalette.resolveOverlayStylesheetPath(resolvedAppearance);
        if (resolvedFamily.supportsClassicPalette() && paletteOverlayPath != null) {
            String paletteOverlay = resolveOptionalStylesheet(resourceAnchor, paletteOverlayPath);
            if (paletteOverlay != null) {
                stylesheets.add(paletteOverlay);
            }
        }
        return stylesheets;
    }

    private void load() {
        String storedFamily = blankToNull(preferencesStore.get(PREF_THEME_FAMILY_KEY, null));
        String storedAppearance = blankToNull(preferencesStore.get(PREF_THEME_APPEARANCE_KEY, null));
        String storedClassicPalette = blankToNull(preferencesStore.get(PREF_THEME_CLASSIC_PALETTE_KEY, null));

        if (storedFamily != null || storedAppearance != null || storedClassicPalette != null) {
            currentThemeFamily = storedFamily != null ? ThemeFamily.fromPreference(storedFamily) : defaultThemeFamily;
            currentAppearance = storedAppearance != null ? ThemeAppearance.fromPreference(storedAppearance) : defaultAppearance;
            currentClassicPalette = storedClassicPalette != null
                ? ClassicThemePalette.fromPreference(storedClassicPalette)
                : defaultClassicPalette;
            ThemeFamily resolvedFamily = currentThemeFamily;
            ThemeAppearance resolvedAppearance = currentAppearance;
            ClassicThemePalette resolvedPalette = currentClassicPalette;
            enforceThemeAvailability();
            if (resolvedFamily != currentThemeFamily
                || resolvedAppearance != currentAppearance
                || resolvedPalette != currentClassicPalette) {
                save();
            }
            return;
        }

        String defaultLegacyStyle = defaultThemeFamily.getBoundScheduleCardStyle();
        String legacyStyle = ScheduleCardStyleSupport.normalizeStyleId(
            preferencesStore.get(
                LEGACY_PREF_SCHEDULE_CARD_STYLE_KEY,
                preferencesStore.get(LEGACY_PREF_TIMELINE_CARD_STYLE_KEY, defaultLegacyStyle)
            )
        );

        currentThemeFamily = ThemeFamily.fromBoundCardStyle(legacyStyle);
        currentAppearance = defaultAppearance;
        currentClassicPalette = currentThemeFamily.supportsClassicPalette()
            ? ClassicThemePalette.fromPreference(blankToNull(preferencesStore.get(LEGACY_PREF_THEME_KEY, null)))
            : defaultClassicPalette;
        enforceThemeAvailability();
        save();
    }

    private void enforceThemeAvailability() {
        if (currentThemeFamily == ThemeFamily.MACARON && !experimentalFeaturesService.isLabsEnabled()) {
            currentThemeFamily = ThemeFamily.CLASSIC;
            currentAppearance = defaultAppearance;
            currentClassicPalette = defaultClassicPalette;
            return;
        }
        if (!currentThemeFamily.supportsClassicPalette() && currentClassicPalette == null) {
            currentClassicPalette = defaultClassicPalette;
        }
    }

    private void save() {
        preferencesStore.put(PREF_THEME_FAMILY_KEY, currentThemeFamily.getId());
        preferencesStore.put(PREF_THEME_APPEARANCE_KEY, currentAppearance.getId());
        preferencesStore.put(PREF_THEME_CLASSIC_PALETTE_KEY, currentClassicPalette.getId());
        preferencesStore.remove(LEGACY_PREF_IMPORTED_THEME_KEY);
    }

    private String resolveRequiredStylesheet(Class<?> resourceAnchor, String resourcePath) {
        URL resource = resourceAnchor.getResource(resourcePath);
        if (resource == null) {
            throw new IllegalStateException("Missing required stylesheet: " + resourcePath);
        }
        return resource.toExternalForm();
    }

    private String resolveOptionalStylesheet(Class<?> resourceAnchor, String resourcePath) {
        if (resourcePath == null || resourcePath.isBlank()) {
            return null;
        }
        URL resource = resourceAnchor.getResource(resourcePath);
        return resource != null ? resource.toExternalForm() : null;
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
