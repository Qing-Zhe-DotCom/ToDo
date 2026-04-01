package com.example.controller;

import java.sql.SQLException;
import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.example.model.Schedule;
import com.example.view.FlowchartView;
import com.example.view.HeatmapView;
import com.example.view.InfoPanelView;
import com.example.view.ScheduleDialog;
import com.example.view.ScheduleListView;
import com.example.view.TimelineView;
import com.example.view.View;

import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.Node;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.Labeled;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Separator;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.geometry.Side;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

public class MainController {
    
    private BorderPane root;
    private Scene scene;
    
    // 各个视图
    private ScheduleListView scheduleListView;
    private TimelineView timelineView;
    private HeatmapView heatmapView;
    private FlowchartView flowchartView;
    private InfoPanelView infoPanelView;
    
    // 当前选中的视图
    private View currentView;
    private Schedule selectedSchedule;

    private VBox sidebar;
    private VBox bottomActions;
    private Separator bottomActionsSeparator;
    private TextField searchField;
    private ToggleButton collapseToggle;
    private ToggleButton featurePanelToggle;
    private boolean sidebarCollapsed = false;
    private boolean featurePanelExpanded = false;
    private final Map<Labeled, String[]> collapsibleLabels = new LinkedHashMap<>();
    private String importedThemeStylesheet;
    private final Map<String, String> builtinThemes = createBuiltinThemeMap();
    
    // 主题管理
    private String currentTheme = "light";
    
    public MainController() {
        initializeUI();
    }
    
    private void initializeUI() {
        root = new BorderPane();
        root.getStyleClass().add("root");

        // 创建左侧导航栏
        createSidebar();
        
        // 创建右侧信息面板
        createInfoPanel();
        
        // 初始化各个视图
        scheduleListView = new ScheduleListView(this);
        timelineView = new TimelineView(this);
        heatmapView = new HeatmapView(this);
        flowchartView = new FlowchartView(this);
        //infoPanelView = new InfoPanelView(this);
        
        // 默认显示日程列表视图
        showView(scheduleListView);
    }

    private void createSidebar() {
        sidebar = new VBox(8);
        sidebar.getStyleClass().add("sidebar");
        sidebar.setPrefWidth(220);

        collapseToggle = new ToggleButton("« 收起");
        collapseToggle.getStyleClass().addAll("nav-button", "sidebar-collapse-button");
        collapseToggle.setMaxWidth(Double.MAX_VALUE);
        collapseToggle.setOnAction(e -> {
            sidebarCollapsed = collapseToggle.isSelected();
            updateSidebarCollapseState();
        });

        searchField = new TextField();
        searchField.getStyleClass().add("search-field");
        searchField.setPromptText("搜索日程...");
        searchField.setOnAction(e -> performSearch(searchField.getText()));

        // 导航按钮
        ToggleGroup navGroup = new ToggleGroup();

        ToggleButton scheduleBtn = createNavButton("📋", "日程管理", navGroup);
        scheduleBtn.setSelected(true);
        scheduleBtn.setOnAction(e -> showView(scheduleListView));

        ToggleButton timelineBtn = createNavButton("🕒", "日程时间轴", navGroup);
        timelineBtn.setOnAction(e -> showView(timelineView));

        ToggleButton heatmapBtn = createNavButton("🔥", "日程热力图", navGroup);
        heatmapBtn.setOnAction(e -> showView(heatmapView));

        ToggleButton flowchartBtn = createNavButton("🧭", "日程流程图", navGroup);
        flowchartBtn.setOnAction(e -> showView(flowchartView));

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        Label functionTitle = new Label("侧边功能栏");
        functionTitle.getStyleClass().add("label-hint");

        Button newScheduleButton = createActionButton("➕", "新建日程", this::openNewScheduleDialog);
        Button themeButton = createActionButton("🎨", "主题", () -> {});
        themeButton.setOnAction(e -> showThemeMenu(themeButton));
        Button loginButton = createActionButton("👤", "登录", this::showLoginDialog);
        Button settingsButton = createActionButton("⚙️", "设置", this::showSettingsDialog);
        Button exitButton = createActionButton("⏻", "退出", Platform::exit);

        bottomActions = new VBox(6);
        bottomActions.getStyleClass().add("sidebar-bottom-actions");
        bottomActions.getChildren().addAll(
            functionTitle,
            newScheduleButton,
            themeButton,
            loginButton,
            settingsButton,
            exitButton
        );

        featurePanelToggle = new ToggleButton("▸");
        featurePanelToggle.getStyleClass().addAll("nav-button", "sidebar-feature-toggle");
        featurePanelToggle.setMaxWidth(Double.MAX_VALUE);
        featurePanelToggle.setOnAction(e -> {
            featurePanelExpanded = featurePanelToggle.isSelected();
            updateFeaturePanelState();
        });

        bottomActionsSeparator = new Separator();

        sidebar.getChildren().addAll(
            collapseToggle,
            searchField,
            new Separator(),
            scheduleBtn,
            timelineBtn,
            heatmapBtn,
            flowchartBtn,
            spacer,
            bottomActionsSeparator,
            bottomActions,
            featurePanelToggle
        );

        updateSidebarCollapseState();
        root.setLeft(sidebar);
    }

    private ToggleButton createNavButton(String icon, String text, ToggleGroup group) {
        String expandedText = icon + "  " + text;
        ToggleButton button = new ToggleButton(expandedText);
        button.getStyleClass().add("nav-button");
        button.setToggleGroup(group);
        button.setMaxWidth(Double.MAX_VALUE);
        registerCollapsibleControl(button, expandedText, icon, text);
        return button;
    }

    private Button createActionButton(String icon, String text, Runnable action) {
        String expandedText = icon + "  " + text;
        Button button = new Button(expandedText);
        button.getStyleClass().addAll("nav-button", "sidebar-action-button");
        button.setMaxWidth(Double.MAX_VALUE);
        button.setOnAction(e -> action.run());
        registerCollapsibleControl(button, expandedText, icon, text);
        return button;
    }

    private void registerCollapsibleControl(Labeled control, String expandedText, String collapsedText, String tooltipText) {
        collapsibleLabels.put(control, new String[] { expandedText, collapsedText });
        control.setTooltip(new Tooltip(tooltipText));
    }

    private void updateSidebarCollapseState() {
        if (sidebar == null) return;

        double collapsedWidth = 52;
        double expandedWidth = 220;
        sidebar.setPrefWidth(sidebarCollapsed ? collapsedWidth : expandedWidth);
        sidebar.setMinWidth(sidebarCollapsed ? collapsedWidth : expandedWidth);
        sidebar.setMaxWidth(sidebarCollapsed ? collapsedWidth : expandedWidth);
        sidebar.setPadding(sidebarCollapsed ? new Insets(6) : new Insets(10));
        sidebar.setSpacing(sidebarCollapsed ? 6 : 8);
        sidebar.setAlignment(sidebarCollapsed ? Pos.TOP_CENTER : Pos.TOP_LEFT);

        if (collapseToggle != null) {
            collapseToggle.setText(sidebarCollapsed ? "»" : "« 收起");
        }

        if (searchField != null) {
            searchField.setVisible(!sidebarCollapsed);
            searchField.setManaged(!sidebarCollapsed);
        }

        for (Map.Entry<Labeled, String[]> entry : collapsibleLabels.entrySet()) {
            Labeled control = entry.getKey();
            String[] labels = entry.getValue();
            control.setText(sidebarCollapsed ? labels[1] : labels[0]);
            control.setAlignment(sidebarCollapsed ? Pos.CENTER : Pos.CENTER_LEFT);
            control.setPadding(sidebarCollapsed ? new Insets(4, 0, 4, 0) : new Insets(12, 15, 12, 15));
            if (sidebarCollapsed) {
                control.setMinSize(30, 30);
                control.setPrefSize(30, 30);
            } else {
                control.setMinWidth(0);
                control.setPrefHeight(36);
            }
        }

        updateFeaturePanelState();
    }

    private void updateFeaturePanelState() {
        if (featurePanelToggle != null) {
            featurePanelToggle.setText(featurePanelExpanded ? "▾" : "▸");
            featurePanelToggle.setAlignment(sidebarCollapsed ? Pos.CENTER : Pos.CENTER_LEFT);
            featurePanelToggle.setPadding(sidebarCollapsed ? new Insets(4, 0, 4, 0) : new Insets(8, 12, 8, 12));
        }
        if (bottomActions != null) {
            bottomActions.setVisible(featurePanelExpanded);
            bottomActions.setManaged(featurePanelExpanded);
        }
        if (bottomActionsSeparator != null) {
            bottomActionsSeparator.setVisible(featurePanelExpanded);
            bottomActionsSeparator.setManaged(featurePanelExpanded);
        }
    }

    private void createInfoPanel() {
        infoPanelView = new InfoPanelView(this);
        infoPanelView.hideImmediately();
        root.setRight(infoPanelView.getView());
    }
    
    private void showView(View view) {
        currentView = view;
        root.setCenter(view.getView());
        view.refresh();
    }
    
    public void showScheduleDetails(Schedule schedule) {
        this.selectedSchedule = schedule;
        infoPanelView.setSchedule(schedule);
        infoPanelView.showWithAnimation();
    }

    public void closeScheduleDetails() {
        infoPanelView.hideWithAnimation();
    }

    public boolean isScheduleSelected(Schedule schedule) {
        if (selectedSchedule == null || schedule == null) {
            return false;
        }
        if (selectedSchedule.getId() > 0 && schedule.getId() > 0) {
            return selectedSchedule.getId() == schedule.getId();
        }
        return selectedSchedule == schedule;
    }
    
    public Schedule getSelectedSchedule() {
        return selectedSchedule;
    }
    
    public void refreshAllViews() {
        if (currentView != null) {
            currentView.refresh();
        }
        infoPanelView.refresh();
    }
    
    private void switchTheme(String theme) {
        importedThemeStylesheet = null;
        currentTheme = theme;
        applyThemeStylesheets(resolveBuiltinThemeStylesheets(theme));
    }

    private void applyThemeStylesheets(List<String> stylesheets) {
        if (scene == null || stylesheets.isEmpty()) {
            return;
        }
        scene.getStylesheets().clear();
        scene.getStylesheets().addAll(stylesheets);
        refreshAllViews();
    }

    private List<String> resolveBuiltinThemeStylesheets(String theme) {
        List<String> stylesheets = new ArrayList<>();
        if ("dark".equals(theme)) {
            stylesheets.add(resolveResourceStylesheet("/styles/dark-theme.css"));
            return stylesheets;
        }

        stylesheets.add(resolveResourceStylesheet("/styles/light-theme.css"));
        if ("mint".equals(theme)) {
            stylesheets.add(resolveResourceStylesheet("/styles/mint-theme.css"));
        } else if ("ocean".equals(theme)) {
            stylesheets.add(resolveResourceStylesheet("/styles/ocean-theme.css"));
        } else if ("sunset".equals(theme)) {
            stylesheets.add(resolveResourceStylesheet("/styles/sunset-theme.css"));
        }
        return stylesheets;
    }

    private String resolveResourceStylesheet(String resourcePath) {
        return getClass().getResource(resourcePath).toExternalForm();
    }

    private Map<String, String> createBuiltinThemeMap() {
        Map<String, String> themes = new LinkedHashMap<>();
        themes.put("light", "浅色");
        themes.put("dark", "深色");
        themes.put("mint", "薄荷");
        themes.put("ocean", "海洋");
        themes.put("sunset", "落日");
        return themes;
    }

    private void showThemeMenu(Button anchor) {
        ContextMenu menu = new ContextMenu();

        for (Map.Entry<String, String> entry : builtinThemes.entrySet()) {
            MenuItem item = new MenuItem("使用" + entry.getValue() + "主题");
            item.setOnAction(e -> switchTheme(entry.getKey()));
            menu.getItems().add(item);
        }

        menu.getItems().add(new SeparatorMenuItem());
        MenuItem importItem = new MenuItem("导入主题...");
        importItem.setOnAction(e -> importThemeFromFile());
        menu.getItems().add(importItem);

        menu.show(anchor, Side.RIGHT, 0, 0);
    }

    private void importThemeFromFile() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("导入主题文件");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSS 文件", "*.css"));
        File selected = chooser.showOpenDialog(root.getScene() != null ? root.getScene().getWindow() : null);
        if (selected == null) {
            return;
        }

        importedThemeStylesheet = selected.toURI().toString();
        currentTheme = "imported";
        applyThemeStylesheets(List.of(importedThemeStylesheet));
        showInfo("主题已导入", "已应用外部主题文件:\n" + selected.getAbsolutePath());
    }
    
    private void performSearch(String keyword) {
        if (keyword != null && !keyword.trim().isEmpty()) {
            scheduleListView.searchSchedules(keyword.trim());
            showView(scheduleListView);
        }
    }
    
    public void openNewScheduleDialog() {
        ScheduleDialog dialog = new ScheduleDialog(null, this);
        Optional<Schedule> result = dialog.showAndWait();
        
        result.ifPresent(schedule -> {
            try {
                scheduleListView.addSchedule(schedule);
                refreshAllViews();
            } catch (SQLException e) {
                showError("添加日程失败", e.getMessage());
            }
        });
    }
    
    public void openEditScheduleDialog(Schedule schedule) {
        ScheduleDialog dialog = new ScheduleDialog(schedule, this);
        Optional<Schedule> result = dialog.showAndWait();
        
        result.ifPresent(updatedSchedule -> {
            try {
                scheduleListView.updateSchedule(updatedSchedule);
                refreshAllViews();
            } catch (SQLException e) {
                showError("更新日程失败", e.getMessage());
            }
        });
    }
    
    private void showLoginDialog() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("登录");
        alert.setHeaderText("用户登录");
        alert.setContentText("登录功能开发中...");
        alert.showAndWait();
    }
    
    private void showSettingsDialog() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("设置");
        alert.setHeaderText("应用设置与关于");
        String themeDisplay = "imported".equals(currentTheme) ? "外部导入主题" : builtinThemes.getOrDefault(currentTheme, "浅色");
        alert.setContentText("主题: " + themeDisplay + "\n版本: 1.0\nToDo 日程管理应用");
        alert.showAndWait();
    }
    
    public void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("错误");
        alert.setHeaderText(title);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    public void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("提示");
        alert.setHeaderText(title);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    public BorderPane getRoot() {
        return root;
    }

    public String getCurrentTheme() {
        return currentTheme;
    }
    
    public void setScene(Scene scene) {
        this.scene = scene;
        setupGlobalInfoPanelInteractions();
    }

    private void setupGlobalInfoPanelInteractions() {
        if (scene == null) {
            return;
        }

        scene.addEventFilter(MouseEvent.MOUSE_PRESSED, event -> {
            if (!infoPanelView.isPanelVisible()) {
                return;
            }

            Object target = event.getTarget();
            if (target instanceof Node && isDescendant((Node) target, infoPanelView.getView())) {
                return;
            }

            closeScheduleDetails();
        });

        scene.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.ESCAPE && infoPanelView.isPanelVisible()) {
                closeScheduleDetails();
                event.consume();
            }
        });
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
    
    public void initialize() {
        // 初始化数据
        refreshAllViews();
    }
    
    public void shutdown() {
        // 清理资源
    }
}
