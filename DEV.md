# ToDo 开发维护说明

本文档描述的是当前仓库已经落地的真实开发基线；未来路线会单列说明，但不会把规划写成已完成事实。

如果你只想快速了解项目，请先看 [README.md](./README.md)。  
如果你更关心当前整体架构，请看 [ARCHITECTURE.md](./ARCHITECTURE.md)。  
如果你更关心数据库与运行基线，请看 [DATABASE_AND_RUNTIME.md](./DATABASE_AND_RUNTIME.md)。

## 1. 当前开发基线

当前项目是一个单 Maven 模块的 JavaFX 桌面应用：

- Maven 模块目录：[`todo/`](./todo/)
- 模块名：`com.example`
- 启动入口：[`com.example.MainApp`](./todo/src/main/java/com/example/MainApp.java)
- JavaFX 壳层：[`main-shell.fxml`](./todo/src/main/resources/com/example/ui/main-shell.fxml)

当前构建技术基线：

- Java 21
- JavaFX 21.0.7
- Maven 3.9+
- Maven Compiler Plugin 3.15.0
- Maven Surefire Plugin 3.5.5
- JavaFX Maven Plugin 0.0.8
- Maven Toolchains Plugin 3.2.0
- Maven Enforcer Plugin 3.6.2

关键文件：

- [`todo/pom.xml`](./todo/pom.xml)
- [`todo/src/main/java/module-info.java`](./todo/src/main/java/module-info.java)

## 2. 当前目录与职责

当前代码组织主要如下：

```text
root
├─ README.md
├─ DEV.md
├─ USER.md
├─ ARCHITECTURE.md
├─ DATABASE_AND_RUNTIME.md
├─ UI_VIEWS.md
└─ todo/
   ├─ pom.xml
   ├─ src/main/java/com/example/
   │  ├─ MainApp.java
   │  ├─ application/
   │  ├─ config/
   │  ├─ controller/
   │  ├─ data/
   │  ├─ databaseutil/
   │  ├─ model/
   │  └─ view/
   ├─ src/main/resources/
   └─ 改造计划/
```

当前分层方向是：

```text
config -> data -> application -> ui
```

但这里的 `ui` 仍然是过渡形态，现实情况是：

- `MainController` 仍承担 shell controller 与较多页面编排职责
- `com.example.view` 里仍保留大量 Java 自定义视图
- `ScheduleDAO` / `Connectdatabase` 仍存在于 legacy 数据访问链路中

## 3. 构建、测试与运行

### 3.1 推荐前提

推荐在 PowerShell 中显式切到 JDK 21：

```powershell
$env:JAVA_HOME='C:\Program Files\Microsoft\jdk-21.0.10.7-hotspot'
$env:JAVA21_HOME=$env:JAVA_HOME
$env:PATH="$env:JAVA_HOME\bin;$env:PATH"
```

### 3.2 编译

```powershell
cd .\todo
mvn -f .\pom.xml clean compile
```

### 3.3 测试

```powershell
cd .\todo
mvn -f .\pom.xml test
```

### 3.4 启动

```powershell
cd .\todo
mvn -f .\pom.xml clean javafx:run
```

### 3.5 为什么推荐 `-f .\pom.xml`

在当前中文路径和 PowerShell 特殊路径表示下，`mvn.cmd` 偶尔会把工作目录识别错误，出现：

```text
there is no POM in this directory (C:\Windows)
```

显式写 `-f .\pom.xml` 是当前最稳的方式。

## 4. 当前测试体系现状

当前测试体系已经恢复到可运行状态，但有一个明确的现实约束：

- `useModulePath=true`
- `forkCount=0`

原因不是回退到 classpath，而是为了绕开当前 Windows 中文路径下，Surefire fork JVM 时 `@argfile` 路径乱码导致的 JPMS 问题。

这意味着：

- 当前测试仍是模块化测试
- 但采用 in-process 执行，而不是 fork 新 JVM

关键配置位置：

- [`todo/pom.xml`](./todo/pom.xml)

当前重点测试主要覆盖：

- 模型与纯逻辑
- 视图的部分静态辅助逻辑
- CSS 选择器与主题文件
- 完成状态协同逻辑
- SQLite 配置、迁移与仓储

完整 JavaFX UI 自动化测试仍未接入。

## 5. 资源过滤与版本号注入

当前工程已经形成应用版本的构建链路：

```text
todo/pom.xml
  -> src/main/resources/application-defaults.properties
  -> ConfigurationLoader
  -> AppProperties
  -> MainController 设置详情页
```

当前真实情况：

- `pom.xml` 中的 `<version>` 是应用版本源头
- `application-defaults.properties` 中的 `todo.app.version` 通过 Maven resources filtering 注入
- `ConfigurationLoader` 会把 `todo.app.version` 读入 `AppProperties`
- 设置中心详情页会显示当前应用版本

要点：

- 如果你改了 `pom.xml` 里的 `<version>`，需要重新编译并重新启动应用
- 当前只形成了“应用版本”链路
- 当前还没有单独的“安装包版本”链路，因为安装包体系尚未落地

## 6. 当前架构现实

### 已经完成的部分

- 启动入口已改成 `MainApp -> FXMLLoader -> MainController`
- 已引入组合根 [`ApplicationContext`](./todo/src/main/java/com/example/application/ApplicationContext.java)
- 已拆出：
  - `config`
  - `data`
  - `application`
- UI 不再像早期那样普遍直接 `new ScheduleDAO()`
- 默认本地运行已切到 SQLite
- SQLite schema migration 已有 V001 / V002 / V003

### 仍处于过渡态的部分

- `MainController` 仍然比较重
- `ScheduleDAO` 仍然被 `JdbcScheduleRepository` 包装使用
- `Connectdatabase` 仍然代表 legacy MySQL 兼容连接入口
- 领域模型仍以 `Schedule` 为中心，尚未升级到全局 UUID 与聚合子表
- 分钟级时间只完成了 `start_at` / `due_at` 这一步，尚未统一全部时间语义

### 当前已确定方向

- 本地数据库：SQLite
- 云端中心库：PostgreSQL
- 同步方式：Local-first Sync
- 数据模型方向：从 `Schedule` 演进到支持分钟级时间、全局 ID、提醒、标签、重复规则和同步字段的模型

更完整的说明见：

- [ARCHITECTURE.md](./ARCHITECTURE.md)
- [DATABASE_AND_RUNTIME.md](./DATABASE_AND_RUNTIME.md)
- [todo/改造计划/](./todo/%E6%94%B9%E9%80%A0%E8%AE%A1%E5%88%92/)

## 7. 当前数据库现实

当前默认配置已经是 SQLite：

- `todo.app.version`
- `todo.db.mode=sqlite`
- `todo.db.driver=org.sqlite.JDBC`
- `todo.db.url=`
- `todo.db.user=`
- `todo.db.password=`
- `todo.app.data-dir=`
- `todo.db.sqlite.path=`

默认模式下意味着：

- 直接运行当前代码时，不要求你先启动本机 MySQL
- 应用会在本地数据目录初始化 SQLite 文件库
- 旧 MySQL 代码链路仍保留，但只用于 legacy / 兼容 / 导入场景，或你显式切换到 `mysql` 模式时

配置入口位于：

- [`todo/src/main/java/com/example/config/ConfigurationLoader.java`](./todo/src/main/java/com/example/config/ConfigurationLoader.java)
- [`todo/src/main/resources/application-defaults.properties`](./todo/src/main/resources/application-defaults.properties)

SQLite 相关的当前关键对象：

- [`todo/src/main/java/com/example/config/AppDataPaths.java`](./todo/src/main/java/com/example/config/AppDataPaths.java)
- [`todo/src/main/java/com/example/data/SqliteConnectionFactory.java`](./todo/src/main/java/com/example/data/SqliteConnectionFactory.java)
- [`todo/src/main/java/com/example/data/SqliteMigrationRunner.java`](./todo/src/main/java/com/example/data/SqliteMigrationRunner.java)
- [`todo/src/main/java/com/example/data/SqliteScheduleRepository.java`](./todo/src/main/java/com/example/data/SqliteScheduleRepository.java)

当前支持的重要环境变量：

- `TODO_CONFIG_FILE`
- `TODO_APP_DATA_DIR`
- `TODO_APP_VERSION`
- `TODO_DB_MODE`
- `TODO_DB_SQLITE_PATH`
- `TODO_DB_DRIVER`
- `TODO_DB_URL`
- `TODO_DB_USER`
- `TODO_DB_PASSWORD`

## 8. 常见开发任务

### 看入口

- [`todo/src/main/java/com/example/MainApp.java`](./todo/src/main/java/com/example/MainApp.java)
- [`todo/src/main/resources/com/example/ui/main-shell.fxml`](./todo/src/main/resources/com/example/ui/main-shell.fxml)

### 看组合根

- [`todo/src/main/java/com/example/application/ApplicationContext.java`](./todo/src/main/java/com/example/application/ApplicationContext.java)

### 看配置加载

- [`todo/src/main/java/com/example/config/ConfigurationLoader.java`](./todo/src/main/java/com/example/config/ConfigurationLoader.java)
- [`todo/src/main/resources/application-defaults.properties`](./todo/src/main/resources/application-defaults.properties)

### 看数据访问

- [`todo/src/main/java/com/example/data/ScheduleRepository.java`](./todo/src/main/java/com/example/data/ScheduleRepository.java)
- [`todo/src/main/java/com/example/data/SqliteScheduleRepository.java`](./todo/src/main/java/com/example/data/SqliteScheduleRepository.java)
- [`todo/src/main/java/com/example/data/JdbcScheduleRepository.java`](./todo/src/main/java/com/example/data/JdbcScheduleRepository.java)
- [`todo/src/main/java/com/example/databaseutil/ScheduleDAO.java`](./todo/src/main/java/com/example/databaseutil/ScheduleDAO.java)
- [`todo/src/main/java/com/example/databaseutil/Connectdatabase.java`](./todo/src/main/java/com/example/databaseutil/Connectdatabase.java)

### 看主视图

- [`todo/src/main/java/com/example/view/ScheduleListView.java`](./todo/src/main/java/com/example/view/ScheduleListView.java)
- [`todo/src/main/java/com/example/view/TimelineView.java`](./todo/src/main/java/com/example/view/TimelineView.java)
- [`todo/src/main/java/com/example/view/HeatmapView.java`](./todo/src/main/java/com/example/view/HeatmapView.java)
- [`todo/src/main/java/com/example/view/InfoPanelView.java`](./todo/src/main/java/com/example/view/InfoPanelView.java)

### 看改造计划

- [`todo/改造计划/`](./todo/%E6%94%B9%E9%80%A0%E8%AE%A1%E5%88%92/)

## 9. 当前技术债

最值得持续关注的技术债：

- `MainController` 仍偏重
- `ScheduleDAO` 与新分层并存
- `Schedule` 仍未升级到全局 UUID 和聚合主模型
- 分钟级时间只完成部分落地
- 完整 UI 自动化测试不足
- 文档与代码仍要持续防止“现状”和“未来路线”混写

## 10. 文档维护原则

根目录说明文档遵循这组约定：

- `README.md`：入口总览
- `DEV.md`：开发维护
- `USER.md`：用户手册
- `ARCHITECTURE.md`：当前架构
- `DATABASE_AND_RUNTIME.md`：数据库与运行基线
- `UI_VIEWS.md`：视图与交互

说明性文档只解释“现在是什么”和“已确定的未来方向”，不代替 [`todo/改造计划/`](./todo/%E6%94%B9%E9%80%A0%E8%AE%A1%E5%88%92/) 的阶段任务书。

## 11. FAQ

### 11.1 为什么 `mvn clean javafx:run` 会报找不到 POM

优先使用：

```powershell
mvn -f .\pom.xml clean javafx:run
```

### 11.2 为什么测试要 `forkCount=0`

因为当前中文路径下，Surefire fork JVM 会触发 JPMS `@argfile` 路径乱码问题；这是现阶段的现实约束。

### 11.3 为什么文档里说分层已经建立，但代码里还有 `databaseutil`

因为项目正处于过渡重构状态。  
`config / data / application` 已初步落地，但 legacy DAO 还未完全退出，尤其是 MySQL 兼容链路仍依赖它们。

### 11.4 为什么设置页版本号改了 `pom.xml` 之后有时没立即变

因为应用版本是在构建阶段注入资源文件的。  
只改 `pom.xml` 还不够，必须重新编译并重启应用。
