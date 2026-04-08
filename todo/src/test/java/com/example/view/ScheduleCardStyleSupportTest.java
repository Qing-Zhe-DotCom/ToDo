package com.example.view;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import com.example.model.Schedule;

import javafx.scene.layout.StackPane;

class ScheduleCardStyleSupportTest {

    @Test
    void normalizeStyleIdFallsBackToClassicAndMigratesLegacyLabels() {
        assertEquals(ScheduleCardStyleSupport.getDefaultStyleId(), ScheduleCardStyleSupport.normalizeStyleId("missing-style"));
        assertEquals(ScheduleCardStyleSupport.getDefaultStyleId(), ScheduleCardStyleSupport.normalizeStyleId(null));
        assertEquals(ScheduleCardStyleSupport.STYLE_ID_MATERIAL_YOU, ScheduleCardStyleSupport.normalizeStyleId("Material You"));
        assertEquals(ScheduleCardStyleSupport.STYLE_ID_CLASSIC, ScheduleCardStyleSupport.normalizeStyleId("经典实体卡片"));
    }

    @Test
    void applyCardPresentationAddsSharedClassesAndScheduleState() {
        Schedule schedule = new Schedule();
        schedule.setPriority(Schedule.PRIORITY_HIGH);
        schedule.setDueDate(LocalDate.now().minusDays(1));

        StackPane card = new StackPane();
        ScheduleCardStyleSupport.applyCardPresentation(
            card,
            schedule,
            ScheduleCardStyleSupport.STYLE_ID_CLASSIC,
            "schedule-card-role-test"
        );

        assertTrue(card.getStyleClass().contains(ScheduleCardStyleSupport.BASE_CARD_CLASS));
        assertTrue(card.getStyleClass().contains("schedule-card-role-test"));
        assertTrue(card.getStyleClass().contains("schedule-card-style-classic"));
        assertTrue(card.getStyleClass().contains("schedule-card-state-priority-high"));
        assertTrue(card.getStyleClass().contains("schedule-card-state-overdue"));
    }
}
