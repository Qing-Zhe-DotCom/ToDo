package com.example.controller;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class MainControllerSearchStateTest {

    @Test
    void clearsSearchWhenTextTransitionsFromKeywordToEmpty() {
        assertTrue(SearchController.shouldClearSearchResults("study", ""));
    }

    @Test
    void clearsSearchWhenTextTransitionsFromKeywordToWhitespace() {
        assertTrue(SearchController.shouldClearSearchResults("study", "   "));
    }

    @Test
    void ignoresRepeatedBlankValues() {
        assertFalse(SearchController.shouldClearSearchResults("", "   "));
    }

    @Test
    void ignoresKeywordEditsThatRemainNonBlank() {
        assertFalse(SearchController.shouldClearSearchResults("study", "study-plan"));
    }
}
