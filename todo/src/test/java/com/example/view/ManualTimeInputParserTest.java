package com.example.view;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

class ManualTimeInputParserTest {

    private final LocalDateTime anchor = LocalDateTime.of(2026, 4, 9, 12, 0);

    @Test
    void parsesFullDateTimeInputs() {
        LocalDateTime parsed = ManualTimeInputParser.parse("2026-05-01 14:30", anchor).orElseThrow();
        assertEquals(LocalDateTime.of(2026, 5, 1, 14, 30), parsed);
    }

    @Test
    void timeOnlyUsesAnchorDate() {
        LocalDateTime parsed = ManualTimeInputParser.parse("08:15", anchor).orElseThrow();
        assertEquals(LocalDateTime.of(2026, 4, 9, 8, 15), parsed);
    }

    @Test
    void digitsCanRepresentHourAndMinute() {
        LocalDateTime parsed = ManualTimeInputParser.parse("1345", anchor).orElseThrow();
        assertEquals(LocalDateTime.of(2026, 4, 9, 13, 45), parsed);
    }

    @Test
    void invalidValuesReturnEmpty() {
        assertTrue(ManualTimeInputParser.parse("not a time", anchor).isEmpty());
    }
}
