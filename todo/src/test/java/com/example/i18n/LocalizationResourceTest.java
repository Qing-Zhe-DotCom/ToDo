package com.example.i18n;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

class LocalizationResourceTest {

    @Test
    void infoPanelShortYearFormatKeyExistsInAllLocales() throws IOException {
        assertTrue(read("/i18n/messages.properties").contains("format.info.shortYearSummary="));
        assertTrue(read("/i18n/messages_zh_CN.properties").contains("format.info.shortYearSummary="));
        assertTrue(read("/i18n/messages_zh_TW.properties").contains("format.info.shortYearSummary="));
    }

    @Test
    void darkModeSidebarKeysExistInAllLocales() throws IOException {
        assertTrue(read("/i18n/messages.properties").contains("sidebar.appearance.darkMode="));
        assertTrue(read("/i18n/messages.properties").contains("sidebar.appearance.switchToDark="));
        assertTrue(read("/i18n/messages.properties").contains("sidebar.appearance.switchToLight="));
        assertTrue(read("/i18n/messages.properties").contains("theme.dark="));

        assertTrue(read("/i18n/messages_zh_CN.properties").contains("sidebar.appearance.darkMode="));
        assertTrue(read("/i18n/messages_zh_CN.properties").contains("sidebar.appearance.switchToDark="));
        assertTrue(read("/i18n/messages_zh_CN.properties").contains("sidebar.appearance.switchToLight="));
        assertTrue(read("/i18n/messages_zh_CN.properties").contains("theme.dark="));

        assertTrue(read("/i18n/messages_zh_TW.properties").contains("sidebar.appearance.darkMode="));
        assertTrue(read("/i18n/messages_zh_TW.properties").contains("sidebar.appearance.switchToDark="));
        assertTrue(read("/i18n/messages_zh_TW.properties").contains("sidebar.appearance.switchToLight="));
        assertTrue(read("/i18n/messages_zh_TW.properties").contains("theme.dark="));
    }

    private static String read(String resourcePath) throws IOException {
        try (InputStream input = LocalizationResourceTest.class.getResourceAsStream(resourcePath)) {
            assertNotNull(input, "Missing resource: " + resourcePath);
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
