package com.example.view;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.model.Schedule;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import javafx.scene.layout.StackPane;

class ScheduleCardStyleSupportTest {

    @Test
    void normalizeStyleNameFallsBackToDefault() {
        assertEquals("温馨治愈风", ScheduleCardStyleSupport.normalizeStyleName("不存在的样式"));
        assertEquals("温馨治愈风", ScheduleCardStyleSupport.normalizeStyleName(null));
        assertEquals("Material You", ScheduleCardStyleSupport.normalizeStyleName("Material You"));
    }

    @Test
    void applyCardPresentationAddsSharedClassesAndScheduleState() {
        Schedule schedule = new Schedule();
        schedule.setPriority("高");
        schedule.setDueDate(LocalDate.now().minusDays(1));

        StackPane card = new StackPane();
        ScheduleCardStyleSupport.applyCardPresentation(
            card,
            schedule,
            "经典实体卡片",
            "schedule-card-role-test"
        );

        assertTrue(card.getStyleClass().contains(ScheduleCardStyleSupport.BASE_CARD_CLASS));
        assertTrue(card.getStyleClass().contains("schedule-card-role-test"));
        assertTrue(card.getStyleClass().contains("schedule-card-style-classic"));
        assertTrue(card.getStyleClass().contains("schedule-card-state-priority-high"));
        assertTrue(card.getStyleClass().contains("schedule-card-state-overdue"));
    }
}
