# ToDo 项目现状总览

本文档基于当前仓库工作区状态编写，基线时间为 **2026-04-03**。内容以现有源码、配置、资源和已验证测试结果为准，不再沿用旧文档中的愿景化描述。

## 文档导航

- [README.md](./README.md)：仓库入口、技术栈、运行方式、功能现状与风险总览
- [DEV.md](./DEV.md)：面向开发者的真实架构、模块职责、数据流、测试与工程风险
- [USER.md](./USER.md)：面向当前版本用户的实际使用手册
- [TimelineView_Documentation.md](./TimelineView_Documentation.md)：时间轴组件补充材料，部分内容与当前实现存在偏差，阅读时请以 `DEV.md` 和源码为准

## 仓库结构

当前仓库是“文档在根目录、应用代码在 `todo/` 子目录”的结构：

```text
ToDo/
├── README.md
├── DEV.md
├── USER.md
├── TimelineView_Documentation.md
└── todo/
    ├── pom.xml
    ├── src/
    │   ├── main/
    │   │   ├── java/
    │   │   └── resources/
    │   └── test/
    └── target/
```

`todo/` 才是 Maven 工程根目录，编译、运行、测试都应从这里执行。

## 项目一句话说明

这是一个基于 **JavaFX** 的桌面日程管理应用，核心功能围绕：

- 日程列表管理
- 时间轴视图
- 热力图视图
- 右侧详情面板
- 主题与时间轴卡片样式切换

当前版本并不是“开箱即用的安装包应用”，而是更接近一个可运行、可继续开发的源码工程。

## 真实技术栈

以 `todo/pom.xml`、`module-info.java` 与当前源码为准：

| 类别 | 当前实现 |
| --- | --- |
| 语言 / 编译目标 | Java 11 |
| UI 框架 | JavaFX 13（`javafx-controls`、`javafx-fxml`、`javafx-swing`） |
| 构建工具 | Maven |
| 数据库驱动 | MySQL Connector/J 8.0.33 |
| 数据库类型 | **MySQL**，不是 SQLite |
| 测试框架 | JUnit 5.10.2 |
| 额外运行依赖 | AWT Taskbar、Java Preferences、XML DOM 解析 |

## 当前功能状态总览

| 模块 / 能力 | 状态 | 说明 |
| --- | --- | --- |
| `ScheduleListView` 日程列表 | 已实现 | 支持加载、搜索、筛选、排序、分组折叠、切换完成状态 |
| `TimelineView` 时间轴 | 已实现 | 支持按持续时长分组、日期范围筛选、横向滚动、卡片悬停动画、双击编辑 |
| `HeatmapView` 热力图 | 已实现 | 支持周 / 月 / 年视图切换、完成统计、点击日期查看当日日程 |
| `InfoPanelView` 右侧详情面板 | 已实现 | 支持查看详情、编辑、删除、标记完成、点击空白处或 `Esc` 关闭 |
| 设置中心 | 已实现 | 包含详情页、主题页、时间轴样式页 |
| 内置主题切换 | 已实现 | 当前有 8 套内置主题：浅色、薄荷、海洋、落日、薰衣草、森林、石板、马卡龙 |
| 外部 CSS 主题导入 | 已实现 | 可在设置中心导入外部 CSS 文件作为附加主题 |
| 时间轴卡片样式切换 | 已实现 | 当前有 7 种卡片风格 |
| 任务栏待办数量角标 | 已实现 | 通过 `MainApp` 更新窗口标题、应用图标与系统任务栏 badge，平台支持时生效 |
| `FlowchartView` 流程图 | **占位 / 开发中** | 当前仅显示“开发中”提示，不是实际流程图 |
| 登录功能 | **占位 / 开发中** | 点击后只弹出提示框 |
| 打包产物 / 安装包 | 未提供 | 仓库内没有正式的 `.jar`、安装器或发布脚本 |

## 运行方式

### 前置条件

- 已安装 Java 11 或更高版本
- 已安装 Maven
- 本地可访问 MySQL 服务
- 已具备 `todo_db` 数据库，或至少允许当前连接信息成功连入 MySQL

### 重要说明：数据库是硬编码连接

当前代码在 `todo/src/main/java/com/example/databaseutil/Connectdatabase.java` 中直接写死了连接参数：

- URL：`jdbc:mysql://localhost:3306/todo_db`
- 用户名：`root`
- 密码：硬编码在源码中

这意味着：

- 项目**不是**零配置启动
- 如果你的本地 MySQL 环境与源码中的连接参数不一致，程序会在数据库连接阶段失败
- 这也是当前仓库最需要优先整改的工程风险之一

### 从源码运行

在仓库根目录进入 `todo/` 后执行：

```bash
cd todo
mvn clean compile javafx:run
```

### 从源码测试

```bash
cd todo
mvn test
```

## 已验证测试状态

已在当前工作区执行并确认：

- 执行时间：`2026-04-03 11:04:02 +08:00`
- 命令：`mvn test`
- 结果：`BUILD SUCCESS`
- 汇总：`8 tests, 0 failures, 0 errors, 0 skipped`

当前测试类共 3 个：

- `HeatmapViewTest`
- `ThemeCssTest`
- `TimelineViewTest`

这些测试主要覆盖纯逻辑方法和 CSS 选择器存在性，不代表数据库集成和整套 UI 交互已经被自动化验证。

## 当前已知限制 / 风险

- 数据库连接信息硬编码在源码中，不适合直接共享或发布
- 当前实际依赖 MySQL，本地必须准备可用数据库环境
- 流程图页面尚未实现真实功能
- 登录入口仍是占位提示
- 仓库没有正式安装包、可执行发布物或一键启动脚本
- 热力图的颜色深浅基于“完成任务的 `updated_at` 日期”，不是简单按当天所有日程数量统计
- 日程列表的搜索仅查询名称和描述，且当前版本没有完善的“恢复全部搜索结果”交互
- 自动化测试覆盖面有限，缺少数据库集成测试和 UI 自动化测试
- `target/` 目录中已有构建产物且当前工作区存在未提交变更，阅读源码时应以 `src/` 为主

## 遗留文件与补充说明

以下内容目前存在于仓库中，但不属于主运行链路：

| 文件 / 资源 | 当前状态 |
| --- | --- |
| `TimelineView_Documentation.md` | 可作为补充阅读，但部分描述落后于当前代码 |
| `todo/src/main/resources/com/example/primary.fxml` | 遗留示例资源，当前运行流程未使用 |
| `todo/src/main/resources/com/example/secondary.fxml` | 遗留示例资源，当前运行流程未使用 |
| `todo/RefactorScript.java` | 占位脚本，当前没有实际重构逻辑 |
| `todo/src/main/resources/database_update.sql` | MySQL 风格的辅助 SQL 文档，当前应用运行时并不直接执行它 |

特别说明：

- `primary.fxml` / `secondary.fxml` 仍引用 `PrimaryController` / `SecondaryController`
- 当前源码中并没有对应控制器类
- 这进一步说明它们属于未接入主流程的遗留资源

## 阅读建议

- 如果你想先快速知道“这个仓库现在到底能做什么”，继续看 [USER.md](./USER.md)
- 如果你想接手维护或继续开发，请直接看 [DEV.md](./DEV.md)
- 如果你想核对具体实现，请以 `todo/src/main` 下的源码为最终依据
