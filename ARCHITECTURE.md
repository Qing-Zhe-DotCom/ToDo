# ToDo 当前技术事实说明

本文档描述的是当前仓库已经落地的真实技术状态；未来已确定路线会单独列出，但不会把目标态误写成当前事实。

如果你是第一次接手仓库，建议先读 [README.md](./README.md)，再读本文。  
如果你更关心开发维护与构建命令，请看 [DEV.md](./DEV.md)。  
如果你更关心用户视角的页面和交互，请看 [USER.md](./USER.md)。

## 1. 当前架构一句话概括

当前项目已经从“UI 直接连数据库的单体 JavaFX 工程”演进到“FXML shell + `ApplicationContext` 组合根 + `config / data / application / ui` 初步分层”的过渡态架构；默认数据主链路已经切到 Stage-B 的 `ScheduleItem` 体系，但 UI 侧仍保留 `Schedule` 兼容桥接。

## 2. 主运行链路

当前主运行链路如下：

```text
MainApp
  -> FXMLLoader(main-shell.fxml)
  -> MainController
  -> ApplicationContext
  -> ScheduleItemService / ThemeService / NavigationService / IconService / LocalizationService
  -> ScheduleItemRepository
  -> SqlScheduleItemRepository
```

关键入口文件：

- [`todo/src/main/java/com/example/MainApp.java`](./todo/src/main/java/com/example/MainApp.java)
- [`todo/src/main/resources/com/example/ui/main-shell.fxml`](./todo/src/main/resources/com/example/ui/main-shell.fxml)
- [`todo/src/main/java/com/example/controller/MainController.java`](./todo/src/main/java/com/example/controller/MainController.java)
- [`todo/src/main/java/com/example/application/ApplicationContext.java`](./todo/src/main/java/com/example/application/ApplicationContext.java)

## 3. 当前模块与包结构

当前 JPMS 模块名仍是 `com.example`，定义见：

- [`todo/src/main/java/module-info.java`](./todo/src/main/java/module-info.java)

当前真实包结构如下：

- `com.example`
  - 应用入口与打包启动器
- `com.example.application`
  - 组合根、应用服务、主题、图标、本地化、提醒、导航、视图模型
- `com.example.config`
  - 配置加载、路径决策、偏好存储
- `com.example.controller`
  - FXML shell controller 与 UI 编排
- `com.example.data`
  - 连接工厂、schema 管理、仓储与 SQL 映射
- `com.example.model`
  - 领域模型、值对象、重复规则、提醒、标签
- `com.example.view`
  - 自定义 JavaFX 视图、动效与交互组件

需要特别说明的是：

- 当前仓库中已经没有独立的 legacy DAO 包目录
- 文档或测试中的旧命名痕迹不代表当前包结构仍然如此

## 4. 当前数据链路与 schema 现实

### 4.1 默认 SQLite 主链路

默认本地运行链路如下：

```text
ApplicationContext
  -> AppDataPaths
  -> SqliteConnectionFactory
  -> SqliteStageBSchemaManager
  -> SqlScheduleItemRepository (SqlDialect.SQLITE)
  -> ScheduleItemService
  -> MainController / Views
```

当前 SQLite 主链路的关键点：

- `ApplicationContext` 在 `sqlite` 模式下创建 `AppDataPaths`
- `SqliteConnectionFactory` 负责创建本地目录、连接 SQLite，并配置：
  - `PRAGMA foreign_keys = ON`
  - `journal_mode = WAL`
  - `synchronous = NORMAL`
- `SqliteStageBSchemaManager` 负责初始化当前默认 schema
- `SqlScheduleItemRepository` 是当前默认仓储实现
- `ScheduleItemService` 是当前默认应用服务入口

### 4.2 MySQL 兼容链路

当前 MySQL 兼容链路如下：

```text
ApplicationContext
  -> JdbcConnectionFactory
  -> MysqlStageBSchemaManager
  -> SqlScheduleItemRepository (SqlDialect.MYSQL)
  -> ScheduleItemService
  -> MainController / Views
```

关键事实：

- MySQL 兼容链路仍保留
- 但它已经不是默认本地运行链路
- 当前兼容模式同样使用 Stage-B schema 管理器与 `SqlScheduleItemRepository`
- 不应再把旧 DAO 风格的兼容链路当成当前主链路

### 4.3 Stage-B schema 的当前默认事实

当前默认 schema 已经不是旧 `schedules` 单表，而是以 `schedule_item` 为中心的结构。  
SQLite 和 MySQL 的 Stage-B schema 都已落地以下核心表：

- `schedule_item`
- `tag`
- `schedule_item_tag`
- `reminder`
- `recurrence_rule`
- `sync_outbox`
- `sync_checkpoint`
- `device_registry`

这意味着当前真实数据库能力已经具备：

- 文本型全局 ID
- 任务主表与标签、提醒、重复规则分表
- 软删除与版本号
- 同步状态与 outbox / checkpoint 基础结构
- 设备注册信息

### 4.4 旧版 SQLite 脚本的定位

仓库里仍保留了旧版 SQLite 脚本，例如：

- [`todo/src/main/resources/db/sqlite/V001__create_schedules.sql`](./todo/src/main/resources/db/sqlite/V001__create_schedules.sql)
- [`todo/src/main/resources/db/sqlite/V002__add_local_sync_columns.sql`](./todo/src/main/resources/db/sqlite/V002__add_local_sync_columns.sql)
- [`todo/src/main/resources/db/sqlite/V003__add_minute_precision_schedule_times.sql`](./todo/src/main/resources/db/sqlite/V003__add_minute_precision_schedule_times.sql)

但当前准确定位是：

- 它们是旧阶段的历史脚本 / 过渡痕迹
- 当前默认运行时 **不再** 通过这些脚本初始化主链路
- `SqliteStageBSchemaManager` 会在检测到旧 `schedules` / `schema_version` 结构时直接报错，而不是自动升级
- 若要使用旧库，需要手动迁移到新 schema 或使用空库

同样地，`MysqlStageBSchemaManager` 也会拒绝旧 `schedules` 单表结构的自动升级。

## 5. 配置加载与运行时现实

### 5.1 默认配置

默认配置来自：

- [`todo/src/main/resources/application-defaults.properties`](./todo/src/main/resources/application-defaults.properties)

当前默认值的关键点包括：

- `todo.app.version=${project.version}`
- `todo.app.default-theme-family=classic`
- `todo.app.default-theme-appearance=light`
- `todo.db.mode=sqlite`
- `todo.db.driver=org.sqlite.JDBC`
- `todo.db.sqlite.path=`

### 5.2 配置优先级

`ConfigurationLoader` 的实际加载规则如下：

1. 先加载 classpath 中的 `application-defaults.properties`
2. 再尝试外部属性文件，优先级依次为：
   - `TODO_CONFIG_FILE` 指定路径
   - `%APPDATA%\ToDo\config\application.properties`
   - 工作目录下的 `application.properties`
   - 工作目录下的 `config\application.properties`
3. 最终在读取单个配置项时，环境变量优先于属性文件

配置入口位于：

- [`todo/src/main/java/com/example/config/ConfigurationLoader.java`](./todo/src/main/java/com/example/config/ConfigurationLoader.java)

### 5.3 本地数据目录

本地数据路径决策来自：

- [`todo/src/main/java/com/example/config/AppDataPaths.java`](./todo/src/main/java/com/example/config/AppDataPaths.java)

默认情况下：

- Windows：`%APPDATA%\ToDo\`
- macOS：`~/Library/Application Support/ToDo/`
- Linux：`$XDG_DATA_HOME/ToDo/` 或 `~/.local/share/ToDo/`

默认 SQLite 文件名为：

- `todo.sqlite`

同时会在数据目录下创建：

- `logs/`
- `backups/`

## 6. FXML shell 与 UI 兼容桥接现实

当前 UI 不是“每个页面一个 `FXML + Controller + ViewModel`”的最终形态，而是：

- 一个轻量 FXML shell
- 一个较重的 `MainController`
- 多个 Java 自定义视图
- 一个右侧详情面板

当前真实情况：

- `main-shell.fxml` 本身非常轻量，只提供根容器
- `MainController` 负责装配左侧边栏、主视图切换和右侧详情面板
- `MainController` 会创建：
  - `ScheduleListView`
  - `TimelineView`
  - `HeatmapView`
  - `FlowchartView`
  - `InfoPanelView`

### 6.1 `Schedule` 兼容桥接

当前默认数据主链路已经切到 `ScheduleItem`，但以下桥接现实仍然存在：

- `NavigationService` 仍保存 `Schedule selectedSchedule`
- `MainController` 使用 `toLegacySchedule` / `toLegacySchedules` 把 `ScheduleItem` 转成 `Schedule`
- 列表、时间轴、热力图、详情面板等现有视图仍以 `Schedule` 作为主要交互对象

因此当前最准确的表述不是“旧模型已经完全消失”，而是：

> 默认持久化和应用服务已升级到 `ScheduleItem` 体系，但 UI 层仍保留 `Schedule` 兼容桥接。

### 6.2 其他界面现实

- `FlowchartView` 当前仍是占位页
- 登录入口当前只是信息提示，不包含账号体系
- 设置中心已经是实装功能，而不是简单占位弹窗
- `ReminderNotificationService` 已接入应用初始化与关闭流程

## 7. 已经完成的演进

当前已经真实完成的部分包括：

- Java 21 / JavaFX 21 / Maven 插件升级
- FXML shell 引入
- `ApplicationContext` 组合根引入
- `config -> data -> application -> ui` 方向初步建立
- 默认本地运行基线切到 SQLite
- Stage-B schema 在 SQLite / MySQL 上都已落地
- `ScheduleItemService` + `ScheduleItemRepository` 主链路落地
- 主题、图标、本地化、字体、设置中心、提醒调度等应用服务已从 shell 控制器中抽出一部分
- 设置详情页已接入应用版本显示链路

## 8. 当前仍保留的 legacy 与技术债

这些内容在当前仓库里仍是真实存在的技术债，不应被文档美化：

- `MainController` 仍是过渡态 shell controller，职责偏重
- `NavigationService` 与部分视图仍围绕 `Schedule` 维护状态
- UI 层还没有彻底收敛到每屏独立控制器 / 视图模型
- 旧版 SQLite 脚本仍保留在仓库中，容易误导新读者
- 测试与部分历史说明里仍有旧命名痕迹

需要特别说明的是：

- 当前 legacy 的重点已经不是“旧 DAO 目录仍在”，而是“新数据层与旧 UI 模型仍在桥接并存”
- 这也是为什么当前最关键的后续工作仍然是继续瘦身 `MainController`，并缩小 `Schedule` 的兼容面

## 9. 已确定的后续演进方向

这些方向已经确定，但尚未在当前代码中全部落地：

### 9.1 本地持久化

- 继续以 SQLite 作为默认本地运行基线
- 继续强化 Stage-B / 本地优先（local-first）能力
- 把 MySQL 兼容链路继续收缩到导入 / 兼容用途

### 9.2 数据模型

- 继续以 `ScheduleItem` 为中心演进
- 逐步减少 `Schedule` 的 UI 兼容桥接面
- 统一更完整的时间语义、提醒、标签、重复规则与同步字段

### 9.3 云端同步

- 云端中心库固定为 PostgreSQL
- 中间层为 API / Sync 服务
- 客户端继续优先读写本地库

### 9.4 UI 层

- 当前 `com.example.view` 中的主视图会继续保留一段时间
- 后续目标仍是更细粒度的 `FXML + Controller + ViewModel`
- `MainController` 需要继续瘦身，把更多业务和状态迁出

## 10. 与改造计划的关系

本文档解释的是“当前技术事实是什么”。  
真正的分阶段演进路线见：

- [`todo/改造计划/`](./todo/%E6%94%B9%E9%80%A0%E8%AE%A1%E5%88%92/)

建议按这个方式理解：

- `ARCHITECTURE.md`：当前事实
- `todo/改造计划/`：后续施工路线
