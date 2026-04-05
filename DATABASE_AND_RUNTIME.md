# ToDo 数据库与运行基线说明

本文档描述的是当前仓库的真实数据库与运行状态；未来已确定路线会单列说明，但不会把目标态写成当前已落地事实。

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

### 当前默认数据库仍是 MySQL

默认配置来自：

- [`todo/src/main/resources/application-defaults.properties`](./todo/src/main/resources/application-defaults.properties)

当前默认值：

- `todo.db.driver=com.mysql.cj.jdbc.Driver`
- `todo.db.url=jdbc:mysql://localhost:3306/todo_db`
- `todo.db.user=root`
- `todo.db.password=`

这意味着当前现实是：

- 程序默认会尝试连接本机 MySQL
- 程序默认假设数据库名为 `todo_db`
- 如果本机 MySQL 未启动、账户不匹配或权限不足，应用相关页面会报数据库错误

### 这不是长期目标

虽然当前默认仍是 MySQL，但它只是现阶段遗留运行基线，不是长期路线。  
已确定的后续方向是：

- 本地持久化切到 SQLite
- 云端同步基线为 PostgreSQL + API/Sync 服务

## 3. 当前配置加载规则

当前配置加载入口在：

- [`todo/src/main/java/com/example/config/ConfigurationLoader.java`](./todo/src/main/java/com/example/config/ConfigurationLoader.java)

当前配置覆盖顺序是：

1. classpath 默认值
2. 根目录 `application.properties`
3. `config/application.properties`
4. 环境变量 `TODO_CONFIG_FILE` 指向的外部配置文件
5. 对应环境变量覆盖具体键值

数据库相关环境变量当前支持：

- `TODO_DB_DRIVER`
- `TODO_DB_URL`
- `TODO_DB_USER`
- `TODO_DB_PASSWORD`
- `TODO_CONFIG_FILE`

## 4. 当前数据库访问链路

当前链路不是简单的“视图直连数据库”，但也还没彻底完成重构。  
真实链路如下：

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

- [`ApplicationContext.java`](./todo/src/main/java/com/example/application/ApplicationContext.java)
- [`ScheduleService.java`](./todo/src/main/java/com/example/application/ScheduleService.java)
- [`ScheduleRepository.java`](./todo/src/main/java/com/example/data/ScheduleRepository.java)
- [`JdbcScheduleRepository.java`](./todo/src/main/java/com/example/data/JdbcScheduleRepository.java)
- [`ScheduleDAO.java`](./todo/src/main/java/com/example/databaseutil/ScheduleDAO.java)
- [`Connectdatabase.java`](./todo/src/main/java/com/example/databaseutil/Connectdatabase.java)
- [`JdbcConnectionFactory.java`](./todo/src/main/java/com/example/data/JdbcConnectionFactory.java)
- [`SchemaInitializer.java`](./todo/src/main/java/com/example/data/SchemaInitializer.java)

## 5. 当前各层的数据库职责

### `ConfigurationLoader`

- 负责决定数据库驱动、URL、用户和密码从哪里来
- 当前仍输出 MySQL 配置对象

### `JdbcConnectionFactory`

- 负责根据 `DatabaseProperties` 构造 JDBC 连接
- 当前使用 MySQL 驱动类名

### `SchemaInitializer`

- 负责在连接成功后尝试保证 `schedules` 表及相关字段存在
- 当前 schema 逻辑仍围绕 MySQL / JDBC 兼容写法展开

### `Connectdatabase`

- 负责把配置、连接工厂和 schema 初始化串起来
- 当前仍是 legacy 风格的静态入口

### `ScheduleDAO`

- 承载当前大量日程 CRUD SQL 逻辑
- 含有较多旧 schema 兼容处理

### `JdbcScheduleRepository`

- 作为新分层里的 Repository 适配层存在
- 目前本质上仍是对 `ScheduleDAO` 的一层包装

## 6. 为什么现在会出现数据库故障

你现在遇到的典型故障是：

```text
Access denied for user 'root'@'localhost'
```

这类问题的本质不是“代码完全坏了”，而是：

- 当前默认运行事实仍依赖本地 MySQL
- 而桌面应用场景下，要求用户正确安装并维护本地 MySQL 并不稳
- 一旦账户、密码、权限、数据库名或服务状态不对，就会影响应用页面加载

这也正是为什么后续路线会固定为 SQLite 本地优先。

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

- 本机有可访问的 MySQL
- `todo_db` 可连接
- 当前用户密码与配置一致

### 推荐运行命令

```powershell
cd .\todo
mvn -f .\pom.xml clean javafx:run
```

## 8. 当前现存问题

### 可用性问题

- 启动和视图加载仍会受到本地 MySQL 环境影响
- 对“下载即用”的桌面体验不友好

### 架构问题

- `ScheduleDAO` 与 `Repository` 并存，迁移尚未完成
- `Connectdatabase` 仍是 legacy 静态入口

### 领域模型问题

- 当前模型仍未支持分钟级时间主模型
- 仍未支持全局 ID、同步字段、提醒/标签/重复规则拆分

### 文档与认知问题

- 很容易误把“SQLite / PostgreSQL 路线”理解成已经落地
- 很容易误把 `Repository` 的存在理解成 MySQL 已经退出默认链路

## 9. 已确定的后续路线

### 9.1 本地数据库

- 将从 MySQL 切到 SQLite
- 目标是让桌面端实现真正的本地优先与零配置可启动

### 9.2 云端数据库

- 将采用 PostgreSQL 作为云端中心库
- 不让客户端直连云数据库
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
