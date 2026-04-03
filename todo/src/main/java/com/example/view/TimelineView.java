package com.example.view;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.example.controller.MainController;
import com.example.databaseutil.ScheduleDAO;
import com.example.model.Schedule;

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

public class TimelineView implements View {

    private double DAY_WIDTH = 90;
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

    private final MainController controller;
    private final ScheduleDAO scheduleDAO;

    private VBox root;
    private StackPane timelineContainer;
    private Pane timelinePane;
    private ScrollPane scrollPane;
    private Label timelineStateLabel;
    private DatePicker startDatePicker;
    private DatePicker endDatePicker;

    private AnimationTimer autoScrollTimer;
    private long lastUpdate = 0;
    private double scrollVelocity = 0;

    public TimelineView(MainController controller) {
        this.controller = controller;
        this.scheduleDAO = new ScheduleDAO();
        initializeUI();
        startAutoScroll();
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
                // Calculate total scrollable width
                double viewportWidth = scrollPane.getViewportBounds().getWidth();
                double contentWidth = timelinePane.getWidth();
                double maxScroll = contentWidth - viewportWidth;
                
                if (maxScroll > 0) {
                    // We want one mouse wheel tick (typically deltaY = 40) to scroll exactly DAY_WIDTH (1 day) or 2*DAY_WIDTH (2 days)
                    // Let's set it to 1.5 days per typical tick for a smooth but precise feel
                    double pixelDelta = Math.signum(event.getDeltaY()) * (DAY_WIDTH * 1.5);
                    
                    // Convert pixel delta to hvalue delta (0.0 to 1.0)
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

        Button newScheduleBtn = new Button("新建日程");
        newScheduleBtn.setGraphic(controller.createSvgIcon("/icons/macaron-logo-new-schedule.svg", null, 20));
        newScheduleBtn.getStyleClass().add("fab-button");
        newScheduleBtn.setOnAction(e -> controller.openNewScheduleDialog());

        HBox buttonBox = new HBox(newScheduleBtn);
        buttonBox.setAlignment(Pos.CENTER_RIGHT);
        buttonBox.setPadding(new Insets(10, 0, 0, 0));

        root.getChildren().addAll(header, timelineContainer, buttonBox);
    }

    private HBox createHeader() {
        HBox header = new HBox(15);
        header.setAlignment(Pos.CENTER_LEFT);

        Label titleLabel = new Label("日程时间轴");
        titleLabel.getStyleClass().add("label-title");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        startDatePicker = new DatePicker();
        startDatePicker.setPromptText("开始日期");
        startDatePicker.setPrefWidth(120);
        startDatePicker.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && endDatePicker.getValue() != null && newVal.isAfter(endDatePicker.getValue())) {
                endDatePicker.setValue(newVal.plusDays(7));
            }
            refresh();
        });

        endDatePicker = new DatePicker();
        endDatePicker.setPromptText("结束日期");
        endDatePicker.setPrefWidth(120);
        endDatePicker.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && startDatePicker.getValue() != null && newVal.isBefore(startDatePicker.getValue())) {
                startDatePicker.setValue(newVal.minusDays(7));
            }
            refresh();
        });

        Button resetBtn = new Button("重置视角");
        resetBtn.getStyleClass().add("button-secondary");
        resetBtn.setOnAction(e -> {
            startDatePicker.setValue(null);
            endDatePicker.setValue(null);
            refresh();
        });

        header.getChildren().addAll(
            titleLabel,
            spacer,
            new Label("日期范围:"),
            startDatePicker,
            new Label("至"),
            endDatePicker,
            resetBtn
        );

        return header;
    }

    @Override
    public Node getView() {
        return root;
    }

    @Override
    public void refresh() {
        showTimelineState("时间轴加载中...");
        Platform.runLater(() -> {
            try {
                drawTimeline();
            } catch (SQLException e) {
                showTimelineState("时间轴加载失败，请检查数据连接");
                controller.showError("加载时间轴失败", e.getMessage());
            }
        });
    }

    private void drawTimeline() throws SQLException {
        timelinePane.getChildren().clear();

        List<Schedule> allSchedules = scheduleDAO.getAllSchedules();
        if (allSchedules.isEmpty()) {
            showTimelineState("暂无可显示的日程，请为日程补充开始日期或截止日期");
            return;
        }

        allSchedules.removeIf(s -> resolveTimelineStart(s) == null || resolveTimelineEnd(s) == null);

        LocalDate rawMinDate = allSchedules.stream()
            .map(TimelineView::resolveTimelineStart)
            .min(LocalDate::compareTo)
            .orElse(LocalDate.now().minusDays(7));

        LocalDate rawMaxDate = allSchedules.stream()
            .map(TimelineView::resolveTimelineEnd)
            .max(LocalDate::compareTo)
            .orElse(LocalDate.now().plusDays(30));

        LocalDate minDate = startDatePicker.getValue() != null ? startDatePicker.getValue() : rawMinDate.minusDays(3);
        LocalDate maxDate = endDatePicker.getValue() != null ? endDatePicker.getValue() : rawMaxDate.plusDays(7);

        if (minDate.isAfter(maxDate)) {
            showTimelineState("起始日期不能晚于结束日期");
            return;
        }

        List<Schedule> shortTasks = new ArrayList<>();
        List<Schedule> mediumTasks = new ArrayList<>();
        List<Schedule> longTasks = new ArrayList<>();

        for (Schedule s : allSchedules) {
            long days = ChronoUnit.DAYS.between(resolveTimelineStart(s), resolveTimelineEnd(s)) + 1;
            if (days < 7) shortTasks.add(s);
            else if (days <= 35) mediumTasks.add(s);
            else longTasks.add(s);
        }

        Comparator<Schedule> cmp = Comparator.comparing(TimelineView::resolveTimelineStart)
            .thenComparing(TimelineView::resolveTimelineEnd)
            .thenComparing(Schedule::getPriorityValue, Comparator.reverseOrder())
            .thenComparing(Schedule::getName, Comparator.nullsLast(String::compareToIgnoreCase));
            
        shortTasks.sort(cmp);
        mediumTasks.sort(cmp);
        longTasks.sort(cmp);

        List<TimelineEntry> timelineEntries = new ArrayList<>();
        double currentY = TRACK_TOP;
        
        currentY = appendGroup(shortTasks, "短期日程 (<7天)", currentY, minDate, maxDate, timelineEntries);
        currentY = appendGroup(mediumTasks, "中期日程 (7-35天)", currentY, minDate, maxDate, timelineEntries);
        currentY = appendGroup(longTasks, "长期日程 (>35天)", currentY, minDate, maxDate, timelineEntries);

        long visibleDays = ChronoUnit.DAYS.between(minDate, maxDate) + 1;
        double totalWidth = LEFT_PADDING + RIGHT_PADDING + visibleDays * DAY_WIDTH;
        double paneHeight = currentY + BOTTOM_PADDING;

        timelinePane.setPrefWidth(totalWidth);
        timelinePane.setPrefHeight(Math.max(320, paneHeight));

        renderTracksBackground(totalWidth, paneHeight);
        renderDateAxis(minDate, maxDate, totalWidth, paneHeight);

        for (TimelineEntry entry : timelineEntries) {
            renderTimelineEntry(entry, minDate, maxDate, totalWidth);
        }

        if (timelineEntries.isEmpty()) {
            showTimelineState("当前日期范围内暂无日程");
        } else {
            hideTimelineState();
        }

        scrollToToday(minDate, visibleDays, totalWidth);
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

        for (Schedule schedule : schedules) {
            LocalDate sDate = resolveTimelineStart(schedule);
            LocalDate eDate = resolveTimelineEnd(schedule);
            if (sDate.isAfter(eDate)) { LocalDate t = sDate; sDate = eDate; eDate = t; }
            
            if (minDate != null && eDate.isBefore(minDate)) continue;
            if (maxDate != null && sDate.isAfter(maxDate)) continue;

            // 寻找第一个不冲突的层级
            int stackIndex = 0;
            boolean conflict = true;
            while (conflict) {
                conflict = false;
                for (TimelineEntry existing : groupEntries) {
                    // 如果存在重叠并且分配在同一层级
                    if (existing.getCardY() == cardStartY + stackIndex * CARD_STACK_OFFSET &&
                        !sDate.isAfter(existing.getEndDate()) && 
                        !eDate.isBefore(existing.getStartDate())) {
                        conflict = true;
                        stackIndex++;
                        break;
                    }
                }
            }
            
            maxStack = Math.max(maxStack, stackIndex);
            
            double cardY = cardStartY + stackIndex * CARD_STACK_OFFSET;
            TimelineEntry entry = new TimelineEntry(schedule, sDate, eDate, cardY);
            groupEntries.add(entry);
            entries.add(entry);
        }
        
        if (maxStack == -1) {
            Label emptyLabel = new Label("暂无该类日程");
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

    private void renderDateAxis(LocalDate minDate, LocalDate maxDate, double totalWidth, double paneHeight) {
                double gridBottom = paneHeight - BOTTOM_PADDING / 2;

        Line axisLine = new Line(LEFT_PADDING, AXIS_LINE_Y, totalWidth - RIGHT_PADDING, AXIS_LINE_Y);
        axisLine.getStyleClass().add("timeline-axis-line");
        axisLine.setStrokeWidth(2);
        timelinePane.getChildren().add(axisLine);

        LocalDate current = minDate;
        int dayIndex = 0;
        while (!current.isAfter(maxDate)) {
            double x = LEFT_PADDING + dayIndex * DAY_WIDTH;

            if (current.equals(LocalDate.now())) {
                Rectangle todayHighlight = new Rectangle(x, 0, DAY_WIDTH, gridBottom);
                todayHighlight.getStyleClass().add("timeline-today-highlight");
                timelinePane.getChildren().add(todayHighlight);
            }

            Line gridLine = new Line(x, AXIS_LINE_Y, x, gridBottom);
            gridLine.getStyleClass().add("timeline-grid-line");
            timelinePane.getChildren().add(gridLine);

            Line tick = new Line(x, AXIS_LINE_Y - 4, x, AXIS_LINE_Y + 6);
            tick.getStyleClass().add("timeline-axis-tick");
            timelinePane.getChildren().add(tick);

            Label dateLabel = new Label(current.format(DATE_FORMATTER));
            dateLabel.getStyleClass().add("schedule-date");
            dateLabel.setAlignment(Pos.CENTER);
            dateLabel.setPrefWidth(DAY_WIDTH);
            dateLabel.setLayoutX(x);
            dateLabel.setLayoutY(AXIS_LABEL_Y);
            if (current.equals(LocalDate.now())) {
                dateLabel.getStyleClass().add("timeline-date-today");
            }
            timelinePane.getChildren().add(dateLabel);

            current = current.plusDays(1);
            dayIndex++;
        }

        double finalX = LEFT_PADDING + dayIndex * DAY_WIDTH;
        Line finalGridLine = new Line(finalX, AXIS_LINE_Y, finalX, gridBottom);
        finalGridLine.getStyleClass().add("timeline-grid-line");
        timelinePane.getChildren().add(finalGridLine);

        Line finalTick = new Line(finalX, AXIS_LINE_Y - 4, finalX, AXIS_LINE_Y + 6);
        finalTick.getStyleClass().add("timeline-axis-tick");
        timelinePane.getChildren().add(finalTick);
    }

    private void renderTimelineEntry(TimelineEntry entry, LocalDate minDate, LocalDate maxDate, double totalWidth) {
        Schedule schedule = entry.getSchedule();
        LocalDate entryStart = entry.getStartDate();
        LocalDate entryEnd = entry.getEndDate();
        LocalDate visualStart = entryStart.isBefore(minDate) ? minDate : entryStart;
        LocalDate visualEnd = entryEnd.isAfter(maxDate) ? maxDate : entryEnd;

        long startOffset = ChronoUnit.DAYS.between(minDate, visualStart);
        long duration = ChronoUnit.DAYS.between(visualStart, visualEnd) + 1;

        double startX = LEFT_PADDING + startOffset * DAY_WIDTH + CARD_INSET_X;
        double width = duration * DAY_WIDTH - CARD_INSET_X * 2;
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

        Rectangle accentBar = new Rectangle(6, CARD_HEIGHT);
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
        titleLabel.setTextOverrun(OverrunStyle.ELLIPSIS);
        titleLabel.setMouseTransparent(true);
        // 确保标题长时显示省略号
        titleLabel.setTextOverrun(OverrunStyle.ELLIPSIS);
        
        Label dateLabel = new Label(entryStart.format(DATE_FORMATTER) + " - " + entryEnd.format(DATE_FORMATTER));
        dateLabel.getStyleClass().addAll("card-date", "schedule-card-subtitle-text");
        dateLabel.setMouseTransparent(true);
        dateLabel.setMinWidth(Region.USE_PREF_SIZE); // 防止日期被过度压缩

        // 处理过短的卡片
        if (width < 104) {
            dateLabel.setVisible(false);
            dateLabel.setManaged(false);
        }

        if (width < 72) {
            titleLabel.setVisible(false);
            titleLabel.setManaged(false);
        }

        HBox.setHgrow(titleLabel, Priority.ALWAYS);
        contentBox.getChildren().addAll(statusControl, titleLabel, dateLabel);

        scheduleCard.getChildren().addAll(cardBackground, accentBar, contentBox);
        StackPane.setAlignment(accentBar, Pos.CENTER_LEFT);

        if (entryStart.isBefore(minDate)) {
            Rectangle leftClip = new Rectangle(8, CARD_HEIGHT);
            leftClip.getStyleClass().addAll("card-clip", "schedule-card-clip");
            StackPane.setAlignment(leftClip, Pos.CENTER_LEFT);
            scheduleCard.getChildren().add(leftClip);
        }

        if (entryEnd.isAfter(maxDate)) {
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
            controller.showScheduleDetails(schedule);
            highlightSelectedScheduleCard(scheduleCard);
            if (e.getClickCount() == 2) {
                controller.openEditScheduleDialog(schedule);
            }
        });

        Tooltip.install(scheduleCard, new Tooltip(buildTooltipText(schedule, entryStart, entryEnd)));
        timelinePane.getChildren().add(scheduleCard);
    }

    private void highlightSelectedScheduleCard(StackPane selectedCard) {
        for (Node node : timelinePane.getChildren()) {
            node.getStyleClass().remove("timeline-schedule-selected");
            node.getStyleClass().remove("schedule-card-state-selected");
        }
        selectedCard.getStyleClass().addAll("timeline-schedule-selected", "schedule-card-state-selected");
    }

    private void scrollToToday(LocalDate minDate, long visibleDays, double totalWidth) {
        Platform.runLater(() -> {
            long todayOffset = ChronoUnit.DAYS.between(minDate, LocalDate.now());
            if (todayOffset < 0 || todayOffset >= visibleDays) {
                scrollPane.setHvalue(0);
                return;
            }

            double todayCenterX = LEFT_PADDING + todayOffset * DAY_WIDTH + DAY_WIDTH / 2;
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

    private List<Schedule> getFilteredSchedules(List<Schedule> schedules) {
        return filterAndSortSchedules(schedules, "全部视图"); // 统一视角，全部渲染
    }

    static List<Schedule> filterAndSortSchedules(List<Schedule> schedules, String level) {
        List<Schedule> renderableSchedules = new ArrayList<>(schedules);
        renderableSchedules.removeIf(schedule -> resolveTimelineStart(schedule) == null || resolveTimelineEnd(schedule) == null);
        
        renderableSchedules.removeIf(schedule -> {
            long days = ChronoUnit.DAYS.between(resolveTimelineStart(schedule), resolveTimelineEnd(schedule)) + 1;
            if (level.startsWith("日级") && days >= 7) return true;
            if (level.startsWith("周级") && (days < 7 || days > 35)) return true;
            if (level.startsWith("月级") && days <= 35) return true;
            return false;
        });

        renderableSchedules.sort(Comparator
            .comparing(TimelineView::resolveTimelineStart)
            .thenComparing(TimelineView::resolveTimelineEnd)
            .thenComparing(Schedule::getPriorityValue, Comparator.reverseOrder())
            .thenComparing(Schedule::getName, Comparator.nullsLast(String::compareToIgnoreCase)));
        return renderableSchedules;
    }

    static List<TimelineEntry> buildTimelineEntries(List<Schedule> schedules, LocalDate minDate, LocalDate maxDate, String level) {
        List<TimelineEntry> entries = new ArrayList<>();
        java.util.Map<LocalDate, Integer> dateCount = new java.util.HashMap<>();

        for (Schedule schedule : filterAndSortSchedules(schedules, level)) {
            LocalDate startDate = resolveTimelineStart(schedule);
            LocalDate endDate = resolveTimelineEnd(schedule);

            if (startDate.isAfter(endDate)) {
                LocalDate temp = startDate;
                startDate = endDate;
                endDate = temp;
            }

            if (minDate != null && endDate.isBefore(minDate)) {
                continue;
            }
            if (maxDate != null && startDate.isAfter(maxDate)) {
                continue;
            }

            int laneIndex = dateCount.getOrDefault(startDate, 0);
            dateCount.put(startDate, laneIndex + 1);

            entries.add(new TimelineEntry(schedule, startDate, endDate, laneIndex));
        }

        return entries;
    }

    static LocalDate resolveTimelineStart(Schedule schedule) {
        if (schedule.getStartDate() != null) {
            return schedule.getStartDate();
        }
        return schedule.getDueDate();
    }

    static LocalDate resolveTimelineEnd(Schedule schedule) {
        if (schedule.getDueDate() != null) {
            return schedule.getDueDate();
        }
        return schedule.getStartDate();
    }

    private String buildTooltipText(Schedule schedule, LocalDate startDate, LocalDate endDate) {
        long duration = ChronoUnit.DAYS.between(startDate, endDate) + 1;
        return schedule.getName() + "\n"
            + "开始: " + startDate + "\n"
            + "截止: " + endDate + "\n"
            + "时长: " + duration + " 天\n"
            + "优先级: " + schedule.getPriority() + "\n"
            + "状态: " + (schedule.isCompleted() ? "已完成" : "未完成");
    }

    public void stopAutoScroll() {
        if (autoScrollTimer != null) {
            autoScrollTimer.stop();
        }
    }

    static final class TimelineEntry {
        private final Schedule schedule;
        private final LocalDate startDate;
        private final LocalDate endDate;
        private final double cardY;

        TimelineEntry(Schedule schedule, LocalDate startDate, LocalDate endDate, double cardY) {
            this.schedule = schedule;
            this.startDate = startDate;
            this.endDate = endDate;
            this.cardY = cardY;
        }

        Schedule getSchedule() {
            return schedule;
        }

        LocalDate getStartDate() {
            return startDate;
        }

        LocalDate getEndDate() {
            return endDate;
        }

        double getCardY() {
            return cardY;
        }
    }
}
