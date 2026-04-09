package com.example.controller;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.application.ProductivityConfig;
import com.example.model.Schedule;

import java.util.EnumSet;

import org.junit.jupiter.api.Test;

class MainControllerSearchStateTest {

    @Test
    void clearsSearchWhenTextTransitionsFromKeywordToEmpty() {
        assertTrue(MainController.shouldClearSearchResults("study", ""));
    }

    @Test
    void clearsSearchWhenTextTransitionsFromKeywordToWhitespace() {
        assertTrue(MainController.shouldClearSearchResults("study", "   "));
    }

    @Test
    void ignoresRepeatedBlankValues() {
        assertFalse(MainController.shouldClearSearchResults("", "   "));
    }

    @Test
    void ignoresKeywordEditsThatRemainNonBlank() {
        assertFalse(MainController.shouldClearSearchResults("study", "study-plan"));
    }

    @Test
    void matchesOnlyConfiguredSearchFields() {
        Schedule schedule = new Schedule();
        schedule.setName("Plan Academy");
        schedule.setDescription("Quarterly review");
        schedule.setNotes("Notes go here");
        schedule.setCategory("Work");
        schedule.setTags("alpha,beta");

        assertTrue(MainController.matchesSearchFields(
            schedule,
            "plan",
            EnumSet.of(ProductivityConfig.SearchField.TITLE)
        ));
        assertFalse(MainController.matchesSearchFields(
            schedule,
            "plan",
            EnumSet.of(ProductivityConfig.SearchField.CATEGORY)
        ));
        assertTrue(MainController.matchesSearchFields(
            schedule,
            "alpha",
            EnumSet.of(ProductivityConfig.SearchField.TAGS)
        ));
    }

    @Test
    void clearSearchButtonOnlyShowsForNonEmptyText() {
        assertFalse(MainController.shouldShowClearSearchButton(null));
        assertFalse(MainController.shouldShowClearSearchButton(""));
        assertTrue(MainController.shouldShowClearSearchButton(" "));
        assertTrue(MainController.shouldShowClearSearchButton("plan"));
    }
}
