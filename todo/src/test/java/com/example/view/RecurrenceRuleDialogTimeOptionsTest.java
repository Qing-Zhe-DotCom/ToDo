package com.example.view;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

class RecurrenceRuleDialogTimeOptionsTest {

    @Test
    void buildTimeOptionsProducesStableSequence() {
        List<String> options = RecurrenceRuleDialog.buildTimeOptions();

        assertEquals(49, options.size());
        assertEquals("00:00", options.get(0));
        assertTrue(options.contains("23:30"));
        assertEquals("23:59", options.get(options.size() - 1));
    }
}

