package com.example.view;

import com.example.application.IconKey;
import com.example.application.CustomOptionsService;
import com.example.controller.MainController;
import com.example.controller.ScheduleCompletionMutation;
import com.example.model.RecurrenceRule;
import com.example.model.Reminder;
import com.example.model.Schedule;
import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.PauseTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ComboBoxBase;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.MenuItem;
import javafx.scene.control.OverrunStyle;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.sql.SQLException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class InfoPanelView implements ScheduleCompletionParticipant {
    private static final double TIME_TOGGLE_SLOT_WIDTH = 96.0;
    private static final double TIME_TRIGGER_WIDTH = 170.0;
    private static final double TIME_TRIGGER_HEIGHT = 54.0;
    private static final String TITLE_PROMPT = "输入日程标题";
    private static final String EMPTY_TITLE_TEXT = "请选择日程";
    private static final String EMPTY_TIME_TEXT = "未设置时间";
    private static final String UNSET_TRIGGER_TEXT = "未设置";
    private static final List<String> STATUS_CLASSES = List.of(
        "info-panel-status-completed",
        "info-panel-status-overdue",
        "info-panel-status-upcoming",
        "info-panel-status-ongoing"
    );
    private static final DateTimeFormatter SUMMARY_FORMATTER = DateTimeFormatter.ofPattern("M月d日 HH:mm");
    private static final DateTimeFormatter YEAR_FORMATTER = DateTimeFormatter.ofPattern("yyyy年");

    private final MainController controller;
    private final IosWheelDateTimePopup wheelPopup;
    private final PauseTransition titleDelay = new PauseTransition(Duration.millis(400));
    private final PauseTransition categoryDelay = new PauseTransition(Duration.millis(400));
    private final PauseTransition tagsDelay = new PauseTransition(Duration.millis(400));
    private final PauseTransition notesDelay = new PauseTransition(Duration.millis(400));

    private VBox root;
    private ScrollPane scrollPane;
    private Schedule currentSchedule;
    private Schedule persistedSchedule;
    private ParallelTransition panelTransition;
    private boolean panelVisible;
    private boolean suspend;

    private ScheduleStatusControl completeControl;
    private Button closeButton;
    private Button deleteButton;
    private Button postponeButton;
    private Button pinButton;
    private Button unpinButton;
    private Label statusLabel;
    private FlowPane chipPane;
    private TextField titleField;
    private CheckBox allDayToggle;
    private CheckBox dueToggle;
    private Button dueTrigger;
    private Label dueTriggerTitle;
    private Label dueTriggerSubtitle;
    private CheckBox startToggle;
    private Button startTrigger;
    private Label startTriggerTitle;
    private Label startTriggerSubtitle;
    private CheckBox reminderToggle;
    private ComboBox<String> priorityBox;
    private TextField categoryField;
    private TextField tagsField;
    private ContextMenu categorySuggestionMenu;
    private ContextMenu tagsSuggestionMenu;
    private boolean categorySuggestionQueryEnabled;
    private TextArea notesArea;
    private FlowPane tagEditorChipPane;
    private VBox reminderListBox;
    private Button addReminderButton;
    private Label recurrenceSummaryLabel;
    private Button recurrenceEditButton;
    private Button recurrenceClearButton;
    private VBox priorityEditor;
    private VBox categoryEditor;
    private VBox tagsEditor;
    private VBox notesEditor;

    public InfoPanelView(MainController controller) {
        this.controller = controller;
        this.wheelPopup = new IosWheelDateTimePopup(controller);
        buildUi();
        wireListeners();
        applyEmptyState();
    }

    public Node getView() {
        return root;
    }

    public void focusTitleEditor() {
        if (titleField == null || currentSchedule == null) {
            return;
        }
        Platform.runLater(() -> {
            titleField.requestFocus();
            titleField.positionCaret(titleField.getText().length());
        });
    }

    public void showWithAnimation() {
        root.setManaged(true);
        root.setVisible(true);
        stopTransition();
        FadeTransition fade = new FadeTransition(Duration.millis(300), root);
        fade.setFromValue(root.getOpacity());
        fade.setToValue(1);
        TranslateTransition slide = new TranslateTransition(Duration.millis(300), root);
        slide.setFromX(root.getTranslateX());
        slide.setToX(0);
        panelTransition = new ParallelTransition(fade, slide);
        panelTransition.setOnFinished(event -> panelVisible = true);
        panelTransition.play();
    }

    public void hideWithAnimation() {
        if (!panelVisible && !root.isVisible()) {
            return;
        }
        closeWheelPopup();
        stopTransition();
        FadeTransition fade = new FadeTransition(Duration.millis(300), root);
        fade.setFromValue(root.getOpacity());
        fade.setToValue(0);
        TranslateTransition slide = new TranslateTransition(Duration.millis(300), root);
        slide.setFromX(root.getTranslateX());
        slide.setToX(24);
        panelTransition = new ParallelTransition(fade, slide);
        panelTransition.setOnFinished(event -> hideImmediately());
        panelTransition.play();
    }

    public void hideImmediately() {
        closeWheelPopup();
        stopTransition();
        panelVisible = false;
        root.setOpacity(0);
        root.setTranslateX(24);
        root.setVisible(false);
        root.setManaged(false);
    }

    public boolean isPanelVisible() {
        return panelVisible;
    }

    public void setSchedule(Schedule schedule) {
        closeWheelPopup();
        if (schedule == null) {
            currentSchedule = null;
            persistedSchedule = null;
            applyEmptyState();
            return;
        }
        currentSchedule = copyOf(schedule);
        persistedSchedule = copyOf(schedule);
        renderForm();
    }

    public void refresh() {
        closeWheelPopup();
        if (currentSchedule == null) {
            return;
        }
        try {
            Schedule updated = controller.findScheduleById(currentSchedule.getId());
            if (updated == null) {
                currentSchedule = null;
                persistedSchedule = null;
                applyEmptyState();
                return;
            }
            currentSchedule = copyOf(updated);
            persistedSchedule = copyOf(updated);
            renderForm();
        } catch (SQLException exception) {
            controller.showError(text("error.refresh.title"), exception.getMessage());
        }
    }

    @Override
    public void applyCompletionMutation(ScheduleCompletionMutation mutation) {
        if (currentSchedule == null || mutation == null || !mutation.matches(currentSchedule)) {
            return;
        }
        mutation.applyTo(currentSchedule);
        updateDerivedState();
    }

    @Override
    public void confirmCompletionMutation(ScheduleCompletionMutation mutation) {
        if (currentSchedule == null || persistedSchedule == null || mutation == null || !mutation.matches(currentSchedule)) {
            return;
        }
        mutation.applyTo(persistedSchedule);
        updateDerivedState();
    }

    @Override
    public void revertCompletionMutation(ScheduleCompletionMutation mutation) {
        if (currentSchedule == null || mutation == null || !mutation.matches(currentSchedule)) {
            return;
        }
        mutation.revertOn(currentSchedule);
        updateDerivedState();
    }

    static DatePresentation buildDatePresentation(LocalDateTime startAt, LocalDateTime dueAt) {
        LocalDateTime start = startAt != null ? startAt : dueAt;
        LocalDateTime end = dueAt != null ? dueAt : startAt;
        if (start == null || end == null) {
            return new DatePresentation(EMPTY_TIME_TEXT, "");
        }
        if (start.isAfter(end)) {
            LocalDateTime temp = start;
            start = end;
            end = temp;
        }
        if (startAt == null && dueAt != null) {
            return new DatePresentation("截止 " + SUMMARY_FORMATTER.format(end), YEAR_FORMATTER.format(end));
        }
        if (startAt != null && dueAt == null) {
            return new DatePresentation("开始于 " + SUMMARY_FORMATTER.format(start), YEAR_FORMATTER.format(start));
        }
        String primary = SUMMARY_FORMATTER.format(start) + " - " + SUMMARY_FORMATTER.format(end);
        String secondary = start.getYear() == end.getYear()
            ? YEAR_FORMATTER.format(start)
            : YEAR_FORMATTER.format(start) + " - " + YEAR_FORMATTER.format(end);
        return new DatePresentation(primary, secondary);
    }

    static TimeTriggerPresentation buildTimeTriggerPresentation(LocalDateTime value) {
        if (value == null) {
            return new TimeTriggerPresentation(UNSET_TRIGGER_TEXT, "", true);
        }
        return new TimeTriggerPresentation(SUMMARY_FORMATTER.format(value), YEAR_FORMATTER.format(value), false);
    }

    static DatePresentation buildDatePresentation(LocalDateTime startAt, LocalDateTime dueAt, boolean allDay) {
        if (!allDay) {
            return buildDatePresentation(startAt, dueAt);
        }
        LocalDateTime start = startAt != null ? startAt : dueAt;
        LocalDateTime end = dueAt != null ? dueAt : startAt;
        if (start == null || end == null) {
            return new DatePresentation(EMPTY_TIME_TEXT, "");
        }
        if (start.isAfter(end)) {
            LocalDateTime temp = start;
            start = end;
            end = temp;
        }
        if (startAt == null && dueAt != null) {
            return new DatePresentation("截止 " + formatDaySummary(end), YEAR_FORMATTER.format(end));
        }
        if (startAt != null && dueAt == null) {
            return new DatePresentation("开始于 " + formatDaySummary(start), YEAR_FORMATTER.format(start));
        }
        String primary = formatDaySummary(start) + " - " + formatDaySummary(end);
        String secondary = start.getYear() == end.getYear()
            ? YEAR_FORMATTER.format(start)
            : YEAR_FORMATTER.format(start) + " - " + YEAR_FORMATTER.format(end);
        return new DatePresentation(primary, secondary);
    }

    static TimeTriggerPresentation buildTimeTriggerPresentation(LocalDateTime value, boolean allDay) {
        if (!allDay) {
            return buildTimeTriggerPresentation(value);
        }
        if (value == null) {
            return new TimeTriggerPresentation(UNSET_TRIGGER_TEXT, "", true);
        }
        return new TimeTriggerPresentation(formatDaySummary(value), YEAR_FORMATTER.format(value), false);
    }

    private static String formatDaySummary(LocalDateTime value) {
        return DateTimeFormatter.ofPattern("M月d日").format(value);
    }

    static DatePresentation buildDatePresentation(MainController controller, LocalDateTime startAt, LocalDateTime dueAt) {
        if (controller == null) {
            return buildDatePresentation(startAt, dueAt);
        }
        LocalDateTime start = startAt != null ? startAt : dueAt;
        LocalDateTime end = dueAt != null ? dueAt : startAt;
        if (start == null || end == null) {
            return new DatePresentation(controller.text("time.unset"), "");
        }
        if (start.isAfter(end)) {
            LocalDateTime temp = start;
            start = end;
            end = temp;
        }
        if (startAt == null && dueAt != null) {
            return new DatePresentation(
                controller.text("time.due.summary", controller.format("format.info.dateTimeSummary", end)),
                controller.format("format.info.yearSummary", end)
            );
        }
        if (startAt != null && dueAt == null) {
            return new DatePresentation(
                controller.text("time.start.summary", controller.format("format.info.dateTimeSummary", start)),
                controller.format("format.info.yearSummary", start)
            );
        }
        String primary = controller.format("format.info.dateTimeSummary", start) + " - " + controller.format("format.info.dateTimeSummary", end);
        String secondary = start.getYear() == end.getYear()
            ? controller.format("format.info.yearSummary", start)
            : controller.format("format.info.yearSummary", start) + " - " + controller.format("format.info.yearSummary", end);
        return new DatePresentation(primary, secondary);
    }

    static DatePresentation buildDatePresentation(MainController controller, LocalDateTime startAt, LocalDateTime dueAt, boolean allDay) {
        if (!allDay) {
            return buildDatePresentation(controller, startAt, dueAt);
        }
        if (controller == null) {
            return buildDatePresentation(startAt, dueAt, true);
        }
        LocalDateTime start = startAt != null ? startAt : dueAt;
        LocalDateTime end = dueAt != null ? dueAt : startAt;
        if (start == null || end == null) {
            return new DatePresentation(controller.text("time.unset"), "");
        }
        if (start.isAfter(end)) {
            LocalDateTime temp = start;
            start = end;
            end = temp;
        }
        if (startAt == null && dueAt != null) {
            return new DatePresentation(
                controller.text("time.due.summary", controller.format("format.info.daySummary", end)),
                controller.format("format.info.yearSummary", end)
            );
        }
        if (startAt != null && dueAt == null) {
            return new DatePresentation(
                controller.text("time.start.summary", controller.format("format.info.daySummary", start)),
                controller.format("format.info.yearSummary", start)
            );
        }
        String primary = controller.format("format.info.daySummary", start) + " - " + controller.format("format.info.daySummary", end);
        String secondary = start.getYear() == end.getYear()
            ? controller.format("format.info.yearSummary", start)
            : controller.format("format.info.yearSummary", start) + " - " + controller.format("format.info.yearSummary", end);
        return new DatePresentation(primary, secondary);
    }

    static TimeTriggerPresentation buildTimeTriggerPresentation(MainController controller, LocalDateTime value) {
        if (controller == null) {
            return buildTimeTriggerPresentation(value);
        }
        if (value == null) {
            return new TimeTriggerPresentation(controller.text("common.unset"), "", true);
        }
        return new TimeTriggerPresentation(
            controller.format("format.info.dateTimeSummary", value),
            controller.format("format.info.yearSummary", value),
            false
        );
    }

    static TimeTriggerPresentation buildTimeTriggerPresentation(MainController controller, LocalDateTime value, boolean allDay) {
        if (controller == null) {
            if (value == null) {
                return new TimeTriggerPresentation(UNSET_TRIGGER_TEXT, "", true);
            }
            String primary = allDay ? formatDaySummary(value) : SUMMARY_FORMATTER.format(value);
            String secondary = String.format("%02d", Math.floorMod(value.getYear(), 100));
            return new TimeTriggerPresentation(primary, secondary, false);
        }
        if (value == null) {
            return new TimeTriggerPresentation(controller.text("common.unset"), "", true);
        }
        return new TimeTriggerPresentation(
            controller.format(allDay ? "format.info.daySummary" : "format.info.dateTimeSummary", value),
            controller.format("format.info.shortYearSummary", value),
            false
        );
    }

    static LocalDateTime defaultDueValue(LocalDate date) {
        return date.atTime(23, 59);
    }

    static LocalDateTime defaultStartValue(LocalDate date) {
        return date.atTime(9, 0);
    }

    static LocalDateTime defaultStartSeed(LocalDateTime createdAt, boolean allDay) {
        LocalDateTime seed = createdAt != null
            ? createdAt.truncatedTo(ChronoUnit.MINUTES)
            : defaultStartValue(LocalDate.now());
        return allDay ? seed.toLocalDate().atStartOfDay() : seed;
    }

    static LocalDateTime defaultReminderValue(LocalDate date, LocalDateTime dueAt) {
        return dueAt != null ? dueAt : defaultStartValue(date);
    }

    static List<String> splitTagChips(String rawTags) {
        if (rawTags == null || rawTags.isBlank()) {
            return Collections.emptyList();
        }
        return Schedule.splitTags(rawTags);
    }

    static boolean shouldShowCategoryChip(String category) {
        return category != null && !category.isBlank() && !Schedule.isDefaultCategory(category);
    }

    private void buildUi() {
        root = new VBox();
        root.getStyleClass().add("info-panel");
        root.setPrefWidth(360);
        root.setMinWidth(340);
        root.setMaxWidth(420);

        completeControl = new ScheduleStatusControl(
            false,
            ScheduleStatusControl.SizePreset.DETAIL,
            "schedule-status-role-detail",
            targetCompleted -> currentSchedule != null && controller.updateScheduleCompletion(currentSchedule, targetCompleted)
        );
        completeControl.getStyleClass().add("info-panel-complete-control");

        deleteButton = actionIconButton(
            IconKey.DELETE,
            text("info.delete"),
            this::deleteSchedule,
            "info-panel-icon-button-danger",
            "info-panel-delete-button"
        );
        postponeButton = actionIconButton(
            IconKey.POSTPONE_DAY,
            text("action.postponeDay"),
            this::postponeScheduleOneDay
        );
        postponeButton.setVisible(false);
        postponeButton.setManaged(false);
        pinButton = actionIconButton(
            IconKey.PIN,
            text("action.pin"),
            this::pinSchedule
        );
        pinButton.setVisible(false);
        pinButton.setManaged(false);
        unpinButton = actionIconButton(
            IconKey.UNPIN,
            text("action.unpin"),
            this::unpinSchedule
        );
        unpinButton.setVisible(false);
        unpinButton.setManaged(false);
        closeButton = iconButton(IconKey.CLOSE, text("info.close"), controller::closeScheduleDetails);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox actionGroup = new HBox(10, completeControl, postponeButton, pinButton, unpinButton, deleteButton);
        actionGroup.setAlignment(Pos.CENTER_LEFT);
        actionGroup.getStyleClass().add("info-panel-action-group");
        HBox header = new HBox(10, actionGroup, spacer, closeButton);
        header.setAlignment(Pos.CENTER_LEFT);
        header.getStyleClass().addAll("info-panel-header", "info-panel-action-row");

        statusLabel = new Label();
        statusLabel.getStyleClass().add("info-panel-status-pill");

        chipPane = new FlowPane();
        chipPane.setHgap(8);
        chipPane.setVgap(8);
        chipPane.getStyleClass().addAll("info-panel-chip-pane", "info-panel-top-chip-pane");
        chipPane.prefWrapLengthProperty().bind(root.widthProperty().subtract(56));
        chipPane.setVisible(false);
        chipPane.setManaged(false);

        titleField = new TextField();
        titleField.getStyleClass().add("info-panel-title-input");
        titleField.setPromptText(text("info.title.prompt"));

        allDayToggle = rowToggle(text("info.allDay"));

        dueToggle = rowToggle(text("info.due"));
        dueTrigger = timeTrigger();
        dueTriggerTitle = triggerLabel("info-panel-time-trigger-title");
        dueTriggerSubtitle = triggerLabel("info-panel-time-trigger-subtitle");
        dueTriggerSubtitle.getStyleClass().add("info-panel-time-trigger-year");
        attachInlineYearTriggerGraphic(dueTrigger, dueTriggerSubtitle, dueTriggerTitle);

        startToggle = rowToggle(text("info.start"));
        startTrigger = timeTrigger();
        startTriggerTitle = triggerLabel("info-panel-time-trigger-title");
        startTriggerSubtitle = triggerLabel("info-panel-time-trigger-subtitle");
        startTriggerSubtitle.getStyleClass().add("info-panel-time-trigger-year");
        attachInlineYearTriggerGraphic(startTrigger, startTriggerSubtitle, startTriggerTitle);

        reminderToggle = rowToggle(text("info.reminder"));

        priorityBox = new ComboBox<>();
        priorityBox.getItems().setAll(Schedule.PRIORITY_HIGH, Schedule.PRIORITY_MEDIUM, Schedule.PRIORITY_LOW);
        priorityBox.getStyleClass().addAll("info-panel-combo", "info-panel-borderless-combo");
        priorityBox.setMaxWidth(Double.MAX_VALUE);
        priorityBox.setButtonCell(createPriorityCell());
        priorityBox.setCellFactory(listView -> createPriorityCell());

        categoryField = textInput(text("category.default"));
        tagsField = textInput(text("info.tags.prompt"));

        notesArea = new TextArea();
        notesArea.getStyleClass().addAll("info-panel-notes-input", "info-panel-borderless-area");
        notesArea.setPrefRowCount(6);
        notesArea.setWrapText(true);
        tagEditorChipPane = new FlowPane(8, 8);
        tagEditorChipPane.getStyleClass().addAll("info-panel-chip-pane", "info-panel-tag-editor-chip-pane");

        reminderListBox = new VBox(8);
        reminderListBox.getStyleClass().add("info-panel-reminder-list");
        addReminderButton = new Button(text("info.reminder.add"));
        addReminderButton.getStyleClass().addAll("button-secondary", "info-panel-secondary-action");

        recurrenceSummaryLabel = new Label(text("recurrence.none"));
        recurrenceSummaryLabel.getStyleClass().add("info-panel-recurrence-summary");
        recurrenceSummaryLabel.setWrapText(true);
        recurrenceEditButton = new Button(text("recurrence.edit"));
        recurrenceEditButton.getStyleClass().addAll("button-secondary", "info-panel-secondary-action");
        recurrenceClearButton = new Button(text("recurrence.clear"));
        recurrenceClearButton.getStyleClass().addAll("button-secondary", "info-panel-secondary-action");
        notesArea.setPromptText(text("info.notes.prompt"));

        priorityEditor = inlineEditor(priorityBox);
        categoryEditor = inlineEditor(categoryField);
        tagsEditor = inlineEditor(tagsField, tagEditorChipPane);
        notesEditor = inlineEditor(notesArea);

        HBox recurrenceActions = new HBox(8, recurrenceEditButton, recurrenceClearButton);
        recurrenceActions.setAlignment(Pos.CENTER_LEFT);
        VBox recurrenceEditor = new VBox(8, recurrenceSummaryLabel, recurrenceActions);
        recurrenceEditor.getStyleClass().add("info-panel-recurrence-editor");

        VBox reminderEditor = new VBox(8, toggleOnlyRow(reminderToggle), reminderListBox, addReminderButton);
        reminderEditor.getStyleClass().add("info-panel-reminder-editor");

        VBox content = new VBox();
        content.getStyleClass().add("info-panel-content");
        content.getChildren().addAll(
            chipPane,
            titleField,
            section(text("info.section.allDay"), toggleOnlyRow(allDayToggle)),
            section(text("info.section.time"), timeRow(startToggle, startTrigger), timeRow(dueToggle, dueTrigger)),
            section(text("info.section.priority"), priorityEditor),
            section(text("info.section.category"), categoryEditor),
            section(text("info.section.tags"), tagsEditor),
            section(text("info.section.reminder"), reminderEditor),
            section(text("info.section.recurrence"), recurrenceEditor),
            section(text("info.section.notes"), notesEditor)
        );

        scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.getStyleClass().add("info-panel-scroll");
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        root.getChildren().addAll(header, scrollPane);
    }

    private void wireListeners() {
        bindTextField(titleField, titleDelay, this::saveTitle);
        bindTextField(categoryField, categoryDelay, this::saveCategory);
        if (tagsField != null) {
            tagsField.setOnAction(event -> commitTagInput(tagsField.getText()));
            tagsField.addEventFilter(KeyEvent.KEY_TYPED, event -> {
                if (suspend || tagsField.isDisabled() || tagsField.getText() == null) {
                    return;
                }
                if (!isTagCommaSplitEnabled()) {
                    return;
                }
                String character = event.getCharacter();
                if (",".equals(character) || "，".equals(character)) {
                    commitTagInput(tagsField.getText());
                    event.consume();
                }
            });
        }
        categoryField.textProperty().addListener((obs, oldValue, newValue) -> {
            if (!suspend) {
                updateChips(newValue, currentSchedule != null ? currentSchedule.getTags() : "");
            }
        });
        notesArea.textProperty().addListener((obs, oldValue, newValue) -> {
            if (suspend) {
                return;
            }
            notesDelay.stop();
            notesDelay.setOnFinished(event -> saveNotes());
            notesDelay.playFromStart();
        });
        notesArea.focusedProperty().addListener((obs, oldValue, focused) -> {
            if (!focused && !suspend) {
                saveNotes();
            }
        });
        priorityBox.valueProperty().addListener((obs, oldValue, newValue) -> {
            if (!suspend && newValue != null) {
                save(text("error.prioritySave.title"), draft -> draft.setPriority(newValue), false);
            }
        });

        dueToggle.selectedProperty().addListener((obs, oldValue, selected) -> {
            if (!suspend) {
                handleDueToggle(selected);
            }
        });
        startToggle.selectedProperty().addListener((obs, oldValue, selected) -> {
            if (!suspend) {
                handleStartToggle(selected);
            }
        });
        allDayToggle.selectedProperty().addListener((obs, oldValue, selected) -> {
            if (!suspend) {
                saveAllDay(selected);
            }
        });
        reminderToggle.selectedProperty().addListener((obs, oldValue, selected) -> {
            if (!suspend) {
                handleReminderToggle(selected);
            }
        });

        dueTrigger.setOnAction(event -> openDuePicker());
        startTrigger.setOnAction(event -> openStartPicker());
        addReminderButton.setOnAction(event -> addReminder());
        recurrenceEditButton.setOnAction(event -> editRecurrenceRule());
        recurrenceClearButton.setOnAction(event -> clearRecurrenceRule());
        scrollPane.vvalueProperty().addListener((obs, oldValue, newValue) -> {
            closeWheelPopup();
            hideOptionSuggestionMenus();
        });

        installCustomOptionSuggestions();
    }

    private void bindTextField(TextField field, PauseTransition delay, Runnable saveAction) {
        field.textProperty().addListener((obs, oldValue, newValue) -> {
            if (suspend) {
                return;
            }
            delay.stop();
            delay.setOnFinished(event -> saveAction.run());
            delay.playFromStart();
        });
        field.setOnAction(event -> saveAction.run());
        field.focusedProperty().addListener((obs, oldValue, focused) -> {
            if (!focused && !suspend) {
                saveAction.run();
            }
        });
    }

    private void saveTitle() {
        String value = titleField.getText() == null ? "" : titleField.getText().strip();
        if (value.isEmpty()) {
            controller.showError(text("error.titleSave.title"), text("error.titleSave.empty"));
            restorePersisted();
            return;
        }
        if (persistedSchedule != null && value.equals(persistedSchedule.getName())) {
            return;
        }
        save(text("error.titleSave.title"), draft -> draft.setName(value), false);
    }

    private void saveCategory() {
        String value = normalizeCategoryInput(categoryField.getText());
        if (persistedSchedule != null && Objects.equals(value, persistedSchedule.getCategory())) {
            return;
        }
        if (!Schedule.isDefaultCategory(value)) {
            CustomOptionsService options = controller.getCustomOptionsService();
            if (options != null && !options.ensureTaskExists(value)) {
                controller.showError(
                    text("error.customOptions.taskLimit.title"),
                    text("error.customOptions.taskLimit.message", CustomOptionsService.MAX_TASKS)
                );
                restorePersisted();
                return;
            }
        }
        save(text("error.categorySave.title"), draft -> draft.setCategory(value), false);
    }

    private void commitTagInput(String rawInput) {
        if (currentSchedule == null || persistedSchedule == null || tagsField == null) {
            return;
        }

        List<String> inputTags = Schedule.splitTags(rawInput);
        if (inputTags.isEmpty()) {
            return;
        }

        List<String> merged = new ArrayList<>();
        LinkedHashSet<String> keys = new LinkedHashSet<>();
        for (String existing : currentSchedule.getTagNames()) {
            if (existing == null || existing.isBlank()) {
                continue;
            }
            String normalized = existing.strip();
            if (normalized.isEmpty()) {
                continue;
            }
            if (keys.add(normalized.toLowerCase(Locale.ROOT))) {
                merged.add(normalized);
            }
        }
        for (String candidate : inputTags) {
            if (candidate == null || candidate.isBlank()) {
                continue;
            }
            String normalized = candidate.strip();
            if (normalized.isEmpty()) {
                continue;
            }
            if (keys.add(normalized.toLowerCase(Locale.ROOT))) {
                merged.add(normalized);
            }
        }

        if (merged.equals(currentSchedule.getTagNames())) {
            tagsField.setText("");
            if (tagsSuggestionMenu != null) {
                tagsSuggestionMenu.hide();
            }
            return;
        }

        CustomOptionsService options = controller.getCustomOptionsService();
        if (options != null && !options.ensureTagsExist(inputTags)) {
            controller.showError(
                text("error.customOptions.tagLimit.title"),
                text("error.customOptions.tagLimit.message", CustomOptionsService.MAX_TAGS)
            );
            return;
        }

        save(text("error.tagsSave.title"), draft -> draft.setTagNames(merged), false);
        tagsField.setText("");
        if (tagsSuggestionMenu != null) {
            tagsSuggestionMenu.hide();
        }
        Platform.runLater(() -> {
            if (tagsField != null) {
                tagsField.requestFocus();
                tagsField.positionCaret(tagsField.getText().length());
            }
        });
    }

    private void saveNotes() {
        String value = notesArea.getText() == null ? "" : notesArea.getText();
        if (persistedSchedule != null && value.equals(persistedSchedule.getDescription())) {
            return;
        }
        save(text("error.notesSave.title"), draft -> draft.setDescription(value), false);
    }

    private void saveAllDay(boolean selected) {
        if (persistedSchedule != null && selected == persistedSchedule.isAllDay()) {
            updateTimeTriggers();
            return;
        }
        save(text("error.allDaySave.title"), draft -> draft.setAllDay(selected), true);
    }

    private void renderTagEditorChips(List<String> tags) {
        tagEditorChipPane.getChildren().clear();
        List<String> normalizedTags = tags != null ? tags : List.of();
        for (String tag : normalizedTags) {
            Button chip = new Button(tag + " ×");
            chip.getStyleClass().addAll("info-panel-chip", "info-panel-chip-tag", "info-panel-chip-action");
            chip.setOnAction(event -> removeTag(tag));
            tagEditorChipPane.getChildren().add(chip);
        }
        boolean hasTags = !normalizedTags.isEmpty();
        tagEditorChipPane.setVisible(hasTags);
        tagEditorChipPane.setManaged(hasTags);
    }

    private void removeTag(String tag) {
        if (currentSchedule == null) {
            return;
        }
        List<String> updatedTags = new ArrayList<>(currentSchedule.getTagNames());
        if (!updatedTags.removeIf(existing -> Objects.equals(existing, tag))) {
            return;
        }
        save(text("error.tagsSave.title"), draft -> draft.setTagNames(updatedTags), false);
    }

    private void addReminder() {
        if (currentSchedule == null) {
            return;
        }
        List<Reminder> reminders = new ArrayList<>(currentSchedule.getReminders());
        reminders.add(new Reminder(defaultReminderValue(LocalDate.now(), currentSchedule.getDueAt())));
        saveReminders(reminders);
    }

    private void editReminder(Reminder reminder, Button owner) {
        if (currentSchedule == null || reminder == null) {
            return;
        }
        openTimePicker(text("info.reminder"), owner, reminder.getRemindAtUtc(), value -> {
            List<Reminder> reminders = new ArrayList<>(currentSchedule.getReminders());
            for (Reminder candidate : reminders) {
                if (Objects.equals(candidate.getId(), reminder.getId())) {
                    candidate.setRemindAtUtc(value);
                    break;
                }
            }
            saveReminders(reminders);
        });
    }

    private void removeReminder(String reminderId) {
        if (currentSchedule == null || reminderId == null) {
            return;
        }
        List<Reminder> reminders = new ArrayList<>(currentSchedule.getReminders());
        reminders.removeIf(reminder -> Objects.equals(reminder.getId(), reminderId));
        saveReminders(reminders);
    }

    private void saveReminders(List<Reminder> reminders) {
        save(text("error.reminderSave.title"), draft -> draft.setReminders(reminders), true);
    }

    private void renderReminderEditor() {
        reminderListBox.getChildren().clear();
        List<Reminder> reminders = currentSchedule != null ? currentSchedule.getReminders() : List.of();
        int index = 1;
        for (Reminder reminder : reminders) {
            Button editButton = new Button(text("info.reminder.item", index, formatReminderButtonText(reminder)));
            editButton.getStyleClass().addAll("button-secondary", "info-panel-reminder-button");
            editButton.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(editButton, Priority.ALWAYS);
            editButton.setOnAction(event -> editReminder(reminder, editButton));

            Button removeButton = new Button(text("common.remove"));
            removeButton.getStyleClass().addAll("button-secondary", "info-panel-secondary-action");
            removeButton.setOnAction(event -> removeReminder(reminder.getId()));

            HBox row = new HBox(8, editButton, removeButton);
            row.setAlignment(Pos.CENTER_LEFT);
            row.getStyleClass().add("info-panel-reminder-row");
            reminderListBox.getChildren().add(row);
            index++;
        }
        boolean hasReminders = !reminders.isEmpty();
        reminderListBox.setVisible(hasReminders);
        reminderListBox.setManaged(hasReminders);
        addReminderButton.setDisable(currentSchedule == null);
    }

    private String formatReminderButtonText(Reminder reminder) {
        if (reminder == null || reminder.getRemindAtUtc() == null) {
            return text("common.unset");
        }
        TimeTriggerPresentation presentation = buildTimeTriggerPresentation(controller, reminder.getRemindAtUtc());
        if (presentation.getSecondaryText().isBlank()) {
            return presentation.getPrimaryText();
        }
        return presentation.getPrimaryText() + " · " + presentation.getSecondaryText();
    }

    private void editRecurrenceRule() {
        if (currentSchedule == null) {
            return;
        }
        LocalDateTime seed = currentSchedule.getStartAt() != null
            ? currentSchedule.getStartAt()
            : (currentSchedule.getDueAt() != null ? currentSchedule.getDueAt() : LocalDateTime.now());
        RecurrenceRuleDialog dialog = new RecurrenceRuleDialog(
            controller,
            currentSchedule.getRecurrenceRule(),
            seed,
            currentSchedule.getTimezone()
        );
        dialog.showAndWait().ifPresent(result -> saveRecurrenceRule(result.getRule()));
    }

    private void clearRecurrenceRule() {
        if (currentSchedule == null || currentSchedule.getRecurrenceRule() == null) {
            return;
        }
        save(text("error.recurrenceSave.title"), draft -> draft.setRecurrenceRule(null), true);
    }

    private void saveRecurrenceRule(RecurrenceRule rule) {
        save(text("error.recurrenceSave.title"), draft -> draft.setRecurrenceRule(rule), true);
    }

    private void updateRecurrenceEditor() {
        String summary = currentSchedule != null && currentSchedule.hasRecurrence()
            ? controller.recurrenceSummary(currentSchedule.getRecurrenceRule())
            : text("recurrence.none");
        recurrenceSummaryLabel.setText(summary);
        recurrenceClearButton.setDisable(currentSchedule == null || currentSchedule.getRecurrenceRule() == null);
    }

    private LocalDateTime normalizeAllDayValue(LocalDateTime value, boolean allDay, boolean endOfDay) {
        if (value == null || !allDay) {
            return value;
        }
        return endOfDay ? value.toLocalDate().atTime(23, 59) : value.toLocalDate().atStartOfDay();
    }

    private void handleDueToggle(boolean selected) {
        closeWheelPopup();
        saveDue(selected ? (currentSchedule != null && currentSchedule.getDueAt() != null
            ? currentSchedule.getDueAt()
            : defaultDueValue(LocalDate.now())) : null);
    }

    private void handleStartToggle(boolean selected) {
        closeWheelPopup();
        if (!selected) {
            saveStart(null);
            return;
        }
        LocalDateTime seed = currentSchedule != null ? currentSchedule.getStartAt() : null;
        if (seed == null) {
            seed = defaultStartSeed(
                currentSchedule != null ? currentSchedule.getCreatedAt() : null,
                currentSchedule != null && currentSchedule.isAllDay()
            );
        }
        saveStart(seed);
    }

    private void handleReminderToggle(boolean selected) {
        closeWheelPopup();
        if (!selected) {
            saveReminders(List.of());
            return;
        }
        if (currentSchedule != null && !currentSchedule.getReminders().isEmpty()) {
            renderReminderEditor();
            updateTimeTriggers();
            return;
        }
        addReminder();
    }

    private void openDuePicker() {
        if (currentSchedule != null && dueToggle.isSelected()) {
            openTimePicker(text("info.due"), dueTrigger, currentSchedule.getDueAt(), this::saveDue);
        }
    }

    private void openStartPicker() {
        if (currentSchedule != null && startToggle.isSelected()) {
            LocalDateTime seed = currentSchedule.getStartAt();
            if (seed == null) {
                seed = defaultStartSeed(currentSchedule.getCreatedAt(), currentSchedule.isAllDay());
            }
            openTimePicker(text("info.start"), startTrigger, seed, this::saveStart);
        }
    }

    private void openTimePicker(String title, Button owner, LocalDateTime currentValue, java.util.function.Consumer<LocalDateTime> saveAction) {
        LocalDateTime seed = currentValue != null ? currentValue : defaultStartValue(LocalDate.now());
        wheelPopup.show(owner, title, seed, value -> {
            closeWheelPopup();
            saveAction.accept(value);
        });
    }

    private void saveDue(LocalDateTime value) {
        LocalDateTime normalizedValue = normalizeAllDayValue(value, currentSchedule != null && currentSchedule.isAllDay(), true);
        if (persistedSchedule != null && Objects.equals(normalizedValue, persistedSchedule.getDueAt())) {
            updateTimeTriggers();
            return;
        }
        save(text("error.dueSave.title"), draft -> draft.setDueAt(normalizedValue), true);
    }

    private void saveStart(LocalDateTime value) {
        LocalDateTime normalizedValue = normalizeAllDayValue(value, currentSchedule != null && currentSchedule.isAllDay(), false);
        if (persistedSchedule != null && Objects.equals(normalizedValue, persistedSchedule.getStartAt())) {
            updateTimeTriggers();
            return;
        }
        save(text("error.startSave.title"), draft -> draft.setStartAt(normalizedValue), true);
    }

    private void save(String errorTitle, Change change, boolean rerender) {
        if (currentSchedule == null || persistedSchedule == null) {
            return;
        }
        Schedule draft = copyOf(currentSchedule);
        try {
            change.apply(draft);
            if (!controller.saveSchedule(draft)) {
                throw new SQLException(text("error.schedulePersist.message"));
            }
            currentSchedule = draft;
            persistedSchedule = copyOf(draft);
            if (rerender) {
                renderForm();
            } else {
                updateDerivedState();
            }
            controller.refreshDataViews();
        } catch (IllegalArgumentException | SQLException exception) {
            controller.showError(errorTitle, exception.getMessage());
            restorePersisted();
        }
    }

    private void restorePersisted() {
        closeWheelPopup();
        if (persistedSchedule == null) {
            return;
        }
        currentSchedule = copyOf(persistedSchedule);
        renderForm();
    }

    private void renderForm() {
        if (currentSchedule == null) {
            applyEmptyState();
            return;
        }
        closeWheelPopup();
        hideOptionSuggestionMenus();
        titleDelay.stop();
        categoryDelay.stop();
        tagsDelay.stop();
        notesDelay.stop();
        suspend = true;
        titleField.setPromptText(text("info.title.prompt"));
        titleField.setText(currentSchedule.getName());
        priorityBox.setValue(currentSchedule.getPriority());
        categoryField.setText(controller.categoryDisplayName(currentSchedule.getCategory()));
        tagsField.setText("");
        notesArea.setText(currentSchedule.getDescription());
        allDayToggle.setSelected(currentSchedule.isAllDay());
        dueToggle.setSelected(currentSchedule.getDueAt() != null);
        startToggle.setSelected(currentSchedule.getStartAt() != null);
        reminderToggle.setSelected(!currentSchedule.getReminders().isEmpty());
        suspend = false;
        updateEditorsEnabled();
        updateDerivedState();
    }

    private void applyEmptyState() {
        closeWheelPopup();
        hideOptionSuggestionMenus();
        suspend = true;
        titleField.setText("");
        priorityBox.setValue(Schedule.DEFAULT_PRIORITY);
        categoryField.setText(text("category.default"));
        tagsField.setText("");
        notesArea.setText("");
        allDayToggle.setSelected(false);
        dueToggle.setSelected(false);
        startToggle.setSelected(false);
        reminderToggle.setSelected(false);
        suspend = false;

        statusLabel.setText("");
        statusLabel.getStyleClass().removeAll(STATUS_CLASSES);
        statusLabel.setVisible(false);
        statusLabel.setManaged(false);
        chipPane.getChildren().clear();
        chipPane.setVisible(false);
        chipPane.setManaged(false);
        tagEditorChipPane.getChildren().clear();
        tagEditorChipPane.setVisible(false);
        tagEditorChipPane.setManaged(false);
        reminderListBox.getChildren().clear();
        reminderListBox.setVisible(false);
        reminderListBox.setManaged(false);
        recurrenceSummaryLabel.setText(text("recurrence.none"));

        completeControl.syncCompleted(false);
        postponeButton.setVisible(false);
        postponeButton.setManaged(false);
        pinButton.setVisible(false);
        pinButton.setManaged(false);
        unpinButton.setVisible(false);
        unpinButton.setManaged(false);
        setDisabled(true);
        updateTimeTriggers();
        titleField.setPromptText(text("info.title.empty"));
    }

    private void updateDerivedState() {
        if (currentSchedule == null) {
            applyEmptyState();
            return;
        }
        statusLabel.getStyleClass().removeAll(STATUS_CLASSES);
        if (currentSchedule.isCompleted()) {
            statusLabel.setText(text("status.completed"));
            statusLabel.getStyleClass().add("info-panel-status-completed");
        } else if (currentSchedule.isOverdue()) {
            statusLabel.setText(text("status.overdue"));
            statusLabel.getStyleClass().add("info-panel-status-overdue");
        } else if (currentSchedule.isUpcoming()) {
            statusLabel.setText(text("status.upcoming"));
            statusLabel.getStyleClass().add("info-panel-status-upcoming");
        } else {
            statusLabel.setText(text("status.ongoing"));
            statusLabel.getStyleClass().add("info-panel-status-ongoing");
        }
        statusLabel.setVisible(true);
        statusLabel.setManaged(true);
        completeControl.syncCompleted(currentSchedule.isCompleted());
        boolean showPostpone = !currentSchedule.isCompleted();
        postponeButton.setVisible(showPostpone);
        postponeButton.setManaged(showPostpone);
        boolean showPin = !currentSchedule.isCompleted() && !currentSchedule.isSuspended();
        pinButton.setVisible(showPin);
        pinButton.setManaged(showPin);
        boolean showUnpin = !currentSchedule.isCompleted() && currentSchedule.isSuspended();
        unpinButton.setVisible(showUnpin);
        unpinButton.setManaged(showUnpin);
        setDisabled(false);

        updateTimeTriggers();
        updateChips(currentSchedule.getCategory(), currentSchedule.getTags());
        renderTagEditorChips(currentSchedule.getTagNames());
        renderReminderEditor();
        updateRecurrenceEditor();
    }

    private void updateChips(String category, String tags) {
        chipPane.getChildren().clear();
        if (statusLabel.isManaged() && !statusLabel.getText().isBlank()) {
            chipPane.getChildren().add(statusLabel);
        }
        if (shouldShowCategoryChip(category)) {
            chipPane.getChildren().add(chip(controller.categoryDisplayName(Schedule.normalizeCategory(category)), "info-panel-chip-category"));
        }
        for (String tag : splitTagChips(tags)) {
            chipPane.getChildren().add(chip(tag, "info-panel-chip-tag"));
        }
        boolean hasContent = !chipPane.getChildren().isEmpty();
        chipPane.setVisible(hasContent);
        chipPane.setManaged(hasContent);
    }

    private void updateEditorsEnabled() {
        boolean disabled = currentSchedule == null;
        setDisabled(disabled);
        dueTrigger.setDisable(disabled || !dueToggle.isSelected());
        startTrigger.setDisable(disabled || !startToggle.isSelected());
        addReminderButton.setDisable(disabled);
        recurrenceEditButton.setDisable(disabled);
        recurrenceClearButton.setDisable(disabled || currentSchedule == null || currentSchedule.getRecurrenceRule() == null);
    }

    private void updateTimeTriggers() {
        configureTimeTrigger(
            dueTrigger,
            dueTriggerTitle,
            dueTriggerSubtitle,
            currentSchedule != null && dueToggle.isSelected() ? currentSchedule.getDueAt() : null,
            currentSchedule != null && currentSchedule.isAllDay()
        );
        configureTimeTrigger(
            startTrigger,
            startTriggerTitle,
            startTriggerSubtitle,
            currentSchedule != null && startToggle.isSelected() ? currentSchedule.getStartAt() : null,
            currentSchedule != null && currentSchedule.isAllDay()
        );
        updateEditorsEnabled();
        renderReminderEditor();
    }

    private void configureTimeTrigger(Button trigger, Label titleLabel, Label subtitleLabel, LocalDateTime value) {
        TimeTriggerPresentation presentation = buildTimeTriggerPresentation(controller, value);
        titleLabel.setText(presentation.getPrimaryText());
        subtitleLabel.setText(presentation.getSecondaryText());
        subtitleLabel.setVisible(!presentation.getSecondaryText().isBlank());
        subtitleLabel.setManaged(!presentation.getSecondaryText().isBlank());
        toggleStyleClass(trigger, "info-panel-time-trigger-unset", presentation.isUnset());
    }

    private void configureTimeTrigger(Button trigger, Label titleLabel, Label subtitleLabel, LocalDateTime value, boolean allDay) {
        TimeTriggerPresentation presentation = buildTimeTriggerPresentation(controller, value, allDay);
        titleLabel.setText(presentation.getPrimaryText());
        subtitleLabel.setText(presentation.getSecondaryText());
        subtitleLabel.setVisible(!presentation.getSecondaryText().isBlank());
        subtitleLabel.setManaged(!presentation.getSecondaryText().isBlank());
        toggleStyleClass(trigger, "info-panel-time-trigger-unset", presentation.isUnset());
    }

    private void setDisabled(boolean disabled) {
        completeControl.setDisable(disabled);
        deleteButton.setDisable(disabled);
        postponeButton.setDisable(disabled);
        pinButton.setDisable(disabled);
        unpinButton.setDisable(disabled);
        titleField.setDisable(disabled);
        allDayToggle.setDisable(disabled);
        dueToggle.setDisable(disabled);
        startToggle.setDisable(disabled);
        reminderToggle.setDisable(disabled);
        priorityBox.setDisable(disabled);
        categoryField.setDisable(disabled);
        tagsField.setDisable(disabled);
        notesArea.setDisable(disabled);
        reminderListBox.setDisable(disabled);
        addReminderButton.setDisable(disabled);
        recurrenceEditButton.setDisable(disabled);
        recurrenceClearButton.setDisable(disabled);
    }

    private void deleteSchedule() {
        closeWheelPopup();
        if (currentSchedule == null) {
            return;
        }
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        controller.applyDialogPreferences(alert.getDialogPane());
        ButtonType confirmType = new ButtonType(text("common.delete"), ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelType = new ButtonType(text("common.cancel"), ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(confirmType, cancelType);
        alert.setTitle(text("alert.confirm.title"));
        alert.setHeaderText(text("info.delete"));
        alert.setContentText(text("info.delete.confirm", currentSchedule.getName()));
        alert.showAndWait().ifPresent(response -> {
            if (response != confirmType) {
                return;
            }
            try {
                controller.removeSchedule(currentSchedule.getId());
                currentSchedule = null;
                persistedSchedule = null;
                applyEmptyState();
                controller.closeScheduleDetails();
                controller.refreshAllViews();
            } catch (SQLException exception) {
                controller.showError(text("error.delete.title"), exception.getMessage());
            }
        });
    }

    private void postponeScheduleOneDay() {
        if (currentSchedule == null || currentSchedule.isCompleted()) {
            return;
        }
        LocalDateTime due = currentSchedule.getDueAt();
        if (due != null) {
            currentSchedule.setDueAt(due.plusDays(1));
        } else {
            currentSchedule.setDueAt(LocalDateTime.now().plusDays(1)
                .withHour(23).withMinute(59).withSecond(0).withNano(0));
        }
        try {
            if (controller.saveSchedule(currentSchedule)) {
                persistedSchedule = copyOf(currentSchedule);
                renderForm();
                controller.refreshAllViews();
            }
        } catch (SQLException exception) {
            controller.showError(text("error.scheduleUpdate.title"), exception.getMessage());
        }
    }

    private void pinSchedule() {
        if (currentSchedule == null || currentSchedule.isCompleted() || currentSchedule.isSuspended()) {
            return;
        }
        currentSchedule.setSuspended(true);
        try {
            if (controller.saveSchedule(currentSchedule)) {
                persistedSchedule = copyOf(currentSchedule);
                updateDerivedState();
                controller.refreshAllViews();
            } else {
                currentSchedule.setSuspended(false);
            }
        } catch (SQLException exception) {
            currentSchedule.setSuspended(false);
            controller.showError(text("error.scheduleUpdate.title"), exception.getMessage());
        }
    }

    private void unpinSchedule() {
        if (currentSchedule == null || currentSchedule.isCompleted() || !currentSchedule.isSuspended()) {
            return;
        }
        currentSchedule.setSuspended(false);
        try {
            if (controller.saveSchedule(currentSchedule)) {
                persistedSchedule = copyOf(currentSchedule);
                updateDerivedState();
                controller.refreshAllViews();
            } else {
                currentSchedule.setSuspended(true);
            }
        } catch (SQLException exception) {
            currentSchedule.setSuspended(true);
            controller.showError(text("error.scheduleUpdate.title"), exception.getMessage());
        }
    }

    private void closeWheelPopup() {
        wheelPopup.hide();
    }

    private void hideOptionSuggestionMenus() {
        if (categorySuggestionMenu != null) {
            categorySuggestionMenu.hide();
        }
        if (tagsSuggestionMenu != null) {
            tagsSuggestionMenu.hide();
        }
    }

    private void installCustomOptionSuggestions() {
        CustomOptionsService options = controller.getCustomOptionsService();
        if (options == null || categoryField == null || tagsField == null) {
            return;
        }

        if (categorySuggestionMenu == null) {
            categorySuggestionMenu = new ContextMenu();
            categorySuggestionMenu.getStyleClass().add("custom-option-suggestion-menu");
            categorySuggestionMenu.setAutoHide(true);
            categorySuggestionMenu.setHideOnEscape(true);
        }
        if (tagsSuggestionMenu == null) {
            tagsSuggestionMenu = new ContextMenu();
            tagsSuggestionMenu.getStyleClass().add("custom-option-suggestion-menu");
            tagsSuggestionMenu.setAutoHide(true);
            tagsSuggestionMenu.setHideOnEscape(true);
        }

        categoryField.focusedProperty().addListener((obs, oldValue, focused) -> {
            if (focused) {
                categorySuggestionQueryEnabled = false;
                refreshCategorySuggestions();
            } else {
                // Defer to allow context-menu clicks to register before hiding.
                Platform.runLater(() -> {
                    if (categorySuggestionMenu != null) {
                        categorySuggestionMenu.hide();
                    }
                });
            }
        });
        categoryField.textProperty().addListener((obs, oldValue, newValue) -> {
            if (suspend) {
                return;
            }
            if (categoryField.isFocused()) {
                categorySuggestionQueryEnabled = true;
                refreshCategorySuggestions();
            }
        });
        categoryField.setOnMouseClicked(event -> {
            if (!suspend && categoryField.isFocused()) {
                refreshCategorySuggestions();
            }
        });
        categoryField.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.ESCAPE) {
                if (categorySuggestionMenu != null) {
                    categorySuggestionMenu.hide();
                }
            }
        });

        tagsField.focusedProperty().addListener((obs, oldValue, focused) -> {
            if (focused) {
                refreshTagSuggestions();
            } else {
                Platform.runLater(() -> {
                    if (tagsSuggestionMenu != null) {
                        tagsSuggestionMenu.hide();
                    }
                });
            }
        });
        tagsField.textProperty().addListener((obs, oldValue, newValue) -> {
            if (suspend) {
                return;
            }
            if (tagsField.isFocused()) {
                refreshTagSuggestions();
            }
        });
        tagsField.setOnMouseClicked(event -> {
            if (!suspend && tagsField.isFocused()) {
                refreshTagSuggestions();
            }
        });
        tagsField.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.ESCAPE) {
                if (tagsSuggestionMenu != null) {
                    tagsSuggestionMenu.hide();
                }
            }
        });

        options.addChangeListener(() -> {
            if (categoryField != null && categoryField.isFocused()) {
                refreshCategorySuggestions();
            }
            if (tagsField != null && tagsField.isFocused()) {
                refreshTagSuggestions();
            }
        });
    }

    private void refreshCategorySuggestions() {
        if (categorySuggestionMenu == null || categoryField == null) {
            return;
        }
        if (!categoryField.isFocused() || categoryField.isDisabled() || !categoryField.isVisible()) {
            categorySuggestionMenu.hide();
            return;
        }

        CustomOptionsService options = controller.getCustomOptionsService();
        if (options == null) {
            categorySuggestionMenu.hide();
            return;
        }

        String rawInput = categoryField.getText();
        String trimmed = rawInput == null ? "" : rawInput.trim();
        String query = categorySuggestionQueryEnabled ? trimmed : "";
        if (trimmed.equals(text("category.default"))) {
            query = "";
        }

        List<MenuItem> items = new ArrayList<>();
        MenuItem unsetItem = new MenuItem(text("category.default"));
        unsetItem.setGraphic(controller.createSvgIcon(IconKey.RESET, text("category.default"), 16));
        unsetItem.setOnAction(event -> {
            categoryField.setText(text("category.default"));
            categoryField.positionCaret(categoryField.getText().length());
            categoryDelay.stop();
            saveCategory();
            categorySuggestionMenu.hide();
        });
        items.add(unsetItem);

        List<String> suggestions = options.suggestTasks(query, 12);
        if (!suggestions.isEmpty()) {
            items.add(new SeparatorMenuItem());
            for (String suggestion : suggestions) {
                if (suggestion == null || suggestion.isBlank()) {
                    continue;
                }
                MenuItem item = new MenuItem(suggestion);
                item.setGraphic(controller.createSvgIcon(IconKey.FOLDER, suggestion, 16));
                item.setOnAction(event -> {
                    categoryField.setText(suggestion);
                    categoryField.positionCaret(categoryField.getText().length());
                    categoryDelay.stop();
                    saveCategory();
                    categorySuggestionMenu.hide();
                });
                items.add(item);
            }
        }

        categorySuggestionMenu.getItems().setAll(items);
        if (!categorySuggestionMenu.isShowing()) {
            categorySuggestionMenu.show(categoryField, Side.BOTTOM, 0, 4);
        }
    }

    private void refreshTagSuggestions() {
        if (tagsSuggestionMenu == null || tagsField == null) {
            return;
        }
        if (!tagsField.isFocused() || tagsField.isDisabled() || !tagsField.isVisible()) {
            tagsSuggestionMenu.hide();
            return;
        }

        CustomOptionsService options = controller.getCustomOptionsService();
        if (options == null) {
            tagsSuggestionMenu.hide();
            return;
        }

        String rawInput = tagsField.getText();
        String query = resolveTagQuery(rawInput);

        LinkedHashSet<String> selectedKeys = new LinkedHashSet<>();
        if (currentSchedule != null) {
            for (String selected : currentSchedule.getTagNames()) {
                if (selected != null && !selected.isBlank()) {
                    selectedKeys.add(selected.toLowerCase(Locale.ROOT));
                }
            }
        }
        for (String selected : Schedule.splitTags(rawInput)) {
            if (selected != null && !selected.isBlank()) {
                selectedKeys.add(selected.toLowerCase(Locale.ROOT));
            }
        }

        List<String> suggestions = options.suggestTags(query, 12);
        List<MenuItem> items = new ArrayList<>();
        for (String suggestion : suggestions) {
            if (suggestion == null || suggestion.isBlank()) {
                continue;
            }
            if (selectedKeys.contains(suggestion.toLowerCase(Locale.ROOT))) {
                continue;
            }
            MenuItem item = new MenuItem(suggestion);
            item.setGraphic(controller.createSvgIcon(IconKey.TAG, suggestion, 16));
            item.setOnAction(event -> {
                commitTagInput(applyTagSuggestion(rawInput, suggestion));
            });
            items.add(item);
        }

        if (items.isEmpty()) {
            tagsSuggestionMenu.hide();
            return;
        }

        tagsSuggestionMenu.getItems().setAll(items);
        if (!tagsSuggestionMenu.isShowing()) {
            tagsSuggestionMenu.show(tagsField, Side.BOTTOM, 0, 4);
        }
    }

    private static String resolveTagQuery(String rawInput) {
        if (rawInput == null) {
            return "";
        }
        int commaIndex = rawInput.lastIndexOf(',');
        int chineseCommaIndex = rawInput.lastIndexOf('，');
        int splitIndex = Math.max(commaIndex, chineseCommaIndex);
        if (splitIndex < 0) {
            return rawInput.trim();
        }
        return rawInput.substring(splitIndex + 1).trim();
    }

    private static String applyTagSuggestion(String rawInput, String suggestion) {
        String normalized = suggestion == null ? "" : suggestion.strip();
        if (normalized.isEmpty()) {
            return rawInput;
        }
        String input = rawInput == null ? "" : rawInput;
        int commaIndex = input.lastIndexOf(',');
        int chineseCommaIndex = input.lastIndexOf('，');
        int splitIndex = Math.max(commaIndex, chineseCommaIndex);
        if (splitIndex < 0) {
            return normalized;
        }
        String prefix = input.substring(0, splitIndex + 1);
        if (!prefix.endsWith(" ")) {
            prefix = prefix + " ";
        }
        return prefix + normalized;
    }

    private boolean isTagCommaSplitEnabled() {
        CustomOptionsService options = controller.getCustomOptionsService();
        return options != null && options.isTagCommaSplitEnabled();
    }

    private String text(String key, Object... args) {
        return controller.text(key, args);
    }

    private String normalizeCategoryInput(String rawCategory) {
        String normalized = rawCategory == null ? "" : rawCategory.trim();
        if (normalized.isEmpty() || normalized.equals(text("category.default"))) {
            return Schedule.DEFAULT_CATEGORY;
        }
        return Schedule.normalizeCategory(normalized);
    }

    public void refreshIcons() {
        if (deleteButton != null) {
            deleteButton.setGraphic(controller.createSvgIcon(IconKey.DELETE, text("info.delete"), 16));
        }
        if (closeButton != null) {
            closeButton.setGraphic(controller.createSvgIcon(IconKey.CLOSE, text("info.close"), 16));
        }
        if (postponeButton != null) {
            postponeButton.setGraphic(controller.createSvgIcon(IconKey.POSTPONE_DAY, text("action.postponeDay"), 16));
        }
        if (pinButton != null) {
            pinButton.setGraphic(controller.createSvgIcon(IconKey.PIN, text("action.pin"), 16));
        }
        if (unpinButton != null) {
            unpinButton.setGraphic(controller.createSvgIcon(IconKey.UNPIN, text("action.unpin"), 16));
        }
    }

    private Button iconButton(IconKey iconKey, String tooltipText, Runnable action) {
        Button button = new Button();
        button.getStyleClass().add("info-panel-toolbar-button");
        button.setGraphic(controller.createSvgIcon(iconKey, tooltipText, 16));
        button.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        button.setOnAction(event -> action.run());
        return button;
    }

    private Button actionIconButton(IconKey iconKey, String tooltipText, Runnable action, String... styleClasses) {
        Button button = new Button();
        button.getStyleClass().add("info-panel-icon-button");
        button.getStyleClass().addAll(styleClasses);
        button.setGraphic(controller.createSvgIcon(iconKey, tooltipText, 16));
        button.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        button.setOnAction(event -> action.run());
        return button;
    }

    private ListCell<String> createPriorityCell() {
        return new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : controller.priorityDisplayName(item));
            }
        };
    }

    private CheckBox rowToggle(String text) {
        CheckBox box = new CheckBox(text);
        box.getStyleClass().add("info-panel-editor-toggle");
        box.setMinWidth(Region.USE_PREF_SIZE);
        box.setTextOverrun(OverrunStyle.CLIP);
        return box;
    }

    private Button timeTrigger() {
        Button button = new Button();
        button.getStyleClass().add("info-panel-time-trigger");
        button.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        button.setAlignment(Pos.CENTER_LEFT);
        button.setMinWidth(TIME_TRIGGER_WIDTH);
        button.setPrefWidth(TIME_TRIGGER_WIDTH);
        button.setMaxWidth(TIME_TRIGGER_WIDTH);
        button.setMinHeight(TIME_TRIGGER_HEIGHT);
        button.setPrefHeight(TIME_TRIGGER_HEIGHT);
        button.setMaxHeight(TIME_TRIGGER_HEIGHT);
        return button;
    }

    private void attachStackedTriggerGraphic(Button trigger, Label titleLabel, Label subtitleLabel) {
        VBox textBox = new VBox(2, titleLabel, subtitleLabel);
        textBox.setAlignment(Pos.CENTER_LEFT);
        textBox.setFillWidth(false);
        textBox.setMouseTransparent(true);
        trigger.setGraphic(textBox);
    }

    private void attachInlineYearTriggerGraphic(Button trigger, Label yearLabel, Label mainLabel) {
        HBox line = new HBox(8, yearLabel, mainLabel);
        line.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(mainLabel, Priority.ALWAYS);
        mainLabel.setMaxWidth(Double.MAX_VALUE);
        line.setMouseTransparent(true);
        trigger.setGraphic(line);
    }

    private Label triggerLabel(String styleClass) {
        Label label = new Label();
        label.getStyleClass().add(styleClass);
        label.setWrapText(false);
        label.setTextOverrun(OverrunStyle.CLIP);
        return label;
    }

    private TextField textInput(String prompt) {
        TextField field = new TextField();
        field.getStyleClass().addAll("info-panel-text-input", "info-panel-borderless-field");
        field.setPromptText(prompt);
        return field;
    }

    private GridPane timeRow(CheckBox toggle, Button trigger) {
        HBox toggleSlot = new HBox(toggle);
        toggleSlot.setAlignment(Pos.CENTER_LEFT);
        toggleSlot.setMinWidth(TIME_TOGGLE_SLOT_WIDTH);
        toggleSlot.setPrefWidth(TIME_TOGGLE_SLOT_WIDTH);
        toggleSlot.setMaxWidth(TIME_TOGGLE_SLOT_WIDTH);
        toggleSlot.getStyleClass().add("info-panel-time-toggle-slot");

        ColumnConstraints toggleColumn = new ColumnConstraints();
        toggleColumn.setMinWidth(TIME_TOGGLE_SLOT_WIDTH);
        toggleColumn.setPrefWidth(TIME_TOGGLE_SLOT_WIDTH);
        toggleColumn.setMaxWidth(TIME_TOGGLE_SLOT_WIDTH);

        ColumnConstraints triggerColumn = new ColumnConstraints();
        triggerColumn.setMinWidth(TIME_TRIGGER_WIDTH);
        triggerColumn.setPrefWidth(TIME_TRIGGER_WIDTH);
        triggerColumn.setMaxWidth(TIME_TRIGGER_WIDTH);

        GridPane row = new GridPane();
        row.setAlignment(Pos.CENTER_LEFT);
        row.setHgap(6);
        row.setVgap(0);
        row.getStyleClass().add("info-panel-time-row");
        row.getColumnConstraints().addAll(toggleColumn, triggerColumn);
        row.add(toggleSlot, 0, 0);
        row.add(trigger, 1, 0);
        return row;
    }

    private HBox toggleOnlyRow(CheckBox toggle) {
        HBox row = new HBox(toggle);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("info-panel-inline-row");
        return row;
    }

    private VBox inlineEditor(Node editor, Node... supportingNodes) {
        VBox wrapper = new VBox(8);
        wrapper.getStyleClass().add("info-panel-inline-editor");
        wrapper.getChildren().add(editor);
        wrapper.getChildren().addAll(supportingNodes);
        installInlineEditorState(wrapper, editor);
        return wrapper;
    }

    private void installInlineEditorState(VBox wrapper, Node focusTarget) {
        Runnable refreshState = () -> {
            boolean interactive = !focusTarget.isDisabled();
            boolean active = interactive && isEditorActive(focusTarget);
            boolean hovered = interactive && wrapper.isHover() && !active;
            toggleStyleClass(wrapper, "info-panel-inline-editor-hover", hovered);
            toggleStyleClass(wrapper, "info-panel-inline-editor-active", active);
        };
        wrapper.hoverProperty().addListener((obs, oldValue, newValue) -> refreshState.run());
        focusTarget.focusedProperty().addListener((obs, oldValue, newValue) -> refreshState.run());
        focusTarget.disableProperty().addListener((obs, oldValue, newValue) -> refreshState.run());
        if (focusTarget instanceof ComboBoxBase<?> comboBoxBase) {
            comboBoxBase.showingProperty().addListener((obs, oldValue, newValue) -> refreshState.run());
        }
        wrapper.setOnMouseClicked(event -> {
            if (focusTarget.isDisabled()) {
                return;
            }
            if (event.getTarget() instanceof Node target && isDescendant(target, focusTarget)) {
                return;
            }
            requestEditorFocus(focusTarget);
        });
        refreshState.run();
    }

    private boolean isEditorActive(Node editor) {
        if (editor instanceof ComboBoxBase<?> comboBoxBase) {
            return comboBoxBase.isFocused() || comboBoxBase.isShowing();
        }
        return editor.isFocused();
    }

    private void requestEditorFocus(Node editor) {
        editor.requestFocus();
        if (editor instanceof TextField field) {
            field.positionCaret(field.getText().length());
        } else if (editor instanceof TextArea area) {
            area.positionCaret(area.getText().length());
        } else if (editor instanceof ComboBoxBase<?> comboBoxBase && !comboBoxBase.isShowing()) {
            comboBoxBase.show();
        }
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

    private VBox section(String title, Node... nodes) {
        Label label = new Label(title);
        label.getStyleClass().add("info-panel-section-title");
        VBox box = new VBox(10);
        box.getStyleClass().add("info-panel-editor-section");
        box.getChildren().add(label);
        box.getChildren().addAll(nodes);
        return box;
    }

    private Label chip(String text, String styleClass) {
        Label chip = new Label(text);
        chip.getStyleClass().addAll("info-panel-chip", styleClass);
        return chip;
    }

    private Schedule copyOf(Schedule source) {
        Schedule copy = new Schedule();
        copy.setId(source.getId());
        copy.setViewKey(source.getViewKey());
        copy.setName(source.getName());
        copy.setDescription(source.getDescription());
        copy.setNotes(source.getNotes());
        copy.setStartAt(source.getStartAt());
        copy.setEndAt(source.getEndAt());
        copy.setDueAt(source.getDueAt());
        copy.setAllDay(source.isAllDay());
        copy.setTimePrecision(source.getTimePrecision());
        copy.setTimezone(source.getTimezone());
        copy.setCompleted(source.isCompleted());
        copy.setPriority(source.getPriority());
        copy.setCategory(source.getCategory());
        copy.setTags(source.getTags());
        copy.setTagObjects(source.getTagObjects());
        copy.setReminderTime(source.getReminderTime());
        copy.setReminders(source.getReminders());
        copy.setRecurrenceRule(source.getRecurrenceRule());
        copy.setColor(source.getColor());
        copy.setCreatedAt(source.getCreatedAt());
        copy.setUpdatedAt(source.getUpdatedAt());
        copy.setDeletedAt(source.getDeletedAt());
        copy.setStatus(source.getStatus());
        copy.setCompletedAt(source.getCompletedAt());
        return copy;
    }

    private void stopTransition() {
        if (panelTransition != null) {
            panelTransition.stop();
            panelTransition = null;
        }
    }

    @FunctionalInterface
    private interface Change {
        void apply(Schedule schedule);
    }

    static final class DatePresentation {
        private final String primaryText;
        private final String secondaryText;

        DatePresentation(String primaryText, String secondaryText) {
            this.primaryText = primaryText;
            this.secondaryText = secondaryText;
        }

        String getPrimaryText() {
            return primaryText;
        }

        String getSecondaryText() {
            return secondaryText;
        }
    }

    static final class TimeTriggerPresentation {
        private final String primaryText;
        private final String secondaryText;
        private final boolean unset;

        TimeTriggerPresentation(String primaryText, String secondaryText, boolean unset) {
            this.primaryText = primaryText;
            this.secondaryText = secondaryText;
            this.unset = unset;
        }

        String getPrimaryText() {
            return primaryText;
        }

        String getSecondaryText() {
            return secondaryText;
        }

        boolean isUnset() {
            return unset;
        }
    }
}
