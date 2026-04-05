package com.example.view;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.geometry.Bounds;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Popup;
import javafx.stage.Screen;
import javafx.stage.Window;
import javafx.util.Duration;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.IntFunction;

final class IosWheelDateTimePopup {
    private static final double POPUP_WIDTH = 336;
    private static final double POPUP_HEIGHT = 352;
    private static final double POPUP_GAP = 8;
    private static final double CELL_HEIGHT = 40;
    private static final int VISIBLE_ROWS = 5;
    private static final double WHEEL_HEIGHT = CELL_HEIGHT * VISIBLE_ROWS;
    private static final double WHEEL_PADDING = CELL_HEIGHT * ((VISIBLE_ROWS - 1) / 2.0);

    private final Popup popup = new Popup();
    private final VBox root = new VBox(16);
    private final Label titleLabel = new Label();
    private final Label yearLabel = new Label();
    private final Button previousYearButton = new Button("<");
    private final Button nextYearButton = new Button(">");
    private final Button saveButton = new Button("保存");
    private final WheelColumn monthColumn = new WheelColumn(84, value -> value + "月");
    private final WheelColumn dayColumn = new WheelColumn(84, value -> value + "日");
    private final WheelColumn hourColumn = new WheelColumn(64, value -> String.format("%02d", value));
    private final WheelColumn minuteColumn = new WheelColumn(64, value -> String.format("%02d", value));

    private int selectedYear;
    private Consumer<LocalDateTime> onSave = value -> { };

    IosWheelDateTimePopup() {
        buildUi();
        wireInteractions();
    }

    void show(Node owner, String title, LocalDateTime seed, Consumer<LocalDateTime> saveAction) {
        if (owner == null || owner.getScene() == null) {
            return;
        }
        Window window = owner.getScene().getWindow();
        if (window == null) {
            return;
        }
        this.onSave = saveAction != null ? saveAction : value -> { };
        titleLabel.setText(title == null ? "时间" : title);
        applySeed(seed != null ? seed : LocalDateTime.now());
        double[] position = resolvePosition(owner);
        if (popup.isShowing()) {
            popup.hide();
        }
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

    private void buildUi() {
        popup.setAutoHide(true);
        popup.setAutoFix(false);
        popup.setHideOnEscape(true);

        titleLabel.getStyleClass().add("ios-wheel-title");

        previousYearButton.getStyleClass().add("ios-wheel-year-button");
        nextYearButton.getStyleClass().add("ios-wheel-year-button");
        yearLabel.getStyleClass().add("ios-wheel-year-label");

        HBox yearSwitch = new HBox(8, previousYearButton, yearLabel, nextYearButton);
        yearSwitch.setAlignment(Pos.CENTER_RIGHT);
        yearSwitch.getStyleClass().add("ios-wheel-year-switch");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox header = new HBox(12, titleLabel, spacer, yearSwitch);
        header.setAlignment(Pos.CENTER_LEFT);
        header.getStyleClass().add("ios-wheel-header");

        HBox columns = new HBox(10, monthColumn.getView(), dayColumn.getView(), hourColumn.getView(), minuteColumn.getView());
        columns.setAlignment(Pos.CENTER);
        columns.getStyleClass().add("ios-wheel-columns");

        Region selectionBox = new Region();
        selectionBox.getStyleClass().add("ios-wheel-selection-box");
        selectionBox.setMouseTransparent(true);
        selectionBox.setMinHeight(CELL_HEIGHT);
        selectionBox.setPrefHeight(CELL_HEIGHT);
        selectionBox.setMaxHeight(CELL_HEIGHT);

        Region topMask = new Region();
        topMask.getStyleClass().add("ios-wheel-mask-top");
        topMask.setMouseTransparent(true);
        topMask.setPrefHeight(WHEEL_PADDING);

        Region bottomMask = new Region();
        bottomMask.getStyleClass().add("ios-wheel-mask-bottom");
        bottomMask.setMouseTransparent(true);
        bottomMask.setPrefHeight(WHEEL_PADDING);

        StackPane wheelHost = new StackPane(columns, selectionBox, topMask, bottomMask);
        wheelHost.getStyleClass().add("ios-wheel-wheel-host");
        wheelHost.setPrefHeight(WHEEL_HEIGHT);
        StackPane.setAlignment(selectionBox, Pos.CENTER);
        StackPane.setAlignment(topMask, Pos.TOP_CENTER);
        StackPane.setAlignment(bottomMask, Pos.BOTTOM_CENTER);

        saveButton.getStyleClass().add("ios-wheel-save-button");
        saveButton.setMaxWidth(Double.MAX_VALUE);

        root.getStyleClass().add("ios-wheel-popup");
        root.setPrefWidth(POPUP_WIDTH);
        root.setPrefHeight(POPUP_HEIGHT);
        root.setMinWidth(POPUP_WIDTH);
        root.setFocusTraversable(true);
        root.getChildren().addAll(header, wheelHost, saveButton);
        popup.getContent().add(root);
    }

    private void wireInteractions() {
        previousYearButton.setOnAction(event -> adjustYear(-1));
        nextYearButton.setOnAction(event -> adjustYear(1));
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
    }

    private void applySeed(LocalDateTime seed) {
        selectedYear = seed.getYear();
        updateYearLabel();
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

    private void adjustYear(int delta) {
        selectedYear += delta;
        updateYearLabel();
        refreshDayColumn(dayColumn.getSelectedValue());
    }

    private void updateYearLabel() {
        yearLabel.setText(selectedYear + "年");
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
    }

    private double[] resolvePosition(Node owner) {
        Bounds bounds = owner.localToScreen(owner.getBoundsInLocal());
        if (bounds == null) {
            return new double[] {0, 0};
        }
        Rectangle2D screenBounds = Screen.getScreensForRectangle(bounds.getMinX(), bounds.getMinY(), bounds.getWidth(), bounds.getHeight())
            .stream()
            .findFirst()
            .orElse(Screen.getPrimary())
            .getVisualBounds();
        double minX = screenBounds.getMinX() + 12;
        double maxX = screenBounds.getMaxX() - POPUP_WIDTH - 12;
        double x = Math.max(minX, Math.min(bounds.getMinX(), maxX));
        double belowY = bounds.getMaxY() + POPUP_GAP;
        double aboveY = bounds.getMinY() - POPUP_HEIGHT - POPUP_GAP;
        double y;
        if (belowY + POPUP_HEIGHT <= screenBounds.getMaxY() - 12) {
            y = belowY;
        } else if (aboveY >= screenBounds.getMinY() + 12) {
            y = aboveY;
        } else {
            y = Math.max(screenBounds.getMinY() + 12, Math.min(belowY, screenBounds.getMaxY() - POPUP_HEIGHT - 12));
        }
        return new double[] {x, y};
    }

    private List<Integer> range(int start, int endInclusive) {
        List<Integer> values = new ArrayList<>(Math.max(endInclusive - start + 1, 0));
        for (int value = start; value <= endInclusive; value++) {
            values.add(value);
        }
        return values;
    }

    private static final class WheelColumn {
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

        private WheelColumn(double width, IntFunction<String> formatter) {
            this.formatter = formatter;
            root.getStyleClass().add("ios-wheel-column");
            root.setPrefWidth(width);
            root.setMinWidth(width);
            root.setMaxWidth(width);
            root.setPrefHeight(WHEEL_HEIGHT);

            content.getStyleClass().add("ios-wheel-column-box");
            scrollPane.setContent(content);
            scrollPane.setFitToWidth(true);
            scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
            scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
            scrollPane.getStyleClass().add("ios-wheel-column-scroll");
            scrollPane.setPrefHeight(WHEEL_HEIGHT);

            root.getChildren().add(scrollPane);
            wire();
        }

        private void wire() {
            scrollPane.vvalueProperty().addListener((obs, oldValue, newValue) -> {
                if (adjusting || values.isEmpty()) {
                    return;
                }
                updateSelectionFromScroll();
                snapDelay.playFromStart();
            });
            scrollPane.setOnMouseReleased(event -> snapToSelection());
            scrollPane.setOnScroll(event -> snapDelay.playFromStart());
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
            Region topSpacer = spacer();
            Region bottomSpacer = spacer();
            List<Node> children = new ArrayList<>(values.size() + 2);
            children.add(topSpacer);
            for (int index = 0; index < values.size(); index++) {
                int value = values.get(index);
                int itemIndex = index;
                Label label = new Label(formatter.apply(value));
                label.getStyleClass().add("ios-wheel-cell");
                label.setAlignment(Pos.CENTER);
                label.setMaxWidth(Double.MAX_VALUE);
                label.setMinHeight(CELL_HEIGHT);
                label.setPrefHeight(CELL_HEIGHT);
                label.setOnMouseClicked(event -> selectIndex(itemIndex, true));
                newLabels.add(label);
                children.add(label);
            }
            children.add(bottomSpacer);
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
            double targetOffset = selectedIndex * CELL_HEIGHT;
            adjusting = true;
            scrollPane.setVvalue(maxOffset <= 0 ? 0 : targetOffset / maxOffset);
            Platform.runLater(() -> adjusting = false);
        }

        private double currentOffset() {
            return scrollPane.getVvalue() * maxOffset();
        }

        private double maxOffset() {
            return Math.max((values.size() - 1) * CELL_HEIGHT, 0);
        }

        private int indexForOffset(double offset) {
            if (values.isEmpty()) {
                return 0;
            }
            int index = (int) Math.round(offset / CELL_HEIGHT);
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
            spacer.setMinHeight(WHEEL_PADDING);
            spacer.setPrefHeight(WHEEL_PADDING);
            return spacer;
        }
    }
}
