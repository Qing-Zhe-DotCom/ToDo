package com.example.application;

import java.util.List;

public enum IconKey {
    ANIM("anim.svg"),
    ARROW_LEFT("arrow_left.svg"),
    ARROW_RIGHT("arrow_right.svg"),
    CALENDAR("calendar.svg"),
    CLOSE("close.svg"),
    DELETE("delete.svg"),
    DETAIL("detail.svg"),
    FOLDER("folder.svg"),
    GRID_HEATMAP("grid_heatmap.svg"),
    LOGOUT("logout.svg"),
    MONTH("month.svg"),
    NEXT("next.svg"),
    NOTES("notes.svg"),
    PLUS("plus.svg"),
    PREV("prev.svg"),
    RESET("reset.svg"),
    SETTINGS("settings.svg"),
    STYLE("style.svg"),
    TAG("tag.svg"),
    THEME_DARK("theme_dark.svg"),
    THEME_LIGHT("theme_light.svg"),
    TIMELINE("timeline.svg"),
    TODAY("today.svg"),
    POSTPONE_DAY("postpone_day.svg"),
    PIN("pin.svg"),
    UNPIN("unpin.svg"),
    YEAR("year.svg");

    private final String fileName;

    IconKey(String fileName) {
        this.fileName = fileName;
    }

    public String getFileName() {
        return fileName;
    }

    public static List<IconKey> supportedValues() {
        return List.of(values());
    }
}
