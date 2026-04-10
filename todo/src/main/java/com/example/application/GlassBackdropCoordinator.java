package com.example.application;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelReader;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundImage;
import javafx.scene.layout.BackgroundPosition;
import javafx.scene.layout.BackgroundRepeat;
import javafx.scene.layout.BackgroundSize;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.stage.Window;
import javafx.util.Duration;

public final class GlassBackdropCoordinator {
    private static final Map<Scene, GlassBackdropCoordinator> COORDINATORS = new WeakHashMap<>();
    private static final double BLUR_RADIUS = 28.0;
    private static final Duration REFRESH_DEBOUNCE = Duration.millis(42);

    private static final Map<String, GlassSurfaceVariant> STYLE_VARIANTS = createStyleVariants();

    private final Scene scene;
    private final PauseTransition refreshDebounce = new PauseTransition(REFRESH_DEBOUNCE);
    private final Map<Region, SurfaceAttachment> attachments = new LinkedHashMap<>();

    private boolean active;
    private boolean refreshing;
    private boolean sceneListenersInstalled;

    private GlassBackdropCoordinator(Scene scene) {
        this.scene = scene;
        refreshDebounce.setOnFinished(event -> refreshNow());
        installSceneListeners();
    }

    public static GlassBackdropCoordinator install(Scene scene) {
        if (scene == null) {
            throw new IllegalArgumentException("scene must not be null");
        }
        synchronized (COORDINATORS) {
            return COORDINATORS.computeIfAbsent(scene, GlassBackdropCoordinator::new);
        }
    }

    public void setActive(boolean active) {
        runOnFxThread(() -> {
            if (this.active == active) {
                if (active) {
                    requestBurstRefresh(Duration.millis(360));
                }
                return;
            }
            this.active = active;
            if (active) {
                requestBurstRefresh(Duration.millis(420));
            } else {
                refreshDebounce.stop();
                clearAll();
            }
        });
    }

    public void requestRefresh() {
        runOnFxThread(() -> {
            if (!active) {
                return;
            }
            refreshDebounce.playFromStart();
        });
    }

    public void requestBurstRefresh(Duration duration) {
        runOnFxThread(() -> {
            if (!active) {
                return;
            }
            requestRefresh();
        });
    }

    private void refreshNow() {
        if (!active || refreshing) {
            return;
        }
        Parent root = scene.getRoot();
        Window window = scene.getWindow();
        if (root == null || window == null || !window.isShowing()) {
            return;
        }

        refreshing = true;
        try {
            root.applyCss();
            root.layout();
            syncAttachments(root);
            if (attachments.isEmpty()) {
                return;
            }

            List<SurfaceAttachment> visibleAttachments = attachments.values().stream()
                .filter(SurfaceAttachment::isVisible)
                .toList();
            if (visibleAttachments.isEmpty()) {
                clearInactiveAttachments();
                return;
            }

            List<SurfaceAttachment> hiddenForSnapshot = new ArrayList<>(visibleAttachments.size());
            for (SurfaceAttachment attachment : visibleAttachments) {
                attachment.hideForSnapshot();
                hiddenForSnapshot.add(attachment);
            }

            WritableImage snapshot = null;
            try {
                SnapshotParameters parameters = new SnapshotParameters();
                parameters.setFill(Color.TRANSPARENT);
                snapshot = root.snapshot(parameters, null);
            } finally {
                for (SurfaceAttachment attachment : hiddenForSnapshot) {
                    attachment.restoreAfterSnapshot();
                }
            }

            if (snapshot == null || snapshot.getPixelReader() == null) {
                clearInactiveAttachments();
                return;
            }

            WritableImage blurredSnapshot = blur(snapshot);
            PixelReader reader = blurredSnapshot.getPixelReader();
            if (reader == null) {
                clearInactiveAttachments();
                return;
            }

            for (SurfaceAttachment attachment : visibleAttachments) {
                attachment.applyBackdrop(reader, blurredSnapshot.getWidth(), blurredSnapshot.getHeight());
            }
        } catch (Exception ignored) {
        } finally {
            refreshing = false;
        }
    }

    private WritableImage blur(WritableImage source) {
        ImageView blurView = new ImageView(source);
        blurView.setEffect(new GaussianBlur(BLUR_RADIUS));
        SnapshotParameters parameters = new SnapshotParameters();
        parameters.setFill(Color.TRANSPARENT);
        return blurView.snapshot(parameters, null);
    }

    private void clearAll() {
        for (SurfaceAttachment attachment : attachments.values()) {
            attachment.detach();
        }
        attachments.clear();
    }

    private void clearInactiveAttachments() {
        for (SurfaceAttachment attachment : attachments.values()) {
            attachment.clearBackdrop();
        }
    }

    private void syncAttachments(Parent root) {
        Map<Region, GlassSurfaceVariant> discovered = new LinkedHashMap<>();
        collectGlassRegions(root, discovered);

        List<Region> toRemove = attachments.keySet().stream()
            .filter(region -> !discovered.containsKey(region))
            .toList();
        for (Region region : toRemove) {
            SurfaceAttachment attachment = attachments.remove(region);
            if (attachment != null) {
                attachment.detach();
            }
        }

        for (Map.Entry<Region, GlassSurfaceVariant> entry : discovered.entrySet()) {
            SurfaceAttachment attachment = attachments.get(entry.getKey());
            if (attachment == null) {
                attachment = new SurfaceAttachment(entry.getKey(), entry.getValue());
                attachments.put(entry.getKey(), attachment);
            } else {
                attachment.updateVariant(entry.getValue());
            }
        }
    }

    private void collectGlassRegions(Node node, Map<Region, GlassSurfaceVariant> discovered) {
        if (node instanceof Region region && node != scene.getRoot()) {
            GlassSurfaceVariant variant = resolveVariant(region);
            if (variant != null) {
                discovered.put(region, variant);
            }
        }
        if (!(node instanceof Parent parent)) {
            return;
        }
        for (Node child : parent.getChildrenUnmodifiable()) {
            collectGlassRegions(child, discovered);
        }
    }

    private GlassSurfaceVariant resolveVariant(Region region) {
        List<String> styleClasses = region.getStyleClass();
        for (Map.Entry<String, GlassSurfaceVariant> entry : STYLE_VARIANTS.entrySet()) {
            if (styleClasses.contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        return null;
    }

    private void installSceneListeners() {
        if (sceneListenersInstalled) {
            return;
        }
        sceneListenersInstalled = true;
        scene.widthProperty().addListener((obs, oldValue, newValue) -> requestBurstRefresh(Duration.millis(280)));
        scene.heightProperty().addListener((obs, oldValue, newValue) -> requestBurstRefresh(Duration.millis(280)));
        scene.addEventFilter(ScrollEvent.ANY, event -> requestBurstRefresh(Duration.millis(220)));
        scene.windowProperty().addListener((obs, oldWindow, newWindow) -> {
            if (newWindow != null) {
                newWindow.showingProperty().addListener((showObs, wasShowing, isShowing) -> {
                    if (isShowing) {
                        requestBurstRefresh(Duration.millis(420));
                    }
                });
            }
        });
    }

    private void runOnFxThread(Runnable action) {
        if (Platform.isFxApplicationThread()) {
            action.run();
        } else {
            Platform.runLater(action);
        }
    }

    private static Map<String, GlassSurfaceVariant> createStyleVariants() {
        Map<String, GlassSurfaceVariant> variants = new LinkedHashMap<>();
        variants.put("settings-inline-value", GlassSurfaceVariant.BADGE);
        variants.put("info-panel-status-pill", GlassSurfaceVariant.BADGE);
        variants.put("heatmap-day-rail-badge", GlassSurfaceVariant.BADGE);

        variants.put("info-panel-inline-editor", GlassSurfaceVariant.PANEL);
        variants.put("info-panel-borderless-combo", GlassSurfaceVariant.PANEL);
        variants.put("info-panel-notes-input", GlassSurfaceVariant.PANEL);
        variants.put("info-panel-title-input", GlassSurfaceVariant.PANEL);
        variants.put("timeline-range-group", GlassSurfaceVariant.PANEL);
        variants.put("timeline-range-picker", GlassSurfaceVariant.PANEL);
        variants.put("timeline-range-icon-wrap", GlassSurfaceVariant.PANEL);
        variants.put("heatmap-sidebar", GlassSurfaceVariant.PANEL);
        variants.put("heatmap-day-rail", GlassSurfaceVariant.PANEL);

        variants.put("settings-card", GlassSurfaceVariant.CARD);
        variants.put("info-panel", GlassSurfaceVariant.CARD);
        variants.put("heatmap-day-panel", GlassSurfaceVariant.CARD);
        variants.put("heatmap-year-card", GlassSurfaceVariant.CARD);
        variants.put("heatmap-day-card", GlassSurfaceVariant.CARD);
        variants.put("timeline-range-pill", GlassSurfaceVariant.CARD);
        variants.put("timeline-state", GlassSurfaceVariant.CARD);
        variants.put("quick-add-shell", GlassSurfaceVariant.CARD);
        variants.put("info-panel-description-card", GlassSurfaceVariant.CARD);
        variants.put("info-panel-time-trigger", GlassSurfaceVariant.CARD);
        variants.put("timeline-schedule-card", GlassSurfaceVariant.CARD);
        variants.put("schedule-card-surface", GlassSurfaceVariant.CARD);
        variants.put("date-picker-popup", GlassSurfaceVariant.CARD);
        variants.put("ios-wheel-popup", GlassSurfaceVariant.CARD);

        variants.put("sidebar", GlassSurfaceVariant.SHELL);
        variants.put("settings-nav", GlassSurfaceVariant.SHELL);
        variants.put("settings-shell", GlassSurfaceVariant.SHELL);
        variants.put("settings-content-host", GlassSurfaceVariant.SHELL);
        variants.put("main-content", GlassSurfaceVariant.SHELL);
        variants.put("timeline-container", GlassSurfaceVariant.SHELL);
        variants.put("timeline-pane", GlassSurfaceVariant.SHELL);
        variants.put("timeline-header", GlassSurfaceVariant.SHELL);
        variants.put("heatmap-meta-bar", GlassSurfaceVariant.SHELL);
        variants.put("heatmap-sidebar-shell", GlassSurfaceVariant.SHELL);
        variants.put("info-panel-header", GlassSurfaceVariant.SHELL);
        return variants;
    }

    private enum GlassSurfaceVariant {
        SHELL(Color.web("#FFFDFD"), 0.62),
        CARD(Color.web("#FFFFFF"), 0.54),
        PANEL(Color.web("#FFFDFE"), 0.68),
        BADGE(Color.web("#FFFFFF"), 0.72);

        private final Color overlayColor;
        private final double overlayStrength;

        GlassSurfaceVariant(Color overlayColor, double overlayStrength) {
            this.overlayColor = overlayColor;
            this.overlayStrength = overlayStrength;
        }
    }

    private final class SurfaceAttachment {
        private final Region region;
        private final ChangeListener<Boolean> visibilityListener = (obs, oldValue, newValue) -> requestRefresh();
        private final ChangeListener<Scene> sceneListener = (obs, oldScene, newScene) -> requestRefresh();

        private GlassSurfaceVariant variant;
        private double opacityBeforeSnapshot = 1.0;

        private SurfaceAttachment(Region region, GlassSurfaceVariant variant) {
            this.region = region;
            this.variant = variant;
            region.visibleProperty().addListener(visibilityListener);
            region.sceneProperty().addListener(sceneListener);
        }

        private boolean isVisible() {
            return region.isVisible()
                && region.getScene() == scene
                && region.getWidth() > 1
                && region.getHeight() > 1;
        }

        private void updateVariant(GlassSurfaceVariant variant) {
            this.variant = variant;
        }

        private void hideForSnapshot() {
            opacityBeforeSnapshot = region.getOpacity();
            region.setOpacity(0.0);
        }

        private void restoreAfterSnapshot() {
            region.setOpacity(opacityBeforeSnapshot);
        }

        private void applyBackdrop(PixelReader reader, double maxWidth, double maxHeight) {
            Bounds bounds = region.localToScene(region.getLayoutBounds());
            if (bounds == null || bounds.getWidth() < 2 || bounds.getHeight() < 2) {
                clearBackdrop();
                return;
            }

            int x = clamp((int) Math.floor(bounds.getMinX()), 0, (int) Math.floor(maxWidth) - 1);
            int y = clamp((int) Math.floor(bounds.getMinY()), 0, (int) Math.floor(maxHeight) - 1);
            int width = clamp((int) Math.ceil(bounds.getWidth()), 1, (int) Math.ceil(maxWidth) - x);
            int height = clamp((int) Math.ceil(bounds.getHeight()), 1, (int) Math.ceil(maxHeight) - y);
            if (width <= 0 || height <= 0) {
                clearBackdrop();
                return;
            }

            WritableImage tinted = tintImage(reader, x, y, width, height, variant);
            BackgroundImage image = new BackgroundImage(
                tinted,
                BackgroundRepeat.NO_REPEAT,
                BackgroundRepeat.NO_REPEAT,
                BackgroundPosition.DEFAULT,
                new BackgroundSize(width, height, false, false, false, false)
            );
            region.setBackground(new Background(image));
        }

        private void clearBackdrop() {
            region.setBackground(null);
        }

        private void detach() {
            clearBackdrop();
            region.visibleProperty().removeListener(visibilityListener);
            region.sceneProperty().removeListener(sceneListener);
        }
    }

    private WritableImage tintImage(
        PixelReader reader,
        int x,
        int y,
        int width,
        int height,
        GlassSurfaceVariant variant
    ) {
        WritableImage target = new WritableImage(width, height);
        PixelWriter writer = target.getPixelWriter();
        for (int py = 0; py < height; py++) {
            for (int px = 0; px < width; px++) {
                Color base = reader.getColor(x + px, y + py);
                Color mixed = base.interpolate(variant.overlayColor, variant.overlayStrength);
                writer.setColor(px, py, new Color(mixed.getRed(), mixed.getGreen(), mixed.getBlue(), base.getOpacity()));
            }
        }
        return target;
    }

    private int clamp(int value, int min, int max) {
        if (max < min) {
            return min;
        }
        return Math.max(min, Math.min(max, value));
    }
}
