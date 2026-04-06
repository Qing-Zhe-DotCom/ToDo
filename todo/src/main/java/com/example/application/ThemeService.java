package com.example.application;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.example.config.AppProperties;
import com.example.config.UserPreferencesStore;
import com.example.view.ScheduleCardStyleSupport;

public final class ThemeService {
    private static final String PREF_THEME_KEY = "todo.theme";
    private static final String PREF_IMPORTED_THEME_KEY = "todo.theme.imported.path";
    private static final String PREF_SCHEDULE_CARD_STYLE_KEY = "todo.schedule.card.style";
    private static final String PREF_TIMELINE_CARD_STYLE_KEY = "todo.timeline.card.style";

    private final UserPreferencesStore preferencesStore;
    private final AppProperties appProperties;
    private final Map<String, String> builtinThemes = createBuiltinThemeMap();
    private final List<String> scheduleCardStyles;

    private String currentTheme;
    private String importedThemeStylesheet;
    private String currentScheduleCardStyle;

    public ThemeService(
        UserPreferencesStore preferencesStore,
        AppProperties appProperties,
        List<String> scheduleCardStyles
    ) {
        this.preferencesStore = preferencesStore;
        this.appProperties = appProperties;
        this.scheduleCardStyles = List.copyOf(scheduleCardStyles);
        load();
    }

    public Map<String, String> getBuiltinThemes() {
        return builtinThemes;
    }

    public List<String> getScheduleCardStyles() {
        return scheduleCardStyles;
    }

    public String getCurrentTheme() {
        return currentTheme;
    }

    public String getCurrentScheduleCardStyle() {
        return currentScheduleCardStyle;
    }

    public void selectBuiltinTheme(String theme) {
        importedThemeStylesheet = null;
        currentTheme = builtinThemes.containsKey(theme) ? theme : appProperties.getDefaultTheme();
        save();
    }

    public void importTheme(Path path) {
        importedThemeStylesheet = path.toUri().toString();
        currentTheme = "imported";
        save();
    }

    public String getImportedThemeStylesheet() {
        return importedThemeStylesheet;
    }

    public void setScheduleCardStyle(String scheduleCardStyle) {
        String normalizedStyleId = ScheduleCardStyleSupport.normalizeStyleId(scheduleCardStyle);
        if (scheduleCardStyles.contains(normalizedStyleId)) {
            currentScheduleCardStyle = normalizedStyleId;
            save();
        }
    }

    public List<String> resolveStylesheets(Class<?> resourceAnchor) {
        List<String> stylesheets = new ArrayList<>();
        stylesheets.add(resolveResourceStylesheet(resourceAnchor, "/styles/base.css"));
        stylesheets.add(resolveResourceStylesheet(resourceAnchor, "/styles/light-theme.css"));

        switch (currentTheme) {
            case "mint":
                stylesheets.add(resolveResourceStylesheet(resourceAnchor, "/styles/mint-theme.css"));
                break;
            case "ocean":
                stylesheets.add(resolveResourceStylesheet(resourceAnchor, "/styles/ocean-theme.css"));
                break;
            case "sunset":
                stylesheets.add(resolveResourceStylesheet(resourceAnchor, "/styles/sunset-theme.css"));
                break;
            case "lavender":
                stylesheets.add(resolveResourceStylesheet(resourceAnchor, "/styles/lavender-theme.css"));
                break;
            case "forest":
                stylesheets.add(resolveResourceStylesheet(resourceAnchor, "/styles/forest-theme.css"));
                break;
            case "slate":
                stylesheets.add(resolveResourceStylesheet(resourceAnchor, "/styles/slate-theme.css"));
                break;
            case "macaron":
                stylesheets.add(resolveResourceStylesheet(resourceAnchor, "/styles/macaron-theme.css"));
                break;
            case "imported":
                if (importedThemeStylesheet != null && !importedThemeStylesheet.isBlank()) {
                    stylesheets.add(importedThemeStylesheet);
                }
                break;
            default:
                break;
        }
        return stylesheets;
    }

    private String resolveResourceStylesheet(Class<?> resourceAnchor, String resourcePath) {
        return resourceAnchor.getResource(resourcePath).toExternalForm();
    }

    private void load() {
        String defaultTheme = appProperties.getDefaultTheme();
        currentTheme = preferencesStore.get(PREF_THEME_KEY, defaultTheme);
        importedThemeStylesheet = blankToNull(preferencesStore.get(PREF_IMPORTED_THEME_KEY, ""));
        if (!builtinThemes.containsKey(currentTheme) && !"imported".equals(currentTheme)) {
            currentTheme = defaultTheme;
        }

        String defaultStyle = ScheduleCardStyleSupport.normalizeStyleId(appProperties.getDefaultScheduleCardStyle());
        currentScheduleCardStyle = ScheduleCardStyleSupport.normalizeStyleId(
            preferencesStore.get(
                PREF_SCHEDULE_CARD_STYLE_KEY,
                preferencesStore.get(PREF_TIMELINE_CARD_STYLE_KEY, defaultStyle)
            )
        );
        if (!scheduleCardStyles.contains(currentScheduleCardStyle)) {
            currentScheduleCardStyle = defaultStyle;
        }
    }

    private void save() {
        preferencesStore.put(PREF_THEME_KEY, currentTheme);
        if (importedThemeStylesheet == null || importedThemeStylesheet.isBlank()) {
            preferencesStore.remove(PREF_IMPORTED_THEME_KEY);
        } else {
            preferencesStore.put(PREF_IMPORTED_THEME_KEY, importedThemeStylesheet);
        }
        preferencesStore.put(PREF_SCHEDULE_CARD_STYLE_KEY, currentScheduleCardStyle);
        preferencesStore.put(PREF_TIMELINE_CARD_STYLE_KEY, currentScheduleCardStyle);
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private Map<String, String> createBuiltinThemeMap() {
        Map<String, String> themes = new LinkedHashMap<>();
        themes.put("light", "theme.light");
        themes.put("mint", "theme.mint");
        themes.put("ocean", "theme.ocean");
        themes.put("sunset", "theme.sunset");
        themes.put("lavender", "theme.lavender");
        themes.put("forest", "theme.forest");
        themes.put("slate", "theme.slate");
        themes.put("macaron", "theme.macaron");
        return themes;
    }
}
