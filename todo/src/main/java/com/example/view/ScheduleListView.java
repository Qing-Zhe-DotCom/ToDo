package com.example.view;

import java.text.MessageFormat;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.stream.Collectors;

import com.example.application.IconKey;
import com.example.application.ScheduleOccurrenceProjector;
import com.example.controller.MainController;
import com.example.controller.ScheduleCompletionCoordinator;
import com.example.controller.ScheduleCompletionMutation;
import com.example.model.Schedule;

import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
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
import javafx.scene.image.WritableImage;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

public class ScheduleListView implements View, ScheduleCompletionParticipant {
    private static final double COMPLETED_CARD_OPACITY = 0.7;
    private static final int SEARCH_LOOKBACK_DAYS = 30;
    private static final int SEARCH_LOOKAHEAD_DAYS = 90;
    private static final int UPCOMING_LOOKAHEAD_DAYS = 30;
    private static final int OVERDUE_LOOKBACK_DAYS = 90;
    private static final Duration QUICK_ADD_FEEDBACK_FADE_IN_DURATION = Duration.millis(140);
    private static final Duration QUICK_ADD_FEEDBACK_HOLD_DURATION = Duration.seconds(2.2);
    private static final Duration QUICK_ADD_FEEDBACK_FADE_OUT_DURATION = Duration.millis(180);
    private static final Duration EXPANDED_SETTLE_DURATION = Duration.millis(160);
    private static final Duration EXPANDED_MORPH_DURATION = Duration.millis(120);
    private static final Duration EXPANDED_REVEAL_DURATION = Duration.millis(90);
    private static final Duration COLLAPSED_SETTLE_DURATION = Duration.millis(150);
    private static final Duration COLLAPSED_MORPH_DURATION = Duration.millis(110);
    private static final Duration COLLAPSED_ABSORB_DURATION = Duration.millis(120);
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
    private StackPane quickAddBadge;
    private Label quickAddFeedback;
    private FadeTransition quickAddFeedbackFadeIn;
    private FadeTransition quickAddFeedbackFadeOut;
    private PauseTransition quickAddFeedbackHold;
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

    enum LandingTargetKind {
        EXPANDED_CARD,
        COLLAPSED_HEADER,
        FALLBACK
    }

    record LandingTargetDecision(LandingTargetKind kind) {
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
            titleLabel.setMinWidth(0);
            LabeledTextAutoFit.install(titleLabel, LabeledTextAutoFit.cardTitleSpec());
            dateLabel = new Label();
            dateLabel.getStyleClass().addAll("schedule-date", "schedule-card-subtitle-text");
            dateLabel.setMinWidth(0);
            LabeledTextAutoFit.install(dateLabel, LabeledTextAutoFit.bodyTextSpec());
            categoryLabel = new Label();
            categoryLabel.getStyleClass().add("category-tag");
            categoryLabel.setMinWidth(0);
            LabeledTextAutoFit.install(categoryLabel, LabeledTextAutoFit.bodyTextSpec());

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

        private StackPane getMotionHost() {
            return cardMotionHost;
        }

        private Node getSurfaceNode() {
            return cardShell;
        }

        private void restoreMotionSteadyState() {
            motionHandle.restoreSteadyState();
            ScheduleLandingTransition.finishTargetHandoff(cardShell);
        }

        private void bindSchedule(Schedule schedule) {
            this.schedule = schedule;
            restoreMotionSteadyState();
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
            boolean usingRelativeDueText = !schedule.isCompleted()
                && schedule.getDueAt() != null;
            if (!dateText.isEmpty() && schedule.isOverdue() && !schedule.isCompleted() && !usingRelativeDueText) {
                dateText += " (" + controller.text("status.overdue") + ")";
            }
            dateLabel.setText(dateText);
            boolean showDate = dateText != null && !dateText.isBlank();
            dateLabel.setVisible(showDate);
            dateLabel.setManaged(showDate);
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
            restoreMotionSteadyState();
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
        LabeledTextAutoFit.install(titleLabel, LabeledTextAutoFit.titleSpec());

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
        Label clockLabel = controller.createHeaderClockLabel();
        toolbar.getChildren().addAll(titleLabel, filterComboBox, spacer, clockLabel);
        return toolbar;
    }

    private VBox buildQuickAddBar() {
        Label quickAddTitle = new Label(controller.text("schedule.list.quickAdd"));
        quickAddTitle.getStyleClass().add("quick-add-title");

        Node quickAddIcon = controller.createSvgIcon(IconKey.PLUS, null, 20);
        quickAddIcon.setMouseTransparent(true);
        quickAddBadge = new StackPane(quickAddIcon);
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

        quickAddFeedback = createQuickAddFeedbackLabel();
        initializeQuickAddFeedbackTransitions();

        HBox quickAddBar = new HBox(12, quickAddBadge, quickAddField);
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

        VBox quickAddSection = new VBox(8, quickAddTitle, quickAddFeedback, quickAddBar);
        quickAddSection.setPadding(new Insets(14, 0, 0, 0));
        quickAddSection.getStyleClass().add("quick-add-section");
        return quickAddSection;
    }

    private Label createQuickAddFeedbackLabel() {
        Label feedbackLabel = new Label();
        feedbackLabel.getStyleClass().add("quick-add-feedback");
        feedbackLabel.setVisible(false);
        feedbackLabel.setOpacity(0);
        feedbackLabel.setMouseTransparent(true);
        feedbackLabel.setFocusTraversable(false);
        feedbackLabel.setWrapText(true);
        feedbackLabel.managedProperty().bind(feedbackLabel.visibleProperty());
        return feedbackLabel;
    }

    private void initializeQuickAddFeedbackTransitions() {
        if (quickAddFeedback == null) {
            return;
        }

        quickAddFeedbackFadeIn = new FadeTransition(QUICK_ADD_FEEDBACK_FADE_IN_DURATION, quickAddFeedback);
        quickAddFeedbackFadeIn.setFromValue(0);
        quickAddFeedbackFadeIn.setToValue(1);

        quickAddFeedbackHold = new PauseTransition(QUICK_ADD_FEEDBACK_HOLD_DURATION);
        quickAddFeedbackHold.setOnFinished(event -> {
            if (quickAddFeedback != null && quickAddFeedback.isVisible()) {
                quickAddFeedbackFadeOut.playFromStart();
            }
        });

        quickAddFeedbackFadeOut = new FadeTransition(QUICK_ADD_FEEDBACK_FADE_OUT_DURATION, quickAddFeedback);
        quickAddFeedbackFadeOut.setFromValue(1);
        quickAddFeedbackFadeOut.setToValue(0);
        quickAddFeedbackFadeOut.setOnFinished(event -> hideQuickAddFeedback());
    }

    @Override
    public void refreshIcons() {
        if (quickAddBadge != null) {
            Node quickAddIcon = controller.createSvgIcon(IconKey.PLUS, null, 20);
            quickAddIcon.setMouseTransparent(true);
            quickAddBadge.getChildren().setAll(quickAddIcon);
        }
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
            Schedule createdSchedule = controller.quickCreateSchedule(title);
            quickAddField.clear();
            focusQuickAddInput();
            showQuickAddFeedback(buildQuickAddSuccessText(
                controller.text("schedule.list.quickAddSuccess"),
                createdSchedule != null ? createdSchedule.getName() : title
            ));
        } catch (SQLException exception) {
            stopQuickAddFeedbackTransitions();
            hideQuickAddFeedback();
            controller.showError(controller.text("error.createSchedule.title"), exception.getMessage());
        }
    }

    private void showQuickAddFeedback(String message) {
        if (quickAddFeedback == null || message == null || message.isBlank()) {
            return;
        }

        stopQuickAddFeedbackTransitions();
        quickAddFeedback.setText(message);
        quickAddFeedback.setAccessibleText(message);
        quickAddFeedback.setVisible(true);
        quickAddFeedback.setOpacity(0);
        quickAddFeedbackFadeIn.playFromStart();
        quickAddFeedbackHold.playFromStart();
    }

    private void hideQuickAddFeedback() {
        if (quickAddFeedback == null) {
            return;
        }

        quickAddFeedback.setVisible(false);
        quickAddFeedback.setOpacity(0);
        quickAddFeedback.setText("");
        quickAddFeedback.setAccessibleText("");
    }

    private void stopQuickAddFeedbackTransitions() {
        if (quickAddFeedbackFadeIn != null) {
            quickAddFeedbackFadeIn.stop();
        }
        if (quickAddFeedbackHold != null) {
            quickAddFeedbackHold.stop();
        }
        if (quickAddFeedbackFadeOut != null) {
            quickAddFeedbackFadeOut.stop();
        }
    }

    static String buildQuickAddSuccessText(String template, String title) {
        String resolvedTemplate = (template == null || template.isBlank()) ? "{0}" : template;
        String resolvedTitle = title == null ? "" : title.strip();
        return MessageFormat.format(resolvedTemplate, resolvedTitle);
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

        ScheduleCompletionCoordinator.PendingCompletion pendingCompletion =
            controller.prepareScheduleCompletion(schedule, targetCompleted);
        if (pendingCompletion == null) {
            return false;
        }

        String cardKey = resolveCardKey(schedule);
        Map<String, Bounds> beforeBounds =
            ScheduleReflowAnimator.captureVisibleCardBounds(listContent, cardKey);

        BooleanSupplier commitAction = () -> {
            ScheduleCardNode sourceNode = cardNodesById.remove(cardKey);
            boolean success = pendingCompletion.commit();
            if (!success && sourceNode != null) {
                cardNodesById.put(cardKey, sourceNode);
            }
            return success;
        };
        Runnable onFailure = () -> {
            pendingCompletion.cancel();
            cardNode.syncAfterFailedCommit();
        };

        if (!canUseStagedTransfer(cardNode)) {
            ScheduleCollapsePopAnimator.playCollapseSource(
                cardNode.getMotionHandle(),
                commitAction,
                () -> handleCommittedListFallback(cardKey, targetCompleted, beforeBounds),
                onFailure,
                null
            );
            return true;
        }

        Point2D stagingPoint = resolveStagingScenePoint(cardNode.getMotionHost());
        if (stagingPoint == null) {
            ScheduleCollapsePopAnimator.playCollapseSource(
                cardNode.getMotionHandle(),
                commitAction,
                () -> handleCommittedListFallback(cardKey, targetCompleted, beforeBounds),
                onFailure,
                null
            );
            return true;
        }

        Node sourceSurface = cardNode.getSurfaceNode();
        ScheduleCardMotionSupport.playDropToStaging(
            cardNode.getMotionHost(),
            stagingPoint,
            new ScheduleCardMotionSupport.StagedArchiveAnimationListener() {
                @Override
                public void onReadyToSettle(ScheduleCardMotionSupport.StagedLanding landing) {
                    handleCommittedListTransfer(cardKey, targetCompleted, beforeBounds, landing, sourceSurface);
                }
            },
            commitAction,
            onFailure
        );
        return true;
    }

    private void handleCommittedListFallback(String scheduleId, boolean targetCompleted, Map<String, Bounds> beforeBounds) {
        ScheduleCollapsePopAnimator.MotionHandle targetHandle = resolveExpandedTargetHandle(scheduleId, targetCompleted);
        if (targetHandle != null) {
            ScheduleCollapsePopAnimator.prepareTargetPopState(targetHandle);
        }

        ScheduleReflowAnimator.playVerticalReflow(listContent, beforeBounds, null);

        if (targetHandle != null) {
            ScheduleCollapsePopAnimator.playPreparedTargetPop(targetHandle, null);
            return;
        }

        Node headerNode = resolveTargetHeaderNode(targetCompleted);
        if (headerNode != null && headerNode.isVisible()) {
            ScheduleReflowAnimator.playCollapsedReceive(
                headerNode,
                CollapsePopKeyframePreset.targetPopDuration(),
                null
            );
        }
    }

    private ScheduleCollapsePopAnimator.MotionHandle resolveExpandedTargetHandle(String scheduleId, boolean targetCompleted) {
        if ((targetCompleted && completedCollapsed) || (!targetCompleted && pendingCollapsed)) {
            return null;
        }
        ScheduleCardNode targetNode = resolveTargetCardNode(scheduleId, targetCompleted);
        if (targetNode == null) {
            return null;
        }
        return targetNode.getMotionHandle();
    }

    private void handleCommittedListTransfer(
        String scheduleId,
        boolean targetCompleted,
        Map<String, Bounds> beforeBounds,
        ScheduleCardMotionSupport.StagedLanding landing,
        Node sourceSurface
    ) {
        ScheduleCardNode targetNode = resolveTargetCardNode(scheduleId, targetCompleted);
        Node headerNode = resolveTargetHeaderNode(targetCompleted);
        LandingTargetDecision decision = resolveLandingTargetDecision(
            targetCompleted ? completedCollapsed : pendingCollapsed,
            targetNode != null,
            headerNode != null && headerNode.isVisible()
        );

        if (decision.kind() == LandingTargetKind.EXPANDED_CARD && targetNode != null) {
            Node targetSurface = targetNode.getSurfaceNode();
            ScheduleLandingTransition.prepareTargetNodeForHandoff(targetSurface, 0.18);
            ScheduleReflowAnimator.playVerticalReflow(
                listContent,
                beforeBounds,
                null,
                null,
                null,
                () -> ScheduleLandingTransition.handoffToExpandedTarget(
                    landing,
                    targetSurface,
                    EXPANDED_SETTLE_DURATION,
                    EXPANDED_MORPH_DURATION,
                    EXPANDED_REVEAL_DURATION,
                    null
                )
            );
            return;
        }

        if (decision.kind() == LandingTargetKind.COLLAPSED_HEADER && headerNode != null) {
            WritableImage previewSnapshot = resolveCollapsedPreviewSnapshot(targetNode, sourceSurface, targetCompleted);
            Point2D headerCenter = resolveNodeSceneCenter(headerNode);
            ScheduleReflowAnimator.playVerticalReflow(
                listContent,
                beforeBounds,
                null,
                null,
                null,
                () -> {
                    if (previewSnapshot == null || headerCenter == null) {
                        landing.finishSuccess();
                        return;
                    }
                    ScheduleLandingTransition.morphIntoCollapsedEntry(
                        landing,
                        previewSnapshot,
                        headerCenter,
                        COLLAPSED_SETTLE_DURATION,
                        COLLAPSED_MORPH_DURATION,
                        COLLAPSED_ABSORB_DURATION,
                        () -> ScheduleReflowAnimator.playCollapsedReceive(headerNode, COLLAPSED_ABSORB_DURATION, null)
                    );
                }
            );
            return;
        }

        ScheduleReflowAnimator.playVerticalReflow(listContent, beforeBounds, null, null, null, landing::finishSuccess);
    }

    private ScheduleCardNode resolveTargetCardNode(String scheduleId, boolean targetCompleted) {
        ScheduleCardNode targetNode = cardNodesById.get(scheduleId);
        if (targetNode == null || targetNode.getSchedule() == null) {
            return null;
        }
        if (targetNode.getSchedule().isCompleted() != targetCompleted) {
            return null;
        }
        VBox targetBox = targetCompleted ? completedCardsBox : pendingCardsBox;
        if (targetBox == null || !targetBox.getChildren().contains(targetNode.getContainer())) {
            return null;
        }
        return targetNode;
    }

    private Node resolveTargetHeaderNode(boolean targetCompleted) {
        return targetCompleted ? completedHeader.getRoot() : pendingHeader.getRoot();
    }

    private boolean canUseStagedTransfer(ScheduleCardNode cardNode) {
        return cardNode != null
            && cardNode.getMotionHost() != null
            && cardNode.getMotionHost().getScene() != null
            && cardNode.getMotionHost().getScene().getRoot() instanceof javafx.scene.layout.Pane;
    }

    private Point2D resolveStagingScenePoint(Node node) {
        if (node == null) {
            return null;
        }
        Bounds sceneBounds = node.localToScene(node.getBoundsInLocal());
        if (sceneBounds == null) {
            return null;
        }
        return new Point2D(sceneBounds.getCenterX(), sceneBounds.getCenterY() + Math.max(24.0, sceneBounds.getHeight() * 0.7));
    }

    private Point2D resolveNodeSceneCenter(Node node) {
        if (node == null) {
            return null;
        }
        Bounds sceneBounds = node.localToScene(node.getBoundsInLocal());
        if (sceneBounds == null) {
            return null;
        }
        return new Point2D(sceneBounds.getCenterX(), sceneBounds.getCenterY());
    }

    private WritableImage resolveCollapsedPreviewSnapshot(
        ScheduleCardNode targetNode,
        Node sourceSurface,
        boolean targetCompleted
    ) {
        if (targetNode != null) {
            WritableImage snapshot = ScheduleLandingTransition.snapshotNode(targetNode.getSurfaceNode());
            if (snapshot != null) {
                return snapshot;
            }
        }
        if (sourceSurface == null) {
            return null;
        }
        if (targetCompleted) {
            return ScheduleLandingTransition.snapshotNodeWithTemporaryClass(
                sourceSurface,
                ScheduleLandingTransition.COMPLETED_PREVIEW_CLASS
            );
        }
        return ScheduleLandingTransition.snapshotNode(sourceSurface);
    }

    private String resolveCardKey(Schedule schedule) {
        return schedule.getViewKey() != null && !schedule.getViewKey().isBlank()
            ? schedule.getViewKey()
            : schedule.getId();
    }

    static LandingTargetDecision resolveLandingTargetDecision(
        boolean targetGroupCollapsed,
        boolean targetCardPresent,
        boolean headerVisible
    ) {
        if (!targetGroupCollapsed && targetCardPresent) {
            return new LandingTargetDecision(LandingTargetKind.EXPANDED_CARD);
        }
        if (targetGroupCollapsed && headerVisible) {
            return new LandingTargetDecision(LandingTargetKind.COLLAPSED_HEADER);
        }
        return new LandingTargetDecision(LandingTargetKind.FALLBACK);
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
        if (schedule.isCompleted()) {
            return "";
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startAt = schedule.getStartAt();
        LocalDateTime dueAt = schedule.getDueAt();

        if (dueAt != null && dueAt.isBefore(now)) {
            return buildDueRelativeText(now, dueAt, controller);
        }
        if (startAt != null && startAt.isAfter(now)) {
            return buildStartRelativeText(now, startAt, controller);
        }
        if (dueAt != null) {
            return buildDueRelativeText(now, dueAt, controller);
        }
        return "";
    }

    static String buildDueRelativeText(LocalDateTime now, LocalDateTime dueAt, MainController controller) {
        if (dueAt == null) {
            return "";
        }
        LocalDateTime reference = now != null ? now : LocalDateTime.now();

        long minutesDelta = ChronoUnit.MINUTES.between(reference, dueAt);
        boolean future = minutesDelta >= 0;

        long absDays = Math.abs(ChronoUnit.DAYS.between(reference.toLocalDate(), dueAt.toLocalDate()));
        if (absDays > 30) {
            return controller != null
                ? controller.text(future
                    ? "time.due.relative.future.moreThanMonth"
                    : "time.due.relative.past.moreThanMonth")
                : (future ? "ends in >1 mo" : "ended >1 mo ago");
        }

        long absMinutes = Math.abs(minutesDelta);
        if (absMinutes >= 24 * 60) {
            long days = absMinutes / (24 * 60);
            return controller != null
                ? controller.text(future ? "time.due.relative.future.days" : "time.due.relative.past.days", days)
                : (future ? "ends in " + days + "d" : "ended " + days + "d ago");
        }
        if (absMinutes >= 12 * 60) {
            long hours = absMinutes / 60;
            return controller != null
                ? controller.text(future ? "time.due.relative.future.hours" : "time.due.relative.past.hours", hours)
                : (future ? "ends in " + hours + "h" : "ended " + hours + "h ago");
        }

        long hours = absMinutes / 60;
        long minutes = absMinutes % 60;
        return controller != null
            ? controller.text(
                future ? "time.due.relative.future.hoursMinutes" : "time.due.relative.past.hoursMinutes",
                hours,
                minutes
            )
            : (future
                ? "ends in " + hours + "h " + minutes + "m"
                : "ended " + hours + "h " + minutes + "m ago");
    }

    static String buildStartRelativeText(LocalDateTime now, LocalDateTime startAt, MainController controller) {
        if (startAt == null) {
            return "";
        }
        LocalDateTime reference = now != null ? now : LocalDateTime.now();
        if (!startAt.isAfter(reference)) {
            return "";
        }

        long minutesDelta = ChronoUnit.MINUTES.between(reference, startAt);
        if (minutesDelta <= 0) {
            return "";
        }

        long absDays = Math.abs(ChronoUnit.DAYS.between(reference.toLocalDate(), startAt.toLocalDate()));
        if (absDays > 30) {
            return controller != null
                ? controller.text("time.start.relative.future.moreThanMonth")
                : "starts in >1 mo";
        }

        long absMinutes = Math.abs(minutesDelta);
        if (absMinutes >= 24 * 60) {
            long days = absMinutes / (24 * 60);
            return controller != null
                ? controller.text("time.start.relative.future.days", days)
                : "starts in " + days + "d";
        }
        if (absMinutes >= 12 * 60) {
            long hours = absMinutes / 60;
            return controller != null
                ? controller.text("time.start.relative.future.hours", hours)
                : "starts in " + hours + "h";
        }

        long hours = absMinutes / 60;
        long minutes = absMinutes % 60;
        return controller != null
            ? controller.text("time.start.relative.future.hoursMinutes", hours, minutes)
            : "starts in " + hours + "h " + minutes + "m";
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
