package com.example.view;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

class ThemeCssTest {

    @Test
    void darkThemeDefinesMenuContrastSelectors() throws IOException {
        String css = readCss("/styles/dark-theme.css");

        assertAll(
            () -> assertTrue(css.contains(".menu-bar > .container > .menu-button > .label")),
            () -> assertTrue(css.contains(".context-menu")),
            () -> assertTrue(css.contains(".context-menu .menu-item:focused")),
            () -> assertTrue(css.contains(".separator-menu-item .line"))
        );
    }

    @Test
    void themeFilesContainHeatmapSelectionAndDayCardStyles() throws IOException {
        String darkCss = readCss("/styles/dark-theme.css");
        String lightCss = readCss("/styles/light-theme.css");

        assertAll(
            () -> assertTrue(darkCss.contains(".heatmap-day-card")),
            () -> assertTrue(darkCss.contains(".heatmap-cell-selected")),
            () -> assertTrue(lightCss.contains(".heatmap-day-card")),
            () -> assertTrue(lightCss.contains(".heatmap-cell-selected"))
        );
    }

    private String readCss(String resourcePath) throws IOException {
        try (InputStream inputStream = getClass().getResourceAsStream(resourcePath)) {
            assertNotNull(inputStream);
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
