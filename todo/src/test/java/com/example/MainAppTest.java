package com.example;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class MainAppTest {
    @Test
    void minWindowSizeMatchesExpectations() {
        assertEquals(1100.0, MainApp.getMinimumWidth(), 0.00001, "minimum width should match the tightened threshold");
        assertEquals(660.0, MainApp.getMinimumHeight(), 0.00001, "minimum height should match the tightened threshold");
    }
}
