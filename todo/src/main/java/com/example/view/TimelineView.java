package com.example.view;

import com.example.controller.ScheduleCompletionMutation;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.example.application.IconKey;
import com.example.application.ScheduleOccurrenceProjector;
import com.example.application.WheelModifier;
import com.example.controller.MainController;
import com.example.model.Schedule;
import com.example.model.ScheduleItem;

import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Point2D;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.OverrunStyle;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;

import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.util.Duration;
import javafx.scene.input.ScrollEvent;
import javafx.util.StringConverter;

public class TimelineView implements View, ScheduleCompletionParticipant {

    private static final double BASE_CELL_WIDTH_PX = 90;
    private static final long BASE_CELL_MINUTES = 360; // 6 hours per grid cell
    private static final double ZOOM_STEP = 1.12;
    private static final double MIN_ZOOM = 0.25;
    private static final double MAX_ZOOM = 16.0;
    private static final String PREF_TIMELINE_ZOOM_FACTOR_KEY = "todo.timeline.zoom.factor";
    private static final Duration ZOOM_ANIMATION_DURATION = Duration.millis(180);
    private static final Interpolator ZOOM_INTERPOLATOR = Interpolator.SPLINE(0.2, 0.76, 0.22, 1.0);

    private static final double LEFT_PADDING = 36;
    private static final double RIGHT_PADDING = 36;
    private static final double AXIS_LABEL_Y = 10;
    private static final double AXIS_LINE_Y = 40;
    private static final double TRACK_TOP = 56;
    private static final double CARD_HEIGHT = 44;
    private static final double CARD_STACK_OFFSET = 52;
    private static final double CARD_INSET_X = 8;
    private static final double BOTTOM_PADDING = 48;
    private static final double MIN_INLINE_TITLE_WIDTH = 60;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MM/dd");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("MM/dd HH:mm");
    private static final DateTimeFormatter RANGE_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final MainController controller;

    private VBox root;
    private StackPane timelineContainer;
    private Pane timelinePane;
    private Pane timelineBackgroundLayer;
    private Pane timelineDecorationLayer;
    private Pane timelineAxisLayer;
    private Pane timelineCardsLayer;
    private ScrollPane scrollPane;
    private Label timelineStateLabel;
    private DatePicker startDatePicker;
    private DatePicker endDatePicker;
    private StackPane rangeIconBox;

    private AnimationTimer autoScrollTimer;
    private long lastUpdate = 0;
    private double scrollVelocity = 0;
    private List<Schedule> loadedSchedules = new ArrayList<>();
    private final List<TimelineAxisDayNode> axisDayNodes = new ArrayList<>();
    private final List<TimelineGridMarkerNode> gridMarkerNodes = new ArrayList<>();
    private final List<TimelineCardNode> renderedCardNodes = new ArrayList<>();
    private final DoubleProperty displayZoomFactorProperty = new SimpleDoubleProperty(1.0);

    private double zoomFactor = 1.0;
    private double displayZoomFactor = 1.0;
    private Timeline zoomTimeline;
    private Double activeZoomAnchorMinuteOffset;
    private double activeZoomAnchorViewportX;
    private Runnable deferredViewportAction;
    private LocalDateTime lastRangeStartAt;
    private LocalDateTime lastRangeEndAtExclusive;
    private LocalDate renderedMinDate;
    private LocalDate renderedMaxDate;
    private double renderedPaneHeight = 320;
    private Rectangle trackBackground;
    private Line topSeparator;
    private Line bottomSeparator;
    private Line axisLine;

    public TimelineView(MainController controller) {
        this.controller = controller;
        loadZoomPreference();
        displayZoomFactor = zoomFactor;
        displayZoomFactorProperty.set(zoomFactor);
        initializeUI();
        startAutoScroll();
    }

    public double getZoomFactor() {
        return zoomFactor;
    }

    public void zoomIn() {
        zoomBy(ZOOM_STEP, null);
    }

    public void zoomOut() {
        zoomBy(1.0 / ZOOM_STEP, null);
    }

    public void setZoomFactor(double newZoomFactor) {
        setZoomFactorInternal(newZoomFactor, null);
    }

    private void zoomBy(double multiplier, Double anchorViewportX) {
        if (multiplier <= 0) {
            return;
        }
        setZoomFactorInternal(zoomFactor * multiplier, anchorViewportX);
    }

    private void setZoomFactorInternal(double newZoomFactor, Double anchorViewportX) {
        double clamped = clampDouble(newZoomFactor, MIN_ZOOM, MAX_ZOOM);
        if (Math.abs(clamped - zoomFactor) < 0.0001 && Math.abs(clamped - displayZoomFactor) < 0.0001) {
            return;
        }
        zoomFactor = clamped;
        persistZoomPreference();

        if (!hasRenderableTimeline()) {
            stopZoomTimeline();
            setDisplayZoomFactor(clamped);
            return;
        }

        double viewportWidth = resolveViewportWidth();
        double safeAnchorViewportX = anchorViewportX != null
            ? anchorViewportX.doubleValue()
            : viewportWidth > 0 ? viewportWidth / 2.0 : 0.0;
        double anchorMinuteOffset = resolveMinuteOffsetAtViewportX(safeAnchorViewportX);
        animateZoomTo(clamped, anchorMinuteOffset, safeAnchorViewportX);
    }

    private void animateZoomTo(double targetZoomFactor, double anchorMinuteOffset, double anchorViewportX) {
        stopZoomTimeline();

        activeZoomAnchorMinuteOffset = anchorMinuteOffset;
        activeZoomAnchorViewportX = anchorViewportX;

        if (resolveViewportWidth() <= 0) {
            setDisplayZoomFactor(targetZoomFactor);
            scheduleDeferredViewportAction(() -> applyViewportAnchor(anchorMinuteOffset, anchorViewportX));
            activeZoomAnchorMinuteOffset = null;
            return;
        }

        double startZoomFactor = displayZoomFactor;
        if (Math.abs(startZoomFactor - targetZoomFactor) < 0.0001) {
            applyViewportAnchor(anchorMinuteOffset, anchorViewportX);
            activeZoomAnchorMinuteOffset = null;
            return;
        }

        zoomTimeline = new Timeline(
            new KeyFrame(Duration.ZERO, new KeyValue(displayZoomFactorProperty, startZoomFactor)),
            new KeyFrame(
                ZOOM_ANIMATION_DURATION,
                new KeyValue(displayZoomFactorProperty, targetZoomFactor, ZOOM_INTERPOLATOR)
            )
        );
        zoomTimeline.setOnFinished(event -> {
            setDisplayZoomFactor(targetZoomFactor);
            applyViewportAnchor(anchorMinuteOffset, anchorViewportX);
            activeZoomAnchorMinuteOffset = null;
            zoomTimeline = null;
        });
        zoomTimeline.playFromStart();
    }

    private long resolveTotalMinutes() {
        if (lastRangeStartAt == null || lastRangeEndAtExclusive == null) {
            return 0;
        }
        long minutes = ChronoUnit.MINUTES.between(lastRangeStartAt, lastRangeEndAtExclusive);
        return Math.max(0, minutes);
    }

    private double resolveViewportCenterMinuteOffset() {
        double viewportWidth = resolveViewportWidth();
        return resolveMinuteOffsetAtViewportX(viewportWidth / 2.0);
    }

    private void loadZoomPreference() {
        zoomFactor = clampDouble(
            controller.parseDoublePreference(PREF_TIMELINE_ZOOM_FACTOR_KEY, 1.0),
            MIN_ZOOM,
            MAX_ZOOM
        );
    }

    private void persistZoomPreference() {
        controller.putPreference(PREF_TIMELINE_ZOOM_FACTOR_KEY, Double.toString(zoomFactor));
    }

    private static double clampDouble(double value, double min, double max) {
        if (Double.isNaN(value)) {
            return min;
        }
        return Math.max(min, Math.min(max, value));
    }

    private void initializeUI() {
        root = new VBox(10);
        root.getStyleClass().add("main-content");
        root.setPadding(new Insets(15));

        HBox header = createHeader();

        timelineContainer = new StackPane();
        timelineContainer.getStyleClass().add("timeline-container");
        VBox.setVgrow(timelineContainer, Priority.ALWAYS);

        timelinePane = new Pane();
        timelinePane.getStyleClass().add("timeline-pane");
        timelineBackgroundLayer = new Pane();
        timelineDecorationLayer = new Pane();
        timelineAxisLayer = new Pane();
        timelineCardsLayer = new Pane();
        timelinePane.getChildren().addAll(
            timelineBackgroundLayer,
            timelineDecorationLayer,
            timelineAxisLayer,
            timelineCardsLayer
        );

        scrollPane = new ScrollPane(timelinePane);
        scrollPane.getStyleClass().add("timeline-scroll");
        scrollPane.setFitToHeight(false);
        scrollPane.setPannable(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.ALWAYS);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.viewportBoundsProperty().addListener((obs, oldBounds, newBounds) -> {
            if (newBounds != null && newBounds.getWidth() > 0 && deferredViewportAction != null) {
                Runnable action = deferredViewportAction;
                deferredViewportAction = null;
                action.run();
            }
        });
        displayZoomFactorProperty.addListener((obs, oldValue, newValue) -> {
            displayZoomFactor = newValue.doubleValue();
            updateRenderedTimelineGeometry();
        });

        timelinePane.setOnScroll(event -> {
            if (event.getDeltaY() != 0) {
                WheelModifier modifier = controller.getTimelineZoomWheelModifier();
                if (modifier != null && modifier.matches(event)) {
                    double multiplier = event.getDeltaY() > 0 ? ZOOM_STEP : (1.0 / ZOOM_STEP);
                    zoomBy(multiplier, resolveMouseAnchorViewportX(event));
                    event.consume();
                    return;
                }

                TimelineMetrics metrics = buildMetrics(displayZoomFactor);
                double viewportWidth = resolveViewportWidth();
                double maxScroll = Math.max(0.0, metrics.contentWidth() - viewportWidth);

                if (maxScroll > 0) {
                    double pixelDelta = Math.signum(event.getDeltaY()) * (BASE_CELL_MINUTES * metrics.pixelsPerMinute());
                    double hvalueDelta = pixelDelta / maxScroll;
                    double hvalue = TimelineZoomGeometry.clamp(scrollPane.getHvalue() - hvalueDelta, 0.0, 1.0);
                    scrollPane.setHvalue(hvalue);
                }
                event.consume();
            }
        });

        timelineStateLabel = new Label();
        timelineStateLabel.getStyleClass().addAll("label-subtitle", "timeline-state");
        timelineStateLabel.setVisible(false);
        timelineStateLabel.setManaged(false);

        timelineContainer.getChildren().addAll(scrollPane, timelineStateLabel);

        root.getChildren().addAll(header, timelineContainer);
    }

    private HBox createHeader() {
        HBox header = new HBox(18);
        header.setAlignment(Pos.CENTER_LEFT);
        header.getStyleClass().add("timeline-header");

        Label titleLabel = new Label(text("view.timeline.title"));
        titleLabel.getStyleClass().add("label-title");
        LabeledTextAutoFit.install(titleLabel, LabeledTextAutoFit.titleSpec());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        startDatePicker = createRangeDatePicker(text("view.timeline.startDate"));
        startDatePicker.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && endDatePicker.getValue() != null && newVal.isAfter(endDatePicker.getValue())) {
                endDatePicker.setValue(newVal.plusDays(7));
            }
            refresh();
        });

        endDatePicker = createRangeDatePicker(text("view.timeline.endDate"));
        endDatePicker.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && startDatePicker.getValue() != null && newVal.isBefore(startDatePicker.getValue())) {
                startDatePicker.setValue(newVal.minusDays(7));
            }
            refresh();
        });

        StackPane rangeLabelBox = new StackPane();
        rangeLabelBox.getStyleClass().add("timeline-range-label-box");
        rangeLabelBox.setMinWidth(Region.USE_PREF_SIZE);

        Label rangeLabel = new Label(text("view.timeline.range"));
        rangeLabel.getStyleClass().add("timeline-range-label");
        rangeLabel.setWrapText(false);
        rangeLabel.setTextOverrun(OverrunStyle.CLIP);
        rangeLabel.setMinWidth(Region.USE_PREF_SIZE);
        rangeLabelBox.getChildren().add(rangeLabel);

        rangeIconBox = new StackPane(controller.createSvgIcon(IconKey.CALENDAR, null, 18));
        rangeIconBox.getStyleClass().add("timeline-range-icon-wrap");

        Label rangeConnector = new Label("→");
        rangeConnector.getStyleClass().add("timeline-range-connector");

        HBox rangePill = new HBox(10, rangeIconBox, startDatePicker, rangeConnector, endDatePicker);
        rangePill.setAlignment(Pos.CENTER_LEFT);
        rangePill.getStyleClass().add("timeline-range-pill");
        HBox.setHgrow(startDatePicker, Priority.ALWAYS);
        HBox.setHgrow(endDatePicker, Priority.ALWAYS);

        Button resetBtn = new Button(text("view.timeline.reset"));
        resetBtn.getStyleClass().addAll("button-secondary", "timeline-range-reset");
        LabeledTextAutoFit.install(resetBtn, LabeledTextAutoFit.buttonSpec());
        resetBtn.setOnAction(e -> {
            startDatePicker.setValue(null);
            endDatePicker.setValue(null);
            refresh();
        });

        HBox rangeGroup = new HBox(12, rangeLabelBox, rangePill, resetBtn);
        rangeGroup.setAlignment(Pos.CENTER_RIGHT);
        rangeGroup.getStyleClass().add("timeline-range-group");

        Label clockLabel = controller.createHeaderClockLabel();
        header.getChildren().addAll(titleLabel, spacer, rangeGroup, clockLabel);

        return header;
    }

    private DatePicker createRangeDatePicker(String promptText) {
        DatePicker picker = new DatePicker();
        picker.setPromptText(promptText);
        picker.setPrefWidth(166);
        picker.setMaxWidth(Double.MAX_VALUE);
        picker.setConverter(createRangeDateConverter());
        picker.getStyleClass().add("timeline-range-picker");
        DatePickerArrowSupport.install(picker, controller);
        return picker;
    }

    @Override
    public void refreshIcons() {
        if (rangeIconBox != null) {
            rangeIconBox.getChildren().setAll(controller.createSvgIcon(IconKey.CALENDAR, null, 18));
        }
    }

    private String text(String key, Object... args) {
        return controller.text(key, args);
    }

    private StringConverter<LocalDate> createRangeDateConverter() {
        return new StringConverter<LocalDate>() {
            @Override
            public String toString(LocalDate value) {
                return formatRangeDate(value);
            }

            @Override
            public LocalDate fromString(String text) {
                return parseRangeDate(text);
            }
        };
    }

    static String formatRangeDate(LocalDate value) {
        return value == null ? "" : RANGE_DATE_FORMATTER.format(value);
    }

    static LocalDate parseRangeDate(String text) {
        if (text == null || text.trim().isEmpty()) {
            return null;
        }
        return LocalDate.parse(text.trim(), RANGE_DATE_FORMATTER);
    }

    private static LocalDate minLocalDate(LocalDate left, LocalDate right) {
        if (left == null) {
            return right;
        }
        if (right == null) {
            return left;
        }
        return left.isBefore(right) ? left : right;
    }

    private static LocalDate maxLocalDate(LocalDate left, LocalDate right) {
        if (left == null) {
            return right;
        }
        if (right == null) {
            return left;
        }
        return left.isAfter(right) ? left : right;
    }

    @Override
    public Node getView() {
        return root;
    }

    @Override
    public void refresh() {
        showTimelineState(text("view.timeline.loading"));
        Platform.runLater(() -> {
            try {
                drawTimeline();
            } catch (SQLException e) {
                showTimelineState(text("view.timeline.loadFailed"));
                controller.showError(text("error.timelineLoad.title"), e.getMessage());
            }
        });
    }

    private void drawTimeline() throws SQLException {
        loadedSchedules = new ArrayList<>(controller.applyPendingCompletionMutations(controller.loadAllSchedules()));
        renderTimeline(loadedSchedules, null, true);
    }

    private void renderTimeline(List<Schedule> allSchedules) {
        renderTimeline(allSchedules, null, true);
    }

    private void renderTimeline(List<Schedule> allSchedules, Double anchorMinuteOffset, boolean scrollToToday) {
        stopZoomTimeline();
        activeZoomAnchorMinuteOffset = null;

        List<Schedule> baseSchedules = new ArrayList<>(allSchedules == null ? List.of() : allSchedules);
        baseSchedules.removeIf(schedule -> resolveTimelineStartAt(schedule) == null || resolveTimelineEndAt(schedule) == null);
        if (baseSchedules.isEmpty()) {
            clearRenderedTimeline();
            showTimelineState(text("view.timeline.emptyWithoutDates"));
            return;
        }

        LocalDate rawMinDate = baseSchedules.stream()
            .map(TimelineView::resolveTimelineStartDate)
            .min(LocalDate::compareTo)
            .orElse(LocalDate.now().minusDays(7));
        LocalDate rawMaxDate = baseSchedules.stream()
            .map(TimelineView::resolveTimelineEndDate)
            .max(LocalDate::compareTo)
            .orElse(LocalDate.now().plusDays(30));

        boolean hasRecurring = baseSchedules.stream().anyMatch(Schedule::hasRecurrence);
        LocalDate minDate = startDatePicker.getValue() != null
            ? startDatePicker.getValue()
            : (hasRecurring ? minLocalDate(rawMinDate.minusDays(3), LocalDate.now().minusDays(7)) : rawMinDate.minusDays(3));
        LocalDate maxDate = endDatePicker.getValue() != null
            ? endDatePicker.getValue()
            : (hasRecurring ? maxLocalDate(rawMaxDate.plusDays(7), LocalDate.now().plusDays(30)) : rawMaxDate.plusDays(7));

        if (minDate.isAfter(maxDate)) {
            clearRenderedTimeline();
            showTimelineState(text("view.timeline.invalidRange"));
            return;
        }

        List<Schedule> projectedSchedules = ScheduleOccurrenceProjector.projectForRange(baseSchedules, minDate, maxDate, false);
        List<Schedule> shortTasks = new ArrayList<>();
        List<Schedule> mediumTasks = new ArrayList<>();
        List<Schedule> longTasks = new ArrayList<>();
        for (Schedule schedule : projectedSchedules) {
            long days = ChronoUnit.DAYS.between(resolveTimelineStartDate(schedule), resolveTimelineEndDate(schedule)) + 1;
            if (days < 7) {
                shortTasks.add(schedule);
            } else if (days <= 35) {
                mediumTasks.add(schedule);
            } else {
                longTasks.add(schedule);
            }
        }

        Comparator<Schedule> comparator = Comparator.comparing(TimelineView::resolveTimelineStartAt)
            .thenComparing(TimelineView::resolveTimelineEndAt)
            .thenComparing(Schedule::getPriorityValue, Comparator.reverseOrder())
            .thenComparing(Schedule::getName, Comparator.nullsLast(String::compareToIgnoreCase));
        shortTasks.sort(comparator);
        mediumTasks.sort(comparator);
        longTasks.sort(comparator);

        clearRenderedTimelineNodes();
        renderedMinDate = minDate;
        renderedMaxDate = maxDate;
        lastRangeStartAt = minDate.atStartOfDay();
        lastRangeEndAtExclusive = maxDate.plusDays(1).atStartOfDay();

        List<TimelineEntry> timelineEntries = new ArrayList<>();
        double currentY = TRACK_TOP;
        currentY = appendGroup(shortTasks, text("view.timeline.group.short"), currentY, minDate, maxDate, timelineEntries);
        currentY = appendGroup(mediumTasks, text("view.timeline.group.medium"), currentY, minDate, maxDate, timelineEntries);
        currentY = appendGroup(longTasks, text("view.timeline.group.long"), currentY, minDate, maxDate, timelineEntries);

        renderedPaneHeight = Math.max(320, currentY + BOTTOM_PADDING);
        renderTracksBackground(renderedPaneHeight);
        renderDateAxis(minDate, maxDate);
        for (TimelineEntry entry : timelineEntries) {
            TimelineCardNode cardNode = createTimelineCardNode(entry, lastRangeStartAt, lastRangeEndAtExclusive);
            if (cardNode != null) {
                renderedCardNodes.add(cardNode);
            }
        }
        updateRenderedTimelineGeometry();

        if (timelineEntries.isEmpty()) {
            showTimelineState(text("view.timeline.emptyInRange"));
        } else {
            hideTimelineState();
        }

        if (anchorMinuteOffset != null) {
            queueOrApplyViewportAnchor(anchorMinuteOffset.doubleValue(), resolveViewportWidth() / 2.0);
        } else if (scrollToToday) {
            queueOrApplyTodayAnchor();
        }
    }

    private void clearRenderedTimeline() {
        clearRenderedTimelineNodes();
        renderedMinDate = null;
        renderedMaxDate = null;
        lastRangeStartAt = null;
        lastRangeEndAtExclusive = null;
        renderedPaneHeight = 320;
        if (timelinePane != null) {
            timelinePane.setPrefWidth(LEFT_PADDING + RIGHT_PADDING);
            timelinePane.setPrefHeight(renderedPaneHeight);
        }
        if (scrollPane != null) {
            scrollPane.setHvalue(0);
        }
    }

    private void clearRenderedTimelineNodes() {
        if (timelineBackgroundLayer != null) {
            timelineBackgroundLayer.getChildren().clear();
        }
        if (timelineDecorationLayer != null) {
            timelineDecorationLayer.getChildren().clear();
        }
        if (timelineAxisLayer != null) {
            timelineAxisLayer.getChildren().clear();
        }
        if (timelineCardsLayer != null) {
            timelineCardsLayer.getChildren().clear();
        }
        axisDayNodes.clear();
        gridMarkerNodes.clear();
        renderedCardNodes.clear();
        trackBackground = null;
        topSeparator = null;
        bottomSeparator = null;
        axisLine = null;
        deferredViewportAction = null;
    }

    private boolean hasRenderableTimeline() {
        return renderedMinDate != null && renderedMaxDate != null && lastRangeStartAt != null && lastRangeEndAtExclusive != null;
    }

    private double appendGroup(List<Schedule> schedules, String title, double startY, LocalDate minDate, LocalDate maxDate, List<TimelineEntry> entries) {
        Label headerLabel = new Label(title);
        headerLabel.getStyleClass().add("timeline-group-header");
        headerLabel.setLayoutX(LEFT_PADDING);
        headerLabel.setLayoutY(startY + 10);
        timelineDecorationLayer.getChildren().add(headerLabel);
        
        Line sep = new Line(LEFT_PADDING, startY + 35, LEFT_PADDING + 200, startY + 35);
        sep.getStyleClass().add("timeline-group-sep");
        timelineDecorationLayer.getChildren().add(sep);

        double cardStartY = startY + 50;
        
        // 改进的堆叠算法，处理不同长度日程的重叠
        List<TimelineEntry> groupEntries = new ArrayList<>();
        int maxStack = -1;
        LocalDateTime rangeStartAt = minDate != null ? minDate.atStartOfDay() : null;
        LocalDateTime rangeEndAtExclusive = maxDate != null ? maxDate.plusDays(1).atStartOfDay() : null;

        for (Schedule schedule : schedules) {
            LocalDateTime sAt = resolveTimelineStartAt(schedule);
            LocalDateTime eAt = resolveTimelineEndAt(schedule);
            if (sAt == null || eAt == null) {
                continue;
            }
            if (sAt.isAfter(eAt)) {
                LocalDateTime t = sAt;
                sAt = eAt;
                eAt = t;
            }

            if (rangeStartAt != null && eAt.isBefore(rangeStartAt)) {
                continue;
            }
            if (rangeEndAtExclusive != null && !sAt.isBefore(rangeEndAtExclusive)) {
                continue;
            }

            // 寻找第一个不冲突的层级
            int stackIndex = 0;
            boolean conflict = true;
            while (conflict) {
                conflict = false;
                for (TimelineEntry existing : groupEntries) {
                    // 如果存在重叠并且分配在同一层级
                    if (existing.getCardY() == cardStartY + stackIndex * CARD_STACK_OFFSET &&
                        !sAt.isAfter(existing.getEndAt()) &&
                        !eAt.isBefore(existing.getStartAt())) {
                        conflict = true;
                        stackIndex++;
                        break;
                    }
                }
            }
            
            maxStack = Math.max(maxStack, stackIndex);
            
            double cardY = cardStartY + stackIndex * CARD_STACK_OFFSET;
            TimelineEntry entry = new TimelineEntry(schedule, sAt, eAt, cardY);
            groupEntries.add(entry);
            entries.add(entry);
        }
        
        if (maxStack == -1) {
            Label emptyLabel = new Label(text("view.timeline.emptyGroup"));
            emptyLabel.getStyleClass().add("timeline-group-empty");
            emptyLabel.setLayoutX(LEFT_PADDING);
            emptyLabel.setLayoutY(cardStartY);
            timelineDecorationLayer.getChildren().add(emptyLabel);
            return cardStartY + 50;
        }
        
        return cardStartY + (maxStack + 1) * CARD_STACK_OFFSET + 20;
    }

    private void renderTracksBackground(double paneHeight) {
        trackBackground = new Rectangle(LEFT_PADDING, TRACK_TOP, 0, Math.max(0, paneHeight - TRACK_TOP - BOTTOM_PADDING));
        trackBackground.getStyleClass().add("timeline-track-bg");
        timelineBackgroundLayer.getChildren().add(trackBackground);

        topSeparator = new Line(LEFT_PADDING, TRACK_TOP, LEFT_PADDING, TRACK_TOP);
        topSeparator.getStyleClass().add("timeline-track-sep");
        timelineBackgroundLayer.getChildren().add(topSeparator);

        bottomSeparator = new Line(
            LEFT_PADDING,
            paneHeight - BOTTOM_PADDING,
            LEFT_PADDING,
            paneHeight - BOTTOM_PADDING
        );
        bottomSeparator.getStyleClass().add("timeline-track-sep");
        timelineBackgroundLayer.getChildren().add(bottomSeparator);
    }

    private void renderDateAxis(LocalDate minDate, LocalDate maxDate) {
        axisLine = new Line(LEFT_PADDING, AXIS_LINE_Y, LEFT_PADDING, AXIS_LINE_Y);
        axisLine.getStyleClass().add("timeline-axis-line");
        axisLine.setStrokeWidth(2);
        timelineAxisLayer.getChildren().add(axisLine);

        LocalDate current = minDate;
        int dayIndex = 0;
        while (!current.isAfter(maxDate)) {
            Rectangle todayHighlight = null;
            if (current.equals(LocalDate.now())) {
                todayHighlight = new Rectangle();
                todayHighlight.getStyleClass().add("timeline-today-highlight");
                timelineAxisLayer.getChildren().add(todayHighlight);
            }

            Label dateLabel = new Label(current.format(DATE_FORMATTER));
            dateLabel.getStyleClass().add("schedule-date");
            dateLabel.setAlignment(Pos.CENTER);
            dateLabel.setLayoutY(AXIS_LABEL_Y);
            if (current.equals(LocalDate.now())) {
                dateLabel.getStyleClass().add("timeline-date-today");
            }
            timelineAxisLayer.getChildren().add(dateLabel);
            axisDayNodes.add(new TimelineAxisDayNode(dayIndex, todayHighlight, dateLabel));

            current = current.plusDays(1);
            dayIndex++;
        }

        for (long minuteOffset = 0; minuteOffset <= resolveTotalMinutes(); minuteOffset += BASE_CELL_MINUTES) {
            Line gridLine = new Line();
            gridLine.getStyleClass().add("timeline-grid-line");
            timelineAxisLayer.getChildren().add(gridLine);

            Line tick = new Line();
            tick.getStyleClass().add("timeline-axis-tick");
            timelineAxisLayer.getChildren().add(tick);

            gridMarkerNodes.add(new TimelineGridMarkerNode(minuteOffset, gridLine, tick));
        }
    }

    private TimelineCardNode createTimelineCardNode(
        TimelineEntry entry,
        LocalDateTime rangeStartAt,
        LocalDateTime rangeEndAtExclusive
    ) {
        Schedule schedule = entry.getSchedule();
        LocalDateTime entryStartAt = entry.getStartAt();
        LocalDateTime entryEndAt = entry.getEndAt();
        if (entryStartAt == null || entryEndAt == null) {
            return null;
        }

        LocalDateTime maxEndInclusive = rangeEndAtExclusive != null ? rangeEndAtExclusive.minusMinutes(1) : null;
        LocalDateTime visualStart = rangeStartAt != null && entryStartAt.isBefore(rangeStartAt) ? rangeStartAt : entryStartAt;
        LocalDateTime visualEnd = maxEndInclusive != null && entryEndAt.isAfter(maxEndInclusive) ? maxEndInclusive : entryEndAt;
        if (visualEnd.isBefore(visualStart)) {
            return null;
        }

        long startOffsetMinutes = rangeStartAt != null ? ChronoUnit.MINUTES.between(rangeStartAt, visualStart) : 0;
        long durationMinutesInclusive = Math.max(1, ChronoUnit.MINUTES.between(visualStart, visualEnd) + 1);

        double cardY = entry.getCardY();

        StackPane scheduleCard = new StackPane();
        scheduleCard.setCursor(Cursor.HAND);
        scheduleCard.setPrefHeight(CARD_HEIGHT);
        scheduleCard.setLayoutY(cardY);
        scheduleCard.getStyleClass().add("timeline-schedule-card");
        ScheduleCardStyleSupport.applyCardPresentation(
            scheduleCard,
            schedule,
            controller.getCurrentScheduleCardStyle(),
            "schedule-card-role-timeline"
        );
        
        scheduleCard.setUserData(schedule);
        if (controller.isScheduleSelected(schedule)) {
            scheduleCard.getStyleClass().addAll("timeline-schedule-selected", "schedule-card-state-selected");
        }

        Rectangle cardBackground = new Rectangle();
        cardBackground.getStyleClass().addAll("card-bg", "schedule-card-layer");

        Rectangle accentBar = new Rectangle();
        accentBar.getStyleClass().addAll("card-accent-bar", "schedule-card-accent");

        // 改进布局：将标题和日期放入水平容器以防止重叠，并处理截断
        HBox contentBox = new HBox(10);
        contentBox.setAlignment(Pos.CENTER_LEFT);
        contentBox.setPadding(new Insets(0, 10, 0, 12));
        ScheduleStatusControl statusControl = new ScheduleStatusControl(
            schedule.isCompleted(),
            ScheduleStatusControl.SizePreset.TIMELINE,
            "schedule-status-role-timeline",
            targetCompleted -> controller.updateScheduleCompletion(schedule, targetCompleted)
        );

        Label titleLabel = new Label(schedule.getName());
        titleLabel.getStyleClass().addAll("card-title", "schedule-card-title-text");
        titleLabel.setTextOverrun(OverrunStyle.CLIP);
        titleLabel.setMouseTransparent(true);
        LabeledTextAutoFit.install(titleLabel, LabeledTextAutoFit.cardTitleSpec());
        
        HBox.setHgrow(titleLabel, Priority.ALWAYS);
        contentBox.getChildren().addAll(statusControl, titleLabel);

        Tooltip tooltip = new Tooltip(schedule.getName() + "\n" + formatEntryRangeLabel(entryStartAt, entryEndAt, schedule));
        tooltip.setWrapText(true);
        tooltip.setMaxWidth(520);
        Tooltip.install(scheduleCard, tooltip);

        scheduleCard.getChildren().addAll(cardBackground, accentBar, contentBox);
        StackPane.setAlignment(accentBar, Pos.CENTER_LEFT);

        Rectangle leftClip = null;
        if (rangeStartAt != null && entryStartAt.isBefore(rangeStartAt)) {
            leftClip = new Rectangle(8, CARD_HEIGHT);
            leftClip.getStyleClass().addAll("card-clip", "schedule-card-clip");
            StackPane.setAlignment(leftClip, Pos.CENTER_LEFT);
            scheduleCard.getChildren().add(leftClip);
        }

        Rectangle rightClip = null;
        if (rangeEndAtExclusive != null && !entryEndAt.isBefore(rangeEndAtExclusive)) {
            rightClip = new Rectangle(8, CARD_HEIGHT);
            rightClip.getStyleClass().addAll("card-clip", "schedule-card-clip");
            StackPane.setAlignment(rightClip, Pos.CENTER_RIGHT);
            scheduleCard.getChildren().add(rightClip);
        }

        ScaleTransition scaleIn = new ScaleTransition(Duration.millis(150), scheduleCard);
        scaleIn.setToX(1.02);
        scaleIn.setToY(1.05);

        ScaleTransition scaleOut = new ScaleTransition(Duration.millis(150), scheduleCard);
        scaleOut.setToX(1.0);
        scaleOut.setToY(1.0);

        TranslateTransition transIn = new TranslateTransition(Duration.millis(150), scheduleCard);
        transIn.setToY(-12);
        transIn.setToX(-6);

        TranslateTransition transOut = new TranslateTransition(Duration.millis(150), scheduleCard);
        transOut.setToY(0);
        transOut.setToX(0);

        scheduleCard.setOnMouseEntered(e -> {
            double baseViewOrder = ((Number) scheduleCard.getProperties().getOrDefault("timeline.baseViewOrder", 0.0)).doubleValue();
            scheduleCard.setViewOrder(baseViewOrder - 1_000_000);
            scheduleCard.pseudoClassStateChanged(javafx.css.PseudoClass.getPseudoClass("hover"), true);
            scaleIn.playFromStart();
            transIn.playFromStart();
        });

        scheduleCard.setOnMouseExited(e -> {
            double baseViewOrder = ((Number) scheduleCard.getProperties().getOrDefault("timeline.baseViewOrder", 0.0)).doubleValue();
            scheduleCard.setViewOrder(baseViewOrder);
            scheduleCard.pseudoClassStateChanged(javafx.css.PseudoClass.getPseudoClass("hover"), false);
            scaleOut.playFromStart();
            transOut.playFromStart();
        });

        scheduleCard.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                controller.showScheduleDetailsAndFocusTitle(schedule);
            } else {
                controller.showScheduleDetails(schedule);
            }
            highlightSelectedScheduleCard(scheduleCard);
        });

        Tooltip.install(scheduleCard, new Tooltip(buildTooltipText(schedule, entryStartAt, entryEndAt)));
        timelineCardsLayer.getChildren().add(scheduleCard);
        TimelineCardNode cardNode = new TimelineCardNode(
            startOffsetMinutes,
            durationMinutesInclusive,
            cardY,
            scheduleCard,
            cardBackground,
            accentBar,
            contentBox,
            titleLabel,
            leftClip,
            rightClip
        );
        updateTimelineCardGeometry(cardNode, buildMetrics(displayZoomFactor).pixelsPerMinute());
        return cardNode;
    }

    private void highlightSelectedScheduleCard(StackPane selectedCard) {
        for (Node node : timelineCardsLayer.getChildren()) {
            node.getStyleClass().remove("timeline-schedule-selected");
            node.getStyleClass().remove("schedule-card-state-selected");
        }
        selectedCard.getStyleClass().addAll("timeline-schedule-selected", "schedule-card-state-selected");
    }

    private void updateRenderedTimelineGeometry() {
        if (!hasRenderableTimeline()) {
            return;
        }

        TimelineMetrics metrics = buildMetrics(displayZoomFactor);
        timelinePane.setPrefWidth(metrics.contentWidth());
        timelinePane.setPrefHeight(renderedPaneHeight);

        double trackWidth = Math.max(0.0, metrics.contentWidth() - LEFT_PADDING - RIGHT_PADDING);
        double gridBottom = renderedPaneHeight - BOTTOM_PADDING / 2.0;

        if (trackBackground != null) {
            trackBackground.setX(LEFT_PADDING);
            trackBackground.setY(TRACK_TOP);
            trackBackground.setWidth(trackWidth);
            trackBackground.setHeight(Math.max(0, renderedPaneHeight - TRACK_TOP - BOTTOM_PADDING));
        }
        if (topSeparator != null) {
            topSeparator.setStartX(LEFT_PADDING);
            topSeparator.setEndX(metrics.contentWidth() - RIGHT_PADDING);
            topSeparator.setStartY(TRACK_TOP);
            topSeparator.setEndY(TRACK_TOP);
        }
        if (bottomSeparator != null) {
            bottomSeparator.setStartX(LEFT_PADDING);
            bottomSeparator.setEndX(metrics.contentWidth() - RIGHT_PADDING);
            bottomSeparator.setStartY(renderedPaneHeight - BOTTOM_PADDING);
            bottomSeparator.setEndY(renderedPaneHeight - BOTTOM_PADDING);
        }
        if (axisLine != null) {
            axisLine.setStartX(LEFT_PADDING);
            axisLine.setEndX(metrics.contentWidth() - RIGHT_PADDING);
            axisLine.setStartY(AXIS_LINE_Y);
            axisLine.setEndY(AXIS_LINE_Y);
        }

        double dayWidthPx = 1440 * metrics.pixelsPerMinute();
        for (TimelineAxisDayNode axisDayNode : axisDayNodes) {
            double x = LEFT_PADDING + axisDayNode.dayIndex() * dayWidthPx;
            if (axisDayNode.highlight() != null) {
                axisDayNode.highlight().setX(x);
                axisDayNode.highlight().setY(0);
                axisDayNode.highlight().setWidth(dayWidthPx);
                axisDayNode.highlight().setHeight(gridBottom);
            }
            axisDayNode.label().setPrefWidth(dayWidthPx);
            axisDayNode.label().setLayoutX(x);
        }

        for (TimelineGridMarkerNode gridMarkerNode : gridMarkerNodes) {
            double x = LEFT_PADDING + gridMarkerNode.minuteOffset() * metrics.pixelsPerMinute();
            gridMarkerNode.gridLine().setStartX(x);
            gridMarkerNode.gridLine().setEndX(x);
            gridMarkerNode.gridLine().setStartY(AXIS_LINE_Y);
            gridMarkerNode.gridLine().setEndY(gridBottom);
            gridMarkerNode.tick().setStartX(x);
            gridMarkerNode.tick().setEndX(x);
            gridMarkerNode.tick().setStartY(AXIS_LINE_Y - 4);
            gridMarkerNode.tick().setEndY(AXIS_LINE_Y + 6);
        }

        for (TimelineCardNode cardNode : renderedCardNodes) {
            updateTimelineCardGeometry(cardNode, metrics.pixelsPerMinute());
        }

        if (activeZoomAnchorMinuteOffset != null) {
            applyViewportAnchor(activeZoomAnchorMinuteOffset.doubleValue(), activeZoomAnchorViewportX, metrics);
        }
    }

    private void updateTimelineCardGeometry(TimelineCardNode cardNode, double pixelsPerMinute) {
        double durationPx = cardNode.durationMinutesInclusive() * pixelsPerMinute;
        double insetX = Math.min(CARD_INSET_X, Math.max(0, durationPx * 0.18));
        double startX = LEFT_PADDING + cardNode.startOffsetMinutes() * pixelsPerMinute + insetX;
        double width = Math.max(1.0, durationPx - insetX * 2.0);
        double baseViewOrder = -(cardNode.cardY() * 10000 + startX);

        cardNode.card().setPrefSize(width, CARD_HEIGHT);
        cardNode.card().setLayoutX(startX);
        cardNode.card().setLayoutY(cardNode.cardY());
        cardNode.card().setViewOrder(baseViewOrder);
        cardNode.card().getProperties().put("timeline.baseViewOrder", baseViewOrder);
        cardNode.cardBackground().setWidth(width);
        cardNode.cardBackground().setHeight(CARD_HEIGHT);
        cardNode.accentBar().setWidth(Math.min(6.0, width));
        cardNode.accentBar().setHeight(CARD_HEIGHT);
        cardNode.contentBox().setPrefSize(width, CARD_HEIGHT);
        boolean showInlineTitle = width >= MIN_INLINE_TITLE_WIDTH;
        cardNode.titleLabel().setVisible(showInlineTitle);
        cardNode.titleLabel().setManaged(showInlineTitle);
    }

    private void queueOrApplyViewportAnchor(double minuteOffset, double viewportAnchorX) {
        if (resolveViewportWidth() <= 0) {
            scheduleDeferredViewportAction(() -> applyViewportAnchor(minuteOffset, viewportAnchorX));
            return;
        }
        applyViewportAnchor(minuteOffset, viewportAnchorX);
    }

    private void queueOrApplyTodayAnchor() {
        if (!hasRenderableTimeline()) {
            scrollPane.setHvalue(0);
            return;
        }

        LocalDate today = LocalDate.now();
        if (renderedMinDate == null || renderedMaxDate == null || today.isBefore(renderedMinDate) || today.isAfter(renderedMaxDate)) {
            scrollPane.setHvalue(0);
            return;
        }

        if (resolveViewportWidth() <= 0) {
            scheduleDeferredViewportAction(() -> applyViewportAnchor(resolveTodayMinuteOffset(), resolveViewportWidth() * 0.42));
            return;
        }

        applyViewportAnchor(resolveTodayMinuteOffset(), resolveViewportWidth() * 0.42);
    }

    private void applyViewportAnchor(double minuteOffset, double viewportAnchorX) {
        applyViewportAnchor(minuteOffset, viewportAnchorX, buildMetrics(displayZoomFactor));
    }

    private void applyViewportAnchor(double minuteOffset, double viewportAnchorX, TimelineMetrics metrics) {
        double viewportWidth = resolveViewportWidth();
        if (scrollPane == null || viewportWidth <= 0) {
            return;
        }

        double hvalue = TimelineZoomGeometry.hvalueForAnchor(
            minuteOffset,
            TimelineZoomGeometry.clamp(viewportAnchorX, 0.0, viewportWidth),
            viewportWidth,
            metrics.contentWidth(),
            LEFT_PADDING,
            metrics.pixelsPerMinute()
        );
        scrollPane.setHvalue(TimelineZoomGeometry.clamp(hvalue, 0.0, 1.0));
    }

    private double resolveMinuteOffsetAtViewportX(double viewportAnchorX) {
        if (!hasRenderableTimeline() || scrollPane == null) {
            return 0.0;
        }

        TimelineMetrics metrics = buildMetrics(displayZoomFactor);
        return TimelineZoomGeometry.minuteOffsetAtViewportX(
            viewportAnchorX,
            scrollPane.getHvalue(),
            resolveViewportWidth(),
            metrics.contentWidth(),
            LEFT_PADDING,
            metrics.pixelsPerMinute(),
            metrics.totalMinutes()
        );
    }

    private double resolveMouseAnchorViewportX(ScrollEvent event) {
        if (event == null || scrollPane == null || timelinePane == null) {
            return resolveViewportWidth() / 2.0;
        }
        TimelineMetrics metrics = buildMetrics(displayZoomFactor);
        Point2D pointInContent = timelinePane.sceneToLocal(event.getSceneX(), event.getSceneY());
        double scrollX = TimelineZoomGeometry.scrollX(scrollPane.getHvalue(), resolveViewportWidth(), metrics.contentWidth());
        return TimelineZoomGeometry.clamp(pointInContent.getX() - scrollX, 0.0, resolveViewportWidth());
    }

    private double resolveTodayMinuteOffset() {
        if (lastRangeStartAt == null) {
            return 0.0;
        }
        return ChronoUnit.MINUTES.between(lastRangeStartAt, LocalDate.now().atTime(LocalTime.NOON));
    }

    private double resolveViewportWidth() {
        return scrollPane != null ? Math.max(0.0, scrollPane.getViewportBounds().getWidth()) : 0.0;
    }

    private void scheduleDeferredViewportAction(Runnable action) {
        deferredViewportAction = action;
    }

    private TimelineMetrics buildMetrics(double zoom) {
        long totalMinutes = resolveTotalMinutes();
        double pixelsPerMinute = TimelineZoomGeometry.pixelsPerMinute(BASE_CELL_WIDTH_PX, zoom, BASE_CELL_MINUTES);
        double totalWidth = TimelineZoomGeometry.totalWidth(LEFT_PADDING, RIGHT_PADDING, totalMinutes, pixelsPerMinute);
        return new TimelineMetrics(pixelsPerMinute, totalWidth, totalMinutes);
    }

    private void setDisplayZoomFactor(double zoom) {
        if (Math.abs(displayZoomFactorProperty.get() - zoom) < 0.0001) {
            displayZoomFactor = zoom;
            updateRenderedTimelineGeometry();
            return;
        }
        displayZoomFactorProperty.set(zoom);
    }

    private void stopZoomTimeline() {
        if (zoomTimeline != null) {
            zoomTimeline.stop();
            zoomTimeline = null;
        }
    }

    private void startAutoScroll() {
        autoScrollTimer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (now - lastUpdate > 60_000_000_000L) {
                    lastUpdate = now;
                }
            }
        };
        autoScrollTimer.start();
    }

    private void showTimelineState(String message) {
        timelineStateLabel.setText(message);
        timelineStateLabel.setVisible(true);
        timelineStateLabel.setManaged(true);
    }

    private void hideTimelineState() {
        timelineStateLabel.setVisible(false);
        timelineStateLabel.setManaged(false);
    }

    @Override
    public void applyCompletionMutation(ScheduleCompletionMutation mutation) {
        if (mutation == null || loadedSchedules.isEmpty()) {
            return;
        }
        boolean changed = false;
        for (Schedule schedule : loadedSchedules) {
            if (!mutation.matches(schedule)) {
                continue;
            }
            mutation.applyTo(schedule);
            changed = true;
        }
        if (changed) {
            renderTimeline(loadedSchedules, resolveViewportCenterMinuteOffset(), false);
        }
    }

    @Override
    public void confirmCompletionMutation(ScheduleCompletionMutation mutation) {
        if (mutation == null || loadedSchedules.isEmpty()) {
            return;
        }
        renderTimeline(loadedSchedules, resolveViewportCenterMinuteOffset(), false);
    }

    @Override
    public void revertCompletionMutation(ScheduleCompletionMutation mutation) {
        if (mutation == null || loadedSchedules.isEmpty()) {
            return;
        }
        boolean changed = false;
        for (Schedule schedule : loadedSchedules) {
            if (!mutation.matches(schedule)) {
                continue;
            }
            mutation.revertOn(schedule);
            changed = true;
        }
        if (changed) {
            renderTimeline(loadedSchedules, resolveViewportCenterMinuteOffset(), false);
        }
    }

    private List<Schedule> getFilteredSchedules(List<Schedule> schedules) {
        return filterAndSortSchedules(schedules, "all");
    }

    static List<Schedule> filterAndSortSchedules(List<Schedule> schedules, String level) {
        List<Schedule> renderableSchedules = new ArrayList<>(schedules);
        renderableSchedules.removeIf(schedule -> resolveTimelineStartAt(schedule) == null || resolveTimelineEndAt(schedule) == null);
        
        renderableSchedules.removeIf(schedule -> {
            long days = ChronoUnit.DAYS.between(resolveTimelineStartDate(schedule), resolveTimelineEndDate(schedule)) + 1;
            if (level.startsWith("day") && days >= 7) return true;
            if (level.startsWith("week") && (days < 7 || days > 35)) return true;
            if (level.startsWith("month") && days <= 35) return true;
            return false;
        });

        renderableSchedules.sort(Comparator
            .comparing(TimelineView::resolveTimelineStartAt)
            .thenComparing(TimelineView::resolveTimelineEndAt)
            .thenComparing(Schedule::getPriorityValue, Comparator.reverseOrder())
            .thenComparing(Schedule::getName, Comparator.nullsLast(String::compareToIgnoreCase)));
        return renderableSchedules;
    }

    static List<TimelineEntry> buildTimelineEntries(List<Schedule> schedules, LocalDate minDate, LocalDate maxDate, String level) {
        List<TimelineEntry> entries = new ArrayList<>();
        java.util.Map<LocalDate, Integer> dateCount = new java.util.HashMap<>();

        for (Schedule schedule : filterAndSortSchedules(schedules, level)) {
            LocalDateTime startAt = resolveTimelineStartAt(schedule);
            LocalDateTime endAt = resolveTimelineEndAt(schedule);
            if (startAt == null || endAt == null) {
                continue;
            }
            if (startAt.isAfter(endAt)) {
                LocalDateTime temp = startAt;
                startAt = endAt;
                endAt = temp;
            }
            LocalDate startDate = startAt.toLocalDate();
            LocalDate endDate = endAt.toLocalDate();

            if (minDate != null && endDate.isBefore(minDate)) {
                continue;
            }
            if (maxDate != null && startDate.isAfter(maxDate)) {
                continue;
            }

            int laneIndex = dateCount.getOrDefault(startDate, 0);
            dateCount.put(startDate, laneIndex + 1);

            entries.add(new TimelineEntry(schedule, startAt, endAt, laneIndex));
        }

        return entries;
    }

    static LocalDate resolveTimelineStartDate(Schedule schedule) {
        LocalDateTime startAt = resolveTimelineStartAt(schedule);
        return startAt != null ? startAt.toLocalDate() : null;
    }

    static LocalDate resolveTimelineEndDate(Schedule schedule) {
        LocalDateTime endAt = resolveTimelineEndAt(schedule);
        return endAt != null ? endAt.toLocalDate() : null;
    }

    static LocalDateTime resolveTimelineStartAt(Schedule schedule) {
        TimelineRange range = resolveTimelineRange(schedule);
        return range != null ? range.startAt : null;
    }

    static LocalDateTime resolveTimelineEndAt(Schedule schedule) {
        TimelineRange range = resolveTimelineRange(schedule);
        return range != null ? range.endAt : null;
    }

    private static TimelineRange resolveTimelineRange(Schedule schedule) {
        if (schedule == null) {
            return null;
        }

        LocalDateTime effectiveStart = schedule.getEffectiveStartAt();
        LocalDateTime effectiveEnd = schedule.getEffectiveEndAt();
        if (effectiveStart == null || effectiveEnd == null) {
            return null;
        }

        LocalDateTime startAt = effectiveStart;
        LocalDateTime endAt = effectiveEnd;

        boolean allDay = schedule.isAllDay() || ScheduleItem.TIME_PRECISION_DAY.equals(schedule.getTimePrecision());
        if (allDay) {
            LocalDate startDate = startAt.toLocalDate();
            LocalDate endDate = endAt.toLocalDate();
            startAt = startDate.atStartOfDay();
            endAt = endDate.atTime(23, 59);
        } else {
            boolean hasStart = schedule.getStartAt() != null;
            boolean hasEndLike = schedule.getEndAt() != null || schedule.getDueAt() != null;

            if (!hasStart && hasEndLike) {
                // Due/end only: render as starting at 00:00 of the end date.
                startAt = endAt.toLocalDate().atStartOfDay();
            } else if (hasStart && !hasEndLike) {
                // Start only: render as running through 23:59 of that same day.
                endAt = startAt.toLocalDate().atTime(23, 59);
            }
        }

        if (startAt.isAfter(endAt)) {
            LocalDateTime temp = startAt;
            startAt = endAt;
            endAt = temp;
        }

        return new TimelineRange(startAt, endAt);
    }

    private String buildTooltipText(Schedule schedule, LocalDateTime startAt, LocalDateTime endAt) {
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        LocalDateTime rawStartAt = schedule.getStartAt();
        LocalDateTime rawDueAt = schedule.getDueAt();
        long duration = ChronoUnit.DAYS.between(startAt.toLocalDate(), endAt.toLocalDate()) + 1;
        return schedule.getName() + "\n"
            + text("view.timeline.tooltip.start", rawStartAt != null ? rawStartAt.format(dateTimeFormatter) : text("common.unset")) + "\n"
            + text("view.timeline.tooltip.due", rawDueAt != null ? rawDueAt.format(dateTimeFormatter) : text("common.unset")) + "\n"
            + text("view.timeline.tooltip.duration", duration) + "\n"
            + text("view.timeline.tooltip.priority", controller.priorityDisplayName(schedule.getPriority())) + "\n"
            + text("view.timeline.tooltip.status", schedule.isCompleted() ? text("status.completed") : text("status.pending"));
    }

    public void stopAutoScroll() {
        if (autoScrollTimer != null) {
            autoScrollTimer.stop();
        }
    }

    private record TimelineMetrics(double pixelsPerMinute, double contentWidth, long totalMinutes) {
    }

    private record TimelineAxisDayNode(int dayIndex, Rectangle highlight, Label label) {
    }

    private record TimelineGridMarkerNode(long minuteOffset, Line gridLine, Line tick) {
    }

    private record TimelineCardNode(
        long startOffsetMinutes,
        long durationMinutesInclusive,
        double cardY,
        StackPane card,
        Rectangle cardBackground,
        Rectangle accentBar,
        HBox contentBox,
        Label titleLabel,
        Rectangle leftClip,
        Rectangle rightClip
    ) {
    }

    static final class TimelineEntry {
        private final Schedule schedule;
        private final LocalDateTime startAt;
        private final LocalDateTime endAt;
        private final double cardY;

        TimelineEntry(Schedule schedule, LocalDateTime startAt, LocalDateTime endAt, double cardY) {
            this.schedule = schedule;
            this.startAt = startAt;
            this.endAt = endAt;
            this.cardY = cardY;
        }

        Schedule getSchedule() {
            return schedule;
        }

        LocalDateTime getStartAt() {
            return startAt;
        }

        LocalDateTime getEndAt() {
            return endAt;
        }

        double getCardY() {
            return cardY;
        }
    }

    private static final class TimelineRange {
        private final LocalDateTime startAt;
        private final LocalDateTime endAt;

        private TimelineRange(LocalDateTime startAt, LocalDateTime endAt) {
            this.startAt = startAt;
            this.endAt = endAt;
        }
    }

    private static String formatEntryRangeLabel(LocalDateTime startAt, LocalDateTime endAt, Schedule schedule) {
        if (startAt == null || endAt == null) {
            return "";
        }

        boolean allDay = schedule != null
            && (schedule.isAllDay() || ScheduleItem.TIME_PRECISION_DAY.equals(schedule.getTimePrecision()));
        if (allDay) {
            LocalDate startDate = startAt.toLocalDate();
            LocalDate endDate = endAt.toLocalDate();
            if (startDate.equals(endDate)) {
                return startDate.format(DATE_FORMATTER);
            }
            return startDate.format(DATE_FORMATTER) + " - " + endDate.format(DATE_FORMATTER);
        }

        if (startAt.toLocalDate().equals(endAt.toLocalDate())) {
            return startAt.format(TIME_FORMATTER) + " - " + endAt.format(TIME_FORMATTER);
        }
        return startAt.format(DATE_TIME_FORMATTER) + " - " + endAt.format(DATE_TIME_FORMATTER);
    }
}
