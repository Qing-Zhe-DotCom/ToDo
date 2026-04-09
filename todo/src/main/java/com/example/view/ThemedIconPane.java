package com.example.view;

import java.util.Objects;
import java.util.function.Supplier;

import com.example.application.IconKey;
import com.example.application.IconTheme;
import com.example.application.IconThemeService;

import javafx.scene.Group;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Rectangle;

public final class ThemedIconPane extends Pane {
    private final IconKey iconKey;
    private final double size;
    private final Supplier<IconTheme> iconThemeSupplier;
    private final IconThemeService iconThemeService;
    private final SvgIconRenderer iconRenderer;
    private final Class<?> resourceAnchor;

    public ThemedIconPane(
        IconKey iconKey,
        String title,
        double size,
        Supplier<IconTheme> iconThemeSupplier,
        IconThemeService iconThemeService,
        SvgIconRenderer iconRenderer,
        Class<?> resourceAnchor
    ) {
        this.iconKey = Objects.requireNonNull(iconKey, "iconKey");
        this.size = size;
        this.iconThemeSupplier = Objects.requireNonNull(iconThemeSupplier, "iconThemeSupplier");
        this.iconThemeService = Objects.requireNonNull(iconThemeService, "iconThemeService");
        this.iconRenderer = Objects.requireNonNull(iconRenderer, "iconRenderer");
        this.resourceAnchor = Objects.requireNonNull(resourceAnchor, "resourceAnchor");

        getStyleClass().add("sidebar-svg-icon");
        setMinSize(size, size);
        setPrefSize(size, size);
        setMaxSize(size, size);

        Rectangle clip = new Rectangle(size, size);
        clip.setArcWidth(size);
        clip.setArcHeight(size);
        setClip(clip);

        if (title != null && !title.isEmpty()) {
            setAccessibleText(title);
            Tooltip.install(this, new Tooltip(title));
        }
        refreshIcon();
    }

    public IconKey getIconKey() {
        return iconKey;
    }

    public void refreshIcon() {
        IconTheme iconTheme = iconThemeSupplier.get();
        String resourcePath = iconThemeService.resolveIconResource(resourceAnchor, iconTheme, iconKey);
        Group iconGroup = iconRenderer.loadSvgGraphic(resourceAnchor, resourcePath, size);
        getChildren().setAll(iconGroup);
    }
}
