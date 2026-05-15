package com.example.application;

import com.example.model.ScheduleItem;

public final class NavigationService {
    public enum Screen {
        LIST,
        TIMELINE,
        HEATMAP
    }

    private Screen currentScreen = Screen.LIST;
    private ScheduleItem selectedScheduleItem;

    public Screen getCurrentScreen() {
        return currentScreen;
    }

    public void setCurrentScreen(Screen currentScreen) {
        this.currentScreen = currentScreen;
    }

    public ScheduleItem getSelectedScheduleItem() {
        return selectedScheduleItem;
    }

    public void setSelectedScheduleItem(ScheduleItem item) {
        this.selectedScheduleItem = item;
    }

    public void clearSelectedScheduleItem() {
        selectedScheduleItem = null;
    }

    public boolean isSelected(ScheduleItem item) {
        if (selectedScheduleItem == null || item == null) {
            return false;
        }
        if (selectedScheduleItem.getId() != null && !selectedScheduleItem.getId().isBlank()
            && item.getId() != null && !item.getId().isBlank()) {
            return selectedScheduleItem.getId().equals(item.getId());
        }
        return selectedScheduleItem == item;
    }
}
