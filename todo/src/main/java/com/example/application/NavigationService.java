package com.example.application;

import com.example.model.Schedule;

public final class NavigationService {
    public enum Screen {
        LIST,
        TIMELINE,
        HEATMAP,
        FLOWCHART
    }

    private Screen currentScreen = Screen.LIST;
    private Schedule selectedSchedule;

    public Screen getCurrentScreen() {
        return currentScreen;
    }

    public void setCurrentScreen(Screen currentScreen) {
        this.currentScreen = currentScreen;
    }

    public Schedule getSelectedSchedule() {
        return selectedSchedule;
    }

    public void setSelectedSchedule(Schedule selectedSchedule) {
        this.selectedSchedule = selectedSchedule;
    }

    public void clearSelectedSchedule() {
        selectedSchedule = null;
    }

    public boolean isSelected(Schedule schedule) {
        return selectedSchedule == schedule;
    }
}
