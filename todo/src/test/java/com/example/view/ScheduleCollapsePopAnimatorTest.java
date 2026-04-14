package com.example.view;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;

class ScheduleCollapsePopAnimatorTest {

    @Test
    void restoreSteadyStateClearsTransientMotionFlags() {
        StackPane host = new StackPane();
        HBox shell = new HBox();
        ScheduleCollapsePopAnimator.MotionHandle handle =
            ScheduleCollapsePopAnimator.bindMotionHandle(host, shell, null);

        host.setManaged(false);
        host.setVisible(false);
        host.setMouseTransparent(true);
        shell.setMouseTransparent(true);
        shell.setOpacity(0.0);
        shell.setScaleX(0.85);
        shell.setScaleY(0.85);
        shell.setTranslateX(12.0);
        shell.setTranslateY(8.0);
        shell.setRotate(7.0);
        host.setCache(true);
        shell.setCache(true);

        handle.restoreSteadyState();

        assertTrue(host.isManaged());
        assertTrue(host.isVisible());
        assertFalse(host.isMouseTransparent());
        assertFalse(shell.isMouseTransparent());
        assertEquals(1.0, shell.getOpacity(), 0.0001);
        assertEquals(1.0, shell.getScaleX(), 0.0001);
        assertEquals(1.0, shell.getScaleY(), 0.0001);
        assertEquals(0.0, shell.getTranslateX(), 0.0001);
        assertEquals(0.0, shell.getTranslateY(), 0.0001);
        assertEquals(0.0, shell.getRotate(), 0.0001);
        assertFalse(host.isCache());
        assertFalse(shell.isCache());
    }
}
