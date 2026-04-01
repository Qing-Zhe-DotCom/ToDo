# ToDo 日程管理系统 - 开发者技术设计文档

本文档旨在为新加入或对本项目感兴趣的开发者提供全面的架构、设计和开发规范说明。

## 目录
- [项目概览与架构](#项目概览与架构)
  - [1. 目录结构](#1-目录结构)
  - [2. 模块职责边界](#2-模块职责边界)
- [核心设计与数据流](#核心设计与数据流)
  - [1. MVC 设计模式](#1-mvc-设计模式)
  - [2. 关键算法与组件 (TimelineView)](#2-关键算法与组件-timelineview)
- [核心 API 与接口说明](#核心-api-与接口说明)
- [环境搭建与测试策略](#环境搭建与测试策略)
- [代码规范与 CI/CD 约定](#代码规范与-cicd-约定)

---

## 项目概览与架构

本项目是一个基于 **JavaFX 11+** 构建的桌面端日程管理应用。它不依赖外部的复杂后端服务器，而是使用嵌入式的 **SQLite 数据库** 作为持久化层。

### 1. 目录结构

```text
c:\Users\12493\Desktop\ToDo\todo\
├── src/
│   ├── main/
│   │   ├── java/com/example/
│   │   │   ├── controller/      # MVC 控制器，负责协调模型和视图
│   │   │   ├── databaseutil/    # DAO 层，处理 SQLite 数据库连接和 CRUD 操作
│   │   │   ├── model/           # MVC 模型，POJO 实体类
│   │   │   ├── view/            # MVC 视图，JavaFX 界面组件及自定义控件
│   │   │   └── MainApp.java     # 应用程序入口
│   │   └── resources/           # FXML 文件、CSS 样式表、数据库初始化 SQL 脚本
│   └── test/
│       └── java/com/example/    # JUnit 5 单元测试代码
├── pom.xml                      # Maven 构建配置文件
└── README.md                    # 项目入口说明
```

### 2. 模块职责边界

- **Model (`com.example.model`)**：仅包含 `Schedule` 等纯数据载体。
- **DAO (`com.example.databaseutil`)**：`ScheduleDAO` 等类负责向 Model 注入来自 SQLite 的数据，隐藏 SQL 拼接细节。
- **View (`com.example.view`)**：实现 `View` 接口的各种自定义 JavaFX 面板（如 `TimelineView`, `HeatmapView`）。它们**仅**负责界面渲染和将用户交互事件回调给 Controller。
- **Controller (`com.example.controller`)**：如 `MainController`，作为中枢神经。它负责监听全局状态（如当前选中视图、CSS 主题），向 DAO 请求数据并下发给当前激活的 View 进行重绘。

---

## 核心设计与数据流

### 1. MVC 设计模式
本系统严格遵循 MVC 设计模式，所有数据流均呈单向闭环：
`User Input (View) -> Controller.handleAction() -> DAO.update() -> Controller.refreshAllViews() -> View.draw()`

### 2. 关键算法与组件 (TimelineView)
在 `TimelineView` 中，为了在横向上实现日程卡片的自然层叠，系统采用了**自定义布局和智能垂直分组算法**：
1. **获取数据**：调用 `scheduleDAO.getAllSchedules()` 并按起始时间、优先级等进行混合排序。
2. **分组过滤**：在 `appendGroup()` 中根据日程跨度（`duration`）将日程划分为三个集合：
   - 短期 (`<7`天)
   - 中期 (`7-35`天)
   - 长期 (`>35`天)
3. **坐标映射**：
   - **X 轴**：通过 `ChronoUnit.DAYS.between(minDate, startDate) * DAY_WIDTH` 将时间映射为屏幕像素 X 坐标。
   - **Y 轴 (智能错落)**：利用 `java.util.Map<LocalDate, Integer>` 统计某一天已存在的日程数量，为同天开始的卡片分配递增的 `StackIndex`，再通过 `stackIndex * 20` 计算出带有垂直错落感的 Y 坐标。

---

## 核心 API 与接口说明

### 接口 `com.example.view.View`
系统所有独立视图面板必须实现的基础接口。
```java
public interface View {
    /** 返回当前视图的 JavaFX 根节点，供主界面挂载 */
    Node getView();
    /** Controller 数据或状态更新时调用的重绘回调 */
    void refresh();
}
```

### 核心类 `com.example.view.TimelineView`
- **`private double appendGroup(List<Schedule> schedules, String title, double startY, LocalDate minDate, LocalDate maxDate, List<TimelineEntry> entries)`**
  - **职责**：渲染某个周期（短/中/长期）的分组标题、分隔线，并计算该组内所有日程的 X/Y 绝对坐标。
  - **返回值**：该分组渲染完毕后的底部 Y 坐标（`double`），作为下一个分组的 `startY`，确保垂直方向上的无缝衔接。

---

## 环境搭建与测试策略

### 1. 环境搭建
- **JDK**：要求 Java 11 及以上版本。
- **构建工具**：Apache Maven 3.6+。
- 依赖项：主要包含 `javafx-controls`, `sqlite-jdbc`, `junit-jupiter` 等。无需安装额外数据库服务器。

### 2. 测试策略
使用 **JUnit 5** 作为测试框架。运行 `mvn test` 即可执行全部测试。
- **单元测试**：针对模型验证、DAO 层的基础 SQL 映射、以及视图层（如 `TimelineViewTest`）中独立于 JavaFX Toolkit 渲染循环的纯逻辑静态方法（如日期解析、分组计算）进行无头测试。

---

## 代码规范与 CI/CD 约定

- **代码规范**：遵循标准 Google Java Style Guide。
- **提交流程**：
  1. 所有功能变更需通过 `mvn test` 并在本地验证 `mvn compile javafx:run` 可正常启动。
  2. 修复 `TimelineView` 相关的视觉变动时，必须确保四种以上风格切换（尤其是`赛博霓虹`和`扁平马卡龙`）不会引发空指针或颜色转换异常（如避免将 `Double` 强制给 `RGB(int)`）。
- **CI/CD**：计划未来在 GitHub Actions 引入自动化流程，推送 `main` 分支时自动触发 `mvn clean test` 并在 Release 节点使用 `jlink` 或 `jpackage` 打包跨平台可执行文件。