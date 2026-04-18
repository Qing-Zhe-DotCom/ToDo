# ToDo

本文档描述的是当前仓库已经落地的真实状态；未来路线会单列说明，但不会把计划误写成现状。

如果你只是想先把项目跑起来，先看本文；如果你准备继续维护代码，再看 [DEV.md](./DEV.md)；如果你想了解当前技术事实，请看 [ARCHITECTURE.md](./ARCHITECTURE.md)；如果你想从用户视角了解界面与交互，请看 [USER.md](./USER.md)。

## 项目概览

当前仓库中的 ToDo 是一个以源码运行方式为主的 JavaFX 桌面应用，真正的 Maven 模块位于 [`todo/`](./todo/)。

当前真实基线：

- 单 Maven 模块
- Java 21
- JavaFX 21.0.7
- JPMS 模块名：`com.example`
- UI 入口：FXML shell + Java 自定义视图
- 默认本地数据库：SQLite
- 当前默认数据层已进入 Stage-B：`ScheduleItemService` + `SqlScheduleItemRepository` + `SqliteStageBSchemaManager`
- MySQL 兼容链路仍保留，但不是默认本地运行前提
- 已确定的后续方向：本地 SQLite + 云端 PostgreSQL + Local-first Sync

## 当前能力

当前已经实际可用的能力包括：

- 日程列表管理
- 时间轴视图
- 热力图视图
- 右侧详情面板
- 搜索、搜索历史与搜索建议
- 设置中心
- 主题、图标包、语言与字体切换
- 提醒同步与待办数量驱动的窗口标题 / 任务栏角标
- 设置详情页中的应用版本显示

当前仍在过渡中的部分：

- `MainController` 仍是过渡态 shell controller
- `Schedule` 仍作为部分 UI 的兼容桥接模型存在
- 流程图页面仍是占位页
- 登录入口仍是占位提示
- 正式安装包 / 发布包尚未落地
- 云同步、账号体系、PostgreSQL 服务端尚未落地

## 快速开始

### 1. 进入模块目录

```powershell
cd .\todo
```

### 2. 准备环境

必备前提：

- Java 21
- Maven 3.9+

推荐在 PowerShell 中显式切到 JDK 21：

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

启动注意事项：

- 默认运行不要求你先启动本机 MySQL
- 默认数据库模式来自 [`todo/src/main/resources/application-defaults.properties`](./todo/src/main/resources/application-defaults.properties)，当前为 `sqlite`
- 在当前中文路径与 PowerShell 环境下，显式写 `-f .\pom.xml` 是最稳妥的方式

## 文档导航

当前根目录说明性文档分工如下：

- [README.md](./README.md)
  - 项目入口、快速开始、能力概览
- [DEV.md](./DEV.md)
  - 开发维护、构建测试、开发入口、FAQ
- [ARCHITECTURE.md](./ARCHITECTURE.md)
  - 当前技术事实：架构、数据链路、配置加载、legacy 边界
- [USER.md](./USER.md)
  - 用户视角的主界面、交互、设置和常见问题

## 改造计划入口

根目录说明文档解释的是“现在是什么”；`todo/改造计划/` 解释的是“接下来分阶段怎么改”。

当前仓库中的改造计划文档包括：

- [阶段 B：数据层可演进化与同步建模](./todo/%E6%94%B9%E9%80%A0%E8%AE%A1%E5%88%92/02-%E9%98%B6%E6%AE%B5B-%E6%95%B0%E6%8D%AE%E5%B1%82%E5%8F%AF%E6%BC%94%E8%BF%9B%E5%8C%96%E4%B8%8E%E5%90%8C%E6%AD%A5%E5%BB%BA%E6%A8%A1.md)
- [阶段 C：云端化与多端同步](./todo/%E6%94%B9%E9%80%A0%E8%AE%A1%E5%88%92/03-%E9%98%B6%E6%AE%B5C-%E4%BA%91%E7%AB%AF%E5%8C%96%E4%B8%8E%E5%A4%9A%E7%AB%AF%E5%90%8C%E6%AD%A5.md)
- [阶段 D：商业化与运营底座](./todo/%E6%94%B9%E9%80%A0%E8%AE%A1%E5%88%92/04-%E9%98%B6%E6%AE%B5D-%E5%95%86%E4%B8%9A%E5%8C%96%E4%B8%8E%E8%BF%90%E8%90%A5%E5%BA%95%E5%BA%A7.md)

建议按这个顺序理解仓库：

1. [README.md](./README.md)
2. [DEV.md](./DEV.md)
3. [ARCHITECTURE.md](./ARCHITECTURE.md)
4. [USER.md](./USER.md)
