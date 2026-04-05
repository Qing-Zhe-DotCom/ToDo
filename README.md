# ToDo

本文档描述的是当前仓库的真实状态；后续已确定的改造路线会单独列出说明，但不把计划当成现状。

如果你只想快速跑起来，先看本文档；如果你要继续维护，请看 [DEV.md](./DEV.md)；如果你想了解当前界面和交互，请看 [USER.md](./USER.md) 与 [UI_VIEWS.md](./UI_VIEWS.md)。

## 项目概览

当前仓库中的 ToDo 是一个以源码运行方式为主的 JavaFX 桌面应用，Maven 模块位于 [`todo/`](./todo/)。

当前真实基线：

- 单 Maven 模块
- Java 21
- JavaFX 21.0.7
- JPMS 模块名：`com.example`
- UI 入口：FXML shell + Java 自定义视图
- 分层方向：`config -> data -> application -> ui`
- 当前数据库默认运行事实：仍是本地 MySQL 配置
- 已确定但尚未落地的后续路线：本地 SQLite + 云端 PostgreSQL + Local-first Sync

## 当前状态

当前已经实际存在的能力：

- 日程列表管理
- 时间轴视图
- 热力图视图
- 右侧详情面板
- 设置中心
- 主题切换
- 日程卡片样式切换
- 待办数量驱动的窗口标题 / 任务栏角标

当前仍然是过渡态的部分：

- `MainController` 仍是过渡期 shell controller
- `ScheduleDAO` / `Connectdatabase` 仍在 legacy 数据访问链路中
- 默认数据库配置仍指向本地 MySQL
- 流程图页面仍是占位页
- 登录入口仍是占位提示

## 快速开始

### 1. 进入模块目录

```powershell
cd .\todo
```

### 2. 确认 JDK 21

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

## 常见启动坑

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
如果你当前终端仍在旧 JDK 或 JDK 25 上运行 Maven，先切到 JDK 21 再执行命令。

### 3. 数据库连接失败

当前默认配置仍然指向本地 MySQL：

- `jdbc:mysql://localhost:3306/todo_db`
- 用户：`root`
- 密码：空字符串

如果你的本地数据库环境不同，应用会在加载视图时报告数据库连接错误。详见 [DATABASE_AND_RUNTIME.md](./DATABASE_AND_RUNTIME.md)。

## 文档导航

根目录说明文档负责解释“现在是什么”：

- [README.md](./README.md)
  - 项目入口、快速开始、文档导航
- [DEV.md](./DEV.md)
  - 构建、测试、维护、技术债与开发说明
- [USER.md](./USER.md)
  - 源码运行版用户手册
- [ARCHITECTURE.md](./ARCHITECTURE.md)
  - 当前架构、分层、重构状态与 legacy 边界
- [DATABASE_AND_RUNTIME.md](./DATABASE_AND_RUNTIME.md)
  - 当前数据库、配置、运行基线与后续路线
- [UI_VIEWS.md](./UI_VIEWS.md)
  - 主视图、交互和跨视图行为说明

`todo/改造计划/` 负责解释“接下来按什么阶段改”：

- [阶段 A：本地可用化与 SQLite 替换](./todo/%E6%94%B9%E9%80%A0%E8%AE%A1%E5%88%92/01-%E9%98%B6%E6%AE%B5A-%E6%9C%AC%E5%9C%B0%E5%8F%AF%E7%94%A8%E5%8C%96%E4%B8%8ESQLite%E6%9B%BF%E6%8D%A2.md)
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

- 构建与测试链路已经升级到 Java 21 / JavaFX 21 / Maven 新插件版本。
- UI 已经从纯代码根节点走向 FXML shell。
- 数据访问已初步收口到 `Repository + Service` 方向。
- 但默认数据库仍是 MySQL，且仍会影响实际可用性。

### 已确定但未完成的路线

- 本地持久化将切到 SQLite
- 云端同步将以 PostgreSQL + API/Sync 服务为基线
- 领域模型将从当前 `Schedule` 逐步演进到支持：
  - 分钟级时间
  - 全局 ID
  - 提醒 / 标签 / 重复规则
  - 同步字段

## 备注

如果你第一次接手这个仓库，建议阅读顺序：

1. [README.md](./README.md)
2. [DEV.md](./DEV.md)
3. [ARCHITECTURE.md](./ARCHITECTURE.md)
4. [DATABASE_AND_RUNTIME.md](./DATABASE_AND_RUNTIME.md)
5. [UI_VIEWS.md](./UI_VIEWS.md)
6. [todo/改造计划/](./todo/%E6%94%B9%E9%80%A0%E8%AE%A1%E5%88%92/)
