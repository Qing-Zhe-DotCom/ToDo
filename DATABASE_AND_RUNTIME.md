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

## 3. 当前配置加载规则

当前配置加载入口位于：

- [`todo/src/main/java/com/example/config/ConfigurationLoader.java`](./todo/src/main/java/com/example/config/ConfigurationLoader.java)

当前配置覆盖顺序是：

1. classpath 默认值
2. 根目录 `application.properties`
3. `config/application.properties`
4. 环境变量 `TODO_CONFIG_FILE` 指向的外部配置文件
5. 对应环境变量覆盖具体键值

数据库相关环境变量当前支持：

- `TODO_DB_MODE`
- `TODO_DB_DRIVER`
- `TODO_DB_URL`
- `TODO_DB_USER`
- `TODO_DB_PASSWORD`
- `TODO_DB_SQLITE_PATH`
- `TODO_APP_DATA_DIR`
- `TODO_CONFIG_FILE`

模式相关配置对象位于：

- [`todo/src/main/java/com/example/config/DatabaseProperties.java`](./todo/src/main/java/com/example/config/DatabaseProperties.java)

当前支持：

- `isSqliteMode()`
- `isMysqlMode()`

## 4. 当前数据库访问链路

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

- 负责决定数据库模式、驱动、URL、用户和密码从哪里来
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

### `SqliteScheduleRepository`

- 负责默认 SQLite 模式下的日程读写
- 是当前默认本地运行主链路上的 Repository 实现

### `Connectdatabase`

- 负责 legacy MySQL 配置、连接工厂与 schema 初始化串联
- 当前仍保留静态入口风格
- 内部保留 `resolveLegacyMySqlProperties()` 作为 MySQL fallback 逻辑

### `ScheduleDAO`

- 承载旧的较大体量日程 CRUD SQL 逻辑
- 主要用于 legacy MySQL 兼容链路

### `JdbcScheduleRepository`

- 作为新分层里的 MySQL 兼容适配层存在
- 本质上仍是对 `ScheduleDAO` 的一层包装

## 6. 为什么有时仍会出现 MySQL 报错

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

## 7. 当前可运行要求

如果你要按当前事实运行源码版，需要满足：

### JDK

- Java 21

推荐：

```powershell
$env:JAVA_HOME='C:\Program Files\Microsoft\jdk-21.0.10.7-hotspot'
$env:JAVA21_HOME=$env:JAVA_HOME
$env:PATH="$env:JAVA_HOME\bin;$env:PATH"
```

### Maven

- Maven 3.9+

### 数据库

默认 SQLite 模式下：

- 不要求本机安装 MySQL
- 只要求当前用户对应用数据目录有写权限

如果你显式使用 MySQL 兼容模式：

- 本机或目标地址上有可访问的 MySQL
- `todo_db` 或对应 schema 可连接
- 当前用户密码与配置一致

### 推荐运行命令

```powershell
cd .\todo
mvn -f .\pom.xml clean javafx:run
```

## 8. 当前现存问题

### 可用性问题

- SQLite 默认链路已经落地，但首次启动、损坏恢复和迁移体验仍可继续强化
- 外部配置一旦切回 MySQL，用户仍可能遇到历史数据库环境问题

### 架构问题

- `ScheduleDAO` 与 `Repository` 并存，迁移尚未完全完成
- `Connectdatabase` 仍是 legacy 静态入口

### 领域模型问题

- 当前模型仍未升级到分钟级时间主模型
- 仍未完整支持全局 ID、同步字段、提醒 / 标签 / 重复规则拆分

### 文档与认知问题

- 很容易误把“SQLite / PostgreSQL 路线”理解成所有后续工作都已经完成
- 也很容易因为仓库里仍存在 MySQL 代码，就误判默认运行仍依赖 MySQL

## 9. 已确定的后续路线

### 9.1 本地数据库

- 当前默认本地数据库基线继续保持 SQLite
- 目标是把桌面端做成真正的本地优先与零配置可启动体验

### 9.2 云端数据库

- 云端将采用 PostgreSQL 作为中心库
- 客户端不直连云数据库
- 通过 API / Sync 服务完成登录、同步、审计与后续商业化扩展

### 9.3 数据模型

- 当前 `Schedule` 会逐步演进到更适合同步和商业化的模型
- 重点升级方向：
  - 分钟级时间
  - 全局 UUID
  - 提醒 / 标签 / 重复规则
  - 同步状态
  - 软删除

## 10. 与改造计划的关系

本文档只解释“当前数据库与运行事实是什么”。  
真正的分阶段改造路线见：

- [`todo/改造计划/`](./todo/%E6%94%B9%E9%80%A0%E8%AE%A1%E5%88%92/)

建议按这套方式理解：

- `DATABASE_AND_RUNTIME.md`：当前事实
- `todo/改造计划/01-04`：后续怎么改
