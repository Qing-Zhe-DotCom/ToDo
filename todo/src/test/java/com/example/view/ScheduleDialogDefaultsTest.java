package com.example.view;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ScheduleDialogDefaultsTest {

    @Test
    void newSchedulesFallBackToDefaultCategoryAndTag() {
        assertEquals("默认", ScheduleDialog.resolveCategoryValue("   ", false));
        assertEquals("无", ScheduleDialog.resolveTagsValue("   ", false));
    }

    @Test
    void editModePreservesIntentionalClears() {
        assertEquals("", ScheduleDialog.resolveCategoryValue("   ", true));
        assertEquals("", ScheduleDialog.resolveTagsValue("   ", true));
    }

    @Test
    void explicitValuesAreTrimmedAndKept() {
        assertEquals("工作", ScheduleDialog.resolveCategoryValue(" 工作 ", false));
        assertEquals("项目A,项目B", ScheduleDialog.resolveTagsValue(" 项目A,项目B ", true));
    }
}
