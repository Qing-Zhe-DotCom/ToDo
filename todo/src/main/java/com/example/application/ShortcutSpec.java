package com.example.application;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

public final class ShortcutSpec {
    private final boolean ctrl;
    private final boolean alt;
    private final boolean shift;
    private final boolean meta;
    private final KeyCode keyCode;

    public ShortcutSpec(boolean ctrl, boolean alt, boolean shift, boolean meta, KeyCode keyCode) {
        this.ctrl = ctrl;
        this.alt = alt;
        this.shift = shift;
        this.meta = meta;
        this.keyCode = Objects.requireNonNull(keyCode, "keyCode");
    }

    public static ShortcutSpec of(boolean ctrl, boolean alt, boolean shift, boolean meta, KeyCode keyCode) {
        return new ShortcutSpec(ctrl, alt, shift, meta, keyCode);
    }

    public boolean isCtrl() {
        return ctrl;
    }

    public boolean isAlt() {
        return alt;
    }

    public boolean isShift() {
        return shift;
    }

    public boolean isMeta() {
        return meta;
    }

    public KeyCode getKeyCode() {
        return keyCode;
    }

    public boolean hasAnyModifier() {
        return ctrl || alt || shift || meta;
    }

    public boolean matches(KeyEvent event) {
        if (event == null) {
            return false;
        }
        return event.getCode() == keyCode && matchesModifiers(event);
    }

    public boolean matchesModifiers(KeyEvent event) {
        if (event == null) {
            return false;
        }
        return event.isControlDown() == ctrl
            && event.isAltDown() == alt
            && event.isShiftDown() == shift
            && event.isMetaDown() == meta;
    }

    public static ShortcutSpec fromKeyEvent(KeyEvent event) {
        if (event == null) {
            return null;
        }
        KeyCode keyCode = event.getCode();
        if (keyCode == null || keyCode.isModifierKey() || keyCode == KeyCode.UNDEFINED) {
            return null;
        }
        ShortcutSpec spec = new ShortcutSpec(
            event.isControlDown(),
            event.isAltDown(),
            event.isShiftDown(),
            event.isMetaDown(),
            keyCode
        );
        return spec.hasAnyModifier() ? spec : null;
    }

    public String toPreferenceString() {
        List<String> parts = new ArrayList<>();
        if (ctrl) {
            parts.add("CTRL");
        }
        if (alt) {
            parts.add("ALT");
        }
        if (shift) {
            parts.add("SHIFT");
        }
        if (meta) {
            parts.add("META");
        }
        parts.add(keyCode.name());
        return String.join("+", parts);
    }

    public String toDisplayString() {
        List<String> parts = new ArrayList<>();
        if (ctrl) {
            parts.add("Ctrl");
        }
        if (alt) {
            parts.add("Alt");
        }
        if (shift) {
            parts.add("Shift");
        }
        if (meta) {
            parts.add("Meta");
        }
        parts.add(keyCode.getName());
        return String.join(" + ", parts);
    }

    public static ShortcutSpec parsePreference(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        boolean ctrl = false;
        boolean alt = false;
        boolean shift = false;
        boolean meta = false;
        KeyCode keyCode = null;

        String[] parts = value.trim().toUpperCase(Locale.ROOT).split("\\+");
        for (String rawPart : parts) {
            String part = rawPart.trim();
            if (part.isEmpty()) {
                continue;
            }
            switch (part) {
                case "CTRL", "CONTROL" -> ctrl = true;
                case "ALT" -> alt = true;
                case "SHIFT" -> shift = true;
                case "META", "CMD", "COMMAND", "WIN" -> meta = true;
                default -> {
                    try {
                        keyCode = KeyCode.valueOf(part);
                    } catch (IllegalArgumentException ignored) {
                        return null;
                    }
                }
            }
        }

        if (keyCode == null || keyCode.isModifierKey() || keyCode == KeyCode.UNDEFINED) {
            return null;
        }

        ShortcutSpec spec = new ShortcutSpec(ctrl, alt, shift, meta, keyCode);
        return spec.hasAnyModifier() ? spec : null;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof ShortcutSpec that)) {
            return false;
        }
        return ctrl == that.ctrl
            && alt == that.alt
            && shift == that.shift
            && meta == that.meta
            && keyCode == that.keyCode;
    }

    @Override
    public int hashCode() {
        return Objects.hash(ctrl, alt, shift, meta, keyCode);
    }

    @Override
    public String toString() {
        return toPreferenceString();
    }
}

