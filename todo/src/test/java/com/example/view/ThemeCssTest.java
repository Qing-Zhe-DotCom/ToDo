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
        String sidebarBlock = extractCssBlock(baseContent, ".heatmap-sidebar");
        String collapsedSidebarBlock = extractCssBlock(baseContent, ".heatmap-sidebar-collapsed");
        String expandedSidebarBlock = extractCssBlock(baseContent, ".heatmap-sidebar-expanded");
        String dayRailBlock = extractCssBlock(baseContent, ".heatmap-day-rail");
        String yearCardBlock = extractCssBlock(baseContent, ".heatmap-year-card");
        String yearCellBlock = extractCssBlock(baseContent, ".heatmap-year-cell");
        
        assertAll("theme files heatmap specific styles",
            () -> assertTrue(baseContent.contains(".heatmap-cell-selected {")),
            () -> assertTrue(baseContent.contains(".heatmap-meta-bar {")),
            () -> assertTrue(baseContent.contains(".heatmap-body {")),
            () -> assertTrue(baseContent.contains(".heatmap-main-pane {")),
            () -> assertTrue(baseContent.contains(".heatmap-day-panel {")),
            () -> assertTrue(baseContent.contains(".heatmap-sidebar-shell {")),
            () -> assertTrue(baseContent.contains(".heatmap-sidebar {")),
            () -> assertTrue(baseContent.contains(".heatmap-sidebar-collapsed {")),
            () -> assertTrue(baseContent.contains(".heatmap-sidebar-expanded {")),
            () -> assertTrue(baseContent.contains(".heatmap-day-title {")),
            () -> assertTrue(baseContent.contains(".heatmap-day-scroll {")),
            () -> assertTrue(baseContent.contains(".heatmap-day-rail {")),
            () -> assertTrue(baseContent.contains(".heatmap-year-card {")),
            () -> assertTrue(baseContent.contains(".heatmap-year-title {")),
            () -> assertTrue(baseContent.contains(".heatmap-year-month-grid {")),
            () -> assertTrue(baseContent.contains(".heatmap-year-cell {")),
            () -> assertTrue(baseContent.contains(".heatmap-completed-zone {")),
            () -> assertTrue(baseContent.contains(".heatmap-completed-zone-label {")),
            () -> assertTrue(sidebarBlock != null && !sidebarBlock.contains("-fx-pref-width")),
            () -> assertTrue(sidebarBlock != null && !sidebarBlock.contains("-fx-min-width")),
            () -> assertTrue(sidebarBlock != null && !sidebarBlock.contains("-fx-max-width")),
            () -> assertTrue(collapsedSidebarBlock != null && collapsedSidebarBlock.contains("-fx-pref-width: 40px")),
            () -> assertTrue(collapsedSidebarBlock != null && collapsedSidebarBlock.contains("-fx-min-width: 40px")),
            () -> assertTrue(collapsedSidebarBlock != null && collapsedSidebarBlock.contains("-fx-max-width: 40px")),
            () -> assertTrue(expandedSidebarBlock != null && expandedSidebarBlock.contains("-fx-pref-width: 280px")),
            () -> assertTrue(expandedSidebarBlock != null && expandedSidebarBlock.contains("-fx-min-width: 280px")),
            () -> assertTrue(expandedSidebarBlock != null && expandedSidebarBlock.contains("-fx-max-width: 280px")),
            () -> assertTrue(dayRailBlock != null && dayRailBlock.contains("-fx-spacing: 0")),
            () -> assertTrue(yearCardBlock != null && yearCardBlock.contains("-fx-border-color: -color-border-dark")),
            () -> assertTrue(yearCardBlock != null && yearCardBlock.contains("-fx-effect: dropshadow")),
            () -> assertTrue(yearCellBlock != null && yearCellBlock.contains("-fx-stroke-width: 0.75"))
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

    @Test
    void baseCssContainsSharedScheduleCardStyleSelectors() throws IOException {
        String baseContent = readCss("/styles/base.css");

        assertAll(
            () -> assertTrue(baseContent.contains(".schedule-card-surface {")),
            () -> assertTrue(baseContent.contains(".schedule-card-role-list")),
            () -> assertTrue(baseContent.contains(".schedule-card-role-heatmap")),
            () -> assertTrue(baseContent.contains(".schedule-card-role-timeline")),
            () -> assertTrue(baseContent.contains(".schedule-card-style-classic")),
            () -> assertTrue(baseContent.contains(".schedule-card-style-material-you")),
            () -> assertTrue(baseContent.contains(".schedule-card-motion-host")),
            () -> assertTrue(baseContent.contains(".schedule-card-motion-shell")),
            () -> assertTrue(baseContent.contains(".schedule-card-transition-completed")),
            () -> assertTrue(baseContent.contains(".schedule-card-transition-receiving"))
        );
    }

    @Test
    void baseCssContainsSharedScheduleStatusSelectors() throws IOException {
        String baseContent = readCss("/styles/base.css");

        assertAll(
            () -> assertTrue(baseContent.contains(".schedule-status-control {")),
            () -> assertTrue(baseContent.contains(".schedule-status-ring-base")),
            () -> assertTrue(baseContent.contains(".schedule-status-ring-segment-a")),
            () -> assertTrue(baseContent.contains(".schedule-status-ring-segment-b")),
            () -> assertTrue(baseContent.contains(".schedule-status-ring-segment-c")),
            () -> assertTrue(baseContent.contains(".schedule-status-check")),
            () -> assertTrue(baseContent.contains(".schedule-status-state-pending")),
            () -> assertTrue(baseContent.contains(".schedule-status-state-completed")),
            () -> assertTrue(baseContent.contains(".schedule-status-state-busy")),
            () -> assertTrue(baseContent.contains(".schedule-status-role-list")),
            () -> assertTrue(baseContent.contains(".schedule-status-role-timeline")),
            () -> assertTrue(baseContent.contains(".schedule-status-role-heatmap")),
            () -> assertTrue(baseContent.contains(".schedule-status-size-list")),
            () -> assertTrue(baseContent.contains(".schedule-status-size-timeline")),
            () -> assertTrue(baseContent.contains(".schedule-status-size-heatmap"))
        );
    }

    @Test
    void baseCssContainsTimelineRangeCapsuleSelectors() throws IOException {
        String baseContent = readCss("/styles/base.css");

        assertAll(
            () -> assertTrue(baseContent.contains(".timeline-range-group {")),
            () -> assertTrue(baseContent.contains(".timeline-range-pill {")),
            () -> assertTrue(baseContent.contains(".timeline-range-picker {")),
            () -> assertTrue(baseContent.contains(".timeline-range-reset {"))
        );
    }

    private String readCss(String resourcePath) throws IOException {
        try (InputStream inputStream = getClass().getResourceAsStream(resourcePath)) {
            assertNotNull(inputStream);
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private String extractCssBlock(String content, String selector) {
        int start = content.indexOf(selector + " {");
        if (start < 0) {
            return null;
        }
        int end = content.indexOf("}", start);
        if (end < 0) {
            return null;
        }
        return content.substring(start, end + 1);
    }
}
