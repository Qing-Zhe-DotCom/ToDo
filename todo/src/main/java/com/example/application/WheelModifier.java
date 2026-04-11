package com.example.application;

import java.util.List;

import javafx.scene.input.ScrollEvent;

public enum WheelModifier {
    CTRL("ctrl"),
    ALT("alt"),
    SHIFT("shift"),
    META("meta"),
    NONE("none");

    private final String id;

    WheelModifier(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public boolean matches(ScrollEvent event) {
        if (event == null) {
            return false;
        }

        return switch (this) {
            case CTRL -> event.isControlDown();
            case ALT -> event.isAltDown();
            case SHIFT -> event.isShiftDown();
            case META -> event.isMetaDown();
            case NONE -> !event.isControlDown() && !event.isAltDown() && !event.isShiftDown() && !event.isMetaDown();
        };
    }

    public static List<WheelModifier> supportedValues() {
        return List.of(values());
    }

    public static WheelModifier fromPreference(String value) {
        if (value == null || value.isBlank()) {
            return CTRL;
        }
        for (WheelModifier modifier : values()) {
            if (modifier.id.equalsIgnoreCase(value)) {
                return modifier;
            }
        }
        return CTRL;
    }
}

