package com.example.controller;

import java.sql.SQLException;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.example.model.Schedule;
import com.example.view.FlowchartView;
import com.example.view.HeatmapView;
import com.example.view.InfoPanelView;
import com.example.view.ScheduleDialog;
import com.example.view.ScheduleListView;
import com.example.view.TimelineView;
import com.example.view.View;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.Bounds;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Separator;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.util.Duration;

public class MainController {
    
    private BorderPane root;
    private Scene scene;
    
    // 各个视图
    private ScheduleListView scheduleListView;
    private TimelineView timelineView;
    private HeatmapView heatmapView;
    private FlowchartView flowchartView;
    private InfoPanelView infoPanelView;
    private StackPane infoPanelHost;
    private Timeline infoPanelAnimation;
    
    // 当前选中的视图
    private View currentView;
    private Schedule selectedSchedule;
    
    // 主题管理
    private String currentTheme = "light";
    private String importedThemePath;
    private static final double INFO_PANEL_EXPANDED_WIDTH = 320;
    private boolean infoPanelVisible = false;
    
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
        VBox sidebar = new VBox(8);
        sidebar.getStyleClass().add("sidebar");
        sidebar.setPrefWidth(170);
        
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

        Region spacer = new Region();
        VBox.setVgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

        Button collapseToggle = new Button("⚙ 功能菜单 ▼");
        collapseToggle.getStyleClass().addAll("button-secondary", "sidebar-footer-toggle");
        collapseToggle.setMaxWidth(Double.MAX_VALUE);

        VBox actionPanel = new VBox(6);
        actionPanel.getStyleClass().add("sidebar-action-panel");

        Button newScheduleAction = createSidebarActionButton("📝 新建日程", this::openNewScheduleDialog);
        Button themeAction = createSidebarActionButton("🎨 主题", () -> {});
        Button loginAction = createSidebarActionButton("👤 登录", this::showLoginDialog);
        Button settingsAction = createSidebarActionButton("⚙ 设置", this::showSettingsDialog);
        Button aboutAction = createSidebarActionButton("ℹ 关于", this::showAboutDialog);
        Button exitAction = createSidebarActionButton("⏻ 退出", Platform::exit);
        themeAction.setOnMouseClicked(e -> showThemeMenu(themeAction));

        actionPanel.getChildren().addAll(
            newScheduleAction,
            themeAction,
            loginAction,
            settingsAction,
            aboutAction,
            exitAction
        );

        collapseToggle.setOnAction(e -> {
            boolean nextVisible = !actionPanel.isVisible();
            actionPanel.setVisible(nextVisible);
            actionPanel.setManaged(nextVisible);
            collapseToggle.setText(nextVisible ? "⚙ 功能菜单 ▼" : "⚙ 功能菜单 ▲");
        });
        
        sidebar.getChildren().addAll(
            searchField,
            new Separator(),
            scheduleBtn,
            timelineBtn,
            heatmapBtn,
            flowchartBtn,
            spacer,
            new Separator(),
            collapseToggle,
            actionPanel
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

    private Button createSidebarActionButton(String text, Runnable action) {
        Button button = new Button(text);
        button.getStyleClass().add("sidebar-action-button");
        button.setMaxWidth(Double.MAX_VALUE);
        button.setWrapText(true);
        button.setOnAction(e -> action.run());
        return button;
    }
    
    private void createInfoPanel() {
        infoPanelView = new InfoPanelView(this);
        infoPanelHost = new StackPane(infoPanelView.getView());
        infoPanelHost.getStyleClass().add("info-panel-host");
        infoPanelHost.setPrefWidth(0);
        infoPanelHost.setMinWidth(0);
        infoPanelHost.setMaxWidth(INFO_PANEL_EXPANDED_WIDTH);
        infoPanelView.getView().setVisible(false);
        infoPanelView.getView().setManaged(false);
        infoPanelView.getView().setOpacity(0);
        infoPanelView.getView().setTranslateX(30);
        root.setRight(infoPanelHost);
    }
    
    private void showView(View view) {
        currentView = view;
        root.setCenter(view.getView());
        view.refresh();
    }
    
    public void showScheduleDetails(Schedule schedule) {
        this.selectedSchedule = schedule;
        infoPanelView.setSchedule(schedule);
        showInfoPanelAnimated();
        if (currentView != null) {
            currentView.refresh();
        }
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

    public void hideScheduleDetailsPanel() {
        hideInfoPanelAnimated();
    }
    
    private void switchTheme(String theme) {
        applyBuiltInTheme(theme);
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
        scene.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.ESCAPE) {
                hideInfoPanelAnimated();
            }
        });
        scene.addEventFilter(MouseEvent.MOUSE_PRESSED, event -> {
            if (!infoPanelVisible || infoPanelHost == null) {
                return;
            }
            Bounds bounds = infoPanelHost.localToScene(infoPanelHost.getBoundsInLocal());
            if (bounds == null || !bounds.contains(event.getSceneX(), event.getSceneY())) {
                hideInfoPanelAnimated();
            }
        });
    }
    
    public void initialize() {
        // 初始化数据
        refreshAllViews();
    }
    
    public void shutdown() {
        // 清理资源
    }

    private void showInfoPanelAnimated() {
        if (infoPanelHost == null) {
            return;
        }
        if (infoPanelVisible && infoPanelHost.getPrefWidth() >= INFO_PANEL_EXPANDED_WIDTH - 1) {
            return;
        }
        if (infoPanelAnimation != null) {
            infoPanelAnimation.stop();
        }
        infoPanelVisible = true;
        infoPanelView.getView().setVisible(true);
        infoPanelView.getView().setManaged(true);
        infoPanelAnimation = new Timeline(
            new KeyFrame(Duration.ZERO,
                new KeyValue(infoPanelHost.prefWidthProperty(), infoPanelHost.getPrefWidth()),
                new KeyValue(infoPanelView.getView().opacityProperty(), infoPanelView.getView().getOpacity()),
                new KeyValue(infoPanelView.getView().translateXProperty(), infoPanelView.getView().getTranslateX())
            ),
            new KeyFrame(Duration.millis(300),
                new KeyValue(infoPanelHost.prefWidthProperty(), INFO_PANEL_EXPANDED_WIDTH),
                new KeyValue(infoPanelView.getView().opacityProperty(), 1.0),
                new KeyValue(infoPanelView.getView().translateXProperty(), 0)
            )
        );
        infoPanelAnimation.play();
    }

    private void hideInfoPanelAnimated() {
        if (infoPanelHost == null || !infoPanelVisible) {
            return;
        }
        if (infoPanelAnimation != null) {
            infoPanelAnimation.stop();
        }
        infoPanelVisible = false;
        infoPanelAnimation = new Timeline(
            new KeyFrame(Duration.ZERO,
                new KeyValue(infoPanelHost.prefWidthProperty(), infoPanelHost.getPrefWidth()),
                new KeyValue(infoPanelView.getView().opacityProperty(), infoPanelView.getView().getOpacity()),
                new KeyValue(infoPanelView.getView().translateXProperty(), infoPanelView.getView().getTranslateX())
            ),
            new KeyFrame(Duration.millis(300),
                new KeyValue(infoPanelHost.prefWidthProperty(), 0),
                new KeyValue(infoPanelView.getView().opacityProperty(), 0),
                new KeyValue(infoPanelView.getView().translateXProperty(), 30)
            )
        );
        infoPanelAnimation.setOnFinished(e -> {
            infoPanelView.getView().setVisible(false);
            infoPanelView.getView().setManaged(false);
        });
        infoPanelAnimation.play();
    }

    private void showThemeMenu(Button owner) {
        ContextMenu menu = new ContextMenu();

        MenuItem lightItem = new MenuItem("浅色主题");
        lightItem.setOnAction(e -> applyBuiltInTheme("light"));

        MenuItem darkItem = new MenuItem("深色主题");
        darkItem.setOnAction(e -> applyBuiltInTheme("dark"));

        MenuItem mintItem = new MenuItem("薄荷主题");
        mintItem.setOnAction(e -> applyBuiltInTheme("mint"));

        MenuItem sunsetItem = new MenuItem("日落主题");
        sunsetItem.setOnAction(e -> applyBuiltInTheme("sunset"));

        MenuItem oceanItem = new MenuItem("海洋主题");
        oceanItem.setOnAction(e -> applyBuiltInTheme("ocean"));

        MenuItem importItem = new MenuItem("导入主题...");
        importItem.setOnAction(e -> importThemeFromFile());

        menu.getItems().addAll(lightItem, darkItem, mintItem, sunsetItem, oceanItem, new SeparatorMenuItem(), importItem);
        menu.show(owner, javafx.geometry.Side.TOP, 0, 0);
    }

    private void applyBuiltInTheme(String theme) {
        if (scene == null) {
            currentTheme = theme;
            return;
        }
        importedThemePath = null;
        List<String> styles = new ArrayList<>();
        if ("dark".equals(theme)) {
            styles.add("/styles/dark-theme.css");
        } else {
            styles.add("/styles/light-theme.css");
            if ("mint".equals(theme)) {
                styles.add("/styles/mint-theme.css");
            } else if ("sunset".equals(theme)) {
                styles.add("/styles/sunset-theme.css");
            } else if ("ocean".equals(theme)) {
                styles.add("/styles/ocean-theme.css");
            }
        }
        applyThemeStylesheets(styles);
        currentTheme = theme;
        refreshAllViews();
    }

    private void importThemeFromFile() {
        if (scene == null) {
            return;
        }
        FileChooser chooser = new FileChooser();
        chooser.setTitle("导入主题CSS");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSS 文件", "*.css"));
        File selected = chooser.showOpenDialog(scene.getWindow());
        if (selected == null) {
            return;
        }
        importedThemePath = selected.toURI().toString();
        List<String> styles = new ArrayList<>();
        styles.add("/styles/light-theme.css");
        styles.add(importedThemePath);
        applyThemeStylesheets(styles);
        currentTheme = "custom";
        refreshAllViews();
        showInfo("主题导入成功", "已应用外部主题: " + selected.getName());
    }

    private void applyThemeStylesheets(List<String> stylePaths) {
        if (scene == null) {
            return;
        }
        scene.getStylesheets().clear();
        for (String path : stylePaths) {
            if (path == null || path.isEmpty()) {
                continue;
            }
            if (path.startsWith("/")) {
                scene.getStylesheets().add(getClass().getResource(path).toExternalForm());
            } else {
                scene.getStylesheets().add(path);
            }
        }
    }
}
