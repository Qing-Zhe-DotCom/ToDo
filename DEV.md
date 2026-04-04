# ToDo 开发者现状文档

本文档基于当前工作区状态编写，基线时间为 **2026-04-04**。它描述的是“现在这份代码真实实现了什么”，而不是历史设计稿或未来愿景。

## 1. 项目概览

当前项目是一个使用 **JavaFX** 构建的桌面日程管理应用。真正的 Maven 工程位于 `todo/` 目录，当前运行主链路已经明显从早期的 FXML 示例工程演进为“代码组装主界面 + 多视图 + DAO + 补充动效支撑”的结构。

和旧文档相比，当前最重要的变化有：

- 持久化层当前实际连接 **MySQL**，不是 SQLite
- 主界面主要由 `MainController` 直接构建，而不是依赖 FXML 装配
- 新增了一套完成状态协调子系统，用于处理乐观更新、异步持久化、回滚与跨视图同步
- 新增共享的日程卡片样式与状态控件，已经接入列表、时间轴和热力图
- `HeatmapView` 当前主要通过已加载数据在内存中计算完成统计，而不是完全依赖 DAO 聚合结果

## 2. 工程目录与职责

```text
todo/
├─ pom.xml
├─ RefactorScript.java
├─ src/
│  ├─ main/
│  │  ├─ java/
│  │  │  ├─ com/example/MainApp.java
│  │  │  ├─ com/example/controller/
│  │  │  ├─ com/example/databaseutil/
│  │  │  ├─ com/example/model/
│  │  │  └─ com/example/view/
│  │  └─ resources/
│  │     ├─ styles/
│  │     ├─ icons/
│  │     ├─ database_update.sql
│  │     └─ com/example/*.fxml
│  └─ test/
│     └─ java/
└─ target/
```

### 2.1 当前主链路

```text
MainApp
  -> MainController
    -> ScheduleListView / TimelineView / HeatmapView / FlowchartView
    -> InfoPanelView
      -> ScheduleDAO
        -> Connectdatabase
          -> MySQL
```

### 2.2 不在主链路中的遗留或补充内容

- `RefactorScript.java`：仍然只是占位脚本
- `primary.fxml` / `secondary.fxml`：保留在资源目录中，但当前主界面不依赖它们
- `database_update.sql`：更像参考 SQL 文档，不是启动时自动执行的迁移入口

## 3. 启动层

## 3.1 `MainApp`

`MainApp` 是 JavaFX 应用入口。当前职责包括：

- 创建 `MainController`
- 创建 `Scene`
- 初始加载 `light-theme.css`
- 设置窗口最小尺寸 `1200 x 700`
- 设置应用标题与图标
- 监听待办数量并刷新窗口标题、图标和任务栏角标
- 在 `stop()` 中调用 `mainController.shutdown()`

### 3.1.1 任务栏角标与图标

`MainApp` 并不只负责启动，还做了平台相关的桌面集成：

- 使用 `java.awt.Taskbar`
- 根据待办数量动态生成带数字角标的应用图标
- 平台支持时设置任务栏 badge，超过 99 时显示 `99+`

## 3.2 `MainController`

`MainController` 是运行时的核心编排器。它直接构建主界面布局，并维护视图切换、主题、选中状态、设置中心以及完成状态协同。

### 3.2.1 主要职责

- 构建根节点 `BorderPane`
- 组装左侧边栏、中间内容区、右侧详情面板
- 初始化 `ScheduleListView`、`TimelineView`、`HeatmapView`、`FlowchartView`
- 维护当前视图与当前选中的 `Schedule`
- 打开新建 / 编辑对话框
- 管理主题与外部 CSS 导入
- 管理全局日程卡片样式
- 维护侧边栏折叠状态与“更多功能”面板状态
- 负责全局搜索入口
- 负责全局详情面板关闭交互（点击空白区域、按 `Esc`）

### 3.2.2 持久化的偏好项

当前通过 `java.util.prefs.Preferences` 保存：

- `todo.theme`
- `todo.theme.imported.path`
- `todo.timeline.card.style`

### 3.2.3 设置中心现状

设置中心当前只有三页：

- `详情`
- `主题`
- `样式`

其中：

- 主题色卡点击即预览并立即保存
- 卡片样式只会在对话框确认后保存
- 当前没有独立的深色主题体系；所有内置主题仍然基于浅色变量覆盖

## 4. 完成状态协调子系统

这是当前代码里最值得关注的新结构。

### 4.1 核心类

- `ScheduleCompletionCoordinator`
- `ScheduleCompletionMutation`
- `ScheduleCompletionParticipant`

### 4.2 真实职责

`ScheduleCompletionCoordinator` 负责：

- 阻止同一条日程在持久化完成前被重复提交
- 创建包含“旧完成状态 / 目标完成状态 / 乐观更新时间”的变更对象
- 在 UI 线程中先应用乐观变更
- 在后台线程调用 `ScheduleDAO.updateScheduleStatus(...)`
- 持久化成功后确认变更
- 持久化失败后回滚变更并上报错误

### 4.3 当前参与协同的视图

当前会响应完成状态变更的参与者有：

- `ScheduleListView`
- `TimelineView`
- `HeatmapView`
- `InfoPanelView`

这意味着同一条日程在一个视图中切换完成状态后，其他视图会尽量同步表现出相同状态，而不是等整页刷新后再看到结果。

## 5. 模型层：`Schedule`

`Schedule` 仍然是当前唯一核心业务模型，使用 JavaFX Property 封装字段。

### 5.1 字段

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | `int` | 主键 |
| `name` | `String` | 日程名称 |
| `description` | `String` | 描述 |
| `startDate` | `LocalDate` | 开始日期 |
| `dueDate` | `LocalDate` | 截止日期 |
| `completed` | `boolean` | 是否完成 |
| `priority` | `String` | 优先级，当前使用“高 / 中 / 低”字符串 |
| `category` | `String` | 分类 |
| `tags` | `String` | 标签文本 |
| `reminderTime` | `LocalDateTime` | 提醒时间 |
| `color` | `String` | 颜色 HEX 文本 |
| `createdAt` | `LocalDateTime` | 创建时间 |
| `updatedAt` | `LocalDateTime` | 更新时间 |

### 5.2 模型自带行为

- `isOverdue()`：未完成且截止日期早于今天
- `isUpcoming()`：未完成且截止日期在未来 7 天内
- `getPriorityValue()`：高 = 3，中 = 2，低 = 1
- 兼容字符串日期 / 时间格式化访问器

## 6. 持久化层：MySQL + DAO

## 6.1 `Connectdatabase`

当前数据库连接层的实现特征非常明确：

- 数据库类型：MySQL
- URL：`jdbc:mysql://localhost:3306/todo_db`
- 用户名：`root`
- 密码：硬编码在源码中

### 6.1.1 启动时 schema 保证

`Connectdatabase.getConnection()` 获取连接后会执行 `ensureSchema(connection)`。

当前逻辑会：

- 如果 `schedules` 表不存在则自动创建
- 如果缺少新列，则通过 `ALTER TABLE` 补齐

当前关注字段包括：

- `start_date`
- `due_date`
- `completed`
- `priority`
- `category`
- `tags`
- `reminder_time`
- `color`
- `created_at`
- `updated_at`

### 6.1.2 风险

当前 schema 管理仍属于“运行时兜底补列”，而不是成熟的迁移体系。

## 6.2 `ScheduleDAO`

`ScheduleDAO` 继续承担所有数据库访问职责。

### 6.2.1 CRUD 与查询

当前仍保留这些主要能力：

- 新增 / 更新 / 删除 / 按 ID 查询
- 查询全部日程
- 按状态、日期范围、分类、优先级查询
- 搜索名称或描述
- 查询即将到期与逾期日程
- 统计总数、完成数、每日完成数
- 获取分类列表与分页结果

### 6.2.2 兼容旧表结构

`addSchedule()` 和 `updateSchedule()` 在遇到 `Unknown column` 时，会回落到旧 schema 写法；部分查询也对旧列缺失做了兼容。

这说明当前 DAO 依然处在“兼容过渡期”，尚未完成彻底收口。

## 7. 视图层现状

`ScheduleListView`、`TimelineView`、`HeatmapView`、`FlowchartView` 实现了 `View` 接口；`InfoPanelView` 作为独立详情面板存在。

```java
public interface View {
    Node getView();
    void refresh();
}
```

## 7.1 `ScheduleListView`

这是当前最成熟的主视图。

### 已实现行为

- 加载全部日程
- 搜索名称 / 描述
- 排序：按日期、按优先级、按分类
- 筛选：全部、未完成、已完成、高优先级、即将到期
- `显示过去日程` 复选框
- 待办 / 已完成分组
- `已完成` 分组折叠
- 卡片左侧共享状态控件
- 通过 `ScheduleCompletionCoordinator.PendingCompletion` 驱动的完成态迁移动画

### 当前排序细节

- 未完成项按当前下拉排序规则排序
- 已完成项始终聚合在完成分组中
- 已完成项内部优先按 `updatedAt` 倒序，再按 `createdAt` 倒序，再按 `id` 倒序

### 当前搜索现状

- 搜索只查询 `name` 和 `description`
- `clearSearch()` 已存在，但全局搜索框清空后不会自动触发它
- 因此“恢复默认列表”的交互还没有完全接通

## 7.2 `TimelineView`

时间轴是当前最复杂的自定义渲染视图之一。

### 真实实现

当前不是“日 / 周 / 月 / 全部”切换模型，而是：

- 统一绘制一条横向时间轴
- 根据日程持续天数自动分成三组：短期、中期、长期
- 用日期范围选择器控制当前可视区间

### 当前能力

- 顶部开始 / 结束日期选择器
- `重置视角` 按钮
- 鼠标滚轮横向滚动
- 今天列高亮与自动滚动到今天附近
- 卡片悬停抬升
- 点击卡片打开详情
- 双击卡片打开编辑弹窗
- 卡片内共享状态控件，直接触发完成状态切换
- 响应完成状态协同更新

### 共享样式接入

当前时间轴卡片已接入：

- `ScheduleCardStyleSupport`
- `ScheduleStatusControl`

因此卡片视觉与状态控件会受到全局样式配置影响。

## 7.3 `HeatmapView`

热力图是当前第二个高度定制化视图。

### 当前能力

- 周 / 月 / 年视图切换
- 上一周期 / 下一周期 / 回到今天
- 视图周期摘要文本
- 点击日期查看该日期对应日程
- 下方面板中的日程卡片支持详情、编辑与状态切换
- 完成区接收反馈动画
- 响应完成状态协同更新

### 当前统计来源

旧文档曾将热力图描述为直接依赖 `ScheduleDAO.getDailyCompletionStats(...)`。当前实际行为已经变化：

- 主流程先从 DAO 加载全部日程
- 再通过 `buildDailyCompletionStats(loadedSchedules, startDate, endDate)` 在内存中统计完成数
- 通过 `buildSchedulesByDate(...)` 生成“某天覆盖哪些日程”的映射

也就是说：

- 颜色深浅来自已完成日程的 `updatedAt`
- 下方卡片列表来自开始 / 截止日期覆盖关系

### 视图状态说明

当前统计摘要文本使用英文格式：

`completed X items across Y active days`

这与项目主体中文文案并不完全统一，是现存的小型 UI 一致性问题。

## 7.4 `InfoPanelView`

右侧详情面板当前已经比较完整。

### 已实现行为

- 显示标题、状态、开始 / 截止日期
- 显示优先级、分类、标签、提醒时间、描述
- 支持编辑、删除、标记完成 / 标记未完成
- 支持显隐动画
- 响应完成状态协同更新

### 关闭交互

实际关闭逻辑不只在面板内部，而是由 `MainController.setupGlobalInfoPanelInteractions()` 在 `Scene` 上统一处理：

- 点击面板外区域关闭
- 按 `Esc` 关闭

## 7.5 `ScheduleDialog`

当前新建 / 编辑共用同一个对话框。

### 字段

- 名称
- 描述
- 开始日期
- 截止日期
- 优先级分段按钮
- 分类
- 标签
- 颜色标记
- 提醒开关
- 提醒日期
- 提醒时间

### 校验

- 名称不能为空
- 截止日期不能为空
- 若开始日期存在，则必须满足 `startDate <= dueDate`

## 7.6 `FlowchartView`

当前 `FlowchartView` 仍然只是占位页：

- 有标题
- 有“开发中”提示
- `refresh()` 为空实现
- 没有真正的数据读取、布局和交互逻辑

## 8. 共享样式与动效支撑

这是当前代码相较旧版最显著的扩张区域。

### 8.1 共享卡片样式

核心类：`ScheduleCardStyleSupport`

当前内置样式名：

- 经典实体卡片
- 清新扁平
- 温馨治愈风
- 现代高级极简风
- 新粗野主义
- Material You
- 拟物浮雕风

默认样式：`温馨治愈风`

### 8.2 共享状态控件

核心类：`ScheduleStatusControl`

当前提供 3 套尺寸预设：

- `LIST`
- `TIMELINE`
- `HEATMAP`

它不再是普通复选框，而是带预览、回退、完成勾选动画的专用控件。

### 8.3 列表 / 热力图迁移动画支撑

主流程中已实际使用的类包括：

- `ScheduleReflowAnimator`
- `ScheduleCollapsePopAnimator`
- `CollapsePopKeyframePreset`

### 8.4 已存在但尚未成为主流程唯一入口的补充类

仓库中还保留了这些动效支撑类：

- `ScheduleCardMotionSupport`
- `ScheduleLandingTransition`

它们已经有测试与内部协作代码，但当前主流程仍然主要由上面的 reflow / collapse-pop 体系驱动。

## 9. CSS、资源与模块声明

## 9.1 CSS 结构

当前主题加载顺序为：

1. `base.css`
2. `light-theme.css`
3. 当前选中的内置主题覆盖文件
4. 若为导入主题，则再追加用户选择的外部 CSS

## 9.2 图标资源

当前大量图标使用 SVG 资源，`MainController` 中保留了将 SVG 转成 JavaFX 节点的解析逻辑。

## 9.3 `module-info.java`

当前模块依赖：

- `javafx.controls`
- `javafx.fxml`
- `javafx.swing`
- `java.sql`
- `java.prefs`
- `java.xml`
- `java.desktop`

其中：

- `javafx.fxml` 当前更多是为了遗留 FXML 资源保留
- `java.xml` 主要用于 SVG DOM 解析
- `java.desktop` 主要用于 Taskbar 集成

## 10. 测试现状

## 10.1 当前测试源码范围

当前共有 10 个测试类，主要覆盖：

- 时间轴日期解析辅助方法
- 热力图日期覆盖与完成统计辅助方法
- 共享卡片样式映射
- 状态交互模型
- 重排动画可见卡片检索
- 列表排序规则
- 落位交接辅助类
- collapse-pop 关键帧预设
- 完成状态协调器逻辑
- CSS 关键选择器存在性

## 10.2 最近一次执行结果

最近一次执行：

- 时间：`2026-04-04 15:03:58 +08:00`
- 命令：`mvn test`
- 结果：`BUILD FAILURE`
- 表现：Surefire 无法创建 `com.example.controller.ScheduleCompletionCoordinatorTest`
- 日志：`Tests run: 0`

### 当前结论

- 测试源码能够完成编译
- 完整测试套件目前无法正常启动
- 问题看起来位于 Surefire / JPMS 测试装载阶段，而不是普通 Java 编译阶段

因此，当前文档不能再写成“测试已通过”。

## 11. 当前代码与旧说明的偏差

| 旧说法 | 当前真实状态 |
| --- | --- |
| 使用 SQLite | 当前连接 MySQL |
| 主界面主要依赖 FXML | 当前主界面主要由 `MainController` 直接构建 |
| 流程图已完成 | 当前只是占位页 |
| 时间轴有日 / 周 / 月 / 全部切换 | 当前按时长自动分组 |
| 热力图直接依赖 DAO 完成统计 | 当前主流程主要用已加载数据在内存中计算 |
| 整套测试已通过 | 当前 `mvn test` 会在测试启动阶段失败 |

## 12. 当前工程风险与后续建议

按优先级看，当前最值得尽快处理的点是：

1. 外置数据库配置，去掉源码中的硬编码连接信息
2. 修复 Surefire / JPMS 测试装载问题，恢复 `mvn test` 可直接执行
3. 明确热力图文案语言与整体 UI 文案语言的一致性
4. 把全局搜索的“清空后恢复默认列表”交互真正接通
5. 清理遗留 FXML 与占位脚本，降低维护歧义
6. 决定是否继续推进流程图模块，或先把它从导航主入口中降级

## 13. 结论

当前项目已经具备一个可运行、可继续演进的桌面任务管理骨架，并且视图层、状态交互层和视觉层明显比旧版本成熟得多。

但同时也要明确：

- 数据库配置仍然偏原型化
- 流程图与登录仍是占位功能
- 测试体系虽然扩展了源码覆盖面，但主入口还未修复

如果接下来继续开发，建议优先把“数据库配置外置 + 测试恢复 + 交互收口”处理完，再继续增加新功能。
