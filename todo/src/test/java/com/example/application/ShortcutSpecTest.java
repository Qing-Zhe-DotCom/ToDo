package com.example.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import org.junit.jupiter.api.Test;

class ShortcutSpecTest {

    @Test
    void parsePreferenceAndRoundTripFormatting() {
        ShortcutSpec spec = ShortcutSpec.parsePreference("CTRL+EQUALS");
        assertNotNull(spec);
        assertEquals("CTRL+EQUALS", spec.toPreferenceString());
    }

    @Test
    void parsePreferenceRejectsMissingModifiers() {
        assertNull(ShortcutSpec.parsePreference("EQUALS"));
        assertNull(ShortcutSpec.parsePreference("  "));
    }

    @Test
    void fromKeyEventRejectsModifierOnlyAndNoModifier() {
        KeyEvent controlOnly = new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.CONTROL, false, true, false, false);
        assertNull(ShortcutSpec.fromKeyEvent(controlOnly));

        KeyEvent noModifier = new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.EQUALS, false, false, false, false);
        assertNull(ShortcutSpec.fromKeyEvent(noModifier));
    }

    @Test
    void matchesHonorsKeyAndExactModifiers() {
        ShortcutSpec spec = ShortcutSpec.parsePreference("CTRL+EQUALS");
        assertNotNull(spec);

        KeyEvent ctrlEquals = new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.EQUALS, false, true, false, false);
        assertTrue(spec.matches(ctrlEquals));

        KeyEvent ctrlShiftEquals = new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.EQUALS, true, true, false, false);
        assertFalse(spec.matches(ctrlShiftEquals));

        KeyEvent ctrlMinus = new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.MINUS, false, true, false, false);
        assertFalse(spec.matches(ctrlMinus));
    }
}

