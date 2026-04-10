package com.example.view;

import com.example.controller.MainController;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Popup;
import javafx.stage.Window;
import javafx.util.Duration;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.IntFunction;

final class IosWheelDateTimePopup {
    private MainController controller;

    private static final double BASE_POPUP_WIDTH = 328;
    private static final double BASE_POPUP_HEIGHT = 288;
    private static final double POPUP_GAP = 8;
    private static final int VISIBLE_ROWS = 5;

    private static final double BASE_CELL_HEIGHT = 34;

    private static final double BASE_POPUP_PADDING = 14;
    private static final double BASE_POPUP_SPACING = 12;
    private static final double BASE_COLUMN_SPACING = 6;

    private static final double BASE_MONTH_WIDTH = 66;
    private static final double BASE_DAY_WIDTH = 66;
    private static final double BASE_HOUR_WIDTH = 50;
    private static final double BASE_MINUTE_WIDTH = 50;
    private static final double BASE_YEAR_WIDTH = 44;

    private static final double BASE_TITLE_FONT_SIZE = 15;
    private static final double BASE_CELL_FONT_SIZE = 15;
    private static final double BASE_SAVE_BUTTON_PADDING_V = 9;
    private static final double BASE_SAVE_BUTTON_PADDING_H = 16;

    private static final int YEAR_MIN = 2000;
    private static final int YEAR_MAX = 2099;

    private static final double WINDOW_MARGIN = 12;

    private final Popup popup = new Popup();
    private final VBox root = new VBox();
    private final HBox header = new HBox();
    private final HBox columns = new HBox();
    private final StackPane wheelHost = new StackPane();

    private final Region selectionBox = new Region();
    private final Region topMask = new Region();
    private final Region bottomMask = new Region();

    private final Label titleLabel = new Label();
    private final Button saveButton = new Button();
    private final WheelColumn monthColumn = new WheelColumn(BASE_MONTH_WIDTH, this::formatMonth);
    private final WheelColumn dayColumn = new WheelColumn(BASE_DAY_WIDTH, this::formatDay);
    private final WheelColumn hourColumn = new WheelColumn(BASE_HOUR_WIDTH, value -> String.format("%02d", value));
    private final WheelColumn minuteColumn = new WheelColumn(BASE_MINUTE_WIDTH, value -> String.format("%02d", value));
    private final WheelColumn yearColumn = new WheelColumn(BASE_YEAR_WIDTH, IosWheelDateTimePopup::formatYear2);

    private int selectedYear;
    private Consumer<LocalDateTime> onSave = value -> { };

    private Window monitoredWindow;
    private Node monitoredOwner;
    private final ChangeListener<Number> windowBoundsListener = (obs, oldValue, newValue) -> repositionAndResize();

    private WheelSizing sizing = WheelSizing.base();

    IosWheelDateTimePopup() {
        this(null);
    }

    IosWheelDateTimePopup(MainController controller) {
        this.controller = controller;
        buildUi();
        wireInteractions();
        refreshLocalizedText();
    }

    void show(Node owner, String title, LocalDateTime seed, Consumer<LocalDateTime> saveAction) {
        if (owner == null || owner.getScene() == null) {
            return;
        }
        Window window = owner.getScene().getWindow();
        if (window == null) {
            return;
        }

        WheelSizing newSizing = resolveSizing(window);
        applySizing(newSizing);

        this.onSave = saveAction != null ? saveAction : value -> { };
        refreshLocalizedText();
        titleLabel.setText(title == null ? text("info.time") : title);
        applySeed(seed != null ? seed : LocalDateTime.now());
        double[] position = resolvePosition(owner);
        if (popup.isShowing()) {
            popup.hide();
        }
        monitoredOwner = owner;
        attachWindowListeners(window);
        popup.show(window, position[0], position[1]);
        Platform.runLater(() -> {
            alignColumns();
            root.requestFocus();
        });
    }

    void hide() {
        popup.hide();
    }

    static int daysInMonth(int year, int month) {
        return YearMonth.of(year, month).lengthOfMonth();
    }

    static int clampDay(int year, int month, int day) {
        return Math.max(1, Math.min(day, daysInMonth(year, month)));
    }

    static String formatYear2(int year) {
        return String.format("%02d", Math.floorMod(year, 100));
    }

    static int clampYear(int year) {
        return Math.max(YEAR_MIN, Math.min(year, YEAR_MAX));
    }

    private void buildUi() {
        popup.setAutoHide(true);
        popup.setAutoFix(false);
        popup.setHideOnEscape(true);
        popup.setOnHidden(event -> {
            monitoredOwner = null;
            detachWindowListeners();
        });

        titleLabel.getStyleClass().add("ios-wheel-title");

        header.getChildren().add(titleLabel);
        header.setAlignment(Pos.CENTER_LEFT);
        header.getStyleClass().add("ios-wheel-header");

        columns.getChildren().addAll(
            monthColumn.getView(),
            dayColumn.getView(),
            hourColumn.getView(),
            minuteColumn.getView(),
            yearColumn.getView()
        );
        columns.setAlignment(Pos.CENTER);
        columns.getStyleClass().add("ios-wheel-columns");

        selectionBox.getStyleClass().add("ios-wheel-selection-box");
        selectionBox.setMouseTransparent(true);

        topMask.getStyleClass().add("ios-wheel-mask-top");
        topMask.setMouseTransparent(true);

        bottomMask.getStyleClass().add("ios-wheel-mask-bottom");
        bottomMask.setMouseTransparent(true);

        wheelHost.getChildren().addAll(columns, selectionBox, topMask, bottomMask);
        wheelHost.getStyleClass().add("ios-wheel-wheel-host");
        StackPane.setAlignment(selectionBox, Pos.CENTER);
        StackPane.setAlignment(topMask, Pos.TOP_CENTER);
        StackPane.setAlignment(bottomMask, Pos.BOTTOM_CENTER);

        saveButton.getStyleClass().add("ios-wheel-save-button");
        saveButton.setMaxWidth(Double.MAX_VALUE);

        root.getStyleClass().add("ios-wheel-popup");
        root.setFocusTraversable(true);
        root.getChildren().addAll(this.header, wheelHost, saveButton);
        popup.getContent().add(root);

        applySizing(WheelSizing.base());
    }

    private void wireInteractions() {
        saveButton.setOnAction(event -> {
            hide();
            onSave.accept(buildSelection());
        });
        root.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ESCAPE) {
                hide();
                event.consume();
            }
        });
        monthColumn.setSelectionListener(value -> refreshDayColumn(dayColumn.getSelectedValue()));
        yearColumn.setSelectionListener(value -> {
            selectedYear = value;
            refreshDayColumn(dayColumn.getSelectedValue());
        });
    }

    private void applySeed(LocalDateTime seed) {
        selectedYear = clampYear(seed.getYear());
        yearColumn.setItems(range(YEAR_MIN, YEAR_MAX), selectedYear);
        monthColumn.setItems(range(1, 12), seed.getMonthValue());
        refreshDayColumn(seed.getDayOfMonth());
        hourColumn.setItems(range(0, 23), seed.getHour());
        minuteColumn.setItems(range(0, 59), seed.getMinute());
    }

    private void refreshDayColumn(int preferredDay) {
        int month = monthColumn.getSelectedValue();
        int selectedDay = clampDay(selectedYear, month, preferredDay);
        dayColumn.setItems(range(1, daysInMonth(selectedYear, month)), selectedDay);
    }

    private LocalDateTime buildSelection() {
        return LocalDateTime.of(
            selectedYear,
            monthColumn.getSelectedValue(),
            clampDay(selectedYear, monthColumn.getSelectedValue(), dayColumn.getSelectedValue()),
            hourColumn.getSelectedValue(),
            minuteColumn.getSelectedValue()
        );
    }

    private void alignColumns() {
        monthColumn.snapToSelection();
        dayColumn.snapToSelection();
        hourColumn.snapToSelection();
        minuteColumn.snapToSelection();
        yearColumn.snapToSelection();
    }

    private WheelSizing resolveSizing(Window window) {
        if (window == null) {
            return WheelSizing.base();
        }
        double availW = Math.max(0, window.getWidth() - WINDOW_MARGIN * 2);
        double availH = Math.max(0, window.getHeight() - WINDOW_MARGIN * 2);
        double scaleW = availW / BASE_POPUP_WIDTH;
        double scaleH = availH / BASE_POPUP_HEIGHT;
        double scale = Math.min(1.0, Math.min(scaleW, scaleH));
        if (!Double.isFinite(scale) || scale <= 0) {
            scale = 1.0;
        }
        return new WheelSizing(scale);
    }

    private void applySizing(WheelSizing sizing) {
        this.sizing = sizing;

        root.setPrefSize(sizing.popupWidth, sizing.popupHeight);
        root.setMinSize(sizing.popupWidth, sizing.popupHeight);
        root.setPadding(new Insets(sizing.popupPadding));
        root.setSpacing(sizing.popupSpacing);

        titleLabel.setStyle(String.format("-fx-font-size: %.1fpx;", sizing.titleFontSize));

        columns.setSpacing(sizing.columnSpacing);

        wheelHost.setPrefHeight(sizing.wheelHeight);
        wheelHost.setMinHeight(sizing.wheelHeight);

        selectionBox.setMinHeight(sizing.cellHeight);
        selectionBox.setPrefHeight(sizing.cellHeight);
        selectionBox.setMaxHeight(sizing.cellHeight);

        topMask.setPrefHeight(sizing.wheelPadding);
        bottomMask.setPrefHeight(sizing.wheelPadding);

        saveButton.setPadding(new Insets(
            sizing.saveButtonPaddingV,
            sizing.saveButtonPaddingH,
            sizing.saveButtonPaddingV,
            sizing.saveButtonPaddingH
        ));

        monthColumn.applySizing(sizing.monthWidth, sizing);
        dayColumn.applySizing(sizing.dayWidth, sizing);
        hourColumn.applySizing(sizing.hourWidth, sizing);
        minuteColumn.applySizing(sizing.minuteWidth, sizing);
        yearColumn.applySizing(sizing.yearWidth, sizing);

        Platform.runLater(this::alignColumns);
    }

    private void repositionAndResize() {
        if (!popup.isShowing() || monitoredOwner == null) {
            return;
        }
        Window window = monitoredOwner.getScene() != null ? monitoredOwner.getScene().getWindow() : monitoredWindow;
        if (window == null) {
            return;
        }

        LocalDateTime current = buildSelection();
        WheelSizing newSizing = resolveSizing(window);
        applySizing(newSizing);
        applySeed(current);

        double[] position = resolvePosition(monitoredOwner);
        popup.setX(position[0]);
        popup.setY(position[1]);
    }

    private void attachWindowListeners(Window window) {
        detachWindowListeners();
        if (window == null) {
            return;
        }
        monitoredWindow = window;
        monitoredWindow.xProperty().addListener(windowBoundsListener);
        monitoredWindow.yProperty().addListener(windowBoundsListener);
        monitoredWindow.widthProperty().addListener(windowBoundsListener);
        monitoredWindow.heightProperty().addListener(windowBoundsListener);
    }

    private void detachWindowListeners() {
        if (monitoredWindow == null) {
            return;
        }
        monitoredWindow.xProperty().removeListener(windowBoundsListener);
        monitoredWindow.yProperty().removeListener(windowBoundsListener);
        monitoredWindow.widthProperty().removeListener(windowBoundsListener);
        monitoredWindow.heightProperty().removeListener(windowBoundsListener);
        monitoredWindow = null;
    }

    private double[] resolvePosition(Node owner) {
        Bounds bounds = owner.localToScreen(owner.getBoundsInLocal());
        if (bounds == null) {
            return new double[] {0, 0};
        }
        Window window = owner.getScene() != null ? owner.getScene().getWindow() : null;
        if (window == null) {
            return new double[] {bounds.getMinX(), bounds.getMaxY() + POPUP_GAP};
        }

        double windowMinX = window.getX();
        double windowMinY = window.getY();
        double windowMaxX = windowMinX + window.getWidth();
        double windowMaxY = windowMinY + window.getHeight();

        double minX = windowMinX + WINDOW_MARGIN;
        double maxX = windowMaxX - sizing.popupWidth - WINDOW_MARGIN;
        double x;
        if (maxX < minX) {
            x = windowMinX + Math.max(0, (window.getWidth() - sizing.popupWidth) / 2.0);
        } else {
            x = clamp(bounds.getMinX(), minX, maxX);
        }
        double belowY = bounds.getMaxY() + POPUP_GAP;
        double aboveY = bounds.getMinY() - sizing.popupHeight - POPUP_GAP;
        double y;
        if (belowY + sizing.popupHeight <= windowMaxY - WINDOW_MARGIN) {
            y = belowY;
        } else if (aboveY >= windowMinY + WINDOW_MARGIN) {
            y = aboveY;
        } else {
            double minY = windowMinY + WINDOW_MARGIN;
            double maxY = windowMaxY - sizing.popupHeight - WINDOW_MARGIN;
            if (maxY < minY) {
                y = windowMinY + Math.max(0, (window.getHeight() - sizing.popupHeight) / 2.0);
            } else {
                y = clamp(belowY, minY, maxY);
            }
        }
        return new double[] {x, y};
    }

    private static double clamp(double value, double min, double max) {
        if (max < min) {
            return min;
        }
        return Math.max(min, Math.min(value, max));
    }

    private List<Integer> range(int start, int endInclusive) {
        List<Integer> values = new ArrayList<>(Math.max(endInclusive - start + 1, 0));
        for (int value = start; value <= endInclusive; value++) {
            values.add(value);
        }
        return values;
    }

    private void refreshLocalizedText() {
        saveButton.setText(text("common.save"));
    }

    private String formatMonth(int value) {
        return text("wheel.month.option", value);
    }

    private String formatDay(int value) {
        return text("wheel.day.option", value);
    }

    private String text(String key, Object... args) {
        if (controller != null) {
            return controller.text(key, args);
        }
        return switch (key) {
            case "common.save" -> "保存";
            case "info.time" -> "时间";
            case "wheel.year.label" -> args[0] + "年";
            case "wheel.month.option" -> args[0] + "月";
            case "wheel.day.option" -> args[0] + "日";
            default -> key;
        };
    }

    private static final class WheelColumn {
        private static final double DRAG_START_THRESHOLD = 3;
        private final StackPane root = new StackPane();
        private final ScrollPane scrollPane = new ScrollPane();
        private final VBox content = new VBox();
        private final PauseTransition snapDelay = new PauseTransition(Duration.millis(120));
        private final IntFunction<String> formatter;

        private List<Integer> values = List.of();
        private List<Label> labels = List.of();
        private java.util.function.IntConsumer selectionListener = value -> { };
        private boolean adjusting;
        private int selectedIndex;
        private boolean dragging;
        private boolean suppressClick;
        private double dragPressScreenY;
        private double dragPressOffset;

        private double cellHeight;
        private double wheelPadding;
        private double wheelHeight;
        private double fontSize;
        private Region topSpacer;
        private Region bottomSpacer;

        private WheelColumn(double width, IntFunction<String> formatter) {
            this.formatter = formatter;
            root.getStyleClass().add("ios-wheel-column");

            content.getStyleClass().add("ios-wheel-column-box");
            scrollPane.setContent(content);
            scrollPane.setFitToWidth(true);
            scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
            scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
            scrollPane.getStyleClass().add("ios-wheel-column-scroll");

            root.getChildren().add(scrollPane);
            applySizing(width, WheelSizing.base());
            wire();
        }

        private void wire() {
            scrollPane.vvalueProperty().addListener((obs, oldValue, newValue) -> {
                if (adjusting || values.isEmpty()) {
                    return;
                }
                updateSelectionFromScroll();
                if (!dragging) {
                    snapDelay.playFromStart();
                }
            });
            scrollPane.setOnScroll(event -> snapDelay.playFromStart());

            scrollPane.addEventHandler(MouseEvent.MOUSE_PRESSED, event -> {
                if (event.getButton() != MouseButton.PRIMARY) {
                    return;
                }
                dragging = false;
                suppressClick = false;
                dragPressScreenY = event.getScreenY();
                dragPressOffset = currentOffset();
                snapDelay.stop();
            });

            scrollPane.addEventHandler(MouseEvent.MOUSE_DRAGGED, event -> {
                if (!event.isPrimaryButtonDown()) {
                    return;
                }
                double deltaY = event.getScreenY() - dragPressScreenY;
                if (!dragging) {
                    if (Math.abs(deltaY) < DRAG_START_THRESHOLD) {
                        return;
                    }
                    dragging = true;
                    suppressClick = true;
                }
                double maxOffset = maxOffset();
                double targetOffset = clamp(dragPressOffset - deltaY, 0, maxOffset);
                scrollPane.setVvalue(maxOffset <= 0 ? 0 : targetOffset / maxOffset);
                snapDelay.stop();
                event.consume();
            });

            scrollPane.addEventHandler(MouseEvent.MOUSE_RELEASED, event -> {
                if (!dragging) {
                    return;
                }
                dragging = false;
                snapToSelection();
                event.consume();
            });
            snapDelay.setOnFinished(event -> snapToSelection());
        }

        private StackPane getView() {
            return root;
        }

        private void setSelectionListener(java.util.function.IntConsumer selectionListener) {
            this.selectionListener = selectionListener != null ? selectionListener : value -> { };
        }

        private void setItems(List<Integer> values, int selectedValue) {
            this.values = List.copyOf(values);
            List<Label> newLabels = new ArrayList<>(values.size());
            topSpacer = spacer();
            bottomSpacer = spacer();
            List<Node> children = new ArrayList<>(values.size() + 2);
            children.add(topSpacer);
            for (int index = 0; index < values.size(); index++) {
                int value = values.get(index);
                int itemIndex = index;
                Label label = new Label(formatter.apply(value));
                label.getStyleClass().add("ios-wheel-cell");
                label.setAlignment(Pos.CENTER);
                label.setMaxWidth(Double.MAX_VALUE);
                label.setMinHeight(cellHeight);
                label.setPrefHeight(cellHeight);
                label.setStyle(String.format("-fx-font-size: %.1fpx;", fontSize));
                label.setOnMouseClicked(event -> {
                    if (suppressClick) {
                        return;
                    }
                    selectIndex(itemIndex, true);
                });
                newLabels.add(label);
                children.add(label);
            }
            children.add(this.bottomSpacer);
            content.getChildren().setAll(children);
            this.labels = List.copyOf(newLabels);

            int newIndex = Math.max(0, this.values.indexOf(selectedValue));
            if (newIndex < 0) {
                newIndex = 0;
            }
            selectIndex(newIndex, false);
            Platform.runLater(this::snapToSelection);
        }

        private void selectIndex(int index, boolean notify) {
            if (values.isEmpty()) {
                selectedIndex = 0;
                return;
            }
            selectedIndex = Math.max(0, Math.min(index, values.size() - 1));
            updateCellStyles();
            if (notify) {
                selectionListener.accept(values.get(selectedIndex));
            }
            snapToSelection();
        }

        private void updateSelectionFromScroll() {
            int index = indexForOffset(currentOffset());
            if (index != selectedIndex) {
                selectedIndex = index;
                updateCellStyles();
                selectionListener.accept(values.get(selectedIndex));
            }
        }

        private void updateCellStyles() {
            for (int index = 0; index < labels.size(); index++) {
                Label label = labels.get(index);
                if (index == selectedIndex) {
                    if (!label.getStyleClass().contains("ios-wheel-cell-selected")) {
                        label.getStyleClass().add("ios-wheel-cell-selected");
                    }
                } else {
                    label.getStyleClass().remove("ios-wheel-cell-selected");
                }
            }
        }

        private void snapToSelection() {
            if (values.isEmpty()) {
                return;
            }
            double maxOffset = maxOffset();
            double targetOffset = selectedIndex * cellHeight;
            adjusting = true;
            scrollPane.setVvalue(maxOffset <= 0 ? 0 : targetOffset / maxOffset);
            Platform.runLater(() -> adjusting = false);
        }

        private double currentOffset() {
            return scrollPane.getVvalue() * maxOffset();
        }

        private double maxOffset() {
            return Math.max((values.size() - 1) * cellHeight, 0);
        }

        private int indexForOffset(double offset) {
            if (values.isEmpty()) {
                return 0;
            }
            int index = (int) Math.round(offset / cellHeight);
            return Math.max(0, Math.min(index, values.size() - 1));
        }

        private int getSelectedValue() {
            if (values.isEmpty()) {
                return 1;
            }
            return values.get(selectedIndex);
        }

        private Region spacer() {
            Region spacer = new Region();
            spacer.setMinHeight(wheelPadding);
            spacer.setPrefHeight(wheelPadding);
            return spacer;
        }

        private void applySizing(double width, WheelSizing sizing) {
            root.setPrefWidth(width);
            root.setMinWidth(width);
            root.setMaxWidth(width);

            cellHeight = Math.max(1, sizing.cellHeight);
            wheelPadding = Math.max(0, sizing.wheelPadding);
            wheelHeight = Math.max(1, sizing.wheelHeight);
            fontSize = Math.max(1, sizing.cellFontSize);

            root.setPrefHeight(wheelHeight);
            scrollPane.setPrefHeight(wheelHeight);

            if (topSpacer != null) {
                topSpacer.setMinHeight(wheelPadding);
                topSpacer.setPrefHeight(wheelPadding);
            }
            if (bottomSpacer != null) {
                bottomSpacer.setMinHeight(wheelPadding);
                bottomSpacer.setPrefHeight(wheelPadding);
            }
            for (Label label : labels) {
                label.setMinHeight(cellHeight);
                label.setPrefHeight(cellHeight);
                label.setStyle(String.format("-fx-font-size: %.1fpx;", fontSize));
            }

            Platform.runLater(this::snapToSelection);
        }
    }

    private static final class WheelSizing {
        final double scale;
        final double popupWidth;
        final double popupHeight;
        final double popupPadding;
        final double popupSpacing;
        final double columnSpacing;
        final double cellHeight;
        final double wheelHeight;
        final double wheelPadding;
        final double titleFontSize;
        final double cellFontSize;
        final double monthWidth;
        final double dayWidth;
        final double hourWidth;
        final double minuteWidth;
        final double yearWidth;
        final double saveButtonPaddingV;
        final double saveButtonPaddingH;

        private WheelSizing(double scale) {
            this.scale = scale;
            popupWidth = Math.round(BASE_POPUP_WIDTH * scale);
            popupHeight = Math.round(BASE_POPUP_HEIGHT * scale);
            popupPadding = Math.round(BASE_POPUP_PADDING * scale);
            popupSpacing = Math.round(BASE_POPUP_SPACING * scale);
            columnSpacing = Math.round(BASE_COLUMN_SPACING * scale);
            cellHeight = Math.round(BASE_CELL_HEIGHT * scale);
            wheelHeight = cellHeight * VISIBLE_ROWS;
            wheelPadding = cellHeight * ((VISIBLE_ROWS - 1) / 2.0);
            titleFontSize = BASE_TITLE_FONT_SIZE * scale;
            cellFontSize = BASE_CELL_FONT_SIZE * scale;
            monthWidth = Math.round(BASE_MONTH_WIDTH * scale);
            dayWidth = Math.round(BASE_DAY_WIDTH * scale);
            hourWidth = Math.round(BASE_HOUR_WIDTH * scale);
            minuteWidth = Math.round(BASE_MINUTE_WIDTH * scale);
            yearWidth = Math.round(BASE_YEAR_WIDTH * scale);
            saveButtonPaddingV = Math.round(BASE_SAVE_BUTTON_PADDING_V * scale);
            saveButtonPaddingH = Math.round(BASE_SAVE_BUTTON_PADDING_H * scale);
        }

        static WheelSizing base() {
            return new WheelSizing(1.0);
        }
    }
}
