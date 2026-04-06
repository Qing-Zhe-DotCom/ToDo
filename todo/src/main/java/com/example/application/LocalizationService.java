package com.example.application;

import java.text.MessageFormat;
import java.time.DayOfWeek;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.time.temporal.TemporalAccessor;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.Objects;
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

    private AppLanguage preferredLanguage;

    public LocalizationService(UserPreferencesStore preferencesStore) {
        this.preferencesStore = Objects.requireNonNull(preferencesStore, "preferencesStore");
        this.preferredLanguage = AppLanguage.fromPreference(preferencesStore.get(PREF_LANGUAGE_KEY, AppLanguage.detectDefault().getId()));
        this.activeLanguage = preferredLanguage;
        this.activeBundle = loadBundle(activeLanguage);
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
        ResourceBundle bundle = language == activeLanguage ? activeBundle : loadBundle(language);
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
        return ResourceBundle.getBundle(BUNDLE_BASE_NAME, language.getLocale());
    }

    private String format(String pattern, Locale locale, Object... args) {
        if (args == null || args.length == 0) {
            return pattern;
        }
        MessageFormat messageFormat = new MessageFormat(pattern, locale);
        return messageFormat.format(args);
    }
}
