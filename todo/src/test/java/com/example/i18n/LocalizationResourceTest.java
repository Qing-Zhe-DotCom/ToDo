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
    void customOptionsAndWheelInputKeysExistInAllLocales() throws IOException {
        for (String resource : new String[] {
            "/i18n/messages.properties",
            "/i18n/messages_zh_CN.properties",
            "/i18n/messages_zh_TW.properties"
        }) {
            String content = read(resource);
            assertTrue(content.contains("common.add="));
            assertTrue(content.contains("settings.customOptions.title="));
            assertTrue(content.contains("settings.customOptions.subtitle="));
            assertTrue(content.contains("settings.customOptions.tasks.title="));
            assertTrue(content.contains("settings.customOptions.tasks.prompt="));
            assertTrue(content.contains("settings.customOptions.tags.title="));
            assertTrue(content.contains("settings.customOptions.tags.prompt="));
            assertTrue(content.contains("settings.customOptions.tags.commaSplit.label="));
            assertTrue(content.contains("settings.customOptions.tags.commaSplit.description="));
            assertTrue(content.contains("settings.customOptions.validation.empty="));
            assertTrue(content.contains("settings.customOptions.validation.duplicate="));
            assertTrue(content.contains("settings.customOptions.validation.limit="));
            assertTrue(content.contains("error.customOptions.save.title="));
            assertTrue(content.contains("error.customOptions.taskLimit.title="));
            assertTrue(content.contains("error.customOptions.tagLimit.title="));

            assertTrue(content.contains("settings.custom.title="));
            assertTrue(content.contains("settings.custom.subtitle="));
            assertTrue(content.contains("settings.custom.timeInput.label="));
            assertTrue(content.contains("settings.custom.timeInput.description="));
            assertTrue(content.contains("iosWheel.input.placeholder="));
        }
    }

    @Test
    void shortcutKeysExistInAllLocales() throws IOException {
        for (String resource : new String[] {
            "/i18n/messages.properties",
            "/i18n/messages_zh_CN.properties",
            "/i18n/messages_zh_TW.properties"
        }) {
            String content = read(resource);
            assertTrue(content.contains("settings.shortcuts.title="));
            assertTrue(content.contains("settings.shortcuts.subtitle="));
            assertTrue(content.contains("settings.shortcuts.timelineZoomWheel.label="));
            assertTrue(content.contains("settings.shortcuts.timelineZoomWheel.description="));
            assertTrue(content.contains("settings.shortcuts.timelineZoomIn.label="));
            assertTrue(content.contains("settings.shortcuts.timelineZoomIn.description="));
            assertTrue(content.contains("settings.shortcuts.timelineZoomOut.label="));
            assertTrue(content.contains("settings.shortcuts.timelineZoomOut.description="));
            assertTrue(content.contains("settings.shortcuts.action.set="));
            assertTrue(content.contains("settings.shortcuts.capture.title="));
            assertTrue(content.contains("settings.shortcuts.capture.hint="));
            assertTrue(content.contains("settings.shortcuts.capture.invalid="));
            assertTrue(content.contains("settings.shortcuts.capture.invalidModifier="));
            assertTrue(content.contains("shortcut.modifier.ctrl="));
            assertTrue(content.contains("shortcut.modifier.alt="));
            assertTrue(content.contains("shortcut.modifier.shift="));
            assertTrue(content.contains("shortcut.modifier.meta="));
            assertTrue(content.contains("shortcut.modifier.none="));
        }
    }

    @Test
    void settingsTabKeysExistInAllLocales() throws IOException {
        for (String resource : new String[] {
            "/i18n/messages.properties",
            "/i18n/messages_zh_CN.properties",
            "/i18n/messages_zh_TW.properties"
        }) {
            String content = read(resource);
            assertTrue(content.contains("settings.tab.details="));
            assertTrue(content.contains("settings.tab.personalization="));
            assertTrue(content.contains("settings.tab.custom="));
            assertTrue(content.contains("settings.tab.shortcuts="));
            assertTrue(content.contains("settings.tab.labs="));
            assertTrue(content.contains("settings.tab.data="));
        }
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
