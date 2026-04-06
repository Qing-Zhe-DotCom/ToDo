package com.example.controller;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
}
