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

    @Test
    void dueRelativeKeysExistInAllLocales() throws IOException {
        String[] keys = new String[] {
            "time.due.relative.future.moreThanMonth=",
            "time.due.relative.past.moreThanMonth=",
            "time.due.relative.future.days=",
            "time.due.relative.past.days=",
            "time.due.relative.future.hours=",
            "time.due.relative.past.hours=",
            "time.due.relative.future.hoursMinutes=",
            "time.due.relative.past.hoursMinutes=",
            "time.start.relative.future.moreThanMonth=",
            "time.start.relative.future.days=",
            "time.start.relative.future.hours=",
            "time.start.relative.future.hoursMinutes=",
        };

        String en = read("/i18n/messages.properties");
        String zhCn = read("/i18n/messages_zh_CN.properties");
        String zhTw = read("/i18n/messages_zh_TW.properties");

        for (String key : keys) {
            assertTrue(en.contains(key), "Missing key in messages.properties: " + key);
            assertTrue(zhCn.contains(key), "Missing key in messages_zh_CN.properties: " + key);
            assertTrue(zhTw.contains(key), "Missing key in messages_zh_TW.properties: " + key);
        }
    }

    private static String read(String resourcePath) throws IOException {
        try (InputStream input = LocalizationResourceTest.class.getResourceAsStream(resourcePath)) {
            assertNotNull(input, "Missing resource: " + resourcePath);
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
