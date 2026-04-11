package com.example.view;

import java.util.Objects;

import javafx.application.Platform;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Labeled;
import javafx.scene.control.OverrunStyle;
import javafx.scene.control.Tooltip;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

public final class LabeledTextAutoFit {
    private static final String BINDING_KEY = "todo.labeledTextAutoFit.binding";
    private static final int DEFAULT_BINARY_SEARCH_STEPS = 9;

    private LabeledTextAutoFit() {
    }

    public record FitSpec(double minFontPx, double maxFontPx, boolean enableTooltipFallback) {
        public FitSpec {
            if (Double.isNaN(minFontPx) || minFontPx <= 0) {
                throw new IllegalArgumentException("minFontPx must be > 0");
            }
            if (Double.isNaN(maxFontPx) || maxFontPx < 0) {
                throw new IllegalArgumentException("maxFontPx must be >= 0 (0 means use computed font size)");
            }
        }

        public static FitSpec withComputedMax(double minFontPx) {
            return new FitSpec(minFontPx, 0, true);
        }

        public FitSpec withoutTooltipFallback() {
            return new FitSpec(minFontPx, maxFontPx, false);
        }
    }

    public static FitSpec titleSpec() {
        return FitSpec.withComputedMax(14);
    }

    public static FitSpec buttonSpec() {
        return FitSpec.withComputedMax(11);
    }

    public static FitSpec cardTitleSpec() {
        return FitSpec.withComputedMax(10);
    }

    public static FitSpec bodyTextSpec() {
        return FitSpec.withComputedMax(10);
    }

    public static void install(Labeled labeled, FitSpec spec) {
        if (labeled == null || spec == null) {
            return;
        }

        AutoFitBinding existing = (AutoFitBinding) labeled.getProperties().get(BINDING_KEY);
        if (existing != null) {
            existing.setSpec(spec);
            existing.queueUpdate();
            return;
        }

        AutoFitBinding binding = new AutoFitBinding(labeled, spec);
        labeled.getProperties().put(BINDING_KEY, binding);
        binding.install();
    }

    private static final class AutoFitBinding {
        private static final String TOOLTIP_INSTALLED_KEY = "todo.labeledTextAutoFit.tooltipInstalled";

        private final Labeled labeled;
        private final Text measureNode = new Text();

        private FitSpec spec;
        private boolean updateQueued;
        private double baselineFontPx;

        private AutoFitBinding(Labeled labeled, FitSpec spec) {
            this.labeled = labeled;
            this.spec = spec;
        }

        private void setSpec(FitSpec spec) {
            this.spec = spec;
        }

        private void install() {
            labeled.setWrapText(false);
            labeled.setTextOverrun(OverrunStyle.CLIP);

            labeled.textProperty().addListener(obs -> queueUpdate());
            labeled.widthProperty().addListener(obs -> queueUpdate());
            labeled.fontProperty().addListener(obs -> queueUpdate());
            labeled.graphicProperty().addListener(obs -> queueUpdate());
            labeled.contentDisplayProperty().addListener(obs -> queueUpdate());
            labeled.graphicTextGapProperty().addListener(obs -> queueUpdate());
            labeled.sceneProperty().addListener(obs -> queueUpdate());

            queueUpdate();
        }

        private void queueUpdate() {
            if (updateQueued) {
                return;
            }
            updateQueued = true;
            Platform.runLater(() -> {
                updateQueued = false;
                apply();
            });
        }

        private void apply() {
            if (labeled.getScene() == null) {
                return;
            }

            String text = labeled.getText();
            if (text == null || text.isEmpty()) {
                clearFontSizeOverride();
                clearTooltipFallback();
                return;
            }

            double width = labeled.getWidth();
            if (!(width > 0)) {
                return;
            }

            Font currentFont = labeled.getFont();
            if (currentFont == null) {
                return;
            }

            double currentFontPx = currentFont.getSize();
            if (currentFontPx > 0) {
                baselineFontPx = Math.max(baselineFontPx, currentFontPx);
            }

            double resolvedMaxFontPx = spec.maxFontPx() > 0 ? spec.maxFontPx() : (baselineFontPx > 0 ? baselineFontPx : currentFontPx);
            double resolvedMinFontPx = Math.min(spec.minFontPx(), resolvedMaxFontPx);

            double availableTextWidth = computeAvailableTextWidth();
            if (!(availableTextWidth > 0)) {
                return;
            }

            if (measureTextWidth(text, currentFont, resolvedMaxFontPx) <= availableTextWidth) {
                clearFontSizeOverride();
                clearTooltipFallback();
                return;
            }

            double best = resolvedMinFontPx;
            double low = resolvedMinFontPx;
            double high = resolvedMaxFontPx;

            for (int i = 0; i < DEFAULT_BINARY_SEARCH_STEPS; i++) {
                double mid = (low + high) / 2.0;
                if (measureTextWidth(text, currentFont, mid) <= availableTextWidth) {
                    best = mid;
                    low = mid;
                } else {
                    high = mid;
                }
            }

            best = clamp(best, resolvedMinFontPx, resolvedMaxFontPx);
            applyFontSizeOverride(best);

            boolean fitsAtMin = measureTextWidth(text, currentFont, resolvedMinFontPx) <= availableTextWidth;
            if (!fitsAtMin && spec.enableTooltipFallback()) {
                installTooltipFallback(text);
            } else {
                clearTooltipFallback();
            }
        }

        private double computeAvailableTextWidth() {
            double available = labeled.getWidth() - labeled.snappedLeftInset() - labeled.snappedRightInset();
            if (!(available > 0)) {
                return 0;
            }

            ContentDisplay display = labeled.getContentDisplay();
            if (display == ContentDisplay.LEFT || display == ContentDisplay.RIGHT) {
                Node graphic = labeled.getGraphic();
                if (graphic != null) {
                    double graphicWidth = resolveGraphicWidth(graphic);
                    if (graphicWidth > 0) {
                        available -= graphicWidth;
                        available -= Math.max(0, labeled.getGraphicTextGap());
                    }
                }
            }

            return Math.max(0, available);
        }

        private static double resolveGraphicWidth(Node graphic) {
            if (graphic == null) {
                return 0;
            }
            Bounds bounds = graphic.getLayoutBounds();
            if (bounds != null && bounds.getWidth() > 0) {
                return bounds.getWidth();
            }
            double pref = graphic.prefWidth(-1);
            return Double.isNaN(pref) ? 0 : pref;
        }

        private double measureTextWidth(String value, Font baseFont, double fontPx) {
            Font font = Font.font(baseFont.getFamily(), resolveFontWeight(baseFont), resolveFontPosture(baseFont), fontPx);
            measureNode.setFont(font);
            measureNode.setText(value);
            return Math.ceil(measureNode.getLayoutBounds().getWidth());
        }

        private static FontWeight resolveFontWeight(Font font) {
            if (font == null) {
                return FontWeight.NORMAL;
            }
            String style = font.getStyle();
            if (style == null || style.isBlank()) {
                return FontWeight.NORMAL;
            }
            FontWeight best = null;
            for (String token : style.split("\\s+")) {
                FontWeight candidate = FontWeight.findByName(token);
                if (candidate != null) {
                    best = candidate;
                }
            }
            if (best != null) {
                return best;
            }
            return style.toLowerCase().contains("bold") ? FontWeight.BOLD : FontWeight.NORMAL;
        }

        private static FontPosture resolveFontPosture(Font font) {
            if (font == null) {
                return FontPosture.REGULAR;
            }
            String style = font.getStyle();
            if (style == null || style.isBlank()) {
                return FontPosture.REGULAR;
            }
            for (String token : style.split("\\s+")) {
                FontPosture candidate = FontPosture.findByName(token);
                if (candidate != null) {
                    return candidate;
                }
            }
            return style.toLowerCase().contains("italic") ? FontPosture.ITALIC : FontPosture.REGULAR;
        }

        private void applyFontSizeOverride(double fontPx) {
            String style = stripFontSizeDeclaration(labeled.getStyle());
            StringBuilder next = new StringBuilder();
            if (style != null && !style.isBlank()) {
                next.append(style.trim());
                if (!style.trim().endsWith(";")) {
                    next.append(';');
                }
                next.append(' ');
            }
            next.append("-fx-font-size: ").append(formatPx(fontPx)).append("px;");
            labeled.setStyle(next.toString());
        }

        private void clearFontSizeOverride() {
            String current = labeled.getStyle();
            String stripped = stripFontSizeDeclaration(current);
            if (!Objects.equals(current, stripped)) {
                labeled.setStyle(stripped == null ? "" : stripped.trim());
            }
        }

        private static String stripFontSizeDeclaration(String style) {
            if (style == null || style.isBlank()) {
                return "";
            }
            StringBuilder next = new StringBuilder();
            String[] declarations = style.split(";");
            for (String declaration : declarations) {
                String trimmed = declaration == null ? "" : declaration.trim();
                if (trimmed.isEmpty()) {
                    continue;
                }
                if (trimmed.regionMatches(true, 0, "-fx-font-size", 0, "-fx-font-size".length())) {
                    continue;
                }
                if (next.length() > 0) {
                    next.append("; ");
                }
                next.append(trimmed);
            }
            if (next.length() > 0) {
                next.append(';');
            }
            return next.toString();
        }

        private static String formatPx(double value) {
            double rounded = Math.round(value * 10.0) / 10.0;
            if (Math.abs(rounded - Math.rint(rounded)) < 0.0001) {
                return String.valueOf((int) Math.rint(rounded));
            }
            return String.valueOf(rounded);
        }

        private void installTooltipFallback(String fullText) {
            if (!(labeled.getProperties().get(TOOLTIP_INSTALLED_KEY) instanceof Tooltip)) {
                if (labeled.getTooltip() != null) {
                    return;
                }
                Tooltip tooltip = new Tooltip(fullText);
                tooltip.setWrapText(true);
                tooltip.setMaxWidth(520);
                labeled.setTooltip(tooltip);
                labeled.getProperties().put(TOOLTIP_INSTALLED_KEY, tooltip);
                return;
            }

            Tooltip tooltip = (Tooltip) labeled.getProperties().get(TOOLTIP_INSTALLED_KEY);
            if (tooltip != null) {
                if (labeled.getTooltip() != null && labeled.getTooltip() != tooltip) {
                    labeled.getProperties().remove(TOOLTIP_INSTALLED_KEY);
                    return;
                }
                tooltip.setText(fullText);
                if (labeled.getTooltip() == null) {
                    labeled.setTooltip(tooltip);
                }
            }
        }

        private void clearTooltipFallback() {
            Object marker = labeled.getProperties().get(TOOLTIP_INSTALLED_KEY);
            if (!(marker instanceof Tooltip tooltip)) {
                return;
            }
            if (labeled.getTooltip() == tooltip) {
                labeled.setTooltip(null);
            }
            labeled.getProperties().remove(TOOLTIP_INSTALLED_KEY);
        }

        private static double clamp(double value, double min, double max) {
            return Math.max(min, Math.min(max, value));
        }
    }
}
