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

        String resolvedPage = SettingsDialog.resolveSettingsPage(null, pages, "general-scroll");

        assertEquals("general-scroll", resolvedPage);
        assertNotEquals("general-page", resolvedPage);
    }

    @Test
    void returnsMappedPageWhenSelectionExists() {
        Map<String, String> pages = new LinkedHashMap<>();
        pages.put("general", "general-scroll");
        pages.put("personalization", "personalization-scroll");

        assertEquals("personalization-scroll", SettingsDialog.resolveSettingsPage("personalization", pages, "general-scroll"));
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

        assertEquals("custom-scroll", SettingsDialog.resolveSettingsPage("custom", pages, "general-scroll"));
        assertEquals("shortcuts-scroll", SettingsDialog.resolveSettingsPage("shortcuts", pages, "general-scroll"));
        assertEquals("labs-scroll", SettingsDialog.resolveSettingsPage("labs", pages, "general-scroll"));
        assertEquals("data-scroll", SettingsDialog.resolveSettingsPage("data", pages, "general-scroll"));
    }

    @Test
    void labsThemesAreHiddenFromThemeSelectionWhenLabsAreDisabled() {
        List<ThemeFamily> filtered = ThemeCoordinator.filterThemeFamilies(ThemeFamily.supportedValues(), false);

        assertTrue(filtered.contains(ThemeFamily.MACARON));
        assertTrue(filtered.contains(ThemeFamily.CLASSIC));
    }

    @Test
    void labsThemesAreVisibleInThemeSelectionWhenLabsAreEnabled() {
        List<ThemeFamily> filtered = ThemeCoordinator.filterThemeFamilies(ThemeFamily.supportedValues(), true);

        assertTrue(filtered.contains(ThemeFamily.MACARON));
    }
}
