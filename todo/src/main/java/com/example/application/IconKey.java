package com.example.application;

public enum IconKey {
    APP_LOGO("app-logo.svg"),
    COLLAPSE_TRIANGLE("collapse-triangle.svg"),
    NAV_SCHEDULE("nav-schedule.svg"),
    NAV_TIMELINE("nav-timeline.svg"),
    NAV_HEATMAP("nav-heatmap.svg"),
    NAV_FLOWCHART("nav-flowchart.svg"),
    ACTION_USER("action-user.svg"),
    ACTION_SETTINGS("action-settings.svg"),
    ACTION_LOGOUT("action-logout.svg"),
    SETTINGS_GENERAL("settings-general.svg"),
    SETTINGS_PERSONALIZATION("settings-personalization.svg"),
    SETTINGS_DATA("settings-data.svg"),
    HEATMAP_MONTH("heatmap-month.svg"),
    HEATMAP_YEAR("heatmap-year.svg"),
    HEATMAP_PREVIOUS("heatmap-previous.svg"),
    HEATMAP_TODAY("heatmap-today.svg"),
    HEATMAP_NEXT("heatmap-next.svg"),
    ARROW_LEFT("arrow-left.svg"),
    ARROW_RIGHT("arrow-right.svg"),
    TIMELINE_CALENDAR("timeline-calendar.svg"),
    QUICK_ADD("quick-add.svg"),
    INFO_DELETE("info-delete.svg"),
    INFO_CLOSE("info-close.svg");

    private final String fileName;

    IconKey(String fileName) {
        this.fileName = fileName;
    }

    public String getFileName() {
        return fileName;
    }
}
