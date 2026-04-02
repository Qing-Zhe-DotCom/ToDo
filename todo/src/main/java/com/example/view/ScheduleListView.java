package com.example.view;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
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
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.SVGPath;
import javafx.scene.shape.Shape;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;
import javafx.scene.input.MouseEvent;
import javafx.scene.transform.Scale;
import javafx.scene.transform.Translate;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class ScheduleListView implements View {
    
    private MainController controller;
    private ScheduleDAO scheduleDAO;
    
    private VBox root;
    private ListView<Schedule> scheduleListView;
    private ObservableList<Schedule> schedules;
    
    private CheckBox showPastCheckbox;
    private ComboBox<String> sortComboBox;
    private ComboBox<String> filterComboBox;
    private static final String DONE_ICON_PATH = "/icons/macaron-logo-schedule-done.svg";
    private static final String PENDING_ICON_PATH = "/icons/macaron-logo-schedule-pending.svg";
    private static final String GROUP_ARROW_DOWN_ICON_PATH = "/icons/macaron_arrow-down_icon.svg";
    private static final String GROUP_ARROW_UP_ICON_PATH = "/icons/macaron_arrow-up_icon.svg";
    
    private boolean showingSearchResults = false;
    private String currentSearchKeyword = "";
    private boolean pendingCollapsed = false;
    private boolean completedCollapsed = false;
    
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
        
        // 标题和工具栏
        HBox toolbar = createToolbar();
        
        // 日程列表
        scheduleListView = new ListView<>();
        scheduleListView.getStyleClass().add("schedule-list");
        scheduleListView.setItems(schedules);
        scheduleListView.setCellFactory(lv -> new ScheduleListCell());
        
        // 选择事件
        scheduleListView.getSelectionModel().selectedItemProperty().addListener(
            (obs, oldVal, newVal) -> {
                if (newVal != null) {
                    controller.showScheduleDetails(newVal);
                }
            }
        );
        
        // 新建按钮
        Button newScheduleBtn = new Button("新建日程");
        newScheduleBtn.setGraphic(controller.createSvgIcon("/icons/macaron-logo-new-schedule.svg", null, 20));
        newScheduleBtn.getStyleClass().add("fab-button");
        newScheduleBtn.setOnAction(e -> controller.openNewScheduleDialog());
        
        HBox buttonBox = new HBox(newScheduleBtn);
        buttonBox.setAlignment(Pos.CENTER_RIGHT);
        buttonBox.setPadding(new Insets(10, 0, 0, 0));
        
        VBox.setVgrow(scheduleListView, Priority.ALWAYS);
        
        root.getChildren().addAll(toolbar, scheduleListView, buttonBox);
    }
    
    private HBox createToolbar() {
        HBox toolbar = new HBox(15);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        
        // 标题
        Label titleLabel = new Label("日程管理");
        titleLabel.getStyleClass().add("label-title");
        
        // 显示过去日程复选框
        showPastCheckbox = new CheckBox("显示过去日程");
        showPastCheckbox.getStyleClass().add("check-box");
        showPastCheckbox.setOnAction(e -> {
            if (!showingSearchResults) {
                loadSchedules();
            }
        });
        
        // 排序选择
        sortComboBox = new ComboBox<>();
        sortComboBox.getItems().addAll("按日期排序", "按优先级排序", "按分类排序");
        sortComboBox.setValue("按日期排序");
        sortComboBox.setOnAction(e -> {
            if (!showingSearchResults) {
                loadSchedules();
            }
        });
        
        // 筛选选择
        filterComboBox = new ComboBox<>();
        filterComboBox.getItems().addAll("全部", "未完成", "已完成", "高优先级", "即将到期");
        filterComboBox.setValue("全部");
        filterComboBox.setOnAction(e -> {
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
        Platform.runLater(() -> {
            try {
                List<Schedule> allSchedules = scheduleDAO.getAllSchedules();
                
                // 筛选
                List<Schedule> filtered = allSchedules.stream()
                    .filter(this::applyFilter)
                    .collect(Collectors.toList());
                
                // 排序
                filtered.sort(getComparator());
                
                schedules.setAll(filtered);
                
            } catch (SQLException e) {
                controller.showError("加载日程失败", e.getMessage());
            }
        });
    }
    
    private boolean applyFilter(Schedule schedule) {
        // 是否显示过去日程
        if (!showPastCheckbox.isSelected() && schedule.getDueDate() != null) {
            if (schedule.getDueDate().isBefore(LocalDate.now()) && !schedule.isCompleted()) {
                return false;
            }
        }
        
        // 默认只显示未来7天
        if (!showPastCheckbox.isSelected() && schedule.getDueDate() != null) {
            if (schedule.getDueDate().isAfter(LocalDate.now().plusDays(7))) {
                return false;
            }
        }
        
        // 筛选条件
        String filter = filterComboBox.getValue();
        if (filter == null) filter = "全部";
        
        if ("未完成".equals(filter)) return !schedule.isCompleted();
        if ("已完成".equals(filter)) return schedule.isCompleted();
        if ("高优先级".equals(filter)) return "高".equals(schedule.getPriority());
        if ("即将到期".equals(filter)) return schedule.isUpcoming();
        return true;
    }
    
    private Comparator<Schedule> getComparator() {
        String sort = sortComboBox.getValue();
        if (sort == null) sort = "按日期排序";
        
        if ("按日期排序".equals(sort)) {
            return Comparator
                .comparing((Schedule s) -> s.isCompleted())
                .thenComparing(Schedule::getDueDate, Comparator.nullsLast(Comparator.naturalOrder()));
        } else if ("按优先级排序".equals(sort)) {
            return Comparator
                .comparing((Schedule s) -> s.isCompleted())
                .thenComparing(Schedule::getPriorityValue, Comparator.reverseOrder());
        } else if ("按分类排序".equals(sort)) {
            return Comparator
                .comparing((Schedule s) -> s.isCompleted())
                .thenComparing(Schedule::getCategory, Comparator.nullsFirst(Comparator.naturalOrder()));
        }
        return Comparator.comparing(Schedule::getDueDate, Comparator.nullsLast(Comparator.naturalOrder()));
    }
    
    public void searchSchedules(String keyword) {
        showingSearchResults = true;
        currentSearchKeyword = keyword;
        
        Platform.runLater(() -> {
            try {
                List<Schedule> results = scheduleDAO.searchSchedules(keyword);
                schedules.setAll(results);
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
    
    public void toggleScheduleComplete(Schedule schedule) throws SQLException {
        schedule.setCompleted(!schedule.isCompleted());
        scheduleDAO.updateScheduleStatus(schedule.getId(), schedule.isCompleted());
        refresh();
    }
    
    // 自定义列表单元格
    private class ScheduleListCell extends ListCell<Schedule> {
        private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM-dd");
        
        @Override
        protected void updateItem(Schedule schedule, boolean empty) {
            super.updateItem(schedule, empty);
            
            if (empty || schedule == null) {
                setGraphic(null);
                setText(null);
                getStyleClass().removeAll("completed", "overdue", "upcoming");
                return;
            }
            setManaged(true);
            setVisible(true);
            setPrefHeight(Region.USE_COMPUTED_SIZE);
            setMinHeight(Region.USE_COMPUTED_SIZE);
            setMaxHeight(Region.USE_COMPUTED_SIZE);
            
            // 创建单元格内容
            HBox cell = new HBox(12);
            cell.setAlignment(Pos.CENTER_LEFT);
            cell.setPadding(new Insets(12, 16, 12, 16));
            cell.getStyleClass().add("schedule-card-inner");
            
            // 完成复选框
            ImageView statusIcon = createStatusIconView(schedule.isCompleted());
            statusIcon.setOnMouseClicked(e -> {
                try {
                    toggleScheduleComplete(schedule);
                } catch (SQLException ex) {
                    controller.showError("更新状态失败", ex.getMessage());
                }
                e.consume();
            });
            
            // 优先级标签
            Label priorityLabel = new Label(schedule.getPriority());
            priorityLabel.getStyleClass().add("priority-" + getPriorityClass(schedule.getPriority()));
            
            // 标题
            Label titleLabel = new Label(schedule.getName());
            titleLabel.getStyleClass().add("schedule-title");
            if (schedule.isCompleted()) {
                titleLabel.getStyleClass().add("title-completed");
                cell.setOpacity(0.7);
            }
            
            // 日期
            String dateText = "";
            if (schedule.getDueDate() != null) {
                dateText = schedule.getDueDate().format(formatter);
                if (schedule.isOverdue() && !schedule.isCompleted()) {
                    dateText += " (已过期)";
                }
            }
            Label dateLabel = new Label(dateText);
            dateLabel.getStyleClass().add("schedule-date");
            
            // 分类标签
            Label categoryLabel = new Label(schedule.getCategory());
            categoryLabel.getStyleClass().add("category-tag");
            
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            
            cell.getChildren().addAll(statusIcon, priorityLabel, titleLabel, spacer, dateLabel, categoryLabel);
            
            // 包装容器以支持分组标题
            VBox container = new VBox(5);
            
            int index = getIndex();
            boolean isFirstPending = !schedule.isCompleted() && isFirstPending(index);
            boolean isFirstCompleted = schedule.isCompleted() && isFirstCompleted(index);
            boolean groupCollapsed = schedule.isCompleted() ? completedCollapsed : pendingCollapsed;

            if (isFirstPending) {
                HBox header = createGroupHeaderLabel("待办", pendingCollapsed, () -> {
                    pendingCollapsed = !pendingCollapsed;
                    scheduleListView.refresh();
                });
                container.getChildren().add(header);
            }

            if (isFirstCompleted) {
                HBox header = createGroupHeaderLabel("已完成", completedCollapsed, () -> {
                    completedCollapsed = !completedCollapsed;
                    scheduleListView.refresh();
                });
                container.getChildren().add(header);
            }

            if (!groupCollapsed) {
                container.getChildren().add(cell);
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
            
            // 添加点击事件，确保选择该日程
            if (!groupCollapsed) {
                cell.setOnMouseClicked(e -> {
                    getListView().getSelectionModel().select(schedule);
                    controller.showScheduleDetails(schedule);
                    e.consume();
                });
            }
            
            // 更新样式
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
            Schedule prev = getListView().getItems().get(index - 1);
            return prev != null && prev.isCompleted();
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
            Schedule prev = getListView().getItems().get(index - 1);
            return prev != null && !prev.isCompleted();
        }

        private HBox createGroupHeaderLabel(String title, boolean collapsed, Runnable toggleAction) {
            Label arrowLabel = new Label("▶");
            arrowLabel.setRotate(collapsed ? 0 : 90);
            arrowLabel.getStyleClass().add("schedule-group-arrow");

            Label textLabel = new Label(title);
            textLabel.getStyleClass().add("completed-group-header");
            textLabel.getStyleClass().add("schedule-group-title");

            HBox header = new HBox(6, arrowLabel, textLabel);
            header.setAlignment(Pos.CENTER_LEFT);
            header.getStyleClass().add("schedule-group-toggle");
            header.addEventFilter(MouseEvent.MOUSE_PRESSED, e -> e.consume());
            header.setOnMouseClicked(e -> {
                toggleAction.run();
                e.consume();
            });
            return header;
        }
        
        private String getPriorityClass(String priority) {
            if ("高".equals(priority)) return "high";
            if ("低".equals(priority)) return "low";
            return "medium";
        }
    }

    private ImageView createStatusIconView(boolean completed) {
        String path = completed ? DONE_ICON_PATH : PENDING_ICON_PATH;
        String title = completed ? "已完成日程" : "未完成日程";
        Group iconGroup = loadSvgGraphic(path, 24);
        StackPane container = new StackPane(iconGroup);
        container.setMinSize(24, 24);
        container.setPrefSize(24, 24);
        container.setMaxSize(24, 24);
        container.setAccessibleText(title);
        container.setPickOnBounds(true);
        return snapshotAsImageView(container, 24);
    }

    private ImageView snapshotAsImageView(StackPane container, double size) {
        javafx.scene.SnapshotParameters parameters = new javafx.scene.SnapshotParameters();
        parameters.setFill(Color.TRANSPARENT);
        
        // Ensure container is laid out before snapshotting
        container.layout();
        
        ImageView imageView = new ImageView(container.snapshot(parameters, null));
        imageView.setFitWidth(size);
        imageView.setFitHeight(size);
        imageView.setPreserveRatio(true);
        imageView.setSmooth(true);
        return imageView;
    }

    private Group loadSvgGraphic(String resourcePath, double targetSize) {
        try (java.io.InputStream stream = getClass().getResourceAsStream(resourcePath)) {
            if (stream == null) {
                return new Group();
            }
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(false);
            Document document = factory.newDocumentBuilder().parse(stream);
            Element svg = document.getDocumentElement();
            Group group = new Group();
            double[] viewBox = parseViewBox(svg.getAttribute("viewBox"));
            parseSvgChildren(svg, group, 0, 0);
            double scale = targetSize / Math.max(viewBox[2], viewBox[3]);
            group.getTransforms().add(new Scale(scale, scale));
            group.getTransforms().add(new Translate(-viewBox[0], -viewBox[1]));
            return group;
        } catch (Exception e) {
            return new Group();
        }
    }

    private void parseSvgChildren(Element parent, Group target, double offsetX, double offsetY) {
        NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            org.w3c.dom.Node child = children.item(i);
            if (!(child instanceof Element)) {
                continue;
            }
            Element element = (Element) child;
            String tag = element.getTagName();
            if ("g".equals(tag)) {
                double[] translate = parseTranslate(element.getAttribute("transform"));
                parseSvgChildren(element, target, offsetX + translate[0], offsetY + translate[1]);
                continue;
            }
            Shape shape = createShapeFromElement(element);
            if (shape == null) {
                continue;
            }
            shape.setTranslateX(offsetX);
            shape.setTranslateY(offsetY);
            target.getChildren().add(shape);
        }
    }

    private Shape createShapeFromElement(Element element) {
        String tag = element.getTagName();
        if ("circle".equals(tag)) {
            Circle circle = new Circle(
                parseDouble(element.getAttribute("cx")),
                parseDouble(element.getAttribute("cy")),
                parseDouble(element.getAttribute("r"))
            );
            applyShapeStyle(circle, element);
            return circle;
        }
        if ("rect".equals(tag)) {
            Rectangle rectangle = new Rectangle(
                parseDouble(element.getAttribute("x")),
                parseDouble(element.getAttribute("y")),
                parseDouble(element.getAttribute("width")),
                parseDouble(element.getAttribute("height"))
            );
            double rx = parseDouble(element.getAttribute("rx"));
            if (rx > 0) {
                rectangle.setArcWidth(rx * 2);
                rectangle.setArcHeight(rx * 2);
            }
            applyShapeStyle(rectangle, element);
            return rectangle;
        }
        if ("path".equals(tag)) {
            SVGPath path = new SVGPath();
            path.setContent(element.getAttribute("d"));
            applyShapeStyle(path, element);
            return path;
        }
        return null;
    }

    private void applyShapeStyle(Shape shape, Element element) {
        Color fill = parsePaint(element.getAttribute("fill"));
        Color stroke = parsePaint(element.getAttribute("stroke"));
        if (fill != null) {
            shape.setFill(fill);
        } else {
            shape.setFill(Color.TRANSPARENT);
        }
        if (stroke != null) {
            shape.setStroke(stroke);
            shape.setStrokeWidth(parseDoubleOrDefault(element.getAttribute("stroke-width"), 1));
        }
        String lineCap = element.getAttribute("stroke-linecap");
        if ("round".equalsIgnoreCase(lineCap)) {
            shape.setStrokeLineCap(StrokeLineCap.ROUND);
        }
        String lineJoin = element.getAttribute("stroke-linejoin");
        if ("round".equalsIgnoreCase(lineJoin)) {
            shape.setStrokeLineJoin(StrokeLineJoin.ROUND);
        }
    }

    private Color parsePaint(String value) {
        if (value == null || value.isEmpty() || "none".equalsIgnoreCase(value)) {
            return null;
        }
        try {
            return Color.web(value);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private double[] parseViewBox(String viewBox) {
        if (viewBox == null || viewBox.isEmpty()) {
            return new double[] {0, 0, 100, 100};
        }
        String[] values = viewBox.trim().split("\\s+");
        if (values.length != 4) {
            return new double[] {0, 0, 100, 100};
        }
        return new double[] {
            parseDouble(values[0]),
            parseDouble(values[1]),
            parseDouble(values[2]),
            parseDouble(values[3])
        };
    }

    private double[] parseTranslate(String transform) {
        if (transform == null || !transform.startsWith("translate")) {
            return new double[] {0, 0};
        }
        int start = transform.indexOf('(');
        int end = transform.indexOf(')');
        if (start < 0 || end <= start) {
            return new double[] {0, 0};
        }
        String[] values = transform.substring(start + 1, end).split(",");
        if (values.length == 1) {
            return new double[] {parseDouble(values[0]), 0};
        }
        return new double[] {parseDouble(values[0]), parseDouble(values[1])};
    }

    private double parseDouble(String value) {
        return parseDoubleOrDefault(value, 0);
    }

    private double parseDoubleOrDefault(String value, double defaultValue) {
        if (value == null || value.isEmpty()) {
            return defaultValue;
        }
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }
}
