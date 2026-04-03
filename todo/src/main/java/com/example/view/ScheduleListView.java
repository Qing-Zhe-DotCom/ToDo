package com.example.view;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.example.controller.MainController;
import com.example.databaseutil.ScheduleDAO;
import com.example.model.Schedule;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;

public class ScheduleListView implements View {
    private static final double COMPLETED_CARD_OPACITY = 0.7;

    private final MainController controller;
    private final ScheduleDAO scheduleDAO;

    private VBox root;
    private ListView<Schedule> scheduleListView;
    private ObservableList<Schedule> schedules;

    private CheckBox showPastCheckbox;
    private ComboBox<String> sortComboBox;
    private ComboBox<String> filterComboBox;

    private boolean showingSearchResults = false;
    private String currentSearchKeyword = "";
    private boolean pendingCollapsed = false;
    private boolean completedCollapsed = false;
    private Node completedGroupHeaderNode;
    private final Map<Integer, PendingCollapsePopState> pendingCollapsePopById = new HashMap<>();

    private static final class PendingCollapsePopState {
        private final int scheduleId;
        private final boolean expandedTarget;
        private boolean started;

        private PendingCollapsePopState(
            int scheduleId,
            boolean expandedTarget
        ) {
            this.scheduleId = scheduleId;
            this.expandedTarget = expandedTarget;
        }
    }

    public ScheduleListView(MainController controller) {
        this.controller = controller;
        this.scheduleDAO = new ScheduleDAO();
        this.schedules = FXCollections.observableArrayList();
        initializeUI();
    }

    private void initializeUI() {
        root = new VBox(10);
        root.getStyleClass().add("main-content");
        root.setPadding(new Insets(15));

        HBox toolbar = createToolbar();

        scheduleListView = new ListView<>();
        scheduleListView.getStyleClass().add("schedule-list");
        scheduleListView.setItems(schedules);
        scheduleListView.setCellFactory(listView -> new ScheduleListCell());
        scheduleListView.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue != null) {
                controller.showScheduleDetails(newValue);
            }
        });

        Button newScheduleBtn = new Button("新建日程");
        newScheduleBtn.setGraphic(controller.createSvgIcon("/icons/macaron-logo-new-schedule.svg", null, 20));
        newScheduleBtn.getStyleClass().add("fab-button");
        newScheduleBtn.setOnAction(event -> controller.openNewScheduleDialog());

        HBox buttonBox = new HBox(newScheduleBtn);
        buttonBox.setAlignment(Pos.CENTER_RIGHT);
        buttonBox.setPadding(new Insets(10, 0, 0, 0));

        VBox.setVgrow(scheduleListView, Priority.ALWAYS);
        root.getChildren().addAll(toolbar, scheduleListView, buttonBox);
    }

    private HBox createToolbar() {
        HBox toolbar = new HBox(15);
        toolbar.setAlignment(Pos.CENTER_LEFT);

        Label titleLabel = new Label("日程管理");
        titleLabel.getStyleClass().add("label-title");

        showPastCheckbox = new CheckBox("显示过去日程");
        showPastCheckbox.getStyleClass().add("check-box");
        showPastCheckbox.setOnAction(event -> {
            if (!showingSearchResults) {
                loadSchedules();
            }
        });

        sortComboBox = new ComboBox<>();
        sortComboBox.getItems().addAll("按日期排序", "按优先级排序", "按分类排序");
        sortComboBox.setValue("按日期排序");
        sortComboBox.setOnAction(event -> {
            if (!showingSearchResults) {
                loadSchedules();
            }
        });

        filterComboBox = new ComboBox<>();
        filterComboBox.getItems().addAll("全部", "未完成", "已完成", "高优先级", "即将到期");
        filterComboBox.setValue("全部");
        filterComboBox.setOnAction(event -> {
            if (!showingSearchResults) {
                loadSchedules();
            }
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        toolbar.getChildren().addAll(titleLabel, showPastCheckbox, sortComboBox, filterComboBox, spacer);
        return toolbar;
    }

    @Override
    public Node getView() {
        return root;
    }

    @Override
    public void refresh() {
        if (!showingSearchResults) {
            loadSchedules();
        }
    }

    private void loadSchedules() {
        runListMutation(() -> {
            try {
                List<Schedule> filteredSchedules = scheduleDAO.getAllSchedules().stream()
                    .filter(this::applyFilter)
                    .sorted(getDisplayComparator())
                    .collect(Collectors.toList());
                applySchedules(filteredSchedules);
            } catch (SQLException e) {
                controller.showError("加载日程失败", e.getMessage());
            }
        });
    }

    private boolean applyFilter(Schedule schedule) {
        if (!showPastCheckbox.isSelected() && schedule.getDueDate() != null && schedule.getDueDate().isBefore(LocalDate.now()) && !schedule.isCompleted()) {
            return false;
        }
        if (!showPastCheckbox.isSelected() && schedule.getDueDate() != null && schedule.getDueDate().isAfter(LocalDate.now().plusDays(7))) {
            return false;
        }

        String filter = filterComboBox.getValue();
        if (filter == null) {
            filter = "全部";
        }

        if ("未完成".equals(filter)) {
            return !schedule.isCompleted();
        }
        if ("已完成".equals(filter)) {
            return schedule.isCompleted();
        }
        if ("高优先级".equals(filter)) {
            return "高".equals(schedule.getPriority());
        }
        if ("即将到期".equals(filter)) {
            return schedule.isUpcoming();
        }
        return true;
    }

    private Comparator<Schedule> getComparator() {
        String sort = sortComboBox.getValue();
        if (sort == null) {
            sort = "按日期排序";
        }

        if ("按优先级排序".equals(sort)) {
            return Comparator
                .comparing(Schedule::isCompleted)
                .thenComparing(Schedule::getPriorityValue, Comparator.reverseOrder());
        }
        if ("按分类排序".equals(sort)) {
            return Comparator
                .comparing(Schedule::isCompleted)
                .thenComparing(Schedule::getCategory, Comparator.nullsFirst(Comparator.naturalOrder()));
        }
        return Comparator
            .comparing(Schedule::isCompleted)
            .thenComparing(Schedule::getDueDate, Comparator.nullsLast(Comparator.naturalOrder()));
    }

    public void searchSchedules(String keyword) {
        showingSearchResults = true;
        currentSearchKeyword = keyword;

        runListMutation(() -> {
            try {
                applySchedules(scheduleDAO.searchSchedules(keyword).stream()
                    .sorted(getDisplayComparator())
                    .collect(Collectors.toList()));
            } catch (SQLException e) {
                controller.showError("搜索失败", e.getMessage());
            }
        });
    }

    public void clearSearch() {
        showingSearchResults = false;
        currentSearchKeyword = "";
        loadSchedules();
    }

    private Comparator<Schedule> getDisplayComparator() {
        int sortIndex = sortComboBox != null ? sortComboBox.getSelectionModel().getSelectedIndex() : 0;
        return buildComparatorForSortIndex(sortIndex < 0 ? 0 : sortIndex);
    }

    static Comparator<Schedule> buildComparatorForSortIndex(int sortIndex) {
        Comparator<Schedule> pendingComparator = buildPendingComparatorForSortIndex(sortIndex);
        Comparator<Schedule> completedComparator = Comparator
            .comparing(Schedule::getUpdatedAt, Comparator.nullsLast(Comparator.reverseOrder()))
            .thenComparing(Schedule::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder()))
            .thenComparing(Schedule::getId, Comparator.reverseOrder());

        return (left, right) -> {
            int completedGroup = Boolean.compare(left.isCompleted(), right.isCompleted());
            if (completedGroup != 0) {
                return completedGroup;
            }
            if (left.isCompleted()) {
                return completedComparator.compare(left, right);
            }
            return pendingComparator.compare(left, right);
        };
    }

    private static Comparator<Schedule> buildPendingComparatorForSortIndex(int sortIndex) {
        if (sortIndex == 1) {
            return Comparator
                .comparing(Schedule::getPriorityValue, Comparator.reverseOrder())
                .thenComparing(Schedule::getDueDate, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(Schedule::getUpdatedAt, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(Schedule::getId);
        }
        if (sortIndex == 2) {
            return Comparator
                .comparing(Schedule::getCategory, Comparator.nullsFirst(Comparator.naturalOrder()))
                .thenComparing(Schedule::getDueDate, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(Schedule::getUpdatedAt, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(Schedule::getId);
        }
        return Comparator
            .comparing(Schedule::getDueDate, Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparing(Schedule::getUpdatedAt, Comparator.nullsLast(Comparator.reverseOrder()))
            .thenComparing(Schedule::getId);
    }

    private void applySchedules(List<Schedule> nextSchedules) {
        completedGroupHeaderNode = null;
        schedules.setAll(nextSchedules);
        forceListRefresh();
    }

    private void forceListRefresh() {
        if (scheduleListView == null) {
            return;
        }
        scheduleListView.refresh();
        scheduleListView.requestLayout();
        Platform.runLater(() -> {
            scheduleListView.refresh();
            scheduleListView.requestLayout();
        });
    }

    private void runListMutation(Runnable action) {
        if (Platform.isFxApplicationThread()) {
            action.run();
            return;
        }
        Platform.runLater(action);
    }

    public void addSchedule(Schedule schedule) throws SQLException {
        scheduleDAO.addSchedule(schedule);
        refresh();
    }

    public void updateSchedule(Schedule schedule) throws SQLException {
        scheduleDAO.updateSchedule(schedule);
        refresh();
    }

    public void deleteSchedule(Schedule schedule) throws SQLException {
        scheduleDAO.deleteSchedule(schedule.getId());
        refresh();
    }

    private ScheduleStatusControl createStatusControl(Schedule schedule, Node cardNode, ScheduleStatusControl[] controlRef) {
        ScheduleStatusControl control = new ScheduleStatusControl(
            schedule.isCompleted(),
            ScheduleStatusControl.SizePreset.LIST,
            "schedule-status-role-list",
            targetCompleted -> handleStatusToggle(cardNode, schedule, controlRef[0], targetCompleted)
        );
        controlRef[0] = control;
        return control;
    }

    private boolean handleStatusToggle(Node cardNode, Schedule schedule, ScheduleStatusControl control, boolean targetCompleted) {
        if (!targetCompleted) {
            return controller.updateScheduleCompletion(schedule, false);
        }

        int scheduleId = schedule.getId();
        clearPendingCollapsePop(scheduleId);

        ScheduleCollapsePopAnimator.playListComplete(
            ScheduleCollapsePopAnimator.resolveMotionHandle(cardNode),
            () -> {
                pendingCollapsePopById.put(scheduleId, new PendingCollapsePopState(scheduleId, !completedCollapsed));
                boolean updated = controller.updateScheduleCompletion(schedule, true);
                if (!updated) {
                    clearPendingCollapsePop(scheduleId);
                }
                return updated;
            },
            completedCollapsed ? null : () -> resolveExpandedTargetHandle(scheduleId),
            () -> completedGroupHeaderNode,
            () -> {
                clearPendingCollapsePop(scheduleId);
                if (control != null) {
                    control.syncCompleted(false);
                }
            },
            () -> clearPendingCollapsePop(scheduleId)
        );
        return true;
    }

    private void clearPendingCollapsePop(int scheduleId) {
        pendingCollapsePopById.remove(scheduleId);
    }

    private ScheduleCollapsePopAnimator.MotionHandle resolveExpandedTargetHandle(int scheduleId) {
        PendingCollapsePopState state = pendingCollapsePopById.get(scheduleId);
        if (state == null || !state.expandedTarget) {
            return null;
        }
        ScheduleReflowAnimator.VisibleCard targetCard =
            ScheduleReflowAnimator.findVisibleCardByIdAndCompletion(scheduleListView, scheduleId, true);
        if (targetCard == null) {
            return null;
        }
        ScheduleCollapsePopAnimator.MotionHandle handle =
            ScheduleCollapsePopAnimator.resolveMotionHandle(targetCard.getNode());
        if (handle != null) {
            state.started = true;
        }
        return handle;
    }

    private class ScheduleListCell extends ListCell<Schedule> {
        private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM-dd");

        @Override
        protected void updateItem(Schedule schedule, boolean empty) {
            super.updateItem(schedule, empty);

            setGraphic(null);
            setText(null);
            setManaged(true);
            setVisible(true);
            setOpacity(1.0);
            setTranslateX(0.0);
            setTranslateY(0.0);
            setScaleX(1.0);
            setScaleY(1.0);
            setMouseTransparent(false);
            setPrefHeight(Region.USE_COMPUTED_SIZE);
            setMinHeight(Region.USE_COMPUTED_SIZE);
            setMaxHeight(Region.USE_COMPUTED_SIZE);

            if (empty || schedule == null) {
                getStyleClass().removeAll("completed", "overdue", "upcoming");
                return;
            }

            HBox cardInner = new HBox(12);
            cardInner.setAlignment(Pos.CENTER_LEFT);
            cardInner.setPadding(new Insets(12, 16, 12, 16));
            cardInner.getStyleClass().add("schedule-card-inner");
            cardInner.setOpacity(schedule.isCompleted() ? COMPLETED_CARD_OPACITY : 1.0);
            cardInner.setMouseTransparent(false);
            ScheduleCardStyleSupport.applyCardPresentation(
                cardInner,
                schedule,
                controller.getCurrentScheduleCardStyle(),
                "schedule-card-role-list"
            );

            StackPane cardShell = new StackPane(cardInner);
            cardShell.getStyleClass().add("schedule-card-motion-shell");
            cardShell.setPickOnBounds(false);

            StackPane cardMotionHost = new StackPane(cardShell);
            cardMotionHost.getStyleClass().add("schedule-card-motion-host");
            cardMotionHost.setPickOnBounds(false);
            cardMotionHost.setMaxWidth(Double.MAX_VALUE);
            Rectangle clip = new Rectangle();
            clip.widthProperty().bind(cardMotionHost.widthProperty());
            clip.heightProperty().bind(cardMotionHost.heightProperty());
            cardMotionHost.setClip(clip);

            ScheduleCollapsePopAnimator.MotionHandle motionHandle = ScheduleCollapsePopAnimator.bindMotionHandle(
                cardMotionHost,
                cardShell,
                () -> {
                    cardMotionHost.requestLayout();
                    requestLayout();
                    if (scheduleListView != null) {
                        scheduleListView.requestLayout();
                    }
                }
            );
            ScheduleReflowAnimator.bindCard(cardMotionHost, schedule);

            ScheduleStatusControl[] statusControlRef = new ScheduleStatusControl[1];
            ScheduleStatusControl statusControl = createStatusControl(schedule, cardMotionHost, statusControlRef);

            Label priorityLabel = new Label(schedule.getPriority());
            priorityLabel.getStyleClass().add("priority-" + getPriorityClass(schedule.getPriority()));

            Label titleLabel = new Label(schedule.getName());
            titleLabel.getStyleClass().addAll("schedule-title", "schedule-card-title-text");
            if (schedule.isCompleted()) {
                titleLabel.getStyleClass().add("title-completed");
            }

            String dateText = "";
            if (schedule.getDueDate() != null) {
                dateText = schedule.getDueDate().format(formatter);
                if (schedule.isOverdue() && !schedule.isCompleted()) {
                    dateText += " (已过期)";
                }
            }
            Label dateLabel = new Label(dateText);
            dateLabel.getStyleClass().addAll("schedule-date", "schedule-card-subtitle-text");

            Label categoryLabel = new Label(schedule.getCategory());
            categoryLabel.getStyleClass().add("category-tag");

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            cardInner.getChildren().addAll(statusControl, priorityLabel, titleLabel, spacer, dateLabel, categoryLabel);

            VBox container = new VBox(5);
            int index = getIndex();
            boolean isFirstPending = !schedule.isCompleted() && isFirstPending(index);
            boolean isFirstCompleted = schedule.isCompleted() && isFirstCompleted(index);
            boolean groupCollapsed = schedule.isCompleted() ? completedCollapsed : pendingCollapsed;

            if (isFirstPending) {
                HBox pendingHeader = createGroupHeaderLabel("待办", pendingCollapsed, () -> {
                    pendingCollapsed = !pendingCollapsed;
                    scheduleListView.refresh();
                });
                container.getChildren().add(pendingHeader);
            }

            if (isFirstCompleted) {
                HBox completedHeader = createGroupHeaderLabel("已完成", completedCollapsed, () -> {
                    completedCollapsed = !completedCollapsed;
                    scheduleListView.refresh();
                });
                completedGroupHeaderNode = completedHeader;
                container.getChildren().add(completedHeader);
            }

            PendingCollapsePopState popState = pendingCollapsePopById.get(schedule.getId());
            if (popState != null && schedule.isCompleted() && popState.expandedTarget && !popState.started) {
                ScheduleCollapsePopAnimator.prepareTargetPopState(motionHandle);
            }

            if (!groupCollapsed) {
                container.getChildren().add(cardMotionHost);
            } else if (!isFirstPending && !isFirstCompleted) {
                setGraphic(null);
                setText(null);
                setManaged(false);
                setVisible(false);
                setPrefHeight(0);
                setMinHeight(0);
                setMaxHeight(0);
                getStyleClass().removeAll("completed", "overdue", "upcoming");
                return;
            }

            setGraphic(container);

            if (!groupCollapsed) {
                cardMotionHost.setOnMouseClicked(event -> {
                    getListView().getSelectionModel().select(schedule);
                    controller.showScheduleDetails(schedule);
                    event.consume();
                });
            }

            getStyleClass().removeAll("completed", "overdue", "upcoming");
            if (schedule.isCompleted()) {
                getStyleClass().add("completed");
            } else if (schedule.isOverdue()) {
                getStyleClass().add("overdue");
            } else if (schedule.isUpcoming()) {
                getStyleClass().add("upcoming");
            }
        }

        private boolean isFirstPending(int index) {
            if (index < 0 || index >= getListView().getItems().size()) {
                return false;
            }
            Schedule current = getListView().getItems().get(index);
            if (current == null || current.isCompleted()) {
                return false;
            }
            if (index == 0) {
                return true;
            }
            Schedule previous = getListView().getItems().get(index - 1);
            return previous != null && previous.isCompleted();
        }

        private boolean isFirstCompleted(int index) {
            if (index < 0 || index >= getListView().getItems().size()) {
                return false;
            }
            Schedule current = getListView().getItems().get(index);
            if (current == null || !current.isCompleted()) {
                return false;
            }
            if (index == 0) {
                return true;
            }
            Schedule previous = getListView().getItems().get(index - 1);
            return previous != null && !previous.isCompleted();
        }

        private HBox createGroupHeaderLabel(String title, boolean collapsed, Runnable toggleAction) {
            Label arrowLabel = new Label("▸");
            arrowLabel.setRotate(collapsed ? 0 : 90);
            arrowLabel.getStyleClass().add("schedule-group-arrow");

            Label textLabel = new Label(title);
            textLabel.getStyleClass().addAll("completed-group-header", "schedule-group-title");

            HBox header = new HBox(6, arrowLabel, textLabel);
            header.setAlignment(Pos.CENTER_LEFT);
            header.getStyleClass().add("schedule-group-toggle");
            header.addEventFilter(MouseEvent.MOUSE_PRESSED, event -> event.consume());
            header.setOnMouseClicked(event -> {
                toggleAction.run();
                event.consume();
            });
            return header;
        }

        private String getPriorityClass(String priority) {
            if ("高".equals(priority)) {
                return "high";
            }
            if ("低".equals(priority)) {
                return "low";
            }
            return "medium";
        }
    }
}
