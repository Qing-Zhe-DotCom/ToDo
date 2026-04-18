package com.example.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.example.application.ThemeFamily;

class MainControllerSettingsPageTest {

    @Test
    void fallsBackToGeneralScrollContainerWhenSelectionIsMissing() {
        Map<String, String> pages = new LinkedHashMap<>();
        pages.put("general", "general-scroll");
        pages.put("personalization", "personalization-scroll");

        String resolvedPage = MainController.resolveSettingsPage(null, pages, "general-scroll");

        assertEquals("general-scroll", resolvedPage);
        assertNotEquals("general-page", resolvedPage);
    }

    @Test
    void returnsMappedPageWhenSelectionExists() {
        Map<String, String> pages = new LinkedHashMap<>();
        pages.put("general", "general-scroll");
        pages.put("personalization", "personalization-scroll");

        assertEquals("personalization-scroll", MainController.resolveSettingsPage("personalization", pages, "general-scroll"));
    }

    @Test
    void returnsMappedPageForExpandedTopLevelSections() {
        Map<String, String> pages = new LinkedHashMap<>();
        pages.put("general", "general-scroll");
        pages.put("personalization", "personalization-scroll");
        pages.put("custom", "custom-scroll");
        pages.put("shortcuts", "shortcuts-scroll");
        pages.put("labs", "labs-scroll");
        pages.put("data", "data-scroll");

        assertEquals("custom-scroll", MainController.resolveSettingsPage("custom", pages, "general-scroll"));
        assertEquals("shortcuts-scroll", MainController.resolveSettingsPage("shortcuts", pages, "general-scroll"));
        assertEquals("labs-scroll", MainController.resolveSettingsPage("labs", pages, "general-scroll"));
        assertEquals("data-scroll", MainController.resolveSettingsPage("data", pages, "general-scroll"));
    }

    @Test
    void labsThemesAreHiddenFromThemeSelectionWhenLabsAreDisabled() {
        List<ThemeFamily> filtered = MainController.filterThemeFamilies(ThemeFamily.supportedValues(), false);

        assertFalse(filtered.contains(ThemeFamily.MACARON));
        assertFalse(filtered.contains(ThemeFamily.NEO_BRUTALISM));
        assertTrue(filtered.contains(ThemeFamily.CLASSIC));
    }

    @Test
    void labsThemesAreVisibleInThemeSelectionWhenLabsAreEnabled() {
        List<ThemeFamily> filtered = MainController.filterThemeFamilies(ThemeFamily.supportedValues(), true);

        assertTrue(filtered.contains(ThemeFamily.MACARON));
        assertTrue(filtered.contains(ThemeFamily.NEO_BRUTALISM));
    }
}
