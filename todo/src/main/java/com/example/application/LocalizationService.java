package com.example.application;

import java.text.MessageFormat;
import java.time.DayOfWeek;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.time.temporal.TemporalAccessor;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.Objects;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;

import com.example.config.UserPreferencesStore;
import com.example.model.Schedule;
import com.example.view.ScheduleCardStyleSupport;

public final class LocalizationService {
    private static final String PREF_LANGUAGE_KEY = "todo.language";
    private static final String BUNDLE_BASE_NAME = "i18n.messages";

    private final UserPreferencesStore preferencesStore;
    private final AppLanguage activeLanguage;
    private final ResourceBundle activeBundle;
    private final Map<AppLanguage, ResourceBundle> bundleCache = new EnumMap<>(AppLanguage.class);

    private AppLanguage preferredLanguage;

    public LocalizationService(UserPreferencesStore preferencesStore) {
        this.preferencesStore = Objects.requireNonNull(preferencesStore, "preferencesStore");
        this.preferredLanguage = AppLanguage.fromPreference(preferencesStore.get(PREF_LANGUAGE_KEY, AppLanguage.detectDefault().getId()));
        this.activeLanguage = preferredLanguage;
        this.activeBundle = loadBundle(activeLanguage);
        this.bundleCache.put(activeLanguage, activeBundle);
    }

    public AppLanguage getActiveLanguage() {
        return activeLanguage;
    }

    public AppLanguage getPreferredLanguage() {
        return preferredLanguage;
    }

    public void saveLanguagePreference(AppLanguage language) {
        AppLanguage resolved = language != null ? language : AppLanguage.detectDefault();
        preferredLanguage = resolved;
        preferencesStore.put(PREF_LANGUAGE_KEY, resolved.getId());
    }

    public String text(String key, Object... args) {
        return text(activeLanguage, key, args);
    }

    public String text(AppLanguage language, String key, Object... args) {
        ResourceBundle bundle = bundleCache.computeIfAbsent(language, this::loadBundle);
        return format(bundle.getString(key), language.getLocale(), args);
    }

    public String optionalText(String key, String fallback, Object... args) {
        try {
            return text(key, args);
        } catch (MissingResourceException exception) {
            return fallback;
        }
    }

    public Locale getLocale() {
        return activeLanguage.getLocale();
    }

    public DateTimeFormatter formatter(String patternKey) {
        return formatter(activeLanguage, patternKey);
    }

    public DateTimeFormatter formatter(AppLanguage language, String patternKey) {
        return DateTimeFormatter.ofPattern(text(language, patternKey), language.getLocale());
    }

    public String format(String patternKey, TemporalAccessor value) {
        return format(activeLanguage, patternKey, value);
    }

    public String format(AppLanguage language, String patternKey, TemporalAccessor value) {
        return value == null ? "" : formatter(language, patternKey).format(value);
    }

    public String weekdayShort(DayOfWeek dayOfWeek) {
        return weekdayShort(activeLanguage, dayOfWeek);
    }

    public String weekdayShort(AppLanguage language, DayOfWeek dayOfWeek) {
        return dayOfWeek.getDisplayName(TextStyle.SHORT, language.getLocale());
    }

    public String weekdayNarrow(DayOfWeek dayOfWeek) {
        return weekdayNarrow(activeLanguage, dayOfWeek);
    }

    public String weekdayNarrow(AppLanguage language, DayOfWeek dayOfWeek) {
        return dayOfWeek.getDisplayName(TextStyle.NARROW, language.getLocale());
    }

    public String languageLabel(AppLanguage language) {
        return text(language.getLabelKey());
    }

    public String fontWeightLabel(AppFontWeight fontWeight) {
        return text(fontWeight.getLabelKey());
    }

    public String themeLabel(String themeId) {
        return text("theme." + themeId);
    }

    public String scheduleCardStyleLabel(String styleId) {
        return text(ScheduleCardStyleSupport.getLabelKey(styleId));
    }

    public String priorityLabel(String priority) {
        if (Schedule.PRIORITY_HIGH.equals(priority)) {
            return text("priority.high");
        }
        if (Schedule.PRIORITY_LOW.equals(priority)) {
            return text("priority.low");
        }
        return text("priority.medium");
    }

    public String categoryLabel(String category) {
        return Schedule.isDefaultCategory(category) ? text("category.default") : (category == null ? "" : category);
    }

    private ResourceBundle loadBundle(AppLanguage language) {
        Locale locale = language.getLocale();
        Map<String, Object> entries = new LinkedHashMap<>();
        boolean loadedAny = false;

        for (String resourceName : bundleResourceNames(locale)) {
            Map<String, Object> resourceEntries = loadBundleEntries(resourceName);
            if (resourceEntries.isEmpty()) {
                continue;
            }
            loadedAny = true;
            entries.putAll(resourceEntries);
        }

        if (!loadedAny) {
            throw new MissingResourceException(
                "Can't find bundle for base name " + BUNDLE_BASE_NAME + ", locale " + locale,
                LocalizationService.class.getName(),
                BUNDLE_BASE_NAME
            );
        }

        return new MapResourceBundle(entries);
    }

    private List<String> bundleResourceNames(Locale locale) {
        List<String> resourceNames = new ArrayList<>();
        resourceNames.add(bundleResourceName(""));

        String language = locale.getLanguage();
        if (language.isEmpty()) {
            return resourceNames;
        }

        LinkedHashSet<String> suffixes = new LinkedHashSet<>();
        String script = locale.getScript();
        String country = locale.getCountry();
        String variant = locale.getVariant();

        suffixes.add("_" + language);

        if (!script.isEmpty()) {
            suffixes.add("_" + language + "_" + script);
        }
        if (!country.isEmpty()) {
            suffixes.add("_" + language + "_" + country);
        }
        if (!script.isEmpty() && !country.isEmpty()) {
            suffixes.add("_" + language + "_" + script + "_" + country);
        }
        if (!variant.isEmpty() && !country.isEmpty()) {
            suffixes.add("_" + language + "_" + country + "_" + variant);
        }
        if (!variant.isEmpty() && !script.isEmpty() && !country.isEmpty()) {
            suffixes.add("_" + language + "_" + script + "_" + country + "_" + variant);
        }

        for (String suffix : suffixes) {
            resourceNames.add(bundleResourceName(suffix));
        }
        return resourceNames;
    }

    private String bundleResourceName(String suffix) {
        return BUNDLE_BASE_NAME.replace('.', '/') + suffix + ".properties";
    }

    private Map<String, Object> loadBundleEntries(String resourceName) {
        try (InputStream stream = LocalizationService.class.getModule().getResourceAsStream(resourceName)) {
            if (stream == null) {
                return Collections.emptyMap();
            }

            InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8);
            PropertyResourceBundle bundle = new PropertyResourceBundle(reader);
            Map<String, Object> entries = new LinkedHashMap<>();
            for (String key : bundle.keySet()) {
                entries.put(key, bundle.getObject(key));
            }
            return entries;
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to load localization bundle resource: " + resourceName, exception);
        }
    }

    private String format(String pattern, Locale locale, Object... args) {
        if (args == null || args.length == 0) {
            return pattern;
        }
        MessageFormat messageFormat = new MessageFormat(pattern, locale);
        return messageFormat.format(args);
    }

    private static final class MapResourceBundle extends ResourceBundle {
        private final Map<String, Object> entries;

        private MapResourceBundle(Map<String, Object> entries) {
            this.entries = Map.copyOf(entries);
        }

        @Override
        protected Object handleGetObject(String key) {
            return entries.get(key);
        }

        @Override
        public Enumeration<String> getKeys() {
            return Collections.enumeration(entries.keySet());
        }
    }
}
