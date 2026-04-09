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
    void macaronIsHiddenFromThemeSelectionWhenLabsAreDisabled() {
        List<ThemeFamily> filtered = MainController.filterThemeFamilies(ThemeFamily.supportedValues(), false);

        assertFalse(filtered.contains(ThemeFamily.MACARON));
        assertTrue(filtered.contains(ThemeFamily.CLASSIC));
    }

    @Test
    void macaronIsVisibleInThemeSelectionWhenLabsAreEnabled() {
        List<ThemeFamily> filtered = MainController.filterThemeFamilies(ThemeFamily.supportedValues(), true);

        assertTrue(filtered.contains(ThemeFamily.MACARON));
    }

    @Test
    void settingsShellHeightFallsBackForInvalidDialogHeights() {
        assertEquals(640.0, MainController.resolveSettingsShellHeight(0));
        assertEquals(640.0, MainController.resolveSettingsShellHeight(-12));
    }

    @Test
    void settingsShellHeightUsesMinimumWhenDialogIsTooShort() {
        assertEquals(520.0, MainController.resolveSettingsShellHeight(560.0));
    }

    @Test
    void settingsShellHeightTracksDialogHeightWhenEnoughSpaceExists() {
        assertEquals(674.0, MainController.resolveSettingsShellHeight(760.0));
    }
}
