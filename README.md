# ToDo

本文档描述的是当前仓库已经落地的真实状态；未来路线会明确写出，但不会把计划误写成现状。

如果你只是想先把项目跑起来，先看本文；如果你准备继续维护代码，再看 [DEV.md](./DEV.md)；如果你想了解当前界面与交互，请看 [USER.md](./USER.md) 和 [UI_VIEWS.md](./UI_VIEWS.md)。

## 项目概览

当前仓库中的 ToDo 是一个以源码运行方式为主的 JavaFX 桌面应用，Maven 模块位于 [`todo/`](./todo/)。

当前真实基线：

- 单 Maven 模块
- Java 21
- JavaFX 21.0.7
- JPMS 模块名：`com.example`
- UI 入口：FXML shell + Java 自定义视图
- 分层方向：`config -> data -> application -> ui`
- 当前默认本地运行数据库：SQLite
- MySQL 仅保留为 legacy / 兼容 / 导入链路，不是默认本地运行前提
- 已确定的后续路线：本地 SQLite + 云端 PostgreSQL + Local-first Sync

## 当前能力

当前已经实际可用的能力包括：

- 日程列表管理
- 时间轴视图
- 热力图视图
- 右侧详情面板
- 设置中心
- 主题切换
- 日程卡片样式切换
- 待办数量驱动的窗口标题 / 任务栏角标
- 设置详情页中的应用版本显示

当前仍在过渡中的部分：

- `MainController` 仍然是过渡态的 shell controller
- `com.example.view` 中仍保留大量 Java 自定义视图
- `ScheduleDAO` / `Connectdatabase` 仍存在于 legacy 数据访问链路中
- 流程图页面仍是占位页
- 登录入口仍是占位提示
- 正式安装包 / 发布包尚未落地
- 云同步、账号体系、PostgreSQL 服务端尚未落地

## 快速开始

### 1. 进入模块目录

```powershell
cd .\todo
```

### 2. 显式切到 JDK 21

推荐使用 Microsoft OpenJDK 21：

```powershell
$env:JAVA_HOME='C:\Program Files\Microsoft\jdk-21.0.10.7-hotspot'
$env:JAVA21_HOME=$env:JAVA_HOME
$env:PATH="$env:JAVA_HOME\bin;$env:PATH"
java -version
mvn -version
```

### 3. 编译

```powershell
mvn -f .\pom.xml clean compile
```

### 4. 测试

```powershell
mvn -f .\pom.xml test
```

### 5. 启动桌面应用

```powershell
mvn -f .\pom.xml clean javafx:run
```

## 当前数据库现实

默认本地运行基线已经切到 SQLite，默认配置来自 [`todo/src/main/resources/application-defaults.properties`](./todo/src/main/resources/application-defaults.properties)：

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

这意味着：

- 默认运行时不会先要求你启动本地 MySQL
- 应用会优先使用 SQLite 本地文件库
- Windows 下默认数据目录通常位于 `%APPDATA%\ToDo\`
- 默认 SQLite 文件通常位于 `%APPDATA%\ToDo\todo.sqlite`

当前 SQLite schema 现实：

- `V001__create_schedules.sql`：建立 `schedules` 基表
- `V002__add_local_sync_columns.sql`：补充 `deleted_at`、`version`、`sync_status`、`last_synced_at`、`device_id`、`metadata_json`
- `V003__add_minute_precision_schedule_times.sql`：补充 `start_at`、`due_at`，并把旧日期字段回填为分钟级时间点

这表示数据库基线已经从纯日期模型向分钟级时间迈进一步，但当前仍是：

- 单表 `schedules`
- 自增整数主键
- 字符串 `tags`
- 单字段 `reminder_time`

MySQL 没有被彻底删掉，但它现在只用于：

- legacy 兼容链路
- 特定导入 / 迁移场景
- 你显式把 `todo.db.mode` 或 `TODO_DB_MODE` 切到 `mysql` 的情况

## 当前版本号现实

当前应用版本来源于：

```text
todo/pom.xml
  -> application-defaults.properties 资源过滤
  -> ConfigurationLoader / AppProperties
  -> 设置中心详情页显示
```

这意味着：

- `pom.xml` 中的 `<version>` 是当前唯一应用版本源头
- 设置中心详情页显示的是构建后的应用版本
- 当前尚未形成单独的安装包版本链路

## 常见启动问题

### 1. `There is no POM in this directory (C:\Windows)`

这通常不是 `pom.xml` 丢了，而是 Maven 实际拿到的工作目录跑偏了。  
在当前中文路径和 PowerShell 特殊路径表示下，最稳的方式是始终显式指定 `pom.xml`：

```powershell
mvn -f .\pom.xml clean javafx:run
```

或者直接写绝对路径：

```powershell
mvn -f "C:\Users\12493\...\ToDo\todo\pom.xml" clean javafx:run
```

### 2. Toolchains / JDK 版本不匹配

项目要求 Java 21。  
如果当前终端仍在旧 JDK 或 JDK 25 上执行 Maven，先切到 JDK 21 再运行命令。

### 3. 数据库相关报错

默认模式下，优先检查的是 SQLite 本地目录和文件写入权限，而不是 MySQL 服务：

- 当前用户是否可写 `%APPDATA%\ToDo\`
- 是否通过 `TODO_APP_DATA_DIR` 或 `TODO_DB_SQLITE_PATH` 指到了不可写目录
- 是否手动把数据库模式切成了 `mysql`

如果你确实在使用 legacy MySQL 兼容链路，再检查：

- `TODO_DB_MODE=mysql`
- `TODO_DB_URL`
- `TODO_DB_USER`
- `TODO_DB_PASSWORD`

## 文档导航

根目录说明文档负责解释“现在是什么”：

- [README.md](./README.md)
  - 项目入口、快速开始、当前基线
- [DEV.md](./DEV.md)
  - 构建、测试、维护、工程约束与开发说明
- [USER.md](./USER.md)
  - 源码运行版用户手册
- [ARCHITECTURE.md](./ARCHITECTURE.md)
  - 当前架构、分层、运行链路与 legacy 边界
- [DATABASE_AND_RUNTIME.md](./DATABASE_AND_RUNTIME.md)
  - 当前数据库、配置、迁移与运行基线
- [UI_VIEWS.md](./UI_VIEWS.md)
  - 主视图、交互和跨视图行为说明

[`todo/改造计划/`](./todo/%E6%94%B9%E9%80%A0%E8%AE%A1%E5%88%92/) 负责解释“接下来按什么阶段改”：

- [阶段 A：本地可用化与SQLite替换](./todo/%E6%94%B9%E9%80%A0%E8%AE%A1%E5%88%92/01-%E9%98%B6%E6%AE%B5A-%E6%9C%AC%E5%9C%B0%E5%8F%AF%E7%94%A8%E5%8C%96%E4%B8%8ESQLite%E6%9B%BF%E6%8D%A2.md)
- [阶段 B：数据层可演进化与同步建模](./todo/%E6%94%B9%E9%80%A0%E8%AE%A1%E5%88%92/02-%E9%98%B6%E6%AE%B5B-%E6%95%B0%E6%8D%AE%E5%B1%82%E5%8F%AF%E6%BC%94%E8%BF%9B%E5%8C%96%E4%B8%8E%E5%90%8C%E6%AD%A5%E5%BB%BA%E6%A8%A1.md)
- [阶段 C：云端化与多端同步](./todo/%E6%94%B9%E9%80%A0%E8%AE%A1%E5%88%92/03-%E9%98%B6%E6%AE%B5C-%E4%BA%91%E7%AB%AF%E5%8C%96%E4%B8%8E%E5%A4%9A%E7%AB%AF%E5%90%8C%E6%AD%A5.md)
- [阶段 D：商业化与运营底座](./todo/%E6%94%B9%E9%80%A0%E8%AE%A1%E5%88%92/04-%E9%98%B6%E6%AE%B5D-%E5%95%86%E4%B8%9A%E5%8C%96%E4%B8%8E%E8%BF%90%E8%90%A5%E5%BA%95%E5%BA%A7.md)

## 当前代码入口

几个最值得先看的入口：

- Maven 模块描述：[`todo/pom.xml`](./todo/pom.xml)
- JPMS 模块声明：[`todo/src/main/java/module-info.java`](./todo/src/main/java/module-info.java)
- 应用入口：[`todo/src/main/java/com/example/MainApp.java`](./todo/src/main/java/com/example/MainApp.java)
- FXML shell：[`todo/src/main/resources/com/example/ui/main-shell.fxml`](./todo/src/main/resources/com/example/ui/main-shell.fxml)
- 组合根：[`todo/src/main/java/com/example/application/ApplicationContext.java`](./todo/src/main/java/com/example/application/ApplicationContext.java)

## 当前事实与后续路线

### 当前事实

- 构建与测试链路已经升级到 Java 21 / JavaFX 21 / Maven 新插件版本
- UI 已经从纯代码根节点切到 FXML shell
- 数据访问已开始收口到 `Repository + Service` 方向
- 默认本地运行基线已经是 SQLite
- SQLite 迁移已存在 V001 / V002 / V003
- 设置详情页中的应用版本已经来自 `pom.xml`
- `Connectdatabase` / `ScheduleDAO` 仍然保留，但不再是默认本地运行主链路

### 已确定但未完成的路线

- 本地持久化继续以 SQLite 为基线
- 领域模型将从当前 `Schedule` 逐步演进到支持：
  - 分钟级时间
  - 全局 ID
  - 提醒 / 标签 / 重复规则
  - 同步字段
- 云端同步将以 PostgreSQL + API / Sync 服务为基线
- 正式安装包、账号体系、多设备同步、商业化底座尚未进入已落地状态

## 备注

如果你第一次接手这个仓库，建议阅读顺序：

1. [README.md](./README.md)
2. [DEV.md](./DEV.md)
3. [ARCHITECTURE.md](./ARCHITECTURE.md)
4. [DATABASE_AND_RUNTIME.md](./DATABASE_AND_RUNTIME.md)
5. [UI_VIEWS.md](./UI_VIEWS.md)
6. [todo/改造计划/](./todo/%E6%94%B9%E9%80%A0%E8%AE%A1%E5%88%92/)
