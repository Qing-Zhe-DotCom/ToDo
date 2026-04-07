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
            () -> assertTrue(yearCardBlock != null && yearCardBlock.contains("-fx-background-color: transparent")),
            () -> assertTrue(yearCardBlock != null && yearCardBlock.contains("-fx-border-color: transparent")),
            () -> assertTrue(yearCardBlock != null && yearCardBlock.contains("-fx-border-width: 0")),
            () -> assertTrue(yearCardBlock != null && yearCardBlock.contains("-fx-effect: none")),
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
    void classicPaletteAndThemeFamilyFilesArePresent() throws IOException {
        String lavender = readCss("/styles/lavender-theme.css");
        String forest = readCss("/styles/forest-theme.css");
        String slate = readCss("/styles/slate-theme.css");
        String classic = readCss("/styles/theme-classic-light.css");
        String fresh = readCss("/styles/theme-fresh-light.css");
        String cozy = readCss("/styles/theme-cozy-light.css");
        String modernMinimal = readCss("/styles/theme-modern-minimal-light.css");
        String neoBrutalism = readCss("/styles/theme-neo-brutalism-light.css");
        String materialYou = readCss("/styles/theme-material-you-light.css");
        String neumorphism = readCss("/styles/theme-neumorphism-light.css");

        assertAll(
            () -> assertTrue(lavender.contains("-color-primary")),
            () -> assertTrue(forest.contains("-color-primary")),
            () -> assertTrue(slate.contains("-color-primary")),
            () -> assertTrue(classic.contains(".settings-card")),
            () -> assertTrue(fresh.contains(".settings-card")),
            () -> assertTrue(cozy.contains(".settings-card")),
            () -> assertTrue(modernMinimal.contains(".settings-card")),
            () -> assertTrue(neoBrutalism.contains(".settings-card")),
            () -> assertTrue(materialYou.contains(".settings-card")),
            () -> assertTrue(neumorphism.contains(".settings-card"))
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
            () -> assertTrue(baseContent.contains(".schedule-status-role-detail")),
            () -> assertTrue(baseContent.contains(".schedule-status-size-list")),
            () -> assertTrue(baseContent.contains(".schedule-status-size-timeline")),
            () -> assertTrue(baseContent.contains(".schedule-status-size-heatmap")),
            () -> assertTrue(baseContent.contains(".schedule-status-size-detail"))
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

    @Test
    void baseCssContainsInfoPanelSelectors() throws IOException {
        String baseContent = readCss("/styles/base.css");

        assertAll(
            () -> assertTrue(baseContent.contains(".info-panel-header {")),
            () -> assertTrue(baseContent.contains(".info-panel-action-row {")),
            () -> assertTrue(baseContent.contains(".info-panel-action-group {")),
            () -> assertTrue(baseContent.contains(".info-panel-toolbar-button {")),
            () -> assertTrue(baseContent.contains(".info-panel-icon-button {")),
            () -> assertTrue(baseContent.contains(".info-panel-icon-button-danger:hover {")),
            () -> assertTrue(baseContent.contains(".info-panel-status-pill {")),
            () -> assertTrue(baseContent.contains(".info-panel-chip {")),
            () -> assertTrue(baseContent.contains(".info-panel-top-chip-pane {")),
            () -> assertTrue(baseContent.contains(".info-panel-title-input {")),
            () -> assertTrue(baseContent.contains(".info-panel-complete-toggle {")),
            () -> assertTrue(baseContent.contains(".info-panel-inline-row {")),
            () -> assertTrue(baseContent.contains(".info-panel-inline-editor {")),
            () -> assertTrue(baseContent.contains(".info-panel-inline-editor-hover {")),
            () -> assertTrue(baseContent.contains(".info-panel-inline-editor-active {")),
            () -> assertTrue(baseContent.contains(".info-panel-borderless-field")),
            () -> assertTrue(baseContent.contains(".info-panel-borderless-area")),
            () -> assertTrue(baseContent.contains(".info-panel-time-row {")),
            () -> assertTrue(baseContent.contains(".info-panel-time-toggle-slot {")),
            () -> assertTrue(baseContent.contains(".info-panel-time-trigger {")),
            () -> assertTrue(baseContent.contains(".info-panel-time-trigger-title {")),
            () -> assertTrue(baseContent.contains(".info-panel-time-trigger-subtitle {")),
            () -> assertTrue(baseContent.contains(".info-panel-time-trigger-unset")),
            () -> assertTrue(baseContent.contains(".info-panel-borderless-combo {")),
            () -> assertTrue(baseContent.contains(".ios-wheel-popup {")),
            () -> assertTrue(baseContent.contains(".ios-wheel-header {")),
            () -> assertTrue(baseContent.contains(".ios-wheel-year-switch {")),
            () -> assertTrue(baseContent.contains(".ios-wheel-column {")),
            () -> assertTrue(baseContent.contains(".ios-wheel-cell {")),
            () -> assertTrue(baseContent.contains(".ios-wheel-selection-box {")),
            () -> assertTrue(baseContent.contains(".ios-wheel-mask-top {")),
            () -> assertTrue(baseContent.contains(".ios-wheel-mask-bottom {")),
            () -> assertTrue(baseContent.contains(".info-panel-delete-button {"))
        );
    }

    @Test
    void baseCssKeepsTimeTriggerCompact() throws IOException {
        String baseContent = readCss("/styles/base.css");
        String triggerBlock = extractCssBlock(baseContent, ".info-panel-time-trigger");
        String toggleBlock = extractCssBlock(baseContent, ".info-panel-editor-toggle");

        assertAll(
            () -> assertTrue(triggerBlock != null && triggerBlock.contains("-fx-padding: 6 10")),
            () -> assertTrue(triggerBlock != null && triggerBlock.contains("-fx-min-width: 148")),
            () -> assertTrue(triggerBlock != null && triggerBlock.contains("-fx-pref-width: 148")),
            () -> assertTrue(triggerBlock != null && triggerBlock.contains("-fx-max-width: 148")),
            () -> assertTrue(triggerBlock != null && triggerBlock.contains("-fx-min-height: 54")),
            () -> assertTrue(triggerBlock != null && triggerBlock.contains("-fx-pref-height: 54")),
            () -> assertTrue(triggerBlock != null && triggerBlock.contains("-fx-max-height: 54")),
            () -> assertTrue(toggleBlock != null && toggleBlock.contains("-fx-text-overrun: clip"))
        );
    }

    @Test
    void baseCssContainsQuickAddSelectors() throws IOException {
        String baseContent = readCss("/styles/base.css");

        assertAll(
            () -> assertTrue(baseContent.contains(".quick-add-section {")),
            () -> assertTrue(baseContent.contains(".quick-add-title {")),
            () -> assertTrue(baseContent.contains(".quick-add-shell {")),
            () -> assertTrue(baseContent.contains(".quick-add-shell-hover {")),
            () -> assertTrue(baseContent.contains(".quick-add-shell-focused {")),
            () -> assertTrue(baseContent.contains(".quick-add-badge {")),
            () -> assertTrue(baseContent.contains(".quick-add-input {"))
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
