package com.example.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.example.config.UserPreferencesStore;
import com.example.model.RecurrenceRule;

class LocalizationServiceTest {

    @Test
    void invalidLanguagePreferenceFallsBackToLocaleDefault() {
        Locale previous = Locale.getDefault();
        try {
            Locale.setDefault(Locale.ENGLISH);
            LocalizationService service = new LocalizationService(new MapPreferencesStore(Map.of("todo.language", "invalid-value")));
            assertEquals(AppLanguage.ENGLISH, service.getActiveLanguage());
        } finally {
            Locale.setDefault(previous);
        }
    }

    @Test
    void installerDefaultLanguageAppliesWhenNoUserPreferenceExists() {
        Locale previous = Locale.getDefault();
        try {
            Locale.setDefault(Locale.ENGLISH);
            LocalizationService service = new LocalizationService(new MapPreferencesStore(Map.of()), "zh-TW");
            assertEquals(AppLanguage.TRADITIONAL_CHINESE, service.getActiveLanguage());
        } finally {
            Locale.setDefault(previous);
        }
    }

    @Test
    void userPreferenceOverridesInstallerDefaultLanguage() {
        LocalizationService service = new LocalizationService(
            new MapPreferencesStore(Map.of("todo.language", AppLanguage.ENGLISH.getId())),
            "zh-CN"
        );

        assertEquals(AppLanguage.ENGLISH, service.getActiveLanguage());
    }

    @Test
    void invalidInstallerDefaultLanguageFallsBackToLocaleDefault() {
        Locale previous = Locale.getDefault();
        try {
            Locale.setDefault(Locale.ENGLISH);
            LocalizationService service = new LocalizationService(new MapPreferencesStore(Map.of()), "invalid-value");
            assertEquals(AppLanguage.ENGLISH, service.getActiveLanguage());
        } finally {
            Locale.setDefault(previous);
        }
    }

    @Test
    void languageLabelsResolveForAllSupportedLanguages() {
        LocalizationService simplified = serviceFor(AppLanguage.SIMPLIFIED_CHINESE);
        LocalizationService traditional = serviceFor(AppLanguage.TRADITIONAL_CHINESE);
        LocalizationService english = serviceFor(AppLanguage.ENGLISH);

        assertEquals("\u7b80\u4f53\u4e2d\u6587", simplified.languageLabel(AppLanguage.SIMPLIFIED_CHINESE));
        assertEquals("\u7e41\u9ad4\u4e2d\u6587", traditional.languageLabel(AppLanguage.TRADITIONAL_CHINESE));
        assertEquals("English", english.languageLabel(AppLanguage.ENGLISH));
    }

    @Test
    void weekdayAndDateFormattingFollowLanguage() {
        LocalDate sampleDate = LocalDate.of(2026, 4, 7);

        assertEquals("4\u67087\u65e5", serviceFor(AppLanguage.SIMPLIFIED_CHINESE).format("format.info.daySummary", sampleDate));
        assertEquals("4\u67087\u65e5", serviceFor(AppLanguage.TRADITIONAL_CHINESE).format("format.info.daySummary", sampleDate));
        assertEquals("4/7", serviceFor(AppLanguage.ENGLISH).format("format.info.daySummary", sampleDate));

        assertEquals("\u5468\u4e8c", serviceFor(AppLanguage.SIMPLIFIED_CHINESE).weekdayShort(DayOfWeek.TUESDAY));
        assertEquals("\u9031\u4e8c", serviceFor(AppLanguage.TRADITIONAL_CHINESE).weekdayShort(DayOfWeek.TUESDAY));
        assertEquals("Tue", serviceFor(AppLanguage.ENGLISH).weekdayShort(DayOfWeek.TUESDAY));
    }

    @Test
    void recurrenceSummaryIsLocalizedForAllLanguages() {
        RecurrenceRule rule = new RecurrenceRule();
        rule.setActive(true);
        rule.setFrequency(RecurrenceRule.FREQ_WEEKLY);
        rule.setInterval(2);
        rule.setByDays(java.util.List.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY));

        String zhCn = RecurrenceSummaryFormatter.describe(rule, serviceFor(AppLanguage.SIMPLIFIED_CHINESE));
        String zhTw = RecurrenceSummaryFormatter.describe(rule, serviceFor(AppLanguage.TRADITIONAL_CHINESE));
        String en = RecurrenceSummaryFormatter.describe(rule, serviceFor(AppLanguage.ENGLISH));

        assertTrue(zhCn.contains("\u6bcf 2 \u5468"));
        assertTrue(zhTw.contains("\u6bcf 2 \u9031"));
        assertTrue(en.contains("Every 2 weeks"));
    }

    @Test
    void settingsTabsAndThemeFamilyLabelsAreLocalized() {
        LocalizationService simplified = serviceFor(AppLanguage.SIMPLIFIED_CHINESE);
        LocalizationService traditional = serviceFor(AppLanguage.TRADITIONAL_CHINESE);
        LocalizationService english = serviceFor(AppLanguage.ENGLISH);

        assertEquals("\u901a\u7528", simplified.text("settings.tab.details"));
        assertEquals("\u901a\u7528", traditional.text("settings.tab.details"));
        assertEquals("General", english.text("settings.tab.details"));

        assertEquals("\u4e2a\u6027\u5316", simplified.text("settings.tab.personalization"));
        assertEquals("\u500b\u4eba\u5316", traditional.text("settings.tab.personalization"));
        assertEquals("Personalization", english.text("settings.tab.personalization"));

        assertEquals("\u7ecf\u5178", simplified.themeFamilyLabel(ThemeFamily.CLASSIC));
        assertEquals("\u9a6c\u5361\u9f99\u73bb\u7483\u98ce", simplified.themeFamilyLabel(ThemeFamily.MACARON));
        assertEquals("\u73fe\u4ee3\u9ad8\u7d1a\u6975\u7c21\u98a8", traditional.themeFamilyLabel(ThemeFamily.MODERN_MINIMAL));
        assertEquals("\u99ac\u5361\u9f8d\u73bb\u7483\u98a8", traditional.themeFamilyLabel(ThemeFamily.MACARON));
        assertEquals("Material You", english.themeFamilyLabel(ThemeFamily.MATERIAL_YOU));
        assertEquals("Macaron Glass", english.themeFamilyLabel(ThemeFamily.MACARON));
    }

    private LocalizationService serviceFor(AppLanguage language) {
        return new LocalizationService(new MapPreferencesStore(Map.of("todo.language", language.getId())));
    }

    private static final class MapPreferencesStore implements UserPreferencesStore {
        private final Map<String, String> values = new HashMap<>();

        private MapPreferencesStore(Map<String, String> seed) {
            if (seed != null) {
                values.putAll(seed);
            }
        }

        @Override
        public String get(String key, String fallback) {
            return values.getOrDefault(key, fallback);
        }

        @Override
        public void put(String key, String value) {
            values.put(key, value);
        }

        @Override
        public void remove(String key) {
            values.remove(key);
        }
    }
}
