package com.example.view;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import javafx.scene.layout.StackPane;

class ScheduleLandingTransitionTest {

    @Test
    void prepareTargetNodeForHandoffKeepsLayoutAndRestoresBaseState() {
        StackPane node = new StackPane();
        node.setManaged(true);
        node.setVisible(true);
        node.setOpacity(0.7);
        node.setMouseTransparent(false);

        ScheduleLandingTransition.prepareTargetNodeForHandoff(node, 0.28);

        assertTrue(node.isManaged());
        assertTrue(node.isVisible());
        assertEquals(0.28, node.getOpacity(), 0.0001);
        assertTrue(node.isMouseTransparent());
        assertTrue(node.getStyleClass().contains(ScheduleLandingTransition.TARGET_PREPARED_CLASS));

        ScheduleLandingTransition.finishTargetHandoff(node);

        assertTrue(node.isManaged());
        assertTrue(node.isVisible());
        assertEquals(0.7, node.getOpacity(), 0.0001);
        assertFalse(node.isMouseTransparent());
        assertFalse(node.getStyleClass().contains(ScheduleLandingTransition.TARGET_PREPARED_CLASS));
    }
}
