package com.example.controller;

import java.sql.SQLException;
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
import javafx.scene.control.Alert;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.control.Separator;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;

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
    
    // 主题管理
    private String currentTheme = "light";
    
    public MainController() {
        initializeUI();
    }
    
    private void initializeUI() {
        root = new BorderPane();
        root.getStyleClass().add("root");
        
        // 创建顶部菜单栏
        createMenuBar();
        
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
    
    private void createMenuBar() {
        MenuBar menuBar = new MenuBar();
        menuBar.getStyleClass().add("menu-bar");
        
        // 文件菜单
        Menu fileMenu = new Menu("文件");
        MenuItem newScheduleItem = new MenuItem("新建日程 (Ctrl+N)");
        newScheduleItem.setOnAction(e -> openNewScheduleDialog());
        
        MenuItem exitItem = new MenuItem("退出");
        exitItem.setOnAction(e -> Platform.exit());
        
        fileMenu.getItems().addAll(newScheduleItem, new SeparatorMenuItem(), exitItem);
        
        // 视图菜单 - CSS风格选择
        Menu viewMenu = new Menu("选择CSS风格");
        
        RadioMenuItem lightThemeItem = new RadioMenuItem("浅色主题");
        lightThemeItem.setSelected(true);
        lightThemeItem.setOnAction(e -> switchTheme("light"));
        
        RadioMenuItem darkThemeItem = new RadioMenuItem("深色主题");
        darkThemeItem.setOnAction(e -> switchTheme("dark"));
        
        ToggleGroup themeGroup = new ToggleGroup();
        lightThemeItem.setToggleGroup(themeGroup);
        darkThemeItem.setToggleGroup(themeGroup);
        
        viewMenu.getItems().addAll(lightThemeItem, darkThemeItem);
        
        // 用户菜单
        Menu userMenu = new Menu("用户");
        MenuItem loginItem = new MenuItem("登录");
        loginItem.setOnAction(e -> showLoginDialog());
        
        MenuItem settingsItem = new MenuItem("设置");
        settingsItem.setOnAction(e -> showSettingsDialog());
        
        userMenu.getItems().addAll(loginItem, new SeparatorMenuItem(), settingsItem);
        
        // 帮助菜单
        Menu helpMenu = new Menu("帮助");
        MenuItem aboutItem = new MenuItem("关于");
        aboutItem.setOnAction(e -> showAboutDialog());
        
        helpMenu.getItems().add(aboutItem);
        
        menuBar.getMenus().addAll(fileMenu, viewMenu, userMenu, helpMenu);
        root.setTop(menuBar);
    }
    
    private void createSidebar() {
        VBox sidebar = new VBox(5);
        sidebar.getStyleClass().add("sidebar");
        sidebar.setPrefWidth(140);
        
        // 搜索框
        TextField searchField = new TextField();
        searchField.getStyleClass().add("search-field");
        searchField.setPromptText("搜索日程...");
        searchField.setOnAction(e -> performSearch(searchField.getText()));
        
        // 导航按钮
        ToggleGroup navGroup = new ToggleGroup();
        
        ToggleButton scheduleBtn = createNavButton("日程管理", navGroup);
        scheduleBtn.setSelected(true);
        scheduleBtn.setOnAction(e -> showView(scheduleListView));
        
        ToggleButton timelineBtn = createNavButton("日程时间轴", navGroup);
        timelineBtn.setOnAction(e -> showView(timelineView));
        
        ToggleButton heatmapBtn = createNavButton("日程热力图", navGroup);
        heatmapBtn.setOnAction(e -> showView(heatmapView));
        
        ToggleButton flowchartBtn = createNavButton("日程流程图", navGroup);
        flowchartBtn.setOnAction(e -> showView(flowchartView));
        
        sidebar.getChildren().addAll(
            searchField,
            new Separator(),
            scheduleBtn,
            timelineBtn,
            heatmapBtn,
            flowchartBtn
        );
        
        root.setLeft(sidebar);
    }
    
    private ToggleButton createNavButton(String text, ToggleGroup group) {
        ToggleButton button = new ToggleButton(text);
        button.getStyleClass().add("nav-button");
        button.setToggleGroup(group);
        button.setMaxWidth(Double.MAX_VALUE);
        return button;
    }
    
    private void createInfoPanel() {
        infoPanelView = new InfoPanelView(this);
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
        if (scene == null) return;
        
        // 移除当前主题
        scene.getStylesheets().clear();
        
        // 添加新主题
        String themePath = "/styles/" + theme + "-theme.css";
        scene.getStylesheets().add(getClass().getResource(themePath).toExternalForm());
        
        currentTheme = theme;
        refreshAllViews();
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
        alert.setHeaderText("应用设置");
        alert.setContentText("设置功能开发中...");
        alert.showAndWait();
    }
    
    private void showAboutDialog() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("关于");
        alert.setHeaderText("ToDo 日程管理");
        alert.setContentText("版本: 1.0\n一个功能完整、界面美观的JavaFX日程管理应用");
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
    }
    
    public void initialize() {
        // 初始化数据
        refreshAllViews();
    }
    
    public void shutdown() {
        // 清理资源
    }
}
