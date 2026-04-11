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

import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
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

import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.animation.ParallelTransition;
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
    private ScrollPane scrollPane;
    private Label timelineStateLabel;
    private DatePicker startDatePicker;
    private DatePicker endDatePicker;
    private StackPane rangeIconBox;

    private AnimationTimer autoScrollTimer;
    private long lastUpdate = 0;
    private double scrollVelocity = 0;
    private List<Schedule> loadedSchedules = new ArrayList<>();

    private double zoomFactor = 1.0;
    private LocalDateTime lastRangeStartAt;
    private LocalDateTime lastRangeEndAtExclusive;

    public TimelineView(MainController controller) {
        this.controller = controller;
        loadZoomPreference();
        initializeUI();
        startAutoScroll();
    }

    public double getZoomFactor() {
        return zoomFactor;
    }

    public void zoomIn() {
        zoomBy(ZOOM_STEP);
    }

    public void zoomOut() {
        zoomBy(1.0 / ZOOM_STEP);
    }

    public void setZoomFactor(double newZoomFactor) {
        setZoomFactorInternal(newZoomFactor, null);
    }

    private void zoomBy(double multiplier) {
        if (multiplier <= 0) {
            return;
        }
        long anchorMinuteOffset = resolveViewportCenterMinuteOffset();
        setZoomFactorInternal(zoomFactor * multiplier, anchorMinuteOffset);
    }

    private void setZoomFactorInternal(double newZoomFactor, Long anchorMinuteOffset) {
        double clamped = clampDouble(newZoomFactor, MIN_ZOOM, MAX_ZOOM);
        if (Math.abs(clamped - zoomFactor) < 0.0001) {
            return;
        }
        zoomFactor = clamped;
        persistZoomPreference();

        if (loadedSchedules == null || loadedSchedules.isEmpty()) {
            return;
        }

        renderTimeline(loadedSchedules, anchorMinuteOffset, false);
    }

    private double computePixelsPerMinute() {
        return (BASE_CELL_WIDTH_PX * zoomFactor) / BASE_CELL_MINUTES;
    }

    private long resolveTotalMinutes() {
        if (lastRangeStartAt == null || lastRangeEndAtExclusive == null) {
            return 0;
        }
        long minutes = ChronoUnit.MINUTES.between(lastRangeStartAt, lastRangeEndAtExclusive);
        return Math.max(0, minutes);
    }

    private long resolveViewportCenterMinuteOffset() {
        if (scrollPane == null || timelinePane == null) {
            return 0;
        }
        double viewportWidth = scrollPane.getViewportBounds().getWidth();
        double contentWidth = timelinePane.getWidth();
        double maxScroll = Math.max(0, contentWidth - viewportWidth);
        double scrollX = maxScroll <= 0 ? 0 : scrollPane.getHvalue() * maxScroll;
        double centerX = scrollX + viewportWidth / 2.0;

        double pixelsPerMinute = computePixelsPerMinute();
        if (pixelsPerMinute <= 0) {
            return 0;
        }

        double minuteOffset = (centerX - LEFT_PADDING) / pixelsPerMinute;
        if (Double.isNaN(minuteOffset) || Double.isInfinite(minuteOffset)) {
            return 0;
        }

        long totalMinutes = resolveTotalMinutes();
        return (long) clampDouble(minuteOffset, 0, totalMinutes);
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

        scrollPane = new ScrollPane(timelinePane);
        scrollPane.getStyleClass().add("timeline-scroll");
        scrollPane.setFitToHeight(false);
        scrollPane.setPannable(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.ALWAYS);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        
        timelinePane.setOnScroll(event -> {
            if (event.getDeltaY() != 0) {
                WheelModifier modifier = controller.getTimelineZoomWheelModifier();
                if (modifier != null && modifier.matches(event)) {
                    double multiplier = event.getDeltaY() > 0 ? ZOOM_STEP : (1.0 / ZOOM_STEP);
                    zoomBy(multiplier);
                    event.consume();
                    return;
                }

                double viewportWidth = scrollPane.getViewportBounds().getWidth();
                double contentWidth = timelinePane.getWidth();
                double maxScroll = contentWidth - viewportWidth;

                if (maxScroll > 0) {
                    // Horizontal scroll: one wheel tick scrolls one base cell (6 hours).
                    double pixelsPerMinute = computePixelsPerMinute();
                    double pixelDelta = Math.signum(event.getDeltaY()) * (BASE_CELL_MINUTES * pixelsPerMinute);
                    double hvalueDelta = pixelDelta / maxScroll;

                    double hvalue = scrollPane.getHvalue() - hvalueDelta;
                    hvalue = Math.max(-0.02, Math.min(1.02, hvalue));
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

    private void renderTimeline(List<Schedule> allSchedules, Long anchorMinuteOffset, boolean scrollToToday) {
        timelinePane.getChildren().clear();

        List<Schedule> baseSchedules = new ArrayList<>(allSchedules == null ? List.of() : allSchedules);
        if (baseSchedules.isEmpty()) {
            showTimelineState(text("view.timeline.emptyWithoutDates"));
            return;
        }

        baseSchedules.removeIf(s -> resolveTimelineStartAt(s) == null || resolveTimelineEndAt(s) == null);

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
            showTimelineState(text("view.timeline.invalidRange"));
            return;
        }

        allSchedules = ScheduleOccurrenceProjector.projectForRange(baseSchedules, minDate, maxDate, false);

        List<Schedule> shortTasks = new ArrayList<>();
        List<Schedule> mediumTasks = new ArrayList<>();
        List<Schedule> longTasks = new ArrayList<>();

        for (Schedule s : allSchedules) {
            long days = ChronoUnit.DAYS.between(resolveTimelineStartDate(s), resolveTimelineEndDate(s)) + 1;
            if (days < 7) shortTasks.add(s);
            else if (days <= 35) mediumTasks.add(s);
            else longTasks.add(s);
        }

        Comparator<Schedule> cmp = Comparator.comparing(TimelineView::resolveTimelineStartAt)
            .thenComparing(TimelineView::resolveTimelineEndAt)
            .thenComparing(Schedule::getPriorityValue, Comparator.reverseOrder())
            .thenComparing(Schedule::getName, Comparator.nullsLast(String::compareToIgnoreCase));
            
        shortTasks.sort(cmp);
        mediumTasks.sort(cmp);
        longTasks.sort(cmp);

        List<TimelineEntry> timelineEntries = new ArrayList<>();
        double currentY = TRACK_TOP;
        
        currentY = appendGroup(shortTasks, text("view.timeline.group.short"), currentY, minDate, maxDate, timelineEntries);
        currentY = appendGroup(mediumTasks, text("view.timeline.group.medium"), currentY, minDate, maxDate, timelineEntries);
        currentY = appendGroup(longTasks, text("view.timeline.group.long"), currentY, minDate, maxDate, timelineEntries);

        LocalDateTime rangeStartAt = minDate.atStartOfDay();
        LocalDateTime rangeEndAtExclusive = maxDate.plusDays(1).atStartOfDay();
        lastRangeStartAt = rangeStartAt;
        lastRangeEndAtExclusive = rangeEndAtExclusive;

        long totalMinutes = ChronoUnit.MINUTES.between(rangeStartAt, rangeEndAtExclusive);
        double pixelsPerMinute = computePixelsPerMinute();
        double totalWidth = LEFT_PADDING + RIGHT_PADDING + totalMinutes * pixelsPerMinute;
        double paneHeight = currentY + BOTTOM_PADDING;

        timelinePane.setPrefWidth(totalWidth);
        timelinePane.setPrefHeight(Math.max(320, paneHeight));

        renderTracksBackground(totalWidth, paneHeight);
        renderDateAxis(minDate, maxDate, totalWidth, paneHeight, pixelsPerMinute, totalMinutes);

        for (TimelineEntry entry : timelineEntries) {
            renderTimelineEntry(entry, rangeStartAt, rangeEndAtExclusive, totalWidth, pixelsPerMinute);
        }

        if (timelineEntries.isEmpty()) {
            showTimelineState(text("view.timeline.emptyInRange"));
        } else {
            hideTimelineState();
        }

        if (anchorMinuteOffset != null) {
            scrollToMinuteOffset(anchorMinuteOffset, totalWidth, pixelsPerMinute, true);
        } else if (scrollToToday) {
            scrollToToday(minDate, maxDate, totalWidth, pixelsPerMinute);
        }
    }

    private double appendGroup(List<Schedule> schedules, String title, double startY, LocalDate minDate, LocalDate maxDate, List<TimelineEntry> entries) {
        Label headerLabel = new Label(title);
        headerLabel.getStyleClass().add("timeline-group-header");
        headerLabel.setLayoutX(LEFT_PADDING);
        headerLabel.setLayoutY(startY + 10);
        timelinePane.getChildren().add(headerLabel);
        
        Line sep = new Line(LEFT_PADDING, startY + 35, LEFT_PADDING + 200, startY + 35);
        sep.getStyleClass().add("timeline-group-sep");
        timelinePane.getChildren().add(sep);

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
            timelinePane.getChildren().add(emptyLabel);
            return cardStartY + 30 + 20;
        }
        
        return cardStartY + (maxStack + 1) * CARD_STACK_OFFSET + 20; // 20 padding bottom
    }

    private void renderTracksBackground(double totalWidth, double paneHeight) {
        double contentWidth = totalWidth - LEFT_PADDING - RIGHT_PADDING;

        Rectangle trackBackground = new Rectangle(LEFT_PADDING, TRACK_TOP, contentWidth, Math.max(0, paneHeight - TRACK_TOP - BOTTOM_PADDING));
        trackBackground.getStyleClass().add("timeline-track-bg");
        timelinePane.getChildren().add(trackBackground);

        Line topSeparator = new Line(LEFT_PADDING, TRACK_TOP, totalWidth - RIGHT_PADDING, TRACK_TOP);
        topSeparator.getStyleClass().add("timeline-track-sep");
        timelinePane.getChildren().add(topSeparator);

        Line bottomSeparator = new Line(
            LEFT_PADDING,
            paneHeight - BOTTOM_PADDING,
            totalWidth - RIGHT_PADDING,
            paneHeight - BOTTOM_PADDING
        );
        bottomSeparator.getStyleClass().add("timeline-track-sep");
        timelinePane.getChildren().add(bottomSeparator);
    }

    private void renderDateAxis(
        LocalDate minDate,
        LocalDate maxDate,
        double totalWidth,
        double paneHeight,
        double pixelsPerMinute,
        long totalMinutes
    ) {
        double gridBottom = paneHeight - BOTTOM_PADDING / 2;

        Line axisLine = new Line(LEFT_PADDING, AXIS_LINE_Y, totalWidth - RIGHT_PADDING, AXIS_LINE_Y);
        axisLine.getStyleClass().add("timeline-axis-line");
        axisLine.setStrokeWidth(2);
        timelinePane.getChildren().add(axisLine);

        double dayWidthPx = 1440 * pixelsPerMinute;
        if (dayWidthPx <= 0) {
            return;
        }

        // Today highlight + date labels per day.
        LocalDate current = minDate;
        int dayIndex = 0;
        while (!current.isAfter(maxDate)) {
            double x = LEFT_PADDING + dayIndex * dayWidthPx;

            if (current.equals(LocalDate.now())) {
                Rectangle todayHighlight = new Rectangle(x, 0, dayWidthPx, gridBottom);
                todayHighlight.getStyleClass().add("timeline-today-highlight");
                timelinePane.getChildren().add(todayHighlight);
            }

            Label dateLabel = new Label(current.format(DATE_FORMATTER));
            dateLabel.getStyleClass().add("schedule-date");
            dateLabel.setAlignment(Pos.CENTER);
            dateLabel.setPrefWidth(dayWidthPx);
            dateLabel.setLayoutX(x);
            dateLabel.setLayoutY(AXIS_LABEL_Y);
            if (current.equals(LocalDate.now())) {
                dateLabel.getStyleClass().add("timeline-date-today");
            }
            timelinePane.getChildren().add(dateLabel);

            current = current.plusDays(1);
            dayIndex++;
        }

        // Grid + ticks every 6 hours.
        for (long minute = 0; minute <= totalMinutes; minute += BASE_CELL_MINUTES) {
            double x = LEFT_PADDING + minute * pixelsPerMinute;

            Line gridLine = new Line(x, AXIS_LINE_Y, x, gridBottom);
            gridLine.getStyleClass().add("timeline-grid-line");
            timelinePane.getChildren().add(gridLine);

            Line tick = new Line(x, AXIS_LINE_Y - 4, x, AXIS_LINE_Y + 6);
            tick.getStyleClass().add("timeline-axis-tick");
            timelinePane.getChildren().add(tick);
        }
    }

    private void renderTimelineEntry(
        TimelineEntry entry,
        LocalDateTime rangeStartAt,
        LocalDateTime rangeEndAtExclusive,
        double totalWidth,
        double pixelsPerMinute
    ) {
        Schedule schedule = entry.getSchedule();
        LocalDateTime entryStartAt = entry.getStartAt();
        LocalDateTime entryEndAt = entry.getEndAt();
        if (entryStartAt == null || entryEndAt == null) {
            return;
        }

        LocalDateTime maxEndInclusive = rangeEndAtExclusive != null ? rangeEndAtExclusive.minusMinutes(1) : null;
        LocalDateTime visualStart = rangeStartAt != null && entryStartAt.isBefore(rangeStartAt) ? rangeStartAt : entryStartAt;
        LocalDateTime visualEnd = maxEndInclusive != null && entryEndAt.isAfter(maxEndInclusive) ? maxEndInclusive : entryEndAt;
        if (visualEnd.isBefore(visualStart)) {
            return;
        }

        long startOffsetMinutes = rangeStartAt != null ? ChronoUnit.MINUTES.between(rangeStartAt, visualStart) : 0;
        long durationMinutesInclusive = Math.max(1, ChronoUnit.MINUTES.between(visualStart, visualEnd) + 1);

        double durationPx = durationMinutesInclusive * pixelsPerMinute;
        // Avoid negative widths for very short schedules while preserving minute-level proportions.
        double insetX = Math.min(CARD_INSET_X, Math.max(0, durationPx * 0.18));
        double startX = LEFT_PADDING + startOffsetMinutes * pixelsPerMinute + insetX;
        double width = Math.max(1, durationPx - insetX * 2.0);
        double cardY = entry.getCardY();

        StackPane scheduleCard = new StackPane();
        scheduleCard.setCursor(Cursor.HAND);
        scheduleCard.setPrefSize(width, CARD_HEIGHT);
        scheduleCard.setLayoutX(startX);
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

        double baseViewOrder = -(cardY * 10000 + startX);
        scheduleCard.setViewOrder(baseViewOrder);

        Rectangle cardBackground = new Rectangle(width, CARD_HEIGHT);
        cardBackground.getStyleClass().addAll("card-bg", "schedule-card-layer");

        Rectangle accentBar = new Rectangle(Math.min(6, width), CARD_HEIGHT);
        accentBar.getStyleClass().addAll("card-accent-bar", "schedule-card-accent");

        // 改进布局：将标题和日期放入水平容器以防止重叠，并处理截断
        HBox contentBox = new HBox(10);
        contentBox.setAlignment(Pos.CENTER_LEFT);
        contentBox.setPadding(new Insets(0, 10, 0, 12));
        contentBox.setPrefSize(width, CARD_HEIGHT);
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
        
        if (width < 72) {
            titleLabel.setVisible(false);
            titleLabel.setManaged(false);
        }

        HBox.setHgrow(titleLabel, Priority.ALWAYS);
        contentBox.getChildren().addAll(statusControl, titleLabel);

        Tooltip tooltip = new Tooltip(schedule.getName() + "\n" + formatEntryRangeLabel(entryStartAt, entryEndAt, schedule));
        tooltip.setWrapText(true);
        tooltip.setMaxWidth(520);
        Tooltip.install(scheduleCard, tooltip);

        scheduleCard.getChildren().addAll(cardBackground, accentBar, contentBox);
        StackPane.setAlignment(accentBar, Pos.CENTER_LEFT);

        if (rangeStartAt != null && entryStartAt.isBefore(rangeStartAt)) {
            Rectangle leftClip = new Rectangle(8, CARD_HEIGHT);
            leftClip.getStyleClass().addAll("card-clip", "schedule-card-clip");
            StackPane.setAlignment(leftClip, Pos.CENTER_LEFT);
            scheduleCard.getChildren().add(leftClip);
        }

        if (rangeEndAtExclusive != null && !entryEndAt.isBefore(rangeEndAtExclusive)) {
            Rectangle rightClip = new Rectangle(8, CARD_HEIGHT);
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
            scheduleCard.setViewOrder(baseViewOrder - 1_000_000);
            scheduleCard.pseudoClassStateChanged(javafx.css.PseudoClass.getPseudoClass("hover"), true);
            scaleIn.playFromStart();
            transIn.playFromStart();
        });

        scheduleCard.setOnMouseExited(e -> {
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
        timelinePane.getChildren().add(scheduleCard);
    }

    private void highlightSelectedScheduleCard(StackPane selectedCard) {
        for (Node node : timelinePane.getChildren()) {
            node.getStyleClass().remove("timeline-schedule-selected");
            node.getStyleClass().remove("schedule-card-state-selected");
        }
        selectedCard.getStyleClass().addAll("timeline-schedule-selected", "schedule-card-state-selected");
    }

    private void scrollToToday(LocalDate minDate, LocalDate maxDate, double totalWidth, double pixelsPerMinute) {
        Platform.runLater(() -> {
            if (minDate == null || maxDate == null) {
                scrollPane.setHvalue(0);
                return;
            }

            LocalDate today = LocalDate.now();
            if (today.isBefore(minDate) || today.isAfter(maxDate)) {
                scrollPane.setHvalue(0);
                return;
            }

            LocalDateTime rangeStartAt = minDate.atStartOfDay();
            LocalDateTime todayCenterAt = today.atTime(LocalTime.NOON);
            long todayOffsetMinutes = ChronoUnit.MINUTES.between(rangeStartAt, todayCenterAt);
            double todayCenterX = LEFT_PADDING + todayOffsetMinutes * pixelsPerMinute;

            double viewportWidth = scrollPane.getViewportBounds().getWidth();
            double maxScroll = totalWidth - viewportWidth;
            if (maxScroll <= 0) {
                scrollPane.setHvalue(0);
                return;
            }

            double targetScroll = todayCenterX - viewportWidth * 0.42;
            scrollPane.setHvalue(Math.max(0, Math.min(1, targetScroll / maxScroll)));
        });
    }

    private void scrollToMinuteOffset(
        long minuteOffset,
        double totalWidth,
        double pixelsPerMinute,
        boolean center
    ) {
        Platform.runLater(() -> {
            double viewportWidth = scrollPane.getViewportBounds().getWidth();
            double maxScroll = totalWidth - viewportWidth;
            if (maxScroll <= 0) {
                scrollPane.setHvalue(0);
                return;
            }

            double anchorX = LEFT_PADDING + minuteOffset * pixelsPerMinute;
            double targetScroll = center ? anchorX - viewportWidth / 2.0 : anchorX - viewportWidth * 0.42;
            double hvalue = clampDouble(targetScroll / maxScroll, 0, 1);
            scrollPane.setHvalue(hvalue);
        });
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
