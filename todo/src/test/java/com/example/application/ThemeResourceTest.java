package com.example.application;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

class ThemeResourceTest {

    @Test
    void baseAndTokenStylesheetsExist() {
        assertNotNull(getClass().getResource("/styles/base.css"), "Missing /styles/base.css");
        assertNotNull(getClass().getResource("/styles/light-theme.css"), "Missing /styles/light-theme.css");
        assertNotNull(getClass().getResource("/styles/dark-theme.css"), "Missing /styles/dark-theme.css");
    }

    @Test
    void everyThemeFamilyHasLightAndDarkStylesheets() {
        for (ThemeFamily family : ThemeFamily.supportedValues()) {
            for (ThemeAppearance appearance : ThemeAppearance.supportedValues()) {
                String path = family.resolveStylesheetPath(appearance);
                assertNotNull(getClass().getResource(path), () -> "Missing theme stylesheet: " + path);
            }
        }
    }

    @Test
    void classicPaletteOverlaysExistForBothAppearances() {
        for (ClassicThemePalette palette : ClassicThemePalette.supportedValues()) {
            for (ThemeAppearance appearance : ThemeAppearance.supportedValues()) {
                String path = palette.resolveOverlayStylesheetPath(appearance);
                if (path == null) {
                    continue;
                }
                assertNotNull(getClass().getResource(path), () -> "Missing palette overlay stylesheet: " + path);
            }
        }
    }
}

