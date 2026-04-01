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
        String content = readCss("/styles/base.css");
        
        assertAll("dark-theme.css menu contrast selectors",
            () -> assertTrue(content.contains(".menu-bar > .container > .menu-button > .label {")),
            () -> assertTrue(content.contains(".context-menu .menu-item > .label {")),
            () -> assertTrue(content.contains(".context-menu .menu-item:focused > .label,")),
            () -> assertTrue(content.contains(".context-menu .menu-item:hover > .label {"))
        );
    }

    @Test
    void themeFilesContainHeatmapSelectionAndDayCardStyles() throws IOException {
        String baseContent = readCss("/styles/base.css");
        
        assertAll("theme files heatmap specific styles",
            () -> assertTrue(baseContent.contains(".heatmap-cell-selected {")),
            () -> assertTrue(baseContent.contains(".heatmap-day-panel {")),
            () -> assertTrue(baseContent.contains(".heatmap-day-title {")),
            () -> assertTrue(baseContent.contains(".heatmap-day-scroll {"))
        );
    }

    @Test
    void themesContainSidebarTitleAndDialogStyles() throws IOException {
        String baseContent = readCss("/styles/base.css");
        
        assertAll("theme files sidebar title and dialog styles",
            () -> assertTrue(baseContent.contains(".sidebar-function-title")),
            () -> assertTrue(baseContent.contains(".schedule-dialog-pane")),
            () -> assertTrue(baseContent.contains(".schedule-dialog-grid")),
            () -> assertTrue(baseContent.contains(".schedule-dialog-label")),
            () -> assertTrue(baseContent.contains(".schedule-dialog-check"))
        );
    }

    @Test
    void newBuiltinThemeFilesArePresent() throws IOException {
        String lavender = readCss("/styles/lavender-theme.css");
        String forest = readCss("/styles/forest-theme.css");
        String slate = readCss("/styles/slate-theme.css");

        assertAll(
            () -> assertTrue(lavender.contains("-color-primary")),
            () -> assertTrue(forest.contains("-color-primary")),
            () -> assertTrue(slate.contains("-color-primary"))
        );
    }

    private String readCss(String resourcePath) throws IOException {
        try (InputStream inputStream = getClass().getResourceAsStream(resourcePath)) {
            assertNotNull(inputStream);
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
