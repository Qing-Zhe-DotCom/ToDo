# ToDo 数据库与运行基线说明

本文档描述的是当前仓库已经落地的真实数据库与运行状态；未来已确定路线会单列说明，但不会把目标态写成当前已落地事实。

如果你只想快速启动项目，先看 [README.md](./README.md)。  
如果你要继续维护数据库与配置链路，请重点看本文档和 [DEV.md](./DEV.md)。

## 1. 当前运行基线

当前客户端仍是源码运行版桌面应用：

- 运行模块：[`todo/`](./todo/)
- 构建文件：[`todo/pom.xml`](./todo/pom.xml)
- 启动入口：[`todo/src/main/java/com/example/MainApp.java`](./todo/src/main/java/com/example/MainApp.java)
- JavaFX shell：[`todo/src/main/resources/com/example/ui/main-shell.fxml`](./todo/src/main/resources/com/example/ui/main-shell.fxml)

当前技术基线：

- Java 21
- JavaFX 21.0.7
- Maven 3.9+
- Surefire 模块化测试，`forkCount=0`

## 2. 当前数据库事实

### 当前默认数据库已经是 SQLite

默认配置来自：

- [`todo/src/main/resources/application-defaults.properties`](./todo/src/main/resources/application-defaults.properties)

当前默认值：

```properties
todo.app.version=${project.version}
todo.app.default-theme=light
todo.app.default-schedule-card-style=Classic
todo.db.mode=sqlite
todo.db.driver=org.sqlite.JDBC
todo.db.url=
todo.db.user=
todo.db.password=
todo.app.data-dir=
todo.db.sqlite.path=
```

这意味着当前默认现实是：

- 程序默认优先打开本地 SQLite
- 程序默认不再要求你先启动本机 MySQL
- SQLite 文件路径未显式配置时，由应用数据目录自动推导

### 默认本地数据目录

当前路径决策来自：

- [`todo/src/main/java/com/example/config/AppDataPaths.java`](./todo/src/main/java/com/example/config/AppDataPaths.java)

默认情况下：

- Windows：`%APPDATA%\ToDo\`
- macOS：`~/Library/Application Support/ToDo/`
- Linux：`$XDG_DATA_HOME/ToDo/` 或 `~/.local/share/ToDo/`

默认 SQLite 文件名：

- `todo.sqlite`

同时还会在数据目录下创建：

- `logs/`
- `backups/`

### MySQL 仍然存在，但不是默认链路

当前仓库里仍保留 MySQL 相关对象：

- [`todo/src/main/java/com/example/databaseutil/ScheduleDAO.java`](./todo/src/main/java/com/example/databaseutil/ScheduleDAO.java)
- [`todo/src/main/java/com/example/databaseutil/Connectdatabase.java`](./todo/src/main/java/com/example/databaseutil/Connectdatabase.java)
- [`todo/src/main/java/com/example/data/JdbcConnectionFactory.java`](./todo/src/main/java/com/example/data/JdbcConnectionFactory.java)
- [`todo/src/main/java/com/example/data/SchemaInitializer.java`](./todo/src/main/java/com/example/data/SchemaInitializer.java)

但它们现在的定位是：

- legacy 兼容链路
- 导入 / 迁移场景的保留能力
- 你显式切换到 `mysql` 模式时的备用链路

不要再把“仓库里还有 MySQL 代码”理解成“当前默认运行仍依赖 MySQL”。

## 3. 当前 SQLite schema 现实

当前 SQLite 迁移执行入口位于：

- [`todo/src/main/java/com/example/data/SqliteMigrationRunner.java`](./todo/src/main/java/com/example/data/SqliteMigrationRunner.java)

当前已纳入迁移器的版本如下：

### V001：`schedules` 基表

迁移脚本：

- [`todo/src/main/resources/db/sqlite/V001__create_schedules.sql`](./todo/src/main/resources/db/sqlite/V001__create_schedules.sql)

当前真实含义：

- 建立 `schedules` 主表
- 主键仍是 `INTEGER PRIMARY KEY AUTOINCREMENT`
- 仍保留早期模型的单表设计
- `tags` 仍是单字符串
- `reminder_time` 仍是单字段

### V002：本地同步辅助字段

迁移脚本：

- [`todo/src/main/resources/db/sqlite/V002__add_local_sync_columns.sql`](./todo/src/main/resources/db/sqlite/V002__add_local_sync_columns.sql)

当前新增字段：

- `deleted_at`
- `version`
- `sync_status`
- `last_synced_at`
- `device_id`
- `metadata_json`

当前真实含义：

- 本地 schema 已经开始为同步建模预留字段
- 但当前仍没有 `sync_outbox`、`sync_checkpoint`、`device_registry` 等辅助表
- `sync_status` 等字段只是基础预留，不代表云同步已实现

### V003：分钟级时间字段

迁移脚本：

- [`todo/src/main/resources/db/sqlite/V003__add_minute_precision_schedule_times.sql`](./todo/src/main/resources/db/sqlite/V003__add_minute_precision_schedule_times.sql)

当前新增字段：

- `start_at`
- `due_at`

并执行数据回填：

- 旧 `start_date` 会回填为 `T00:00:00`
- 旧 `due_date` 会回填为 `T23:59:00`

当前真实含义：

- 数据库基线已经从纯日期模型向分钟级时间迈进一步
- 但只完成了 `start_at` / `due_at` 这一层
- 还没有统一 `end_at`、`completed_at`、时区、全天任务、时间精度等更完整语义

### 当前 schema 的准确判断

截至当前代码，最准确的描述是：

- SQLite 默认链路：已完成
- V001 / V002 / V003：已落地
- `schedules` 单表：仍是当前事实来源
- 自增整数主键：仍是当前事实
- 分钟级时间：已部分落地
- 同步字段：已部分落地
- `schedule_item` / 子表 / outbox / checkpoint：尚未落地

## 4. 当前 Repository 与数据访问现实

当前链路已经不是简单的“视图直接连数据库”，但也还没有彻底完成最终重构。  
真实链路取决于数据库模式。

### 4.1 默认 SQLite 链路

```text
MainController / Views
  -> ScheduleService
  -> ScheduleRepository
  -> SqliteScheduleRepository
  -> SqliteConnectionFactory
  -> SqliteMigrationRunner
  -> SQLite
```

关键类：

- [`todo/src/main/java/com/example/application/ApplicationContext.java`](./todo/src/main/java/com/example/application/ApplicationContext.java)
- [`todo/src/main/java/com/example/data/ScheduleRepository.java`](./todo/src/main/java/com/example/data/ScheduleRepository.java)
- [`todo/src/main/java/com/example/data/SqliteScheduleRepository.java`](./todo/src/main/java/com/example/data/SqliteScheduleRepository.java)
- [`todo/src/main/java/com/example/data/SqliteConnectionFactory.java`](./todo/src/main/java/com/example/data/SqliteConnectionFactory.java)
- [`todo/src/main/java/com/example/data/SqliteMigrationRunner.java`](./todo/src/main/java/com/example/data/SqliteMigrationRunner.java)

### 4.2 legacy MySQL 兼容链路

```text
MainController / Views
  -> ScheduleService
  -> ScheduleRepository
  -> JdbcScheduleRepository
  -> ScheduleDAO
  -> Connectdatabase
  -> JdbcConnectionFactory
  -> SchemaInitializer
  -> MySQL
```

关键类：

- [`todo/src/main/java/com/example/data/JdbcScheduleRepository.java`](./todo/src/main/java/com/example/data/JdbcScheduleRepository.java)
- [`todo/src/main/java/com/example/databaseutil/ScheduleDAO.java`](./todo/src/main/java/com/example/databaseutil/ScheduleDAO.java)
- [`todo/src/main/java/com/example/databaseutil/Connectdatabase.java`](./todo/src/main/java/com/example/databaseutil/Connectdatabase.java)
- [`todo/src/main/java/com/example/data/JdbcConnectionFactory.java`](./todo/src/main/java/com/example/data/JdbcConnectionFactory.java)
- [`todo/src/main/java/com/example/data/SchemaInitializer.java`](./todo/src/main/java/com/example/data/SchemaInitializer.java)

## 5. 当前各层的数据库职责

### `ConfigurationLoader`

- 负责决定应用版本、数据库模式、驱动、URL、用户和密码从哪里来
- 当前默认输出 SQLite 配置对象

### `AppDataPaths`

- 负责解析应用数据目录
- 负责推导 SQLite 文件路径
- 负责 `logs/` 与 `backups/` 等目录定位

### `SqliteConnectionFactory`

- 负责加载 SQLite JDBC 驱动
- 负责确保本地数据目录存在
- 负责建立 SQLite 连接
- 负责配置 `PRAGMA foreign_keys=ON`、`journal_mode=WAL` 和 `synchronous=NORMAL`

### `SqliteMigrationRunner`

- 负责执行 SQLite schema migration
- 当前会按版本执行：
  - `V001__create_schedules.sql`
  - `V002__add_local_sync_columns.sql`
  - `V003__add_minute_precision_schedule_times.sql`

### `SqliteScheduleRepository`

- 负责默认 SQLite 模式下的日程读写
- 是当前默认本地运行主链路上的 Repository 实现
- 当前已感知 `version`、`sync_status`、`deleted_at`
- 但仍是围绕单表 `schedules` 和 `Schedule` 工作

### 当前必须特别说明的中间态

当前数据层还处于“已经为未来演进预留字段，但行为并未全部升级”的阶段，例如：

- `deleted_at` 列已经存在，但删除逻辑当前仍是物理删除，不是完整 soft delete
- `sync_status` / `version` 已存在，但没有 outbox / checkpoint 流程
- `start_at` / `due_at` 已存在，但没有完整的统一时间语义体系

## 6. 当前配置加载规则

当前配置加载入口位于：

- [`todo/src/main/java/com/example/config/ConfigurationLoader.java`](./todo/src/main/java/com/example/config/ConfigurationLoader.java)

当前配置覆盖顺序是：

1. classpath 默认值
2. 根目录 `application.properties`
3. `config/application.properties`
4. 环境变量 `TODO_CONFIG_FILE` 指向的外部配置文件
5. 对应环境变量覆盖具体键值

当前重要配置 / 环境变量包括：

- `todo.app.version` / `TODO_APP_VERSION`
- `todo.app.data-dir` / `TODO_APP_DATA_DIR`
- `todo.db.mode` / `TODO_DB_MODE`
- `todo.db.driver` / `TODO_DB_DRIVER`
- `todo.db.url` / `TODO_DB_URL`
- `todo.db.user` / `TODO_DB_USER`
- `todo.db.password` / `TODO_DB_PASSWORD`
- `todo.db.sqlite.path` / `TODO_DB_SQLITE_PATH`
- `TODO_CONFIG_FILE`

模式相关配置对象位于：

- [`todo/src/main/java/com/example/config/DatabaseProperties.java`](./todo/src/main/java/com/example/config/DatabaseProperties.java)

当前支持：

- `isSqliteMode()`
- `isMysqlMode()`

## 7. 为什么有时仍会出现 MySQL 报错

你现在遇到的典型历史故障是：

```text
Access denied for user 'root'@'localhost'
```

在当前代码里，这类报错已经不应是“默认首次运行必然遇到”的情况。  
通常只有下面几种场景还会触发它：

- 你显式把 `todo.db.mode` 或 `TODO_DB_MODE` 改成了 `mysql`
- 你在验证 legacy MySQL 兼容链路
- 某次外部配置文件覆盖了默认 SQLite 配置

如果你没有主动切到 MySQL，但仍看到了类似错误，优先检查：

- 当前终端里的环境变量是否残留了 `TODO_DB_MODE=mysql`
- 根目录或 `config/` 下是否存在覆盖配置文件
- `TODO_CONFIG_FILE` 是否指向了旧的 MySQL 配置

## 8. 当前版本号现实

当前应用版本链路位于：

- [`todo/pom.xml`](./todo/pom.xml)
- [`todo/src/main/resources/application-defaults.properties`](./todo/src/main/resources/application-defaults.properties)
- [`todo/src/main/java/com/example/config/ConfigurationLoader.java`](./todo/src/main/java/com/example/config/ConfigurationLoader.java)
- [`todo/src/main/java/com/example/config/AppProperties.java`](./todo/src/main/java/com/example/config/AppProperties.java)
- [`todo/src/main/java/com/example/controller/MainController.java`](./todo/src/main/java/com/example/controller/MainController.java)

当前最准确的描述是：

- 应用版本来自 `pom.xml`
- 通过 resources filtering 注入 `todo.app.version`
- 设置中心详情页会显示这个版本
- 当前还没有安装包版本链路

## 9. 当前现存问题

### 可用性问题

- SQLite 默认链路已经落地，但首次启动、损坏恢复和迁移体验仍可继续强化
- 外部配置一旦切回 MySQL，用户仍可能遇到历史数据库环境问题

### 架构问题

- `ScheduleDAO` 与 `Repository` 并存，迁移尚未完全完成
- `Connectdatabase` 仍是 legacy 静态入口
- `ScheduleRepository` 仍围绕旧模型设计

### 领域模型问题

- 当前模型仍未升级到全局 UUID 主键
- 分钟级时间只部分落地
- 仍未完整支持标签、提醒、重复规则拆表
- 同步辅助表尚未建立

### 文档与认知问题

- 很容易误把“SQLite / PostgreSQL 路线”理解成所有后续工作都已经完成
- 也很容易因为仓库里仍存在 MySQL 代码，就误判默认运行仍依赖 MySQL

## 10. 已确定的后续路线

### 10.1 本地数据库

- 当前默认本地数据库基线继续保持 SQLite
- 目标是把桌面端做成真正的本地优先与零配置可启动体验

### 10.2 数据模型

- 当前 `Schedule` / `schedules` 会逐步演进到更适合同步和商业化的模型
- 重点升级方向：
  - 全局 UUID
  - 更完整的时间语义
  - 提醒 / 标签 / 重复规则子表
  - 同步辅助表
  - 软删除行为真正落地

### 10.3 云端数据库

- 云端将采用 PostgreSQL 作为中心库
- 客户端不直连云数据库
- 通过 API / Sync 服务完成登录、同步、审计与后续商业化扩展

## 11. 与改造计划的关系

本文档只解释“当前数据库与运行事实是什么”。  
真正的分阶段改造路线见：

- [`todo/改造计划/`](./todo/%E6%94%B9%E9%80%A0%E8%AE%A1%E5%88%92/)

建议按这套方式理解：

- `DATABASE_AND_RUNTIME.md`：当前事实
- `todo/改造计划/01-04`：后续怎么改
