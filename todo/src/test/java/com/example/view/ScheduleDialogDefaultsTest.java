package com.example.view;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.example.model.Schedule;

class ScheduleDialogDefaultsTest {

    @Test
    void newSchedulesFallBackToUnclassifiedAndEmptyTags() {
        assertEquals(Schedule.DEFAULT_CATEGORY, ScheduleDialog.resolveCategoryValue("   ", false));
        assertEquals("", ScheduleDialog.resolveTagsValue("   ", false));
    }

    @Test
    void editModeStillPreservesIntentionalClears() {
        assertEquals("", ScheduleDialog.resolveCategoryValue("   ", true));
        assertEquals("", ScheduleDialog.resolveTagsValue("   ", true));
    }

    @Test
    void explicitValuesAreNormalizedAndKept() {
        assertEquals("工作", ScheduleDialog.resolveCategoryValue(" 工作 ", false));
        assertEquals("项目A, 项目B", ScheduleDialog.resolveTagsValue(" 项目A,项目B ", true));
    }
}
