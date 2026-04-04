package com.example.view;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

class InfoPanelViewTest {

    @Test
    void formatsSameYearDateRangeForPanelHeader() {
        InfoPanelView.DatePresentation presentation = InfoPanelView.buildDatePresentation(
            LocalDate.of(2026, 4, 1),
            LocalDate.of(2026, 4, 8)
        );

        assertEquals("4月1日 - 4月8日", presentation.getPrimaryText());
        assertEquals("2026年", presentation.getSecondaryText());
    }

    @Test
    void formatsCrossYearDateRangeForPanelHeader() {
        InfoPanelView.DatePresentation presentation = InfoPanelView.buildDatePresentation(
            LocalDate.of(2026, 12, 31),
            LocalDate.of(2027, 1, 2)
        );

        assertEquals("12月31日 - 1月2日", presentation.getPrimaryText());
        assertEquals("2026年 - 2027年", presentation.getSecondaryText());
    }

    @Test
    void formatsSingleDateWhenOnlyOneBoundaryExists() {
        InfoPanelView.DatePresentation presentation = InfoPanelView.buildDatePresentation(
            LocalDate.of(2026, 4, 1),
            null
        );

        assertEquals("4月1日", presentation.getPrimaryText());
        assertEquals("2026年", presentation.getSecondaryText());
    }

    @Test
    void formatsMissingDatesAsUnsetText() {
        InfoPanelView.DatePresentation presentation = InfoPanelView.buildDatePresentation(null, null);

        assertEquals("未设置日期", presentation.getPrimaryText());
        assertEquals("", presentation.getSecondaryText());
    }

    @Test
    void splitTagChipsTrimsDeduplicatesAndSkipsPlaceholderValues() {
        assertEquals(
            List.of("工作", "复盘", "会议"),
            InfoPanelView.splitTagChips(" 工作, 复盘，工作, 无 , 会议 ")
        );
    }

    @Test
    void categoryChipRulesHideDefaultAndEmptyValues() {
        assertTrue(InfoPanelView.shouldShowCategoryChip("工作"));
        assertFalse(InfoPanelView.shouldShowCategoryChip("默认"));
        assertFalse(InfoPanelView.shouldShowCategoryChip("   "));
        assertFalse(InfoPanelView.shouldShowCategoryChip(null));
    }
}
