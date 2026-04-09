package com.example.view;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.example.application.ScheduleOccurrenceProjector;
import com.example.controller.MainController;
import com.example.controller.ScheduleCompletionCoordinator;
import com.example.controller.ScheduleCompletionMutation;
import com.example.model.Schedule;

import javafx.application.Platform;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.input.InputMethodEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;

public class ScheduleListView implements View, ScheduleCompletionParticipant {
    private static final double COMPLETED_CARD_OPACITY = 0.7;
    private static final int SEARCH_LOOKBACK_DAYS = 30;
    private static final int SEARCH_LOOKAHEAD_DAYS = 90;
    private static final int UPCOMING_LOOKAHEAD_DAYS = 30;
    private static final int OVERDUE_LOOKBACK_DAYS = 90;
    static final String FILTER_MY_DAY = "my-day";
    static final String FILTER_OVERDUE = "overdue";
    static final String FILTER_ALL = "all";
    static final String FILTER_HIGH_PRIORITY = "high-priority";
    static final String FILTER_UPCOMING = "upcoming";

    private final MainController controller;
    private final List<Schedule> loadedSchedules = new ArrayList<>();
    private final Map<String, ScheduleCardNode> cardNodesById = new LinkedHashMap<>();

    private VBox root;
    private ScrollPane listScrollPane;
    private VBox listContent;
    private VBox pendingSection;
    private VBox completedSection;
    private VBox pendingCardsBox;
    private VBox completedCardsBox;
    private GroupHeader pendingHeader;
    private GroupHeader completedHeader;

    private ComboBox<String> filterComboBox;
    private TextField quickAddField;
    private boolean quickAddComposing;

    private boolean showingSearchResults = false;
    private String currentSearchKeyword = "";
    private boolean pendingCollapsed = false;
    private boolean completedCollapsed = false;
    private Node completedGroupHeaderNode;

    private record ProjectionWindow(LocalDate start, LocalDate end) {
    }

    private static final class GroupHeader {
        private final HBox root;
        private final Label arrowLabel;

        private GroupHeader(String title, Runnable toggleAction) {
            arrowLabel = new Label("\u25b6");
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
                if (event.getClickCount() == 2) {
                    controller.showScheduleDetailsAndFocusTitle(this.schedule);
                } else {
                    controller.showScheduleDetails(this.schedule);
                }
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
            priorityLabel.setText(controller.priorityDisplayName(schedule.getPriority()));
            priorityLabel.getStyleClass().removeAll("priority-high", "priority-medium", "priority-low");
            priorityLabel.getStyleClass().add("priority-" + getPriorityClass(schedule.getPriority()));

            titleLabel.setText(schedule.getName());
            titleLabel.getStyleClass().remove("title-completed");
            if (schedule.isCompleted()) {
                titleLabel.getStyleClass().add("title-completed");
            }

            String dateText = buildScheduleDateText(schedule, controller);
            if (!dateText.isEmpty() && schedule.isOverdue() && !schedule.isCompleted()) {
                dateText += " (" + controller.text("status.overdue") + ")";
            }
            dateLabel.setText(dateText);
            categoryLabel.setText(controller.categoryDisplayName(schedule.getCategory()));

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
        initializeUI();
    }

    private void initializeUI() {
        root = new VBox(10);
        root.getStyleClass().add("main-content");
        root.setPadding(new Insets(15));

        HBox toolbar = createToolbar();

        pendingHeader = new GroupHeader(controller.text("schedule.list.pending"), () -> {
            pendingCollapsed = !pendingCollapsed;
            renderSchedules();
        });
        completedHeader = new GroupHeader(controller.text("schedule.list.completed"), () -> {
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

        VBox quickAddBar = buildQuickAddBar();
        root.getChildren().addAll(toolbar, listScrollPane, quickAddBar);
    }

    private HBox createToolbar() {
        HBox toolbar = new HBox(15);
        toolbar.setAlignment(Pos.CENTER_LEFT);

        Label titleLabel = new Label(controller.text("schedule.list.title"));
        titleLabel.getStyleClass().add("label-title");

        filterComboBox = new ComboBox<>();
        filterComboBox.getItems().addAll(
            FILTER_MY_DAY,
            FILTER_OVERDUE,
            FILTER_ALL,
            FILTER_HIGH_PRIORITY,
            FILTER_UPCOMING
        );
        filterComboBox.setValue(FILTER_MY_DAY);
        filterComboBox.setButtonCell(createFilterButtonCell());
        filterComboBox.setCellFactory(listView -> createFilterDropdownCell());
        filterComboBox.setOnAction(event -> {
            if (!showingSearchResults) {
                renderSchedules();
            }
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        toolbar.getChildren().addAll(titleLabel, filterComboBox, spacer);
        return toolbar;
    }

    private VBox buildQuickAddBar() {
        Label quickAddTitle = new Label(controller.text("schedule.list.quickAdd"));
        quickAddTitle.getStyleClass().add("quick-add-title");

        Node quickAddIcon = controller.createSvgIcon("/icons/macaron-logo-simple-plus-blue.svg", null, 20);
        quickAddIcon.setMouseTransparent(true);
        StackPane quickAddBadge = new StackPane(quickAddIcon);
        quickAddBadge.getStyleClass().add("quick-add-badge");
        quickAddBadge.setMouseTransparent(true);

        quickAddField = new TextField();
        quickAddField.getStyleClass().add("quick-add-input");
        HBox.setHgrow(quickAddField, Priority.ALWAYS);
        quickAddField.setPromptText(controller.text("schedule.list.quickAddPrompt"));
        quickAddField.setOnAction(event -> {
            if (!quickAddComposing) {
                submitQuickAdd();
            }
        });
        quickAddField.addEventHandler(InputMethodEvent.INPUT_METHOD_TEXT_CHANGED, event -> {
            quickAddComposing = !event.getComposed().isEmpty();
        });
        quickAddField.focusedProperty().addListener((obs, oldFocused, focused) -> {
            if (!focused) {
                quickAddComposing = false;
            }
        });

        /*

        quickAddButton = new Button("新建日程");
        quickAddButton.getStyleClass().add("quick-add-action");
        quickAddButton.setGraphic(controller.createSvgIcon("/icons/macaron-logo-simple-plus-blue.svg", null, 18));
        quickAddButton.setGraphicTextGap(10);
        quickAddButton.setFocusTraversable(false);
        quickAddButton.setOnAction(event -> submitQuickAdd());

        */
        HBox quickAddBar = new HBox(16, quickAddBadge, quickAddField);
        quickAddBar.setAlignment(Pos.CENTER_LEFT);
        quickAddBar.getStyleClass().add("quick-add-shell");
        quickAddBar.hoverProperty().addListener((obs, oldHover, hovered) ->
            toggleStyleClass(quickAddBar, "quick-add-shell-hover", hovered)
        );
        quickAddBar.setOnMouseClicked(event -> quickAddField.requestFocus());
        quickAddField.focusedProperty().addListener((obs, oldFocused, focused) -> {
            if (!focused) {
                quickAddComposing = false;
            }
            toggleStyleClass(quickAddBar, "quick-add-shell-focused", focused);
        });

        VBox quickAddSection = new VBox(8, quickAddTitle, quickAddBar);
        quickAddSection.setPadding(new Insets(14, 0, 0, 0));
        quickAddSection.getStyleClass().add("quick-add-section");
        return quickAddSection;
    }

    private void submitQuickAdd() {
        if (quickAddField == null) {
            return;
        }

        String title = quickAddField.getText() == null ? "" : quickAddField.getText().strip();
        if (title.isEmpty()) {
            quickAddField.requestFocus();
            return;
        }

        try {
            controller.quickCreateSchedule(title);
            quickAddField.clear();
            quickAddField.requestFocus();
        } catch (SQLException exception) {
            controller.showError(controller.text("error.createSchedule.title"), exception.getMessage());
        }
    }

    @Override
    public Node getView() {
        return root;
    }

    public void focusQuickAddInput() {
        if (quickAddField == null) {
            return;
        }
        Platform.runLater(quickAddField::requestFocus);
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
            List<Schedule> schedules = controller.applyPendingCompletionMutations(controller.loadAllSchedules());
            setLoadedSchedules(schedules);
        } catch (SQLException e) {
            controller.showError(controller.text("error.loadSchedules.title"), e.getMessage());
        }
    }

    private void loadSearchResults(String keyword) {
        try {
            List<Schedule> schedules = controller.applyPendingCompletionMutations(controller.searchSchedules(keyword));
            setLoadedSchedules(schedules);
        } catch (SQLException e) {
            controller.showError(controller.text("error.search.title"), e.getMessage());
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
        if (!showingSearchResults && (currentSearchKeyword == null || currentSearchKeyword.isBlank())) {
            return;
        }
        showingSearchResults = false;
        currentSearchKeyword = "";
        loadSchedules();
    }

    private void renderSchedules() {
        List<Schedule> displayedSchedules = buildDisplayedSchedules();
        Set<String> loadedIds = displayedSchedules.stream()
            .map(schedule -> schedule.getViewKey() != null && !schedule.getViewKey().isBlank()
                ? schedule.getViewKey()
                : schedule.getId())
            .filter(id -> id != null && !id.isBlank())
            .collect(Collectors.toCollection(LinkedHashSet::new));

        List<Node> pendingNodes = new ArrayList<>();
        List<Node> completedNodes = new ArrayList<>();
        for (Schedule schedule : displayedSchedules) {
            String cardKey = schedule.getViewKey() != null && !schedule.getViewKey().isBlank()
                ? schedule.getViewKey()
                : schedule.getId();
            ScheduleCardNode cardNode = cardNodesById.computeIfAbsent(cardKey, id -> new ScheduleCardNode(schedule));
            cardNode.bindSchedule(schedule);
            if (schedule.isCompleted()) {
                completedNodes.add(cardNode.getContainer());
            } else {
                pendingNodes.add(cardNode.getContainer());
            }
        }

        List<String> staleIds = cardNodesById.keySet().stream()
            .filter(id -> !loadedIds.contains(id))
            .collect(Collectors.toList());
        for (String staleId : staleIds) {
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
        Comparator<Schedule> comparator = buildDisplayComparator();
        ProjectionWindow projectionWindow = resolveProjectionWindow(
            showingSearchResults ? FILTER_ALL : (filterComboBox != null ? filterComboBox.getValue() : FILTER_MY_DAY),
            LocalDate.now()
        );
        List<Schedule> projectedSchedules = ScheduleOccurrenceProjector.projectForRange(
            loadedSchedules,
            projectionWindow.start(),
            projectionWindow.end(),
            true
        );
        if (showingSearchResults) {
            return projectedSchedules.stream().sorted(comparator).collect(Collectors.toList());
        }
        return projectedSchedules.stream()
            .filter(schedule -> matchesFilter(schedule, filterComboBox != null ? filterComboBox.getValue() : FILTER_MY_DAY, LocalDate.now()))
            .sorted(comparator)
            .collect(Collectors.toList());
    }

    private ProjectionWindow resolveProjectionWindow(String filter, LocalDate today) {
        LocalDate reference = today != null ? today : LocalDate.now();
        if (FILTER_MY_DAY.equals(filter)) {
            return new ProjectionWindow(reference, reference);
        }
        if (FILTER_OVERDUE.equals(filter)) {
            return new ProjectionWindow(reference.minusDays(OVERDUE_LOOKBACK_DAYS), reference.minusDays(1));
        }
        if (FILTER_UPCOMING.equals(filter)) {
            return new ProjectionWindow(reference, reference.plusDays(UPCOMING_LOOKAHEAD_DAYS));
        }
        return new ProjectionWindow(reference.minusDays(SEARCH_LOOKBACK_DAYS), reference.plusDays(SEARCH_LOOKAHEAD_DAYS));
    }

    private void refreshSelectionState() {
        for (ScheduleCardNode cardNode : cardNodesById.values()) {
            cardNode.bindSchedule(cardNode.getSchedule());
        }
    }

    static boolean matchesFilter(Schedule schedule, String filter, LocalDate today) {
        if (schedule == null) {
            return false;
        }

        LocalDate referenceDate = today != null ? today : LocalDate.now();
        String normalizedFilter = filter != null ? filter : FILTER_MY_DAY;

        if (FILTER_MY_DAY.equals(normalizedFilter)) {
            return schedule.includesDate(referenceDate);
        }
        if (FILTER_OVERDUE.equals(normalizedFilter)) {
            return isOverdueOn(schedule, referenceDate);
        }
        if (FILTER_HIGH_PRIORITY.equals(normalizedFilter)) {
            return Schedule.PRIORITY_HIGH.equals(schedule.getPriority());
        }
        if (FILTER_UPCOMING.equals(normalizedFilter)) {
            return isUpcomingOn(schedule, referenceDate);
        }
        return true;
    }

    static boolean isOverdueOn(Schedule schedule, LocalDate referenceDate) {
        if (schedule == null || schedule.isCompleted() || schedule.getEffectiveEndAt() == null) {
            return false;
        }
        if (referenceDate != null && LocalDate.now().equals(referenceDate)) {
            return schedule.isOverdue();
        }
        return referenceDate != null && schedule.getEffectiveEndDate() != null && schedule.getEffectiveEndDate().isBefore(referenceDate);
    }

    static boolean isUpcomingOn(Schedule schedule, LocalDate referenceDate) {
        if (schedule == null || referenceDate == null || schedule.isCompleted()) {
            return false;
        }

        LocalDate effectiveDeadline = schedule.getEffectiveEndDate();
        if (effectiveDeadline == null) {
            return false;
        }

        long daysUntilDeadline = ChronoUnit.DAYS.between(referenceDate, effectiveDeadline);
        if (daysUntilDeadline < 0) {
            return false;
        }

        long durationDays = schedule.getEffectiveDurationDays();
        if (durationDays <= 0) {
            return false;
        }

        if (durationDays < 7) {
            return daysUntilDeadline == 0;
        }
        if (durationDays <= 35) {
            return daysUntilDeadline <= 3;
        }
        return daysUntilDeadline <= 7;
    }

    static Comparator<Schedule> buildDisplayComparator() {
        Comparator<Schedule> pendingComparator = buildPendingComparator();
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

    private static Comparator<Schedule> buildPendingComparator() {
        return Comparator
            .comparing(Schedule::getDueAt, Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparing(Schedule::getUpdatedAt, Comparator.nullsLast(Comparator.reverseOrder()))
            .thenComparing(Schedule::getId);
    }

    public void addSchedule(Schedule schedule) throws SQLException {
        controller.createSchedule(schedule);
        refresh();
    }

    public void updateSchedule(Schedule schedule) throws SQLException {
        controller.saveSchedule(schedule);
        refresh();
    }

    public void deleteSchedule(Schedule schedule) throws SQLException {
        controller.removeSchedule(schedule.getId());
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

        String cardKey = schedule.getViewKey() != null && !schedule.getViewKey().isBlank()
            ? schedule.getViewKey()
            : schedule.getId();
        Map<String, Bounds> beforeBounds =
            ScheduleReflowAnimator.captureVisibleCardBounds(listContent, cardKey);

        ScheduleCollapsePopAnimator.playCollapseSource(
            cardNode.getMotionHandle(),
            pendingCompletion::commit,
            () -> handleCommittedListCompletion(cardKey, beforeBounds),
            () -> {
                pendingCompletion.cancel();
                cardNode.syncAfterFailedCommit();
            },
            null
        );
        return true;
    }

    private void handleCommittedListCompletion(String scheduleId, Map<String, Bounds> beforeBounds) {
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

    private ScheduleCollapsePopAnimator.MotionHandle resolveExpandedTargetHandle(String scheduleId) {
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

    private void toggleStyleClass(Node node, String styleClass, boolean enabled) {
        if (enabled) {
            if (!node.getStyleClass().contains(styleClass)) {
                node.getStyleClass().add(styleClass);
            }
            return;
        }
        node.getStyleClass().remove(styleClass);
    }

    private String getPriorityClass(String priority) {
        if (Schedule.PRIORITY_HIGH.equals(priority)) {
            return "high";
        }
        if (Schedule.PRIORITY_LOW.equals(priority)) {
            return "low";
        }
        return "medium";
    }

    static String buildScheduleDateText(Schedule schedule) {
        if (schedule == null) {
            return "";
        }

        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("MM-dd HH:mm");
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");

        if (schedule.getStartAt() != null && schedule.getDueAt() != null) {
            if (schedule.getStartAt().toLocalDate().equals(schedule.getDueAt().toLocalDate())) {
                return schedule.getStartAt().format(dateTimeFormatter) + " - " + schedule.getDueAt().format(timeFormatter);
            }
            return schedule.getStartAt().format(dateTimeFormatter) + " - " + schedule.getDueAt().format(dateTimeFormatter);
        }
        if (schedule.getDueAt() != null) {
            return "截止 " + schedule.getDueAt().format(dateTimeFormatter);
        }
        if (schedule.getStartAt() != null) {
            return "开始 " + schedule.getStartAt().format(dateTimeFormatter);
        }
        return "";
    }

    static String buildScheduleDateText(Schedule schedule, MainController controller) {
        if (controller == null) {
            return buildScheduleDateText(schedule);
        }
        if (schedule == null) {
            return "";
        }

        if (schedule.getStartAt() != null && schedule.getDueAt() != null) {
            String start = controller.format("format.list.dateTime", schedule.getStartAt());
            if (schedule.getStartAt().toLocalDate().equals(schedule.getDueAt().toLocalDate())) {
                return start + " - " + controller.format("format.list.time", schedule.getDueAt());
            }
            return start + " - " + controller.format("format.list.dateTime", schedule.getDueAt());
        }
        if (schedule.getDueAt() != null) {
            return controller.text("time.due.summary", controller.format("format.list.dateTime", schedule.getDueAt()));
        }
        if (schedule.getStartAt() != null) {
            return controller.text("time.start.summary", controller.format("format.list.dateTime", schedule.getStartAt()));
        }
        return "";
    }

    private ListCell<String> createFilterButtonCell() {
        return new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : filterLabel(item));
                setTooltip(null);
            }
        };
    }

    private ListCell<String> createFilterDropdownCell() {
        return new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setTooltip(null);
                    return;
                }
                setText(filterLabel(item));
                if (FILTER_UPCOMING.equals(item)) {
                    setTooltip(new Tooltip(controller.text("schedule.list.upcoming.tooltip")));
                } else {
                    setTooltip(null);
                }
            }
        };
    }

    private String filterLabel(String filterId) {
        return switch (filterId) {
            case FILTER_MY_DAY -> controller.text("schedule.filter.myDay");
            case FILTER_OVERDUE -> controller.text("schedule.filter.overdue");
            case FILTER_ALL -> controller.text("schedule.filter.all");
            case FILTER_HIGH_PRIORITY -> controller.text("schedule.filter.highPriority");
            case FILTER_UPCOMING -> controller.text("schedule.filter.upcoming");
            default -> filterId;
        };
    }
}
