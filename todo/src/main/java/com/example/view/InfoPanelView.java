package com.example.view;

import com.example.controller.MainController;
import com.example.controller.ScheduleCompletionMutation;
import com.example.databaseutil.ScheduleDAO;
import com.example.model.Schedule;
import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;

public class InfoPanelView implements ScheduleCompletionParticipant {

    private static final String DEFAULT_CATEGORY = "默认";
    private static final String DEFAULT_TAG = "无";
    private static final String EMPTY_DESCRIPTION_HINT = "这里空空如也，添加点描述吧...";
    private static final String EMPTY_DATE_TEXT = "未设置日期";
    private static final String EMPTY_TITLE_TEXT = "请选择日程";
    private static final List<String> STATUS_VARIANT_CLASSES = List.of(
        "info-panel-status-completed",
        "info-panel-status-overdue",
        "info-panel-status-upcoming",
        "info-panel-status-ongoing"
    );
    private static final List<String> PRIORITY_VARIANT_CLASSES = List.of(
        "info-panel-priority-high",
        "info-panel-priority-medium",
        "info-panel-priority-low"
    );
    private static final DateTimeFormatter MONTH_DAY_FORMATTER = DateTimeFormatter.ofPattern("M月d日");
    private static final DateTimeFormatter YEAR_FORMATTER = DateTimeFormatter.ofPattern("yyyy年");
    private static final DateTimeFormatter REMINDER_FORMATTER = DateTimeFormatter.ofPattern("yyyy年M月d日 HH:mm");

    private final MainController controller;
    private final ScheduleDAO scheduleDAO;

    private VBox root;
    private Schedule currentSchedule;
    private ParallelTransition panelTransition;
    private boolean panelVisible;

    private Label titleLabel;
    private Label statusLabel;
    private Label datePrimaryLabel;
    private Label dateSecondaryLabel;
    private Label priorityValueLabel;
    private Label reminderValueLabel;
    private Label descriptionArea;
    private FlowPane chipPane;

    private HBox dateRow;
    private HBox priorityRow;
    private HBox chipRow;
    private HBox reminderRow;
    private Separator descriptionSeparator;
    private VBox descriptionSection;

    private Button editButton;
    private Button deleteButton;
    private Button completeButton;

    public InfoPanelView(MainController controller) {
        this.controller = controller;
        this.scheduleDAO = new ScheduleDAO();
        initializeUI();
    }

    private void initializeUI() {
        root = new VBox();
        root.getStyleClass().add("info-panel");
        root.setPrefWidth(320);
        root.setMinWidth(320);
        root.setMaxWidth(360);

        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);
        header.getStyleClass().add("info-panel-header");

        Label panelTitle = new Label("日程详情");
        panelTitle.getStyleClass().add("info-panel-heading");

        Region headerSpacer = new Region();
        HBox.setHgrow(headerSpacer, Priority.ALWAYS);

        editButton = createToolbarButton(
            "/icons/macaron_info-edit_icon.svg",
            "编辑日程",
            () -> {
                if (currentSchedule != null) {
                    controller.openEditScheduleDialog(currentSchedule);
                }
            }
        );
        deleteButton = createToolbarButton(
            "/icons/macaron_info-delete_icon.svg",
            "删除日程",
            this::deleteSchedule
        );
        Button closeButton = createToolbarButton(
            "/icons/macaron_info-close_icon.svg",
            "关闭详情",
            controller::closeScheduleDetails
        );

        HBox toolbar = new HBox(6, editButton, deleteButton, closeButton);
        toolbar.setAlignment(Pos.CENTER_RIGHT);
        toolbar.getStyleClass().add("info-panel-toolbar");

        header.getChildren().addAll(panelTitle, headerSpacer, toolbar);

        VBox content = new VBox(16);
        content.getStyleClass().add("info-panel-content");

        statusLabel = new Label();
        statusLabel.getStyleClass().add("info-panel-status-pill");

        titleLabel = new Label(EMPTY_TITLE_TEXT);
        titleLabel.getStyleClass().addAll("info-panel-schedule-title", "info-panel-empty-title");
        titleLabel.setWrapText(true);

        datePrimaryLabel = new Label(EMPTY_DATE_TEXT);
        datePrimaryLabel.getStyleClass().add("info-panel-date-primary");

        dateSecondaryLabel = new Label();
        dateSecondaryLabel.getStyleClass().add("info-panel-date-secondary");

        VBox dateText = new VBox(2, datePrimaryLabel, dateSecondaryLabel);
        dateText.getStyleClass().add("info-panel-date-block");
        dateRow = createMetaRow("/icons/macaron_calendar-date_icon.svg", "日期", dateText);

        Label priorityTextLabel = new Label("优先级:");
        priorityTextLabel.getStyleClass().add("info-panel-meta-label");

        priorityValueLabel = new Label();
        priorityValueLabel.getStyleClass().addAll("info-panel-meta-value", "info-panel-priority-value");

        HBox priorityText = new HBox(6, priorityTextLabel, priorityValueLabel);
        priorityText.setAlignment(Pos.CENTER_LEFT);
        priorityText.getStyleClass().add("info-panel-meta-line");
        priorityRow = createMetaRow("/icons/macaron_info-flag_icon.svg", "优先级", priorityText);

        chipPane = new FlowPane();
        chipPane.setHgap(8);
        chipPane.setVgap(8);
        chipPane.getStyleClass().add("info-panel-chip-pane");
        chipPane.prefWrapLengthProperty().bind(root.widthProperty().subtract(110));
        chipRow = createMetaRow("/icons/macaron_info-tag_icon.svg", "分类与标签", chipPane);

        Label reminderLabel = new Label("提醒:");
        reminderLabel.getStyleClass().add("info-panel-meta-label");

        reminderValueLabel = new Label();
        reminderValueLabel.getStyleClass().add("info-panel-meta-value");

        HBox reminderText = new HBox(6, reminderLabel, reminderValueLabel);
        reminderText.setAlignment(Pos.CENTER_LEFT);
        reminderText.getStyleClass().add("info-panel-meta-line");
        reminderRow = createMetaRow("/icons/macaron_info-bell_icon.svg", "提醒时间", reminderText);

        descriptionSeparator = new Separator();
        descriptionSeparator.getStyleClass().add("info-panel-section-separator");

        HBox descriptionTitleRow = new HBox(
            8,
            createMetaIcon("/icons/macaron_info-notes_icon.svg", "描述", 16),
            createSectionTitle("描述")
        );
        descriptionTitleRow.setAlignment(Pos.CENTER_LEFT);
        descriptionTitleRow.getStyleClass().add("info-panel-section-title-row");

        descriptionArea = new Label(EMPTY_DESCRIPTION_HINT);
        descriptionArea.getStyleClass().addAll("info-panel-description-text", "info-panel-description-placeholder");
        descriptionArea.setWrapText(true);

        StackPane descriptionCard = new StackPane(descriptionArea);
        descriptionCard.setMinHeight(92);
        descriptionCard.setAlignment(Pos.TOP_LEFT);
        descriptionCard.getStyleClass().add("info-panel-description-card");

        descriptionSection = new VBox(10, descriptionTitleRow, descriptionCard);
        descriptionSection.getStyleClass().add("info-panel-description-section");

        content.getChildren().addAll(
            statusLabel,
            titleLabel,
            dateRow,
            priorityRow,
            chipRow,
            reminderRow,
            descriptionSeparator,
            descriptionSection
        );

        ScrollPane contentScroll = new ScrollPane(content);
        contentScroll.setFitToWidth(true);
        contentScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        contentScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        contentScroll.setPannable(true);
        contentScroll.getStyleClass().add("info-panel-scroll");
        VBox.setVgrow(contentScroll, Priority.ALWAYS);

        completeButton = new Button("完成");
        completeButton.getStyleClass().add("info-panel-primary-button");
        completeButton.setMaxWidth(Double.MAX_VALUE);
        completeButton.setGraphic(controller.createSvgIcon("/icons/macaron_info-complete_icon.svg", null, 16));
        completeButton.setGraphicTextGap(8);
        completeButton.setContentDisplay(ContentDisplay.LEFT);
        completeButton.setOnAction(event -> toggleComplete());

        HBox footer = new HBox(completeButton);
        footer.getStyleClass().add("info-panel-footer");
        HBox.setHgrow(completeButton, Priority.ALWAYS);

        root.getChildren().addAll(header, contentScroll, footer);

        applyEmptyState();
    }

    public Node getView() {
        return root;
    }

    public void showWithAnimation() {
        root.setManaged(true);
        root.setVisible(true);
        stopPanelTransition();

        FadeTransition fadeIn = new FadeTransition(Duration.millis(300), root);
        fadeIn.setFromValue(root.getOpacity());
        fadeIn.setToValue(1);

        TranslateTransition slideIn = new TranslateTransition(Duration.millis(300), root);
        slideIn.setFromX(root.getTranslateX());
        slideIn.setToX(0);

        panelTransition = new ParallelTransition(fadeIn, slideIn);
        panelTransition.setOnFinished(event -> panelVisible = true);
        panelTransition.play();
    }

    public void hideWithAnimation() {
        if (!panelVisible && !root.isVisible()) {
            return;
        }

        stopPanelTransition();

        FadeTransition fadeOut = new FadeTransition(Duration.millis(300), root);
        fadeOut.setFromValue(root.getOpacity());
        fadeOut.setToValue(0);

        TranslateTransition slideOut = new TranslateTransition(Duration.millis(300), root);
        slideOut.setFromX(root.getTranslateX());
        slideOut.setToX(24);

        panelTransition = new ParallelTransition(fadeOut, slideOut);
        panelTransition.setOnFinished(event -> hideImmediately());
        panelTransition.play();
    }

    public void hideImmediately() {
        stopPanelTransition();
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
        currentSchedule = schedule;
        updateDisplay();
    }

    private void updateDisplay() {
        if (Platform.isFxApplicationThread()) {
            updateDisplayNow();
            return;
        }
        Platform.runLater(this::updateDisplayNow);
    }

    private void updateDisplayNow() {
        if (currentSchedule == null) {
            applyEmptyState();
            return;
        }

        setButtonsEnabled(true);
        titleLabel.setText(normalizeText(currentSchedule.getName(), "未命名日程"));
        titleLabel.getStyleClass().remove("info-panel-empty-title");

        updateStatusDisplay();

        DatePresentation datePresentation = buildDatePresentation(
            currentSchedule.getStartDate(),
            currentSchedule.getDueDate()
        );
        datePrimaryLabel.setText(datePresentation.getPrimaryText());
        dateSecondaryLabel.setText(datePresentation.getSecondaryText());
        setNodeVisible(dateSecondaryLabel, !datePresentation.getSecondaryText().isEmpty());
        setNodeVisible(dateRow, true);

        updatePriorityDisplay(currentSchedule.getPriority());
        setNodeVisible(priorityRow, true);

        updateChipDisplay(currentSchedule.getCategory(), currentSchedule.getTags());

        String reminderText = formatReminderText(currentSchedule.getReminderTime());
        reminderValueLabel.setText(reminderText);
        setNodeVisible(reminderRow, !reminderText.isEmpty());

        updateDescriptionDisplay(currentSchedule.getDescription());
        setNodeVisible(descriptionSeparator, true);
        setNodeVisible(descriptionSection, true);
    }

    private void applyEmptyState() {
        titleLabel.setText(EMPTY_TITLE_TEXT);
        if (!titleLabel.getStyleClass().contains("info-panel-empty-title")) {
            titleLabel.getStyleClass().add("info-panel-empty-title");
        }

        statusLabel.setText("");
        setNodeVisible(statusLabel, false);
        setNodeVisible(dateRow, false);
        setNodeVisible(priorityRow, false);
        setNodeVisible(chipRow, false);
        setNodeVisible(reminderRow, false);
        setNodeVisible(descriptionSeparator, false);
        setNodeVisible(descriptionSection, false);

        datePrimaryLabel.setText(EMPTY_DATE_TEXT);
        dateSecondaryLabel.setText("");
        priorityValueLabel.setText("");
        reminderValueLabel.setText("");
        chipPane.getChildren().clear();
        updateDescriptionDisplay("");
        updateCompleteButton(false);
        setButtonsEnabled(false);
    }

    private void updateStatusDisplay() {
        statusLabel.getStyleClass().removeAll(STATUS_VARIANT_CLASSES);

        String statusText;
        String statusClass;

        if (currentSchedule.isCompleted()) {
            statusText = "已完成";
            statusClass = "info-panel-status-completed";
        } else if (currentSchedule.isOverdue()) {
            statusText = "已过期";
            statusClass = "info-panel-status-overdue";
        } else if (currentSchedule.isUpcoming()) {
            statusText = "即将到期";
            statusClass = "info-panel-status-upcoming";
        } else {
            statusText = "进行中";
            statusClass = "info-panel-status-ongoing";
        }

        statusLabel.setText(statusText);
        statusLabel.getStyleClass().add(statusClass);
        setNodeVisible(statusLabel, true);
        updateCompleteButton(currentSchedule.isCompleted());
    }

    private void updatePriorityDisplay(String priority) {
        String priorityText = normalizeText(priority, "中");
        priorityValueLabel.setText(priorityText);
        priorityValueLabel.getStyleClass().removeAll(PRIORITY_VARIANT_CLASSES);
        priorityValueLabel.getStyleClass().add("info-panel-priority-" + getPriorityClass(priorityText));
    }

    private void updateChipDisplay(String category, String rawTags) {
        chipPane.getChildren().clear();

        if (shouldShowCategoryChip(category)) {
            chipPane.getChildren().add(createChipLabel(normalizeText(category, ""), "info-panel-chip-category"));
        }

        for (String tag : splitTagChips(rawTags)) {
            chipPane.getChildren().add(createChipLabel(tag, "info-panel-chip-tag"));
        }

        setNodeVisible(chipRow, !chipPane.getChildren().isEmpty());
    }

    private void updateDescriptionDisplay(String description) {
        String normalizedDescription = normalizeText(description, "");
        boolean hasDescription = !normalizedDescription.isEmpty();

        descriptionArea.setText(hasDescription ? normalizedDescription : EMPTY_DESCRIPTION_HINT);
        if (hasDescription) {
            descriptionArea.getStyleClass().remove("info-panel-description-placeholder");
        } else if (!descriptionArea.getStyleClass().contains("info-panel-description-placeholder")) {
            descriptionArea.getStyleClass().add("info-panel-description-placeholder");
        }
    }

    private void updateCompleteButton(boolean completed) {
        completeButton.setText(completed ? "标记未完成" : "完成");
    }

    private void setButtonsEnabled(boolean enabled) {
        editButton.setDisable(!enabled);
        deleteButton.setDisable(!enabled);
        completeButton.setDisable(!enabled);
    }

    private void toggleComplete() {
        if (currentSchedule == null) {
            return;
        }

        controller.updateScheduleCompletion(currentSchedule, !currentSchedule.isCompleted());
    }

    private void deleteSchedule() {
        if (currentSchedule == null) {
            return;
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("确认删除");
        alert.setHeaderText("删除日程");
        alert.setContentText("确定要删除日程 \"" + currentSchedule.getName() + "\" 吗？此操作不可恢复。");

        alert.showAndWait().ifPresent(response -> {
            if (response != ButtonType.OK) {
                return;
            }

            try {
                scheduleDAO.deleteSchedule(currentSchedule.getId());
                currentSchedule = null;
                applyEmptyState();
                controller.refreshAllViews();
            } catch (SQLException exception) {
                controller.showError("删除失败", exception.getMessage());
            }
        });
    }

    public void refresh() {
        if (currentSchedule == null) {
            return;
        }

        try {
            Schedule updated = scheduleDAO.getScheduleById(currentSchedule.getId());
            if (updated != null) {
                setSchedule(updated);
            } else {
                currentSchedule = null;
                updateDisplay();
            }
        } catch (SQLException exception) {
            controller.showError("刷新失败", exception.getMessage());
        }
    }

    @Override
    public void applyCompletionMutation(ScheduleCompletionMutation mutation) {
        if (currentSchedule == null || mutation == null || !mutation.matches(currentSchedule)) {
            return;
        }
        mutation.applyTo(currentSchedule);
        updateDisplay();
    }

    @Override
    public void confirmCompletionMutation(ScheduleCompletionMutation mutation) {
        if (currentSchedule == null || mutation == null || !mutation.matches(currentSchedule)) {
            return;
        }
        updateDisplay();
    }

    @Override
    public void revertCompletionMutation(ScheduleCompletionMutation mutation) {
        if (currentSchedule == null || mutation == null || !mutation.matches(currentSchedule)) {
            return;
        }
        mutation.revertOn(currentSchedule);
        updateDisplay();
    }

    static DatePresentation buildDatePresentation(LocalDate startDate, LocalDate dueDate) {
        LocalDate[] normalizedRange = normalizeDateRange(startDate, dueDate);
        if (normalizedRange == null) {
            return new DatePresentation(EMPTY_DATE_TEXT, "");
        }

        LocalDate normalizedStartDate = normalizedRange[0];
        LocalDate normalizedEndDate = normalizedRange[1];

        if (normalizedStartDate.equals(normalizedEndDate)) {
            return new DatePresentation(
                MONTH_DAY_FORMATTER.format(normalizedStartDate),
                YEAR_FORMATTER.format(normalizedStartDate)
            );
        }

        String primaryText = MONTH_DAY_FORMATTER.format(normalizedStartDate)
            + " - "
            + MONTH_DAY_FORMATTER.format(normalizedEndDate);
        String secondaryText = normalizedStartDate.getYear() == normalizedEndDate.getYear()
            ? YEAR_FORMATTER.format(normalizedStartDate)
            : YEAR_FORMATTER.format(normalizedStartDate) + " - " + YEAR_FORMATTER.format(normalizedEndDate);
        return new DatePresentation(primaryText, secondaryText);
    }

    static List<String> splitTagChips(String rawTags) {
        if (rawTags == null || rawTags.isBlank()) {
            return Collections.emptyList();
        }

        LinkedHashSet<String> tags = new LinkedHashSet<>();
        for (String piece : rawTags.split("[,，]")) {
            String normalized = normalizeText(piece, "");
            if (normalized.isEmpty() || DEFAULT_TAG.equals(normalized)) {
                continue;
            }
            tags.add(normalized);
        }
        return new ArrayList<>(tags);
    }

    static boolean shouldShowCategoryChip(String category) {
        String normalized = normalizeText(category, "");
        return !normalized.isEmpty() && !DEFAULT_CATEGORY.equals(normalized);
    }

    private static LocalDate[] normalizeDateRange(LocalDate startDate, LocalDate dueDate) {
        LocalDate normalizedStartDate = startDate != null ? startDate : dueDate;
        LocalDate normalizedEndDate = dueDate != null ? dueDate : startDate;
        if (normalizedStartDate == null || normalizedEndDate == null) {
            return null;
        }
        if (normalizedStartDate.isAfter(normalizedEndDate)) {
            LocalDate temp = normalizedStartDate;
            normalizedStartDate = normalizedEndDate;
            normalizedEndDate = temp;
        }
        return new LocalDate[] { normalizedStartDate, normalizedEndDate };
    }

    private static String formatReminderText(LocalDateTime reminderTime) {
        if (reminderTime == null) {
            return "";
        }
        return REMINDER_FORMATTER.format(reminderTime);
    }

    private static String normalizeText(String value, String fallback) {
        if (value == null) {
            return fallback;
        }
        String normalized = value.strip();
        return normalized.isEmpty() ? fallback : normalized;
    }

    private static String getPriorityClass(String priority) {
        if ("高".equals(priority)) {
            return "high";
        }
        if ("低".equals(priority)) {
            return "low";
        }
        return "medium";
    }

    private HBox createMetaRow(String iconPath, String iconTitle, Node content) {
        HBox row = new HBox(12, createMetaIcon(iconPath, iconTitle, 18), content);
        row.setAlignment(Pos.TOP_LEFT);
        row.getStyleClass().add("info-panel-meta-row");
        return row;
    }

    private StackPane createMetaIcon(String iconPath, String iconTitle, double size) {
        StackPane iconWrap = new StackPane(controller.createSvgIcon(iconPath, iconTitle, size));
        iconWrap.setMinSize(24, 24);
        iconWrap.setPrefSize(24, 24);
        iconWrap.setMaxSize(24, 24);
        iconWrap.getStyleClass().add("info-panel-meta-icon-wrap");
        return iconWrap;
    }

    private Label createSectionTitle(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("info-panel-section-title");
        return label;
    }

    private Label createChipLabel(String text, String extraStyleClass) {
        Label chip = new Label(text);
        chip.getStyleClass().addAll("info-panel-chip", extraStyleClass);
        return chip;
    }

    private Button createToolbarButton(String iconPath, String tooltipText, Runnable action) {
        Button button = new Button();
        button.getStyleClass().add("info-panel-toolbar-button");
        button.setGraphic(controller.createSvgIcon(iconPath, tooltipText, 16));
        button.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        button.setOnAction(event -> action.run());
        return button;
    }

    private void setNodeVisible(Node node, boolean visible) {
        node.setVisible(visible);
        node.setManaged(visible);
    }

    private void stopPanelTransition() {
        if (panelTransition != null) {
            panelTransition.stop();
            panelTransition = null;
        }
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
}
