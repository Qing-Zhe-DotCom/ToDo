package com.example.view;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ScheduleDialogTest {
    @Test
    void commitTagEntryValueKeepsExistingTagsAndAppendsNew() {
        String input = "alpha, beta";
        assertEquals("alpha, beta", ScheduleDialog.commitTagEntryValue(input));
    }

    @Test
    void commitTagEntryValueDeduplicatesNewTag() {
        String input = "alpha, beta, alpha";
        assertEquals("alpha, beta", ScheduleDialog.commitTagEntryValue(input));
    }

    @Test
    void commitTagEntryValueHandlesTrailingDelimiter() {
        String input = "alpha, ";
        assertEquals("alpha", ScheduleDialog.commitTagEntryValue(input));
    }

    @Test
    void commitTagEntryValueRecognizesChineseDelimiter() {
        String input = "alpha\uFF0Cbeta\uFF0Cbeta";
        assertEquals("alpha, beta", ScheduleDialog.commitTagEntryValue(input));
    }
}
