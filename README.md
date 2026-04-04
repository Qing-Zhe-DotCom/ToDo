# ToDo 项目现状总览

本文档基于当前工作区状态编写，基线时间为 **2026-04-04**。内容以 `todo/src/main`、`todo/src/test` 和当前可执行命令的实际结果为准，而不是沿用旧版产品设想。

## 文档导航

- [README.md](./README.md)：仓库入口、技术栈、功能现状、运行方式与总体风险
- [USER.md](./USER.md)：面向当前源码版本使用者的实际操作说明
- [DEV.md](./DEV.md)：面向维护者的架构、模块职责、数据流与工程风险说明
- [TimelineView_Documentation.md](./TimelineView_Documentation.md)：时间轴组件的单独实现说明

## 本次同步重点

- 完成状态切换已经演进为一套跨视图协同机制，不再只是单个按钮直接写库。
- 列表、时间轴、热力图共用了新的日程卡片样式层与状态控件，列表和热力图还加入了完成态迁移动画反馈。
- 测试源码已经扩展到 10 个测试类，但当前 `mvn test` 会在 Surefire 启动阶段失败，旧文档里的 “BUILD SUCCESS” 已经不准确。

## 仓库结构

当前仓库是“文档在根目录，Maven 工程在 `todo/` 子目录”的结构：

```text
ToDo/
├─ README.md
├─ DEV.md
├─ USER.md
├─ TimelineView_Documentation.md
└─ todo/
   ├─ pom.xml
   ├─ src/
   │  ├─ main/
   │  └─ test/
   └─ target/
```

真正的应用工程根目录是 `todo/`，编译、运行、测试命令都应从这里执行。

## 项目一句话说明

这是一个基于 **JavaFX** 的桌面日程管理应用，当前已经具备以下主线能力：

- 日程列表管理
- 时间轴视图
- 热力图视图
- 右侧详情面板
- 设置中心
- 内置主题切换与外部 CSS 导入
- 全局日程卡片样式切换
- 待办数量驱动的窗口标题与任务栏角标更新

当前仓库仍然是“源码可运行版本”，不是带安装器和稳定发行物的正式发布版。

## 当前技术栈

| 类别 | 当前实现 |
| --- | --- |
| 语言 / 编译目标 | Java 11 |
| UI 框架 | JavaFX 13（`javafx-controls`、`javafx-fxml`、`javafx-swing`） |
| 构建工具 | Maven |
| 数据库驱动 | MySQL Connector/J 8.0.33 |
| 数据库 | MySQL |
| 测试框架 | JUnit 5.10.2 |
| 额外运行依赖 | AWT Taskbar、Java Preferences、XML DOM 解析 |

## 功能现状总览

| 模块 / 能力 | 状态 | 说明 |
| --- | --- | --- |
| `ScheduleListView` 日程列表 | 已实现 | 支持搜索、显示过去日程、筛选、排序、待办/已完成分组、分组折叠、状态切换与完成态迁移动画 |
| `TimelineView` 时间轴 | 已实现 | 支持日期范围筛选、按时长自动分组、横向滚动、今天高亮、点击详情、双击编辑、卡片内状态切换 |
| `HeatmapView` 热力图 | 已实现 | 支持周/月/年视图、统计摘要、点击日期查看当日日程、卡片内状态切换、完成区反馈动画 |
| `InfoPanelView` 右侧详情面板 | 已实现 | 支持查看、编辑、删除、标记完成/未完成，并与主界面选中状态联动 |
| 设置中心 | 已实现 | 目前包含“详情 / 主题 / 样式”三页 |
| 内置主题切换 | 已实现 | 当前有 8 套内置主题：浅色、薄荷、海洋、落日、薰衣草、森林、石板、马卡龙 |
| 外部主题导入 | 已实现 | 可导入外部 CSS 作为附加主题 |
| 日程卡片样式切换 | 已实现 | 当前有 7 种卡片风格，作用于列表、时间轴和热力图 |
| 完成状态协同更新 | 已实现 | 由 `ScheduleCompletionCoordinator` 统一处理乐观更新、确认与回滚 |
| 待办数量角标 | 已实现 | `MainApp` 会同步更新窗口标题、应用图标与任务栏 badge（平台支持时生效） |
| `FlowchartView` 流程图 | 占位 / 开发中 | 当前只有“开发中”页面，不是实际流程图功能 |
| 登录功能 | 占位 / 开发中 | 点击后仅弹出“开发中”提示 |
| 打包产物 / 安装包 | 未提供 | 仓库中没有正式发布的安装器或稳定发行 `jar` |

## 最近的实现变化

相对旧版文档，当前最值得注意的变化有：

- 新增完成状态协调层：`ScheduleCompletionCoordinator`、`ScheduleCompletionMutation`
- 新增共享状态控件：`ScheduleStatusControl`、`ScheduleStatusInteractionModel`
- 新增列表/热力图完成态动画支撑：`ScheduleReflowAnimator`、`ScheduleCollapsePopAnimator`、`CollapsePopKeyframePreset`
- 新增共享卡片样式层：`ScheduleCardStyleSupport`
- 热力图统计现在优先基于已加载日程在内存中计算，不再完全依赖 DAO 查询结果
- 测试源码新增了排序、交互模型、动效辅助类与完成协调器相关测试

## 运行方式

### 前置条件

- 已安装 Java 11 或更高版本
- 已安装 Maven
- 本地可访问 MySQL
- 已具备 `todo_db` 数据库，或至少允许当前连接信息成功连入 MySQL

### 重要说明：数据库连接仍然是硬编码

当前 `todo/src/main/java/com/example/databaseutil/Connectdatabase.java` 中写死了连接信息：

- URL：`jdbc:mysql://localhost:3306/todo_db`
- 用户名：`root`
- 密码：硬编码在源码中

这意味着当前项目并不是零配置启动版本。本地数据库环境与源码不一致时，程序会在连接数据库阶段失败。

### 运行应用

```bash
cd todo
mvn clean compile javafx:run
```

### 运行测试

```bash
cd todo
mvn test
```

## 当前测试状态

当前仓库中共有 **10 个测试类**：

- `ScheduleCompletionCoordinatorTest`
- `CollapsePopKeyframePresetTest`
- `HeatmapViewTest`
- `ScheduleCardStyleSupportTest`
- `ScheduleLandingTransitionTest`
- `ScheduleListViewSortTest`
- `ScheduleReflowAnimatorTest`
- `ScheduleStatusInteractionModelTest`
- `ThemeCssTest`
- `TimelineViewTest`

最近一次实际执行结果如下：

- 执行时间：`2026-04-04 15:03:58 +08:00`
- 命令：`mvn test`
- 结果：`BUILD FAILURE`
- 直接报错：`Unable to create test class 'com.example.controller.ScheduleCompletionCoordinatorTest'`
- 日志中的现象：`Tests run: 0`，并伴随 `Unknown module: com.example specified to --patch-module` 等 JPMS 相关警告

也就是说，当前测试源码已经能被编译到 `target/test-classes`，但完整测试流程会在 Surefire / 模块化测试启动阶段中断，尚未恢复到“整套测试可直接跑通”的状态。

## 当前已知限制 / 风险

- 数据库连接信息硬编码在源码中，不适合直接共享、发布或部署
- 当前真实依赖 MySQL，本地必须准备可用数据库环境
- 流程图页面尚未实现真实功能
- 登录入口仍是占位提示
- 仓库没有正式安装包、一键启动脚本或稳定发行物
- 列表页全局搜索只能在输入非空关键字后触发，清空搜索框不会自动恢复默认列表
- 热力图颜色深浅统计的是“某天完成了多少任务”，不是“某天覆盖了多少任务”
- 自动化测试目前被 Surefire / JPMS 启动问题阻塞
- `primary.fxml`、`secondary.fxml` 等资源仍然保留在仓库中，但当前主界面并不依赖它们

## 建议的阅读顺序

- 想先知道“现在这个项目到底能怎么用”：看 [USER.md](./USER.md)
- 想接手维护或继续开发：看 [DEV.md](./DEV.md)
- 想单独理解时间轴实现：看 [TimelineView_Documentation.md](./TimelineView_Documentation.md)
- 想确认最终实现细节：以 `todo/src/main` 下源码为准
