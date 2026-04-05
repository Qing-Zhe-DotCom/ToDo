# ToDo 开发维护说明

本文档描述的是当前仓库的真实状态；未来路线会单列说明，但不会把计划误写成已完成事实。

如果你只想快速了解项目，请先看 [README.md](./README.md)。  
如果你想看当前整体架构，请看 [ARCHITECTURE.md](./ARCHITECTURE.md)。  
如果你想看数据库与运行基线，请看 [DATABASE_AND_RUNTIME.md](./DATABASE_AND_RUNTIME.md)。

## 1. 当前开发基线

当前项目是一个单 Maven 模块的 JavaFX 桌面应用：

- Maven 模块目录：[`todo/`](./todo/)
- 模块名：`com.example`
- 启动入口：[`com.example.MainApp`](./todo/src/main/java/com/example/MainApp.java)
- JavaFX 壳层：[`main-shell.fxml`](./todo/src/main/resources/com/example/ui/main-shell.fxml)

当前构建技术基线：

- Java 21
- JavaFX 21.0.7
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

在当前中文路径和 PowerShell 特殊路径表示下，`mvn.cmd` 偶尔会把工作目录识别错，出现：

```text
there is no POM in this directory (C:\Windows)
```

显式写 `-f .\pom.xml` 是当前最稳的方式。

## 4. 测试体系现状

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

完整 JavaFX UI 自动化测试仍未接入。

## 5. 当前架构现实

### 已经完成的部分

- 启动入口已改成 `MainApp -> FXMLLoader -> MainController`
- 已引入组合根 [`ApplicationContext`](./todo/src/main/java/com/example/application/ApplicationContext.java)
- 已拆出：
  - `config`
  - `data`
  - `application`
- UI 不再像早期那样普遍直接 `new ScheduleDAO()`

### 仍处于过渡态的部分

- `MainController` 仍然比较重
- `ScheduleDAO` 仍然被 `JdbcScheduleRepository` 包装使用
- `Connectdatabase` 仍然代表默认数据库连接入口
- 默认数据库配置仍指向 MySQL
- 领域模型仍以 `Schedule` 为中心，尚未升级到分钟级时间和同步模型

### 当前已确定方向

- 本地数据库：SQLite
- 云端中心库：PostgreSQL
- 同步方式：Local-first Sync
- 数据模型方向：从 `Schedule` 演进到支持分钟级时间、全局 ID、提醒/标签/重复规则和同步字段的模型

更完整的说明见：

- [ARCHITECTURE.md](./ARCHITECTURE.md)
- [DATABASE_AND_RUNTIME.md](./DATABASE_AND_RUNTIME.md)
- [todo/改造计划](./todo/%E6%94%B9%E9%80%A0%E8%AE%A1%E5%88%92/)

## 6. 当前数据库现实

当前默认配置仍是 MySQL：

- 驱动：`com.mysql.cj.jdbc.Driver`
- URL：`jdbc:mysql://localhost:3306/todo_db`
- 用户：`root`
- 密码：空字符串

这意味着：

- 想直接运行当前代码，必须保证本机数据库连接可用
- 应用目前尚未完成 SQLite 本地化改造
- 数据库故障仍会反映到列表、时间轴、热力图等视图加载中

这部分不要在开发中误解成“已经切到 SQLite”。  
SQLite 是已确定路线，不是当前事实。

## 7. 常见开发任务

### 看入口

- [`todo/src/main/java/com/example/MainApp.java`](./todo/src/main/java/com/example/MainApp.java)
- [`todo/src/main/resources/com/example/ui/main-shell.fxml`](./todo/src/main/resources/com/example/ui/main-shell.fxml)

### 看组合根

- [`todo/src/main/java/com/example/application/ApplicationContext.java`](./todo/src/main/java/com/example/application/ApplicationContext.java)

### 看配置加载

- [`todo/src/main/java/com/example/config/ConfigurationLoader.java`](./todo/src/main/java/com/example/config/ConfigurationLoader.java)
- [`todo/src/main/resources/application-defaults.properties`](./todo/src/main/resources/application-defaults.properties)

### 看主视图

- [`todo/src/main/java/com/example/view/ScheduleListView.java`](./todo/src/main/java/com/example/view/ScheduleListView.java)
- [`todo/src/main/java/com/example/view/TimelineView.java`](./todo/src/main/java/com/example/view/TimelineView.java)
- [`todo/src/main/java/com/example/view/HeatmapView.java`](./todo/src/main/java/com/example/view/HeatmapView.java)
- [`todo/src/main/java/com/example/view/InfoPanelView.java`](./todo/src/main/java/com/example/view/InfoPanelView.java)

### 看改造计划

- [`todo/改造计划/`](./todo/%E6%94%B9%E9%80%A0%E8%AE%A1%E5%88%92/)

## 8. 当前技术债

最值得持续关注的技术债：

- 默认数据库仍是 MySQL，本地可用性不足
- `MainController` 仍偏重
- `ScheduleDAO` 与新分层并存
- 领域模型还没升级到分钟级时间
- UI 自动化测试不足
- 文档与代码容易在“现状”和“未来路线”上混淆

## 9. 文档维护原则

从这次重构开始，根目录说明文档遵循这套约定：

- `README.md`：入口总览
- `DEV.md`：开发维护
- `USER.md`：用户手册
- `ARCHITECTURE.md`：当前架构
- `DATABASE_AND_RUNTIME.md`：数据库与运行基线
- `UI_VIEWS.md`：视图与交互

说明性文档只解释“现在是什么”和“已确定的未来方向”，不代替 [`todo/改造计划`](./todo/%E6%94%B9%E9%80%A0%E8%AE%A1%E5%88%92/) 的阶段任务书。

## 10. FAQ

### 10.1 为什么 `mvn clean javafx:run` 会报找不到 POM？

优先使用：

```powershell
mvn -f .\pom.xml clean javafx:run
```

### 10.2 为什么测试要 `forkCount=0`？

因为当前中文路径下，Surefire fork JVM 会触发 JPMS `@argfile` 路径乱码问题；这是现阶段的现实约束。

### 10.3 为什么文档里说分层已建立，但代码里还有 `databaseutil`？

因为项目正处于过渡重构状态。  
`config / data / application` 已初步落地，但 legacy DAO 还未完全退出。

### 10.4 下一步最优先的工程任务是什么？

按已确定路线，优先级是：

1. 本地 SQLite 化
2. 数据模型升级
3. 云端同步
4. 商业化底座
