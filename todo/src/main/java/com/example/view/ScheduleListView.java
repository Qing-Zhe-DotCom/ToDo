package com.example.view;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.example.controller.MainController;
import com.example.controller.ScheduleCompletionCoordinator;
import com.example.controller.ScheduleCompletionMutation;
import com.example.databaseutil.ScheduleDAO;
import com.example.model.Schedule;

import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;

public class ScheduleListView implements View, ScheduleCompletionParticipant {
    private static final double COMPLETED_CARD_OPACITY = 0.7;

    private final MainController controller;
    private final ScheduleDAO scheduleDAO;
    private final List<Schedule> loadedSchedules = new ArrayList<>();
    private final Map<Integer, ScheduleCardNode> cardNodesById = new LinkedHashMap<>();

    private VBox root;
    private ScrollPane listScrollPane;
    private VBox listContent;
    private VBox pendingSection;
    private VBox completedSection;
    private VBox pendingCardsBox;
    private VBox completedCardsBox;
    private GroupHeader pendingHeader;
    private GroupHeader completedHeader;

    private CheckBox showPastCheckbox;
    private ComboBox<String> sortComboBox;
    private ComboBox<String> filterComboBox;

    private boolean showingSearchResults = false;
    private String currentSearchKeyword = "";
    private boolean pendingCollapsed = false;
    private boolean completedCollapsed = false;
    private Node completedGroupHeaderNode;

    private static final class GroupHeader {
        private final HBox root;
        private final Label arrowLabel;

        private GroupHeader(String title, Runnable toggleAction) {
            arrowLabel = new Label("鈻?");
            arrowLabel.getStyleClass().add("schedule-group-arrow");

            Label textLabel = new Label(title);
            textLabel.getStyleClass().addAll("completed-group-header", "schedule-group-title");

            root = new HBox(6, arrowLabel, textLabel);
            root.setAlignment(Pos.CENTER_LEFT);
            root.getStyleClass().add("schedule-group-toggle");
            root.addEventFilter(MouseEvent.MOUSE_PRESSED, event -> event.consume());
            root.setOnMouseClicked(event -> {
                toggleAction.run();
                event.consume();
            });
        }

        private HBox getRoot() {
            return root;
        }

        private void updateCollapsed(boolean collapsed) {
            arrowLabel.setRotate(collapsed ? 0 : 90);
        }
    }

    private final class ScheduleCardNode {
        private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM-dd");
        private final VBox container;
        private final StackPane cardMotionHost;
        private final StackPane cardShell;
        private final HBox cardInner;
        private final ScheduleStatusControl statusControl;
        private final Label priorityLabel;
        private final Label titleLabel;
        private final Label dateLabel;
        private final Label categoryLabel;
        private final ScheduleCollapsePopAnimator.MotionHandle motionHandle;
        private Schedule schedule;

        private ScheduleCardNode(Schedule schedule) {
            this.schedule = schedule;

            cardInner = new HBox(12);
            cardInner.setAlignment(Pos.CENTER_LEFT);
            cardInner.setPadding(new Insets(12, 16, 12, 16));
            cardInner.getStyleClass().add("schedule-card-inner");

            cardShell = new StackPane(cardInner);
            cardShell.getStyleClass().add("schedule-card-motion-shell");
            cardShell.setPickOnBounds(false);

            cardMotionHost = new StackPane(cardShell);
            cardMotionHost.getStyleClass().add("schedule-card-motion-host");
            cardMotionHost.setPickOnBounds(false);
            cardMotionHost.setMaxWidth(Double.MAX_VALUE);
            Rectangle clip = new Rectangle();
            clip.widthProperty().bind(cardMotionHost.widthProperty());
            clip.heightProperty().bind(cardMotionHost.heightProperty());
            cardMotionHost.setClip(clip);

            motionHandle = ScheduleCollapsePopAnimator.bindMotionHandle(
                cardMotionHost,
                cardShell,
                () -> {
                    cardMotionHost.requestLayout();
                    if (listContent != null) {
                        listContent.requestLayout();
                    }
                }
            );

            priorityLabel = new Label();
            titleLabel = new Label();
            titleLabel.getStyleClass().addAll("schedule-title", "schedule-card-title-text");
            dateLabel = new Label();
            dateLabel.getStyleClass().addAll("schedule-date", "schedule-card-subtitle-text");
            categoryLabel = new Label();
            categoryLabel.getStyleClass().add("category-tag");

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            statusControl = new ScheduleStatusControl(
                schedule != null && schedule.isCompleted(),
                ScheduleStatusControl.SizePreset.LIST,
                "schedule-status-role-list",
                targetCompleted -> handleStatusToggle(this, targetCompleted)
            );

            cardInner.getChildren().addAll(statusControl, priorityLabel, titleLabel, spacer, dateLabel, categoryLabel);

            container = new VBox(cardMotionHost);
            container.getStyleClass().add("schedule-list-card");
            container.setFillWidth(true);
            container.setMaxWidth(Double.MAX_VALUE);
            cardMotionHost.setMaxWidth(Double.MAX_VALUE);

            cardMotionHost.setOnMouseClicked(event -> {
                if (event.getTarget() instanceof Node
                    && isDescendant((Node) event.getTarget(), statusControl)) {
                    event.consume();
                    return;
                }
                controller.showScheduleDetails(this.schedule);
                refreshSelectionState();
                event.consume();
            });

            bindSchedule(schedule);
        }

        private VBox getContainer() {
            return container;
        }

        private Schedule getSchedule() {
            return schedule;
        }

        private ScheduleCollapsePopAnimator.MotionHandle getMotionHandle() {
            return motionHandle;
        }

        private void bindSchedule(Schedule schedule) {
            this.schedule = schedule;
            statusControl.syncCompleted(schedule.isCompleted());

            ScheduleCardStyleSupport.applyCardPresentation(
                cardInner,
                schedule,
                controller.getCurrentScheduleCardStyle(),
                "schedule-card-role-list"
            );

            cardInner.setOpacity(schedule.isCompleted() ? COMPLETED_CARD_OPACITY : 1.0);
            priorityLabel.setText(schedule.getPriority());
            priorityLabel.getStyleClass().removeAll("priority-high", "priority-medium", "priority-low");
            priorityLabel.getStyleClass().add("priority-" + getPriorityClass(schedule.getPriority()));

            titleLabel.setText(schedule.getName());
            titleLabel.getStyleClass().remove("title-completed");
            if (schedule.isCompleted()) {
                titleLabel.getStyleClass().add("title-completed");
            }

            String dateText = "";
            if (schedule.getDueDate() != null) {
                dateText = schedule.getDueDate().format(formatter);
                if (schedule.isOverdue() && !schedule.isCompleted()) {
                    dateText += " (宸茶繃鏈?)";
                }
            }
            dateLabel.setText(dateText);
            categoryLabel.setText(schedule.getCategory());

            container.getStyleClass().removeAll("completed", "overdue", "upcoming", "selected");
            cardInner.getStyleClass().remove("schedule-card-state-selected");
            if (schedule.isCompleted()) {
                container.getStyleClass().add("completed");
            } else if (schedule.isOverdue()) {
                container.getStyleClass().add("overdue");
            } else if (schedule.isUpcoming()) {
                container.getStyleClass().add("upcoming");
            }
            if (controller.isScheduleSelected(schedule)) {
                container.getStyleClass().add("selected");
                cardInner.getStyleClass().add("schedule-card-state-selected");
            }

            ScheduleReflowAnimator.bindCard(container, schedule);
            ScheduleReflowAnimator.bindCard(cardMotionHost, schedule);
        }

        private void syncAfterFailedCommit() {
            motionHandle.restoreSteadyState();
            statusControl.syncCompleted(schedule.isCompleted());
        }
    }

    public ScheduleListView(MainController controller) {
        this.controller = controller;
        this.scheduleDAO = new ScheduleDAO();
        initializeUI();
    }

    private void initializeUI() {
        root = new VBox(10);
        root.getStyleClass().add("main-content");
        root.setPadding(new Insets(15));

        HBox toolbar = createToolbar();

        pendingHeader = new GroupHeader("寰呭姙", () -> {
            pendingCollapsed = !pendingCollapsed;
            renderSchedules();
        });
        completedHeader = new GroupHeader("宸插畬鎴?", () -> {
            completedCollapsed = !completedCollapsed;
            renderSchedules();
        });
        completedGroupHeaderNode = completedHeader.getRoot();

        pendingCardsBox = new VBox(8);
        pendingCardsBox.getStyleClass().add("schedule-list-cards");

        completedCardsBox = new VBox(8);
        completedCardsBox.getStyleClass().add("schedule-list-cards");

        pendingSection = new VBox(8, pendingHeader.getRoot(), pendingCardsBox);
        pendingSection.getStyleClass().add("schedule-list-section");

        completedSection = new VBox(8, completedHeader.getRoot(), completedCardsBox);
        completedSection.getStyleClass().add("schedule-list-section");

        listContent = new VBox(12, pendingSection, completedSection);
        listContent.getStyleClass().addAll("schedule-list", "schedule-list-pane");
        listContent.setFillWidth(true);

        listScrollPane = new ScrollPane(listContent);
        listScrollPane.getStyleClass().addAll("schedule-list", "schedule-list-scroll");
        listScrollPane.setFitToWidth(true);
        listScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        VBox.setVgrow(listScrollPane, Priority.ALWAYS);

        Button newScheduleBtn = new Button("鏂板缓鏃ョ▼");
        newScheduleBtn.setGraphic(controller.createSvgIcon("/icons/macaron-logo-new-schedule.svg", null, 20));
        newScheduleBtn.getStyleClass().add("fab-button");
        newScheduleBtn.setOnAction(event -> controller.openNewScheduleDialog());

        HBox buttonBox = new HBox(newScheduleBtn);
        buttonBox.setAlignment(Pos.CENTER_RIGHT);
        buttonBox.setPadding(new Insets(10, 0, 0, 0));

        root.getChildren().addAll(toolbar, listScrollPane, buttonBox);
    }

    private HBox createToolbar() {
        HBox toolbar = new HBox(15);
        toolbar.setAlignment(Pos.CENTER_LEFT);

        Label titleLabel = new Label("鏃ョ▼绠＄悊");
        titleLabel.getStyleClass().add("label-title");

        showPastCheckbox = new CheckBox("鏄剧ず杩囧幓鏃ョ▼");
        showPastCheckbox.getStyleClass().add("check-box");
        showPastCheckbox.setOnAction(event -> {
            if (!showingSearchResults) {
                loadSchedules();
            }
        });

        sortComboBox = new ComboBox<>();
        sortComboBox.getItems().addAll("鎸夋棩鏈熸帓搴?", "鎸変紭鍏堢骇鎺掑簭", "鎸夊垎绫绘帓搴?");
        sortComboBox.setValue("鎸夋棩鏈熸帓搴?");
        sortComboBox.setOnAction(event -> {
            if (!showingSearchResults) {
                renderSchedules();
            }
        });

        filterComboBox = new ComboBox<>();
        filterComboBox.getItems().addAll("鍏ㄩ儴", "鏈畬鎴?", "宸插畬鎴?", "楂樹紭鍏堢骇", "鍗冲皢鍒版湡");
        filterComboBox.setValue("鍏ㄩ儴");
        filterComboBox.setOnAction(event -> {
            if (!showingSearchResults) {
                renderSchedules();
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
        if (showingSearchResults) {
            loadSearchResults(currentSearchKeyword);
            return;
        }
        loadSchedules();
    }

    private void loadSchedules() {
        try {
            List<Schedule> schedules = controller.applyPendingCompletionMutations(scheduleDAO.getAllSchedules());
            setLoadedSchedules(schedules);
        } catch (SQLException e) {
            controller.showError("鍔犺浇鏃ョ▼澶辫触", e.getMessage());
        }
    }

    private void loadSearchResults(String keyword) {
        try {
            List<Schedule> schedules = controller.applyPendingCompletionMutations(scheduleDAO.searchSchedules(keyword));
            setLoadedSchedules(schedules);
        } catch (SQLException e) {
            controller.showError("鎼滅储澶辫触", e.getMessage());
        }
    }

    private void setLoadedSchedules(List<Schedule> schedules) {
        loadedSchedules.clear();
        if (schedules != null) {
            loadedSchedules.addAll(schedules);
        }
        renderSchedules();
    }

    public void searchSchedules(String keyword) {
        showingSearchResults = true;
        currentSearchKeyword = keyword == null ? "" : keyword.trim();
        loadSearchResults(currentSearchKeyword);
    }

    public void clearSearch() {
        showingSearchResults = false;
        currentSearchKeyword = "";
        loadSchedules();
    }

    private void renderSchedules() {
        List<Schedule> displayedSchedules = buildDisplayedSchedules();
        Set<Integer> loadedIds = loadedSchedules.stream()
            .map(Schedule::getId)
            .filter(id -> id > 0)
            .collect(Collectors.toCollection(LinkedHashSet::new));

        List<Node> pendingNodes = new ArrayList<>();
        List<Node> completedNodes = new ArrayList<>();
        for (Schedule schedule : displayedSchedules) {
            ScheduleCardNode cardNode = cardNodesById.computeIfAbsent(schedule.getId(), id -> new ScheduleCardNode(schedule));
            cardNode.bindSchedule(schedule);
            if (schedule.isCompleted()) {
                completedNodes.add(cardNode.getContainer());
            } else {
                pendingNodes.add(cardNode.getContainer());
            }
        }

        List<Integer> staleIds = cardNodesById.keySet().stream()
            .filter(id -> !loadedIds.contains(id))
            .collect(Collectors.toList());
        for (Integer staleId : staleIds) {
            ScheduleCardNode staleNode = cardNodesById.remove(staleId);
            if (staleNode != null) {
                pendingCardsBox.getChildren().remove(staleNode.getContainer());
                completedCardsBox.getChildren().remove(staleNode.getContainer());
            }
        }

        pendingCardsBox.getChildren().setAll(pendingNodes);
        completedCardsBox.getChildren().setAll(completedNodes);

        pendingHeader.updateCollapsed(pendingCollapsed);
        completedHeader.updateCollapsed(completedCollapsed);

        updateSectionState(pendingSection, pendingHeader.getRoot(), pendingCardsBox, !pendingNodes.isEmpty(), pendingCollapsed);
        updateSectionState(completedSection, completedHeader.getRoot(), completedCardsBox, !completedNodes.isEmpty(), completedCollapsed);
        refreshSelectionState();
    }

    private void updateSectionState(
        VBox section,
        HBox header,
        VBox cardsBox,
        boolean hasCards,
        boolean collapsed
    ) {
        section.setManaged(hasCards);
        section.setVisible(hasCards);
        header.setManaged(hasCards);
        header.setVisible(hasCards);
        cardsBox.setManaged(hasCards && !collapsed);
        cardsBox.setVisible(hasCards && !collapsed);
    }

    private List<Schedule> buildDisplayedSchedules() {
        Comparator<Schedule> comparator = getDisplayComparator();
        if (showingSearchResults) {
            return loadedSchedules.stream().sorted(comparator).collect(Collectors.toList());
        }
        return loadedSchedules.stream()
            .filter(this::applyFilter)
            .sorted(comparator)
            .collect(Collectors.toList());
    }

    private void refreshSelectionState() {
        for (ScheduleCardNode cardNode : cardNodesById.values()) {
            cardNode.bindSchedule(cardNode.getSchedule());
        }
    }

    private boolean applyFilter(Schedule schedule) {
        if (!showPastCheckbox.isSelected()
            && schedule.getDueDate() != null
            && schedule.getDueDate().isBefore(LocalDate.now())
            && !schedule.isCompleted()) {
            return false;
        }
        if (!showPastCheckbox.isSelected()
            && schedule.getDueDate() != null
            && schedule.getDueDate().isAfter(LocalDate.now().plusDays(7))) {
            return false;
        }

        String filter = filterComboBox.getValue();
        if (filter == null) {
            filter = "鍏ㄩ儴";
        }

        if ("鏈畬鎴?".equals(filter)) {
            return !schedule.isCompleted();
        }
        if ("宸插畬鎴?".equals(filter)) {
            return schedule.isCompleted();
        }
        if ("楂樹紭鍏堢骇".equals(filter)) {
            return "楂?".equals(schedule.getPriority());
        }
        if ("鍗冲皢鍒版湡".equals(filter)) {
            return schedule.isUpcoming();
        }
        return true;
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

    private boolean handleStatusToggle(ScheduleCardNode cardNode, boolean targetCompleted) {
        Schedule schedule = cardNode.getSchedule();
        if (schedule == null) {
            return false;
        }
        if (!targetCompleted) {
            return controller.updateScheduleCompletion(schedule, false);
        }

        ScheduleCompletionCoordinator.PendingCompletion pendingCompletion =
            controller.prepareScheduleCompletion(schedule, true);
        if (pendingCompletion == null) {
            return false;
        }

        Map<Integer, Bounds> beforeBounds =
            ScheduleReflowAnimator.captureVisibleCardBounds(listContent, schedule.getId());

        ScheduleCollapsePopAnimator.playCollapseSource(
            cardNode.getMotionHandle(),
            pendingCompletion::commit,
            () -> handleCommittedListCompletion(schedule.getId(), beforeBounds),
            () -> {
                pendingCompletion.cancel();
                cardNode.syncAfterFailedCommit();
            },
            null
        );
        return true;
    }

    private void handleCommittedListCompletion(int scheduleId, Map<Integer, Bounds> beforeBounds) {
        ScheduleCollapsePopAnimator.MotionHandle targetHandle = resolveExpandedTargetHandle(scheduleId);
        if (targetHandle != null) {
            ScheduleCollapsePopAnimator.prepareTargetPopState(targetHandle);
        }

        ScheduleReflowAnimator.playVerticalReflow(listContent, beforeBounds, null);

        if (targetHandle != null) {
            ScheduleCollapsePopAnimator.playPreparedTargetPop(targetHandle, null);
            return;
        }

        if (completedGroupHeaderNode != null && completedGroupHeaderNode.isVisible()) {
            ScheduleReflowAnimator.playCollapsedReceive(
                completedGroupHeaderNode,
                CollapsePopKeyframePreset.targetPopDuration(),
                null
            );
        }
    }

    private ScheduleCollapsePopAnimator.MotionHandle resolveExpandedTargetHandle(int scheduleId) {
        if (completedCollapsed) {
            return null;
        }
        ScheduleCardNode targetNode = cardNodesById.get(scheduleId);
        if (targetNode == null || targetNode.getSchedule() == null || !targetNode.getSchedule().isCompleted()) {
            return null;
        }
        if (!completedCardsBox.getChildren().contains(targetNode.getContainer())) {
            return null;
        }
        return targetNode.getMotionHandle();
    }

    @Override
    public void applyCompletionMutation(ScheduleCompletionMutation mutation) {
        if (mutation == null) {
            return;
        }
        if (applyMutation(mutation, true)) {
            renderSchedules();
        }
    }

    @Override
    public void confirmCompletionMutation(ScheduleCompletionMutation mutation) {
        if (mutation == null) {
            return;
        }
        renderSchedules();
    }

    @Override
    public void revertCompletionMutation(ScheduleCompletionMutation mutation) {
        if (mutation == null) {
            return;
        }
        if (applyMutation(mutation, false)) {
            renderSchedules();
        }
    }

    private boolean applyMutation(ScheduleCompletionMutation mutation, boolean optimistic) {
        boolean changed = false;
        for (Schedule schedule : loadedSchedules) {
            if (!mutation.matches(schedule)) {
                continue;
            }
            if (optimistic) {
                mutation.applyTo(schedule);
            } else {
                mutation.revertOn(schedule);
            }
            changed = true;
        }
        return changed;
    }

    private boolean isDescendant(Node node, Node possibleAncestor) {
        Node current = node;
        while (current != null) {
            if (current == possibleAncestor) {
                return true;
            }
            current = current.getParent();
        }
        return false;
    }

    private String getPriorityClass(String priority) {
        if ("楂?".equals(priority)) {
            return "high";
        }
        if ("浣?".equals(priority)) {
            return "low";
        }
        return "medium";
    }
}
