package com.example.view;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URL;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;

class InfoPanelTitleFontCssTest {

    @BeforeAll
    static void initToolkit() {
        // Ensures JavaFX toolkit is initialized for CSS resolution.
        new JFXPanel();
    }

    @Test
    void titleFontSizeIsLargeEvenWithRootInlineFont() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Double> fontSizeRef = new AtomicReference<>();

        Platform.runLater(() -> {
            StackPane root = new StackPane();
            // Mirrors the intended FontService behavior: apply font family only on the root.
            // Using "-fx-font" (shorthand) here prevents descendants from overriding font-size via stylesheets.
            root.setStyle("-fx-font-family: \"System\";");

            TextField title = new TextField("Hello");
            title.getStyleClass().add("info-panel-title-input");
            root.getChildren().add(title);

            Scene scene = new Scene(root, 420, 240);
            scene.getStylesheets().add(resource("/styles/base.css"));
            scene.getStylesheets().add(resource("/styles/light-theme.css"));
            scene.getStylesheets().add(resource("/styles/theme-classic-light.css"));

            root.applyCss();
            root.layout();

            fontSizeRef.set(title.getFont().getSize());
            latch.countDown();
        });

        assertTrue(latch.await(5, TimeUnit.SECONDS), "Timed out waiting for JavaFX thread");
        assertTrue(
            fontSizeRef.get() != null && fontSizeRef.get() >= 30.0,
            "Expected title font-size >= 30px, but was " + fontSizeRef.get()
        );
    }

    private static String resource(String path) {
        URL url = InfoPanelTitleFontCssTest.class.getResource(path);
        assertNotNull(url, "Missing stylesheet resource: " + path);
        return url.toExternalForm();
    }
}
