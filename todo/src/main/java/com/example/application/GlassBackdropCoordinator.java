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
    private ThemeAppearance appearance = ThemeAppearance.LIGHT;

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

    public void setAppearance(ThemeAppearance appearance) {
        ThemeAppearance resolved = appearance != null ? appearance : ThemeAppearance.LIGHT;
        runOnFxThread(() -> {
            if (this.appearance == resolved) {
                return;
            }
            this.appearance = resolved;
            if (active) {
                requestBurstRefresh(Duration.millis(420));
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

            // 核心改进：只对马卡龙背景层进行快照，而不是整个 Root，避免递归隐藏组件
            Node macaronLayer = findMacaronLayer(root);
            if (macaronLayer == null) {
                clearInactiveAttachments();
                return;
            }

            SnapshotParameters parameters = new SnapshotParameters();
            parameters.setFill(Color.TRANSPARENT);
            WritableImage rawSnapshot = macaronLayer.snapshot(parameters, null);
            
            if (rawSnapshot == null) {
                clearInactiveAttachments();
                return;
            }

            // GPU 加速的模糊处理 (通过 ImageView 离屏渲染)
            WritableImage blurredSnapshot = blur(rawSnapshot);
            
            for (SurfaceAttachment attachment : visibleAttachments) {
                // 将模糊图直接作为背景，通过 Viewport 偏移实现玻璃透视感
                attachment.applyBackdrop(blurredSnapshot, rawSnapshot.getWidth(), rawSnapshot.getHeight());
            }
        } catch (Exception ignored) {
        } finally {
            refreshing = false;
        }
    }

    private Node findMacaronLayer(Parent root) {
        for (Node child : root.getChildrenUnmodifiable()) {
            if (child.getStyleClass().contains("macaron-background-layer")) {
                return child;
            }
        }
        return null;
    }

    private WritableImage blur(WritableImage source) {
        ImageView blurView = new ImageView(source);
        blurView.setEffect(new GaussianBlur(BLUR_RADIUS));
        blurView.setCache(true);
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
        SHELL(Color.web("#FFFDFD"), Color.web("#0F1118"), 0.62),
        CARD(Color.web("#FFFFFF"), Color.web("#111624"), 0.54),
        PANEL(Color.web("#FFFDFE"), Color.web("#0D101A"), 0.68),
        BADGE(Color.web("#FFFFFF"), Color.web("#141B2A"), 0.72);

        private final Color lightOverlayColor;
        private final Color darkOverlayColor;
        private final double overlayStrength;

        GlassSurfaceVariant(Color lightOverlayColor, Color darkOverlayColor, double overlayStrength) {
            this.lightOverlayColor = lightOverlayColor;
            this.darkOverlayColor = darkOverlayColor;
            this.overlayStrength = overlayStrength;
        }

        private Color resolveOverlayColor(ThemeAppearance appearance) {
            ThemeAppearance resolved = appearance != null ? appearance : ThemeAppearance.LIGHT;
            return resolved == ThemeAppearance.DARK ? darkOverlayColor : lightOverlayColor;
        }
    }

    private final class SurfaceAttachment {
        private final Region region;
        private final ChangeListener<Boolean> visibilityListener = (obs, oldValue, newValue) -> requestRefresh();
        private final ChangeListener<Scene> sceneListener = (obs, oldScene, newScene) -> requestRefresh();

        private GlassSurfaceVariant variant;

        private SurfaceAttachment(Region region, GlassSurfaceVariant variant) {
            this.region = region;
            this.variant = variant;
            region.visibleProperty().addListener(visibilityListener);
            region.sceneProperty().addListener(sceneListener);
            // 确保玻璃层不会拦截事件
            region.setMouseTransparent(false); // 容器本身可以有点击事件
            region.setPickOnBounds(false); // 但只响应子节点或非透明区域
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

        private void applyBackdrop(WritableImage blurredImage, double snapshotWidth, double snapshotHeight) {
            Bounds bounds = region.localToScene(region.getLayoutBounds());
            if (bounds == null || bounds.getWidth() < 2 || bounds.getHeight() < 2) {
                clearBackdrop();
                return;
            }

            // 核心对齐逻辑：通过 BackgroundPosition 的负偏移量，让背景图相对于 Region 的左上角对齐到 Scene 坐标系
            BackgroundPosition position = new BackgroundPosition(
                javafx.geometry.Side.LEFT, -bounds.getMinX(), false,
                javafx.geometry.Side.TOP, -bounds.getMinY(), false
            );

            BackgroundImage image = new BackgroundImage(
                blurredImage,
                BackgroundRepeat.NO_REPEAT,
                BackgroundRepeat.NO_REPEAT,
                position,
                new BackgroundSize(snapshotWidth, snapshotHeight, false, false, false, false)
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

    private int clamp(int value, int min, int max) {
        if (max < min) {
            return min;
        }
        return Math.max(min, Math.min(max, value));
    }
}
