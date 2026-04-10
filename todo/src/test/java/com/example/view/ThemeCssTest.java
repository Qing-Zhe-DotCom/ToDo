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
        String macaron = readCss("/styles/theme-macaron-light.css");
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
            () -> assertTrue(macaron.contains(".settings-card")),
            () -> assertTrue(modernMinimal.contains(".settings-card")),
            () -> assertTrue(neoBrutalism.contains(".settings-card")),
            () -> assertTrue(materialYou.contains(".settings-card")),
            () -> assertTrue(neumorphism.contains(".settings-card"))
        );
    }

    @Test
    void themeFamilyFilesStyleWholeAppShellSelectors() throws IOException {
        String classic = readCss("/styles/theme-classic-light.css");
        String fresh = readCss("/styles/theme-fresh-light.css");
        String cozy = readCss("/styles/theme-cozy-light.css");
        String macaron = readCss("/styles/theme-macaron-light.css");
        String modernMinimal = readCss("/styles/theme-modern-minimal-light.css");
        String neoBrutalism = readCss("/styles/theme-neo-brutalism-light.css");
        String materialYou = readCss("/styles/theme-material-you-light.css");
        String neumorphism = readCss("/styles/theme-neumorphism-light.css");

        assertAll("theme family files whole app shell selectors",
            () -> assertThemeFamilyFileThemesWholeShell(classic),
            () -> assertThemeFamilyFileThemesWholeShell(fresh),
            () -> assertThemeFamilyFileThemesWholeShell(cozy),
            () -> assertThemeFamilyFileThemesWholeShell(macaron),
            () -> assertThemeFamilyFileThemesWholeShell(modernMinimal),
            () -> assertThemeFamilyFileThemesWholeShell(neoBrutalism),
            () -> assertThemeFamilyFileThemesWholeShell(materialYou),
            () -> assertThemeFamilyFileThemesWholeShell(neumorphism)
        );
    }

    @Test
    void baseCssUsesBorderlessInputsAndDialogFields() throws IOException {
        String baseContent = readCss("/styles/base.css");
        String searchFieldBlock = extractCssBlock(baseContent, ".search-field");
        String textFieldBlock = extractCssBlock(baseContent, ".text-field, .text-area, .password-field");
        String comboBoxBlock = extractCssBlock(baseContent, ".combo-box");
        String timelineRangePillBlock = extractCssBlock(baseContent, ".timeline-range-pill");
        String modernInputBlock = extractCssBlock(baseContent, ".modern-input");
        String infoPanelInlineEditorBlock = extractCssBlock(baseContent, ".info-panel-inline-editor");
        String infoPanelInlineEditorActiveBlock = extractCssBlock(baseContent, ".info-panel-inline-editor-active");
        String heroTitleFocusBlock = extractCssBlock(baseContent, ".hero-title-input:focused");
        String quickAddInputBlock = extractCssBlock(baseContent, ".quick-add-input");
        String errorBorderBlock = extractCssBlock(baseContent, ".error-border");
        String scheduleDialogFieldBlock = extractBlockByAnchor(baseContent, ".schedule-dialog-pane .combo-box,");

        assertAll("base.css borderless input and dialog fields",
            () -> assertTrue(searchFieldBlock != null && searchFieldBlock.contains("-fx-border-color: transparent")),
            () -> assertTrue(searchFieldBlock != null && searchFieldBlock.contains("-fx-border-width: 0")),
            () -> assertTrue(textFieldBlock != null && textFieldBlock.contains("-fx-border-color: transparent")),
            () -> assertTrue(textFieldBlock != null && textFieldBlock.contains("-fx-border-width: 0")),
            () -> assertTrue(comboBoxBlock != null && comboBoxBlock.contains("-fx-border-color: transparent")),
            () -> assertTrue(comboBoxBlock != null && comboBoxBlock.contains("-fx-border-width: 0")),
            () -> assertTrue(timelineRangePillBlock != null && timelineRangePillBlock.contains("-fx-border-color: transparent")),
            () -> assertTrue(timelineRangePillBlock != null && timelineRangePillBlock.contains("-fx-border-width: 0")),
            () -> assertTrue(modernInputBlock != null && modernInputBlock.contains("-fx-border-color: transparent")),
            () -> assertTrue(modernInputBlock != null && modernInputBlock.contains("-fx-border-width: 0")),
            () -> assertTrue(infoPanelInlineEditorBlock != null && infoPanelInlineEditorBlock.contains("-fx-border-width: 0")),
            () -> assertTrue(infoPanelInlineEditorActiveBlock != null && infoPanelInlineEditorActiveBlock.contains("-fx-border-color: transparent")),
            () -> assertTrue(heroTitleFocusBlock != null && heroTitleFocusBlock.contains("-fx-border-color: transparent")),
            () -> assertTrue(quickAddInputBlock != null && quickAddInputBlock.contains("-fx-background-color: transparent")),
            () -> assertTrue(quickAddInputBlock != null && quickAddInputBlock.contains("-fx-border-width: 0")),
            () -> assertTrue(scheduleDialogFieldBlock != null && scheduleDialogFieldBlock.contains("-fx-border-color: transparent")),
            () -> assertTrue(scheduleDialogFieldBlock != null && scheduleDialogFieldBlock.contains("-fx-border-width: 0")),
            () -> assertTrue(errorBorderBlock != null && errorBorderBlock.contains("-fx-border-width: 1.5"))
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
    void baseCssKeepsTimelineRangeSingleSurfaceContract() throws IOException {
        String baseContent = readCss("/styles/base.css");
        String rangeGroupBlock = extractCssBlock(baseContent, ".timeline-range-group");
        String rangePillBlock = extractCssBlock(baseContent, ".timeline-range-pill");

        assertAll(
            () -> assertTrue(rangeGroupBlock != null && rangeGroupBlock.contains("-fx-background-color: transparent")),
            () -> assertTrue(rangeGroupBlock != null && rangeGroupBlock.contains("-fx-effect: none")),
            () -> assertTrue(rangePillBlock != null && rangePillBlock.contains("-fx-background-color: -color-bg-panel"))
        );
    }

    @Test
    void baseCssMakesScheduleListScrollViewportTransparent() throws IOException {
        String baseContent = readCss("/styles/base.css");
        String viewportBlock = extractCssBlock(baseContent, ".schedule-list-scroll > .viewport");
        String cornerBlock = extractCssBlock(baseContent, ".schedule-list-scroll > .corner");

        assertAll(
            () -> assertTrue(viewportBlock != null && viewportBlock.contains("-fx-background-color: transparent")),
            () -> assertTrue(cornerBlock != null && cornerBlock.contains("-fx-background-color: transparent"))
        );
    }

    @Test
    void baseCssContainsHeaderClockSelector() throws IOException {
        String baseContent = readCss("/styles/base.css");
        String clockBlock = extractCssBlock(baseContent, ".header-clock");

        assertAll(
            () -> assertTrue(baseContent.contains(".header-clock {")),
            () -> assertTrue(clockBlock != null && clockBlock.contains("-fx-text-fill: -color-text-sub")),
            () -> assertTrue(clockBlock != null && clockBlock.contains("-fx-font-size: 12px"))
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
            () -> assertTrue(baseContent.contains(".ios-wheel-columns {")),
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
            () -> assertTrue(triggerBlock != null && triggerBlock.contains("-fx-min-width: 170")),
            () -> assertTrue(triggerBlock != null && triggerBlock.contains("-fx-pref-width: 170")),
            () -> assertTrue(triggerBlock != null && triggerBlock.contains("-fx-max-width: 170")),
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
            () -> assertTrue(baseContent.contains(".quick-add-input {")),
            () -> assertTrue(baseContent.contains(".quick-add-feedback {"))
        );
    }

    @Test
    void baseCssKeepsQuickAddSingleSurfaceContract() throws IOException {
        String baseContent = readCss("/styles/base.css");
        String quickAddSectionBlock = extractCssBlock(baseContent, ".quick-add-section");
        String quickAddShellBlock = extractCssBlock(baseContent, ".quick-add-shell");
        String quickAddBadgeBlock = extractCssBlock(baseContent, ".quick-add-badge");
        String quickAddInputBlock = extractCssBlock(baseContent, ".quick-add-input");
        String quickAddFeedbackBlock = extractCssBlock(baseContent, ".quick-add-feedback");

        assertAll(
            () -> assertTrue(quickAddSectionBlock != null && quickAddSectionBlock.contains("-fx-background-color: transparent")),
            () -> assertTrue(quickAddSectionBlock != null && quickAddSectionBlock.contains("-fx-effect: none")),
            () -> assertTrue(quickAddShellBlock != null && quickAddShellBlock.contains("-fx-background-color: -color-bg-panel")),
            () -> assertTrue(quickAddBadgeBlock != null && quickAddBadgeBlock.contains("-fx-background-color: transparent")),
            () -> assertTrue(quickAddBadgeBlock != null && quickAddBadgeBlock.contains("-fx-pref-width: 36px")),
            () -> assertTrue(quickAddInputBlock != null && quickAddInputBlock.contains("-fx-background-color: transparent")),
            () -> assertTrue(quickAddInputBlock != null && quickAddInputBlock.contains("-fx-padding: 0")),
            () -> assertTrue(quickAddFeedbackBlock != null && quickAddFeedbackBlock.contains("-fx-background-color: -color-primary-soft"))
        );
    }

    @Test
    void themeFilesFlattenQuickAddSecondarySurfaces() throws IOException {
        String classic = readCss("/styles/theme-classic-light.css");
        String fresh = readCss("/styles/theme-fresh-light.css");
        String cozy = readCss("/styles/theme-cozy-light.css");
        String macaron = readCss("/styles/theme-macaron-light.css");
        String modernMinimal = readCss("/styles/theme-modern-minimal-light.css");
        String neoBrutalism = readCss("/styles/theme-neo-brutalism-light.css");
        String materialYou = readCss("/styles/theme-material-you-light.css");
        String neumorphism = readCss("/styles/theme-neumorphism-light.css");

        assertAll(
            () -> assertQuickAddThemeSurfaceReset(classic),
            () -> assertQuickAddThemeSurfaceReset(fresh),
            () -> assertQuickAddThemeSurfaceReset(cozy),
            () -> assertQuickAddThemeSurfaceReset(macaron),
            () -> assertQuickAddThemeSurfaceReset(modernMinimal),
            () -> assertQuickAddThemeSurfaceReset(neoBrutalism),
            () -> assertQuickAddThemeSurfaceReset(materialYou),
            () -> assertQuickAddThemeSurfaceReset(neumorphism)
        );
    }

    @Test
    void themeFilesFlattenTimelineRangeSecondarySurfaces() throws IOException {
        String classic = readCss("/styles/theme-classic-light.css");
        String fresh = readCss("/styles/theme-fresh-light.css");
        String cozy = readCss("/styles/theme-cozy-light.css");
        String macaron = readCss("/styles/theme-macaron-light.css");
        String modernMinimal = readCss("/styles/theme-modern-minimal-light.css");
        String neoBrutalism = readCss("/styles/theme-neo-brutalism-light.css");
        String materialYou = readCss("/styles/theme-material-you-light.css");
        String neumorphism = readCss("/styles/theme-neumorphism-light.css");

        assertAll(
            () -> assertTimelineRangeThemeSurfaceReset(classic),
            () -> assertTimelineRangeThemeSurfaceReset(fresh),
            () -> assertTimelineRangeThemeSurfaceReset(cozy),
            () -> assertTimelineRangeThemeSurfaceReset(macaron),
            () -> assertTimelineRangeThemeSurfaceReset(modernMinimal),
            () -> assertTimelineRangeThemeSurfaceReset(neoBrutalism),
            () -> assertTimelineRangeThemeSurfaceReset(materialYou),
            () -> assertTimelineRangeThemeSurfaceReset(neumorphism)
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

    private String extractLastCssBlock(String content, String selector) {
        int start = content.lastIndexOf(selector + " {");
        if (start < 0) {
            return null;
        }
        int end = content.indexOf("}", start);
        if (end < 0) {
            return null;
        }
        return content.substring(start, end + 1);
    }

    private String extractBlockByAnchor(String content, String anchor) {
        int start = content.indexOf(anchor);
        if (start < 0) {
            return null;
        }
        int end = content.indexOf("}", start);
        if (end < 0) {
            return null;
        }
        return content.substring(start, end + 1);
    }

    private void assertThemeFamilyFileThemesWholeShell(String content) {
        assertAll(
            () -> assertTrue(content.contains(".settings-nav-title")),
            () -> assertTrue(content.contains(".info-panel-header")),
            () -> assertTrue(content.contains(".timeline-header")),
            () -> assertTrue(content.contains(".heatmap-meta-bar")),
            () -> assertTrue(content.contains(".schedule-card-surface")),
            () -> assertTrue(content.contains(".quick-add-shell")),
            () -> assertTrue(content.contains(".quick-add-badge"))
        );
    }

    private void assertQuickAddThemeSurfaceReset(String content) {
        String quickAddSectionBlock = extractLastCssBlock(content, ".quick-add-section");
        String quickAddBadgeBlock = extractLastCssBlock(content, ".quick-add-badge");
        String quickAddInputBlock = extractLastCssBlock(content, ".quick-add-input");
        String quickAddInputFocusedBlock = extractLastCssBlock(content, ".quick-add-input:focused");

        assertAll(
            () -> assertTrue(content.contains(".quick-add-shell")),
            () -> assertTrue(quickAddSectionBlock != null && quickAddSectionBlock.contains("-fx-background-color: transparent")),
            () -> assertTrue(quickAddSectionBlock != null && quickAddSectionBlock.contains("-fx-effect: none")),
            () -> assertTrue(quickAddBadgeBlock != null && quickAddBadgeBlock.contains("-fx-background-color: transparent")),
            () -> assertTrue(quickAddBadgeBlock != null && quickAddBadgeBlock.contains("-fx-effect: none")),
            () -> assertTrue(quickAddInputBlock != null && quickAddInputBlock.contains("-fx-background-color: transparent")),
            () -> assertTrue(quickAddInputBlock != null && quickAddInputBlock.contains("-fx-effect: none")),
            () -> assertTrue(quickAddInputFocusedBlock != null && quickAddInputFocusedBlock.contains("-fx-background-color: transparent")),
            () -> assertTrue(quickAddInputFocusedBlock != null && quickAddInputFocusedBlock.contains("-fx-effect: none"))
        );
    }

    private void assertTimelineRangeThemeSurfaceReset(String content) {
        String rangeGroupBlock = extractLastCssBlock(content, ".timeline-range-group");
        String rangePickerBlock = extractLastCssBlock(content, ".timeline-range-picker");
        String rangeIconWrapBlock = extractLastCssBlock(content, ".timeline-range-icon-wrap");

        assertAll(
            () -> assertTrue(rangeGroupBlock != null && rangeGroupBlock.contains("-fx-background-color: transparent")),
            () -> assertTrue(rangeGroupBlock != null && rangeGroupBlock.contains("-fx-effect: none")),
            () -> assertTrue(rangePickerBlock != null && rangePickerBlock.contains("-fx-background-color: transparent")),
            () -> assertTrue(rangePickerBlock != null && rangePickerBlock.contains("-fx-effect: none")),
            () -> assertTrue(rangeIconWrapBlock != null && rangeIconWrapBlock.contains("-fx-background-color: transparent")),
            () -> assertTrue(rangeIconWrapBlock != null && rangeIconWrapBlock.contains("-fx-effect: none"))
        );
    }
}
