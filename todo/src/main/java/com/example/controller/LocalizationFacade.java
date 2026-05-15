package com.example.controller;

import java.time.DayOfWeek;
import java.time.temporal.TemporalAccessor;
import com.example.application.ClassicThemePalette;
import com.example.application.IconPack;
import com.example.application.LocalizationService;
import com.example.application.RecurrenceSummaryFormatter;
import com.example.application.ThemeFamily;
import com.example.model.RecurrenceRule;

public final class LocalizationFacade {
    private final LocalizationService localizationService;

    public LocalizationFacade(LocalizationService localizationService) {
        this.localizationService = localizationService;
    }

    public String text(String key, Object... args) {
        return localizationService.text(key, args);
    }

    public String format(String patternKey, TemporalAccessor value) {
        return localizationService.format(patternKey, value);
    }

    public String themeFamilyDisplayName(ThemeFamily family) {
        return localizationService.themeFamilyLabel(family);
    }

    public String classicPaletteDisplayName(ClassicThemePalette palette) {
        return localizationService.classicPaletteLabel(palette);
    }

    public String iconPackDisplayName(IconPack iconPack) {
        return localizationService.iconPackLabel(iconPack);
    }

    public String currentThemeDisplayName(ThemeFamily family, ClassicThemePalette palette) {
        ThemeFamily resolvedFamily = family != null ? family : ThemeFamily.CLASSIC;
        if (resolvedFamily.supportsClassicPalette()) {
            return text(
                "settings.current.theme.classicValue",
                themeFamilyDisplayName(resolvedFamily),
                classicPaletteDisplayName(palette)
            );
        }
        return themeFamilyDisplayName(resolvedFamily);
    }

    public String currentIconDisplayName(IconPack iconPack, boolean bindingEnabled) {
        IconPack resolvedPack = iconPack != null ? iconPack : IconPack.CLASSIC;
        return bindingEnabled
            ? text("settings.current.icon.boundValue", iconPackDisplayName(resolvedPack))
            : text("settings.current.icon.unboundValue", iconPackDisplayName(resolvedPack));
    }

    public String scheduleCardStyleDisplayName(String styleId) {
        return localizationService.scheduleCardStyleLabel(styleId);
    }

    public String priorityDisplayName(String priority) {
        return localizationService.priorityLabel(priority);
    }

    public String categoryDisplayName(String category) {
        return localizationService.categoryLabel(category);
    }

    public String recurrenceSummary(RecurrenceRule rule) {
        return RecurrenceSummaryFormatter.describe(rule, localizationService);
    }

    public String weekdayShort(DayOfWeek dayOfWeek) {
        return localizationService.weekdayShort(dayOfWeek);
    }

    public String weekdayNarrow(DayOfWeek dayOfWeek) {
        return localizationService.weekdayNarrow(dayOfWeek);
    }
}
