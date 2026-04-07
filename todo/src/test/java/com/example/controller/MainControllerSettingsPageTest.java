package com.example.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

class MainControllerSettingsPageTest {

    @Test
    void fallsBackToGeneralScrollContainerWhenSelectionIsMissing() {
        Map<String, String> pages = new LinkedHashMap<>();
        pages.put("general", "general-scroll");
        pages.put("theme", "theme-scroll");

        String resolvedPage = MainController.resolveSettingsPage(null, pages, "general-scroll");

        assertEquals("general-scroll", resolvedPage);
        assertNotEquals("general-page", resolvedPage);
    }

    @Test
    void returnsMappedPageWhenSelectionExists() {
        Map<String, String> pages = new LinkedHashMap<>();
        pages.put("general", "general-scroll");
        pages.put("theme", "theme-scroll");

        assertEquals("theme-scroll", MainController.resolveSettingsPage("theme", pages, "general-scroll"));
    }
}
