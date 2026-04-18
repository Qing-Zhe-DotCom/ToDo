# ToDo 开发维护说明

本文档描述的是当前仓库已经落地的真实开发基线；未来路线会单列说明，但不会把规划写成已完成事实。

如果你只想快速启动项目，请先看 [README.md](./README.md)。如果你更关心当前技术事实，请看 [ARCHITECTURE.md](./ARCHITECTURE.md)。如果你想从用户视角理解界面和交互，请看 [USER.md](./USER.md)。

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
└─ todo/
   ├─ pom.xml
   ├─ src/main/java/com/example/
   │  ├─ MainApp.java
   │  ├─ application/
   │  ├─ config/
   │  ├─ controller/
   │  ├─ data/
   │  ├─ model/
   │  └─ view/
   ├─ src/main/resources/
   ├─ src/test/java/
   └─ 改造计划/
```

当前分层方向是：

```text
config -> data -> application -> ui
```

现实中的几个关键点：

- `MainController` 仍承担 shell controller 与较多页面编排职责
- `com.example.view` 里仍保留大量 Java 自定义视图
- 当前主业务链路已经切到 `ScheduleItemService` / `ScheduleItemRepository`
- 部分 UI 与导航状态仍通过 `Schedule` 做兼容桥接

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

- 应用服务与纯逻辑
- 配置与路径决策
- Controller 状态与设置逻辑
- 主题、图标、本地化与字体资源
- JavaFX 视图中的可分离逻辑
- SQLite / 数据层兼容性相关逻辑

需要特别说明的是：测试目录里仍有少量历史命名，例如 `SqliteMigrationRunnerTest`、`SqliteScheduleRepositoryTest`。这些名字反映的是测试演化历史，不代表当前运行时代码仍存在同名实现类。

完整 JavaFX UI 自动化测试仍未接入。

## 5. 资源过滤、版本号与配置注入

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

## 6. 常见开发入口

### 看应用入口

- [`todo/src/main/java/com/example/MainApp.java`](./todo/src/main/java/com/example/MainApp.java)
- [`todo/src/main/resources/com/example/ui/main-shell.fxml`](./todo/src/main/resources/com/example/ui/main-shell.fxml)

### 看组合根

- [`todo/src/main/java/com/example/application/ApplicationContext.java`](./todo/src/main/java/com/example/application/ApplicationContext.java)

### 看配置加载

- [`todo/src/main/java/com/example/config/ConfigurationLoader.java`](./todo/src/main/java/com/example/config/ConfigurationLoader.java)
- [`todo/src/main/java/com/example/config/AppDataPaths.java`](./todo/src/main/java/com/example/config/AppDataPaths.java)
- [`todo/src/main/resources/application-defaults.properties`](./todo/src/main/resources/application-defaults.properties)

### 看应用服务与桥接现实

- [`todo/src/main/java/com/example/application/ScheduleItemService.java`](./todo/src/main/java/com/example/application/ScheduleItemService.java)
- [`todo/src/main/java/com/example/application/NavigationService.java`](./todo/src/main/java/com/example/application/NavigationService.java)
- [`todo/src/main/java/com/example/controller/MainController.java`](./todo/src/main/java/com/example/controller/MainController.java)

### 看数据层主链路

- [`todo/src/main/java/com/example/data/ScheduleItemRepository.java`](./todo/src/main/java/com/example/data/ScheduleItemRepository.java)
- [`todo/src/main/java/com/example/data/SqlScheduleItemRepository.java`](./todo/src/main/java/com/example/data/SqlScheduleItemRepository.java)
- [`todo/src/main/java/com/example/data/SqliteStageBSchemaManager.java`](./todo/src/main/java/com/example/data/SqliteStageBSchemaManager.java)
- [`todo/src/main/java/com/example/data/MysqlStageBSchemaManager.java`](./todo/src/main/java/com/example/data/MysqlStageBSchemaManager.java)
- [`todo/src/main/java/com/example/data/SqliteConnectionFactory.java`](./todo/src/main/java/com/example/data/SqliteConnectionFactory.java)
- [`todo/src/main/java/com/example/data/JdbcConnectionFactory.java`](./todo/src/main/java/com/example/data/JdbcConnectionFactory.java)

### 看主要视图

- [`todo/src/main/java/com/example/view/ScheduleListView.java`](./todo/src/main/java/com/example/view/ScheduleListView.java)
- [`todo/src/main/java/com/example/view/TimelineView.java`](./todo/src/main/java/com/example/view/TimelineView.java)
- [`todo/src/main/java/com/example/view/HeatmapView.java`](./todo/src/main/java/com/example/view/HeatmapView.java)
- [`todo/src/main/java/com/example/view/InfoPanelView.java`](./todo/src/main/java/com/example/view/InfoPanelView.java)
- [`todo/src/main/java/com/example/view/ScheduleDialog.java`](./todo/src/main/java/com/example/view/ScheduleDialog.java)

## 7. 当前技术债

最值得持续关注的技术债：

- `MainController` 仍偏重
- `NavigationService` 和部分视图仍围绕 `Schedule` 维护选中态与兼容桥接
- UI 还没有彻底分拆成每个页面独立的 `FXML + Controller + ViewModel`
- Stage-B schema 已经落地，但旧版 SQLite 脚本与历史命名仍留在仓库中
- 完整 UI 自动化测试不足
- 文档与代码仍需持续防止“现状”和“未来路线”混写

## 8. 文档维护原则

根目录说明文档遵循这组分工：

- `README.md`：入口总览
- `DEV.md`：开发维护
- `ARCHITECTURE.md`：当前技术事实
- `USER.md`：用户与交互说明

说明性文档只解释“现在是什么”和“已确定的未来方向”，不代替 [`todo/改造计划/`](./todo/%E6%94%B9%E9%80%A0%E8%AE%A1%E5%88%92/) 的阶段任务书。

## 9. FAQ

### 9.1 为什么 `mvn clean javafx:run` 会报找不到 POM

优先使用：

```powershell
mvn -f .\pom.xml clean javafx:run
```

### 9.2 为什么测试要 `forkCount=0`

因为当前中文路径下，Surefire fork JVM 会触发 JPMS `@argfile` 路径乱码问题；这是现阶段的现实约束。

### 9.3 为什么当前代码已经切到 `ScheduleItemService`，UI 里却还看得到 `Schedule`

因为项目处于过渡架构期。  
当前默认数据链路已经是 `ScheduleItem` 体系，但 `NavigationService`、`MainController` 和部分视图仍通过 `Schedule` 做兼容桥接。

### 9.4 为什么测试里还能看到旧命名

因为测试是逐步演进过来的。  
少量测试类名仍保留旧阶段命名，但应以当前 `ApplicationContext`、`ScheduleItemService`、`SqlScheduleItemRepository` 和 Stage-B schema 为准理解运行时结构。
