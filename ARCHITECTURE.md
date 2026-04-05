# ToDo 当前架构说明

本文档描述的是当前仓库已经落地的真实架构状态；未来已确定路线会单独列出，但不会把目标态误写成当前已落地事实。

如果你是第一次接手仓库，建议先读 [README.md](./README.md)，再读本文。  
如果你更关心数据库和运行基线，请看 [DATABASE_AND_RUNTIME.md](./DATABASE_AND_RUNTIME.md)。

## 1. 当前架构一句话概括

当前项目已经从“UI 直接打 DAO 的单体 JavaFX 工程”演进到“FXML shell + ApplicationContext 组合根 + `config / data / application / ui` 初步分层”的过渡态架构，但还没有完全到达最终目标态。

## 2. 运行链路

当前主运行链路如下：

```text
MainApp
  -> FXMLLoader(main-shell.fxml)
  -> MainController
  -> ApplicationContext
  -> ThemeService / NavigationService / ScheduleService
  -> ScheduleRepository
```

数据访问在当前代码里分成两条链路：

### 2.1 默认 SQLite 本地链路

```text
ApplicationContext
  -> ScheduleRepository
  -> SqliteScheduleRepository
  -> SqliteConnectionFactory
  -> SqliteMigrationRunner
  -> SQLite
```

### 2.2 legacy MySQL 兼容链路

```text
ApplicationContext
  -> ScheduleRepository
  -> JdbcScheduleRepository
  -> ScheduleDAO
  -> Connectdatabase / JdbcConnectionFactory / SchemaInitializer
  -> MySQL
```

关键入口文件：

- [`todo/src/main/java/com/example/MainApp.java`](./todo/src/main/java/com/example/MainApp.java)
- [`todo/src/main/resources/com/example/ui/main-shell.fxml`](./todo/src/main/resources/com/example/ui/main-shell.fxml)
- [`todo/src/main/java/com/example/controller/MainController.java`](./todo/src/main/java/com/example/controller/MainController.java)
- [`todo/src/main/java/com/example/application/ApplicationContext.java`](./todo/src/main/java/com/example/application/ApplicationContext.java)

## 3. 模块与包结构

当前 JPMS 模块名仍是 `com.example`，定义见：

- [`todo/src/main/java/module-info.java`](./todo/src/main/java/module-info.java)

当前主要包职责如下：

### `com.example`

- 存放 `MainApp`
- 负责桌面应用启动和 JavaFX Stage 初始化

### `com.example.config`

- 负责应用配置与偏好读取
- 当前核心对象：
  - `ConfigurationLoader`
  - `AppProperties`
  - `DatabaseProperties`
  - `AppDataPaths`
  - `JavaPreferencesStore`

### `com.example.data`

- 负责连接工厂、schema 初始化 / migration 和仓储接口
- 当前核心对象：
  - `ConnectionFactory`
  - `JdbcConnectionFactory`
  - `SqliteConnectionFactory`
  - `SchemaInitializer`
  - `SqliteMigrationRunner`
  - `ScheduleRepository`
  - `JdbcScheduleRepository`
  - `SqliteScheduleRepository`

### `com.example.application`

- 负责应用级服务和组合根
- 当前核心对象：
  - `ApplicationContext`
  - `ScheduleService`
  - `ThemeService`
  - `NavigationService`
  - `MainViewModel`

### `com.example.controller`

- 负责 FXML shell controller 和完成状态协调逻辑
- 当前核心对象：
  - `MainController`
  - `ScheduleCompletionCoordinator`
  - `ScheduleCompletionMutation`

### `com.example.view`

- 负责主要视图与大量 JavaFX 自定义组件
- 当前主要视图：
  - `ScheduleListView`
  - `TimelineView`
  - `HeatmapView`
  - `InfoPanelView`
  - `ScheduleDialog`
  - `FlowchartView`

### `com.example.model`

- 当前仍以 `Schedule` 作为主要领域模型
- 该模型仍带有明显的旧阶段痕迹，例如：
  - 主键仍是 `int`
  - 时间字段仍以 `LocalDate` 为主
  - 标签仍是单字符串

## 4. FXML shell 的定位

当前已经引入的 FXML 不是“所有页面都全面 FXML 化”后的最终形态，而是一个较轻量的外壳层。

当前真实情况：

- FXML 入口主要是 `main-shell.fxml`
- `MainController` 负责装配左侧边栏、中央主视图切换和右侧详情面板
- 主要业务视图仍由 Java 代码构造，而不是每个页面都已有独立 `FXML + Controller + ViewModel`

这意味着：

- 当前已经从纯代码启动根节点走向 `FXMLLoader`
- 但还没有完成到“每个屏幕都拥有独立 `FXML + Controller + ViewModel`”的彻底重构

## 5. 已经完成的重构

当前已经真实完成的部分包括：

- Java 21 / JavaFX 21 / Maven 插件升级
- FXML shell 引入
- `ApplicationContext` 组合根引入
- `config -> data -> application -> ui` 方向初步建立
- 主题与偏好从 `MainController` 中抽到 `ThemeService` / `JavaPreferencesStore`
- UI 层不再像旧版本那样普遍直接 `new ScheduleDAO()`
- 默认本地运行基线已切到 SQLite
- `target/` 已停止跟踪

## 6. 当前仍保留的 legacy

这些内容在当前仓库里仍是真实存在的技术债，不应被文档美化掉：

- `MainController` 仍是过渡态 shell controller，职责偏重
- `ScheduleDAO` 仍存在，并且仍是 MySQL 兼容数据访问链路的一部分
- `Connectdatabase` 仍存在，并保留 legacy MySQL fallback 逻辑
- `Schedule` 仍是旧模型与新分层之间的共享对象
- 主要视图仍在 `com.example.view`，还没有完全迁入细粒度的 `ui/<screen>` 结构

需要特别说明的是：

- MySQL 兼容链路仍在仓库中
- 但它已经不是默认本地运行链路
- 当前默认本地运行链路已经是 SQLite

## 7. 跨层协作方式

当前比较稳定的协作链路主要有 3 条：

### 7.1 主题与偏好

```text
MainController
  -> ThemeService
  -> JavaPreferencesStore
```

### 7.2 日程读写

```text
View / Controller
  -> ScheduleService
  -> ScheduleRepository
  -> SqliteScheduleRepository
```

或者在兼容模式下：

```text
View / Controller
  -> ScheduleService
  -> ScheduleRepository
  -> JdbcScheduleRepository
  -> ScheduleDAO
```

### 7.3 完成状态协同

```text
MainController
  -> ScheduleCompletionCoordinator
  -> ScheduleCompletionParticipant views
```

这条协同链路已经接入了列表、时间轴、热力图和详情面板，因此跨视图切换完成状态已经不再是各自孤立实现。

## 8. 为什么说它仍是过渡态

虽然当前已经引入分层、组合根和 FXML shell，但它还没有达到最终架构目标，原因主要是：

- 视图层还没有彻底分拆成每个屏幕独立 controller / viewmodel
- 领域模型还没升级到分钟级时间、全局 ID 和同步字段
- legacy DAO 仍保留在兼容链路中
- `MainController` 还没有完成彻底瘦身

所以当前最准确的描述不是“架构升级已经完成”，而是：

> 架构升级已经起步，并建立了可用的中间态基线。

## 9. 已确定的后续演进方向

这些方向已经确定，但尚未在当前代码中全部落地：

### 9.1 本地持久化

- 继续以 SQLite 作为默认本地运行基线
- 把应用强化成真正的本地优先桌面应用
- 把 MySQL 兼容链路继续收缩到导入 / 兼容用途

### 9.2 云端同步

- 云端中心库固定为 PostgreSQL
- 中间层为 API / Sync 服务
- 客户端继续优先读写本地库

### 9.3 领域模型

- 从当前 `Schedule` 演进到更适合同步和商业化的模型
- 支持：
  - 分钟级时间
  - 全局 UUID
  - 提醒 / 标签 / 重复规则
  - 同步状态
  - 软删除

### 9.4 UI 层

- 当前 `com.example.view` 里的主视图会继续保留一段时间
- 但后续目标是更细粒度的 `FXML + Controller + ViewModel`

## 10. 与改造计划的关系

本文档解释的是“现在的架构是什么”。  
真正的分阶段演进路线见：

- [`todo/改造计划/`](./todo/%E6%94%B9%E9%80%A0%E8%AE%A1%E5%88%92/)

建议按这个方式理解：

- `ARCHITECTURE.md`：当前事实
- `todo/改造计划/`：后续施工路线
