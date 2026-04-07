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
    void languageLabelsResolveForAllSupportedLanguages() {
        LocalizationService simplified = serviceFor(AppLanguage.SIMPLIFIED_CHINESE);
        LocalizationService traditional = serviceFor(AppLanguage.TRADITIONAL_CHINESE);
        LocalizationService english = serviceFor(AppLanguage.ENGLISH);

        assertEquals("简体中文", simplified.languageLabel(AppLanguage.SIMPLIFIED_CHINESE));
        assertEquals("繁體中文", traditional.languageLabel(AppLanguage.TRADITIONAL_CHINESE));
        assertEquals("English", english.languageLabel(AppLanguage.ENGLISH));
    }

    @Test
    void weekdayAndDateFormattingFollowLanguage() {
        LocalDate sampleDate = LocalDate.of(2026, 4, 7);

        assertEquals("4月7日", serviceFor(AppLanguage.SIMPLIFIED_CHINESE).format("format.info.daySummary", sampleDate));
        assertEquals("4月7日", serviceFor(AppLanguage.TRADITIONAL_CHINESE).format("format.info.daySummary", sampleDate));
        assertEquals("4/7", serviceFor(AppLanguage.ENGLISH).format("format.info.daySummary", sampleDate));

        assertEquals("周二", serviceFor(AppLanguage.SIMPLIFIED_CHINESE).weekdayShort(DayOfWeek.TUESDAY));
        assertEquals("週二", serviceFor(AppLanguage.TRADITIONAL_CHINESE).weekdayShort(DayOfWeek.TUESDAY));
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

        assertTrue(zhCn.contains("每 2 周"));
        assertTrue(zhTw.contains("每 2 週"));
        assertTrue(en.contains("Every 2 weeks"));
    }

    @Test
    void generalSettingsTabLabelIsLocalized() {
        assertEquals("\u901a\u7528", serviceFor(AppLanguage.SIMPLIFIED_CHINESE).text("settings.tab.details"));
        assertEquals("\u901a\u7528", serviceFor(AppLanguage.TRADITIONAL_CHINESE).text("settings.tab.details"));
        assertEquals("General", serviceFor(AppLanguage.ENGLISH).text("settings.tab.details"));
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
