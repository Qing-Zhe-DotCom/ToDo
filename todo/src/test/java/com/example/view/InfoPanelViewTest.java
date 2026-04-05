package com.example.view;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
}
