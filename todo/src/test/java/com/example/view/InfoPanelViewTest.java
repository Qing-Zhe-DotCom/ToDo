package com.example.view;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;

class InfoPanelViewTest {

    @Test
    void formatsSameYearDateRangeForPanelHeader() {
        InfoPanelView.DatePresentation presentation = InfoPanelView.buildDatePresentation(
            LocalDateTime.of(2026, 4, 1, 9, 0),
            LocalDateTime.of(2026, 4, 8, 18, 30)
        );

        assertEquals("4月1日 09:00 - 4月8日 18:30", presentation.getPrimaryText());
        assertEquals("2026年", presentation.getSecondaryText());
    }

    @Test
    void formatsSingleDueTimeWhenOnlyDeadlineExists() {
        InfoPanelView.DatePresentation presentation = InfoPanelView.buildDatePresentation(
            null,
            LocalDateTime.of(2026, 4, 1, 23, 59)
        );

        assertEquals("截止 4月1日 23:59", presentation.getPrimaryText());
        assertEquals("2026年", presentation.getSecondaryText());
    }

    @Test
    void formatsMissingDatesAsUnsetText() {
        InfoPanelView.DatePresentation presentation = InfoPanelView.buildDatePresentation(null, null);

        assertEquals("未设置时间", presentation.getPrimaryText());
        assertEquals("", presentation.getSecondaryText());
    }

    @Test
    void buildsTriggerPresentationForSetAndUnsetValues() {
        InfoPanelView.TimeTriggerPresentation unset = InfoPanelView.buildTimeTriggerPresentation(null);
        InfoPanelView.TimeTriggerPresentation set = InfoPanelView.buildTimeTriggerPresentation(
            LocalDateTime.of(2026, 4, 6, 21, 45)
        );

        assertEquals("未设置", unset.getPrimaryText());
        assertEquals("", unset.getSecondaryText());
        assertTrue(unset.isUnset());
        assertEquals("4月6日 21:45", set.getPrimaryText());
        assertEquals("2026年", set.getSecondaryText());
        assertFalse(set.isUnset());
    }

    @Test
    void defaultTimeSeedsMatchDetailPanelRules() {
        LocalDate date = LocalDate.of(2026, 4, 6);
        LocalDateTime due = LocalDateTime.of(2026, 4, 8, 18, 30);

        assertEquals(LocalDateTime.of(2026, 4, 6, 23, 59), InfoPanelView.defaultDueValue(date));
        assertEquals(LocalDateTime.of(2026, 4, 6, 9, 0), InfoPanelView.defaultStartValue(date));
        assertEquals(due, InfoPanelView.defaultReminderValue(date, due));
        assertEquals(LocalDateTime.of(2026, 4, 6, 9, 0), InfoPanelView.defaultReminderValue(date, null));
    }

    @Test
    void wheelPopupHelpersClampMonthLength() {
        assertEquals(29, IosWheelDateTimePopup.daysInMonth(2024, 2));
        assertEquals(28, IosWheelDateTimePopup.daysInMonth(2025, 2));
        assertEquals(30, IosWheelDateTimePopup.clampDay(2026, 4, 31));
        assertEquals(28, IosWheelDateTimePopup.clampDay(2025, 2, 31));
        assertEquals(1, IosWheelDateTimePopup.clampDay(2025, 2, 0));
    }

    @Test
    void splitTagChipsTrimsDeduplicatesAndKeepsOrder() {
        assertEquals(
            List.of("工作", "复盘", "会议"),
            InfoPanelView.splitTagChips(" 工作, 复盘，工作, 会议 ")
        );
    }

    @Test
    void categoryChipRulesHideDefaultAndEmptyValues() {
        assertTrue(InfoPanelView.shouldShowCategoryChip("工作"));
        assertFalse(InfoPanelView.shouldShowCategoryChip("未分类"));
        assertFalse(InfoPanelView.shouldShowCategoryChip("   "));
        assertFalse(InfoPanelView.shouldShowCategoryChip(null));
    }

    @Test
    void wheelPopupHelpersFormatAndClampYearRange() {
        assertEquals("26", IosWheelDateTimePopup.formatYear2(2026));
        assertEquals("00", IosWheelDateTimePopup.formatYear2(2000));
        assertEquals(2000, IosWheelDateTimePopup.clampYear(1999));
        assertEquals(2099, IosWheelDateTimePopup.clampYear(2100));
    }

    @Test
    void wheelPopupCompactInputParsesAsciiAndFullwidthColon() {
        assertEquals(
            LocalDateTime.of(2026, 5, 26, 18, 0),
            IosWheelDateTimePopup.parseCompactInput("26:05:26:18:00")
        );
        assertEquals(
            LocalDateTime.of(2026, 5, 26, 18, 0),
            IosWheelDateTimePopup.parseCompactInput("26：05：26：18：00")
        );
    }

    @Test
    void wheelPopupCompactInputRejectsInvalidDates() {
        assertNull(IosWheelDateTimePopup.parseCompactInput("26:02:31:10:00"));
        assertNull(IosWheelDateTimePopup.parseCompactInput("bad"));
        assertNull(IosWheelDateTimePopup.parseCompactInput(""));
        assertNull(IosWheelDateTimePopup.parseCompactInput(null));
    }

    @Test
    void timeTriggerPresentationForDueStartUsesSingleLineContract() {
        InfoPanelView.TimeTriggerPresentation presentation = InfoPanelView.buildTimeTriggerPresentation(
            null,
            LocalDateTime.of(2099, 8, 29, 23, 59),
            false
        );

        assertEquals("99", presentation.getSecondaryText());
        assertFalse(presentation.getPrimaryText().contains("2099"));
        assertFalse(presentation.isUnset());
    }
}
