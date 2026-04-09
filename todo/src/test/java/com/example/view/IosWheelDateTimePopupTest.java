package com.example.view;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class IosWheelDateTimePopupTest {
    @Test
    void clampYearKeepsWithinPermissibleRange() {
        assertEquals(1900, IosWheelDateTimePopup.clampYear(1895));
        assertEquals(2100, IosWheelDateTimePopup.clampYear(2130));
        assertEquals(2026, IosWheelDateTimePopup.clampYear(2026));
    }
}
