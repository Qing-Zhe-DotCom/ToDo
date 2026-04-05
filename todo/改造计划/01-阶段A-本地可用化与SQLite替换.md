# 阶段 A：本地可用化与 SQLite 替换

本阶段一句话目标：把当前“依赖本地 MySQL 服务和账户权限才能正常使用”的桌面应用，改造成“零配置可启动、单机可稳定运行、数据默认落本地 SQLite”的本地优先应用。

本阶段完成后系统会变成什么状态：用户在未安装 MySQL、未配置数据库账户、甚至首次启动的情况下，也能直接打开应用并完成列表、时间轴、热力图、详情和设置等核心操作；本地数据以 SQLite 文件形式持久化，应用具备备份、恢复、损坏提示和迁移入口。

## 目标与完成定义

### 目标
- 去除当前运行链路对本地 MySQL 服务的强依赖。
- 将桌面端默认本地持久化从 MySQL 改为 SQLite。
- 建立用户级数据目录、数据库文件目录、日志目录和备份目录。
- 让应用启动、视图加载、数据读写、错误提示、数据恢复都围绕“本地优先”设计。
- 为后续阶段的分钟级时间模型、同步字段、软删除、标签、提醒、重复规则打好 schema 基础。

### 完成定义
- 不启动 MySQL 服务，应用仍可正常启动进入主界面。
- 首次启动时自动初始化本地 SQLite 数据库和 schema 版本。
- 列表、时间轴、热力图、详情面板、设置和新增/编辑日程都运行在 SQLite 上。
- 数据库文件位置明确，重启应用后数据持久化正确。
- 数据库损坏、迁移失败、磁盘路径不可写等情况有明确定义的提示和兜底行为。
- 旧 MySQL 用户具备至少一种可执行迁移方案：自动迁移、手动导入、跳过迁移。

### 范围内
- 本地数据库从 MySQL 切换到 SQLite。
- 启动链路、本地配置、本地 schema 管理、数据迁移入口、页面容错。
- 本地数据库文件生命周期、备份和恢复策略。

### 范围外
- 云端 PostgreSQL。
- 多设备同步。
- 商业版、订阅、团队权限。
- 完整重构所有 UI 组件。

## 当前现状与问题清单

### 当前仓库基线
- 当前客户端仍是单 Maven 模块，构建入口在 [todo/pom.xml](/C:/Users/12493/Desktop/ToDoUI重制版/ToDo/todo/pom.xml)。
- 启动入口仍是 [com.example.MainApp](/C:/Users/12493/Desktop/ToDoUI重制版/ToDo/todo/src/main/java/com/example/MainApp.java)，JavaFX 壳层采用 [main-shell.fxml](/C:/Users/12493/Desktop/ToDoUI重制版/ToDo/todo/src/main/resources/com/example/ui/main-shell.fxml)。
- 现代化升级已经完成一部分：Java 21、JavaFX 21、JPMS、`config -> data -> application -> ui` 初步分层已建立。
- 当前组合根在 [ApplicationContext.java](/C:/Users/12493/Desktop/ToDoUI重制版/ToDo/todo/src/main/java/com/example/application/ApplicationContext.java)，但数据访问仍通过 legacy DAO 间接落到 MySQL。
- 当前数据库连接入口在 [Connectdatabase.java](/C:/Users/12493/Desktop/ToDoUI重制版/ToDo/todo/src/main/java/com/example/databaseutil/Connectdatabase.java)，仍直接构造 JDBC MySQL 连接。
- 当前默认数据库配置在 `src/main/resources/application-defaults.properties`，仍为：
  - `todo.db.driver=com.mysql.cj.jdbc.Driver`
  - `todo.db.url=jdbc:mysql://localhost:3306/todo_db`
  - `todo.db.user=root`
  - `todo.db.password=`

### 已暴露的问题
- 当前运行时已经出现 `Access denied for user 'root'@'localhost'`，说明应用基础可用性绑定在用户本机 MySQL 账户权限上。
- 桌面分发场景下，要求用户额外安装并维护 MySQL，不符合“下载即用”的产品体验。
- 当前 `Connectdatabase -> ScheduleDAO -> JdbcScheduleRepository` 仍是以服务型数据库为假设设计，无法天然支持离线、本地备份、便携导出。
- 当前模型 `Schedule` 仍以 `LocalDate` 为主，适配了少量 `LocalDateTime` 字段，但还没有为分钟级时间和同步字段全面建模。
- 启动失败时，数据库故障容易直接反射到页面加载错误，不符合本地优先应用的可靠性预期。

### 为什么本地 MySQL 不适合作为长期方向
- 安装门槛高：用户需安装数据库服务、管理账户、开放端口或维护本地实例。
- 故障形态复杂：权限、服务未启动、端口占用、版本不一致都会影响应用。
- 分发成本高：桌面应用的最佳体验应是零配置、单文件或少量文件启动。
- 离线体验差：桌面应用本应先本地可用，再谈同步。
- 商业化成本高：客服、安装说明、数据恢复、升级过程都会因为 MySQL 增加阻力。

## 目标架构

### 阶段 A 的目标结构
- 启动入口：`MainApp -> ApplicationContext -> Local Repository -> SQLite`
- 本地数据库：默认使用 SQLite 文件，不再依赖本地 MySQL 服务。
- 配置层：数据库配置由“连接字符串/账户密码”转为“数据目录/数据库文件/备份策略/日志策略”。
- 数据访问层：保留 `Repository + Service` 结构，但底层实现替换为 `SQLiteRepository` 族。
- schema 管理：引入本地 migration runner 和 `schema_version` 表，不再只靠一次性 `SchemaInitializer`。
- 用户体验：首次启动自动初始化数据库；数据库损坏时提示恢复、备份或重建；迁移旧数据时提供导入入口。

### 决策固定
- 本地数据库固定为 SQLite，不保留“长期继续使用本地 MySQL”主路线。
- 应用启动默认先打开本地库，任何云端、账号、同步逻辑都不得阻塞启动。
- 本地数据库文件为用户级文件，不放在仓库目录、程序安装目录或临时目录。
- schema 迁移采用自带 SQL 脚本和本地 migration runner，不引入依赖外部服务的方案。

### 目录与文件策略
- Windows 默认数据根目录：`%APPDATA%/ToDo`
- macOS 默认数据根目录：`~/Library/Application Support/ToDo`
- Linux 默认数据根目录：`$XDG_DATA_HOME/ToDo`，若未定义则使用 `~/.local/share/ToDo`
- 本地数据库默认路径：`${app.dataDir}/data/todo.db`
- 备份目录：`${app.dataDir}/backups/`
- 日志目录：`${app.dataDir}/logs/`
- 导入导出临时目录：`${app.dataDir}/tmp/`
- 打包内置 migration 目录：`src/main/resources/db/sqlite/`

### 数据库文件策略
- 主数据库文件：`todo.db`
- WAL 模式开启，默认配套生成 `todo.db-wal` 和 `todo.db-shm`
- 迁移前自动生成时间戳备份：`todo-YYYYMMDD-HHmmss.pre-migration.db`
- 手动导出备份使用压缩文件：`todo-backup-YYYYMMDD-HHmmss.zip`

## 详细任务拆解

### A-01 配置调整任务

| ID | 标签 | 类型 | 任务内容 | 完成标准 |
| --- | --- | --- | --- | --- |
| A-01-01 | 必须先做 | 代码 | 扩展配置模型，引入 `app.dataDir`、`db.sqlite.path`、`db.backup.enabled`、`db.backup.retention`、`db.migration.mode` 等本地化配置项。 | 不再要求默认配置里出现 MySQL URL、root 用户和密码。 |
| A-01-02 | 必须先做 | 代码 | 设计配置优先级：显式环境变量 > 外部 `application.properties` > `config/application.properties` > classpath 默认值。 | 本地路径相关配置可被覆盖，默认值可直接运行。 |
| A-01-03 | 建议本阶段做完 | 设计/文档 | 明确跨平台数据目录规则和路径拼接规范，避免把 Windows 路径写死到业务逻辑。 | 文档和代码都引用统一的数据目录解析策略。 |
| A-01-04 | 建议本阶段做完 | 运维资产 | 提供一个“重置为默认本地库路径”的说明文档和故障排查指引。 | 用户能找到数据库文件和日志目录。 |

### A-02 SQLite 基础接入任务

| ID | 标签 | 类型 | 任务内容 | 完成标准 |
| --- | --- | --- | --- | --- |
| A-02-01 | 必须先做 | 代码 | 在 Maven 依赖中加入 SQLite JDBC 驱动，移除“客户端必须带 MySQL 驱动”的运行前提。 | `mvn clean compile`、`mvn test` 能在 SQLite 模式下通过。 |
| A-02-02 | 必须先做 | 代码 | 新增 `SQLiteConnectionFactory`，统一通过数据目录和数据库文件路径创建连接。 | 本地库无需用户名密码即可连接。 |
| A-02-03 | 必须先做 | 代码 | 更新 `ApplicationContext` 的组装逻辑，根据配置选择 SQLite 实现，并把旧 MySQL 逻辑降级为“仅迁移时可选”。 | 默认运行路径走 SQLite。 |
| A-02-04 | 建议本阶段做完 | 代码 | 为 SQLite 连接启用 WAL、外键约束和必要 pragma。 | 本地数据库具备可靠事务和恢复能力。 |
| A-02-05 | 可延后到下阶段 | 设计/文档 | 制定 SQL 方言差异清单，为后续 PostgreSQL 同步中心库建模做准备。 | SQLite 与 PostgreSQL 的公共字段语义已有映射文档。 |

### A-03 数据访问层切换任务

| ID | 标签 | 类型 | 任务内容 | 完成标准 |
| --- | --- | --- | --- | --- |
| A-03-01 | 必须先做 | 代码 | 保留 `ScheduleRepository` 作为上层稳定接口，新增 SQLite 实现，不再让 UI 感知数据库种类。 | `ScheduleService` 只依赖 Repository。 |
| A-03-02 | 必须先做 | 代码 | 将 `Connectdatabase` 和 `ScheduleDAO` 从“默认运行链路”移出，保留为迁移期兼容组件或导入器。 | 页面层不再通过 MySQL DAO 读写。 |
| A-03-03 | 必须先做 | 代码 | 梳理当前 `ScheduleListView`、`TimelineView`、`HeatmapView`、`InfoPanelView` 对服务层的调用，验证它们在本地库切换后无需改数据库感知逻辑。 | 四类视图从服务层拿到数据即可正常工作。 |
| A-03-04 | 建议本阶段做完 | 代码 | 为 Repository 增加基础错误分类：初始化失败、路径不可写、数据库损坏、schema 不兼容。 | UI 能根据错误类型展示不同恢复建议。 |
| A-03-05 | 可延后到下阶段 | 设计/文档 | 规划 Repository 接口未来如何容纳标签、提醒、重复规则和同步队列。 | 下一阶段的数据层升级有稳定扩展点。 |

### A-04 schema 初始化与版本化任务

| ID | 标签 | 类型 | 任务内容 | 完成标准 |
| --- | --- | --- | --- | --- |
| A-04-01 | 必须先做 | 代码 | 引入 `schema_version` 表和按版本执行的 migration runner。 | 数据库每次启动都能检查版本并按序升级。 |
| A-04-02 | 必须先做 | 代码 | 设计 SQLite 初版 schema，覆盖当前日程主表和最小必要索引。 | 能支撑现有列表、时间轴、热力图和详情页。 |
| A-04-03 | 必须先做 | 代码 | 在初版 schema 中预留后续字段空间：分钟级时间、`deleted_at`、`version`、`sync_status`、`device_id`、`metadata_json`。 | 不需要下一阶段就推翻整套表结构。 |
| A-04-04 | 建议本阶段做完 | 代码 | 迁移脚本按“幂等 + 可重试”设计，失败时保留原库并输出诊断日志。 | 迁移失败不会直接破坏现有数据文件。 |
| A-04-05 | 建议本阶段做完 | 运维资产 | 定义 schema 变更命名规范和 migration 文件命名规范，例如 `V001__init_sqlite.sql`。 | 后续改造时迁移链路可长期维护。 |

### A-05 启动链路与页面容错任务

| ID | 标签 | 类型 | 任务内容 | 完成标准 |
| --- | --- | --- | --- | --- |
| A-05-01 | 必须先做 | 代码 | 让应用启动时先创建数据目录，再初始化本地数据库，再加载主界面。 | 首次启动不因数据库不存在而失败。 |
| A-05-02 | 必须先做 | 代码 | 将数据库初始化失败与页面加载失败解耦，主界面仍可显示，并弹出清晰的错误和恢复选项。 | 用户能看到“重试/恢复备份/重建数据库/打开数据目录”。 |
| A-05-03 | 必须先做 | 代码 | 列表、时间轴、热力图加载失败时，不显示底层 JDBC 文本，而显示用户可理解的错误信息。 | 不再直接暴露 `root@localhost` 等底层错误给终端用户。 |
| A-05-04 | 建议本阶段做完 | 代码 | 在设置界面加入数据库信息区：当前库路径、最后备份时间、打开数据目录、手动备份按钮。 | 用户可以自行定位和备份数据。 |
| A-05-05 | 建议本阶段做完 | 设计/文档 | 设计“只读模式”降级行为，数据库打不开时允许查看最后缓存或空壳界面。 | 故障体验不至于完全黑屏。 |

### A-06 旧 MySQL 到 SQLite 的迁移任务

| ID | 标签 | 类型 | 任务内容 | 完成标准 |
| --- | --- | --- | --- | --- |
| A-06-01 | 必须先做 | 代码 | 提供首次启动迁移检测：若发现旧 MySQL 配置或旧数据源可达，则询问是否迁移。 | 老用户能看到明确迁移入口。 |
| A-06-02 | 必须先做 | 代码 | 提供手动迁移工具，支持从旧 MySQL 导入当前用户数据到 SQLite。 | 即便自动迁移失败，也有可执行兜底方案。 |
| A-06-03 | 建议本阶段做完 | 代码 | 提供 CSV/JSON 导入导出作为第二层备份和迁移通道。 | 迁移不完全依赖数据库直连。 |
| A-06-04 | 建议本阶段做完 | 运维资产 | 定义迁移日志格式和失败原因分类，例如权限失败、字段不匹配、数据损坏。 | 迁移失败后能定位原因。 |
| A-06-05 | 可延后到下阶段 | 设计/文档 | 制定“弃用本地 MySQL”用户公告模板和版本切换说明。 | 升级发布时可直接使用。 |

### A-07 验证与回归任务

| ID | 标签 | 类型 | 任务内容 | 完成标准 |
| --- | --- | --- | --- | --- |
| A-07-01 | 必须先做 | 代码 | 在现有测试体系下补充 SQLite 模式的 Repository 和 schema 初始化测试。 | SQLite 基础行为可自动验证。 |
| A-07-02 | 必须先做 | 测试资产 | 扩展手工回归清单，覆盖首次启动、重启、增删改查、时间轴、热力图、详情面板、设置页。 | 发布前有人能按清单完成验证。 |
| A-07-03 | 建议本阶段做完 | 测试资产 | 加入损坏库、路径不可写、备份恢复、迁移失败等异常场景的测试用例。 | 本地数据故障处理可复现。 |
| A-07-04 | 可延后到下阶段 | 设计/文档 | 预留“多设备同步前的本地一致性测试基线”。 | 下一阶段升级数据模型时可复用。 |

## 接口 / 数据 / 配置改动

### 代码结构改动
- 保持客户端仍为单 Maven 模块，不拆模块。
- 保持启动入口为 `com.example.MainApp`。
- 在 `com.example.data` 下新增 SQLite 相关连接、仓储和迁移实现。
- `com.example.databaseutil` 降级为迁移期兼容和旧 MySQL 导入工具，不再承载默认运行链路。
- `ApplicationContext` 负责决定默认本地持久化实现，禁止页面层直接选择数据库后端。

### 配置改动
- 删除默认配置里的 MySQL 账户依赖。
- 新增配置项：
  - `todo.app.data-dir`
  - `todo.db.mode=sqlite`
  - `todo.db.sqlite.path`
  - `todo.db.backup.enabled`
  - `todo.db.backup.retention`
  - `todo.db.backup.before-migration`
  - `todo.db.recovery.mode`
- 环境变量映射建议：
  - `TODO_APP_DATA_DIR`
  - `TODO_DB_MODE`
  - `TODO_DB_SQLITE_PATH`
  - `TODO_DB_BACKUP_ENABLED`

### 数据改动
- 本阶段主表仍可沿用现有 `schedules` 概念，但必须开始为后续演进留位：
  - `deleted_at`
  - `version`
  - `sync_status`
  - `last_synced_at`
  - `device_id`
  - `metadata_json`
- 时间字段暂可维持兼容命名，但底层存储策略必须开始允许分钟级时间引入。

### 兼容策略
- 允许短期保留 MySQL 导入能力。
- 不允许继续让默认运行路径依赖 MySQL。
- 如发现旧版本配置文件仍指向 MySQL，默认行为应是：
  - 优先启动 SQLite
  - 提示用户可执行迁移
  - 不直接中断应用主界面

## 测试与验收

### 验收命令
```powershell
cd C:\Users\12493\Desktop\ToDoUI重制版\ToDo\todo

$env:JAVA_HOME='C:\Program Files\Microsoft\jdk-21.0.10.7-hotspot'
$env:JAVA21_HOME=$env:JAVA_HOME
$env:PATH="$env:JAVA_HOME\bin;$env:PATH"

mvn clean compile
mvn test
mvn javafx:run
```

### 自动化验收
- 构建成功，且不要求本机安装或启动 MySQL。
- 所有现有测试继续通过，特别是视图测试、主题测试、时间轴和热力图测试。
- 新增的 SQLite 初始化和 migration 测试通过。

### 人工验证场景
- 首次启动：
  - 删除本地数据目录后启动应用，确认能自动建库。
- 正常使用：
  - 新建、编辑、删除、完成日程，重启应用后数据仍存在。
- 页面回归：
  - 列表、时间轴、热力图、详情面板、设置页均能读取 SQLite 数据。
- 备份恢复：
  - 手动触发备份，确认备份文件可见。
  - 模拟数据库损坏后，应用能提示恢复或重建。
- 迁移验证：
  - 若旧 MySQL 数据源可达，能完成导入。
  - 若旧 MySQL 数据源不可达，不影响应用进入 SQLite 模式。

### 失败判定标准
- 应用启动仍要求 MySQL 服务在线。
- 页面仍直接暴露 `root@localhost` 等数据库底层错误。
- 数据库存放在临时目录、仓库目录或不可持久目录。
- 迁移失败后直接覆盖原数据，无法回滚。

## 风险、回滚与兼容

### 主要风险
- SQLite 与 MySQL SQL 方言差异可能导致旧 DAO 无法直接复用。
- 旧数据字段类型不一致，迁移时可能出现日期、提醒时间、标签等字段损坏。
- 当前视图层仍大量依赖旧 `Schedule` 模型，若 schema 改动过快，会引发 UI 连锁修改。
- 用户手动删除数据库文件或锁定目录，可能导致启动异常。

### 风险缓解
- 通过 Repository 层隔离 SQL 方言差异，不让 UI 接触底层实现。
- 迁移前强制备份，不在原库上做破坏性操作。
- 在阶段 A 仅完成“可在 SQLite 上跑通”和“留出扩展位”，不立即做全量数据模型革命。
- 将数据库损坏、路径不可写等异常转化为可恢复的产品流程。

### 回滚策略
- 若 SQLite 切换版本发现严重问题，可保留导出备份并临时开放只读导入模式。
- migration 失败时恢复到迁移前备份文件，不自动重试破坏性升级。
- 保留旧 MySQL 导入器直到阶段 B 完成并验证新模型稳定。

### 兼容要求
- 短期兼容现有 `ScheduleService` 和上层视图调用方式。
- 不强制本阶段就重命名领域对象或重写全部页面。
- 只要新本地库能支撑现有行为，就优先保护稳定性。

## 阶段产出清单
- 一套默认可运行的 SQLite 本地数据库实现。
- 用户级数据目录与数据库文件管理策略。
- 本地 migration runner 和 schema 版本表。
- 数据库故障与恢复的 UI 交互方案。
- 旧 MySQL 导入/迁移入口。
- SQLite 模式下的自动化测试与人工回归清单。
- 发布说明：明确从“本地 MySQL 应用”过渡到“本地 SQLite 应用”。

## 后续阶段依赖
- 阶段 B 必须建立在阶段 A 已稳定使用 SQLite 的前提上。
- 若阶段 A 未完成以下阻塞项，不应进入阶段 B：
  - 默认运行链路仍依赖 MySQL
  - 本地 schema 未版本化
  - 数据库文件路径和备份恢复策略未固定
  - 页面回归尚未在 SQLite 上跑通
- 阶段 B 将基于本阶段产出的本地库继续升级到分钟级时间模型、全局 ID、同步字段、子表拆分和可演进 Repository 设计。
