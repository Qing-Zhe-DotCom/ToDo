# ToDo 开发者现状文档

本文档基于当前仓库工作区状态编写，基线时间为 **2026-04-03**。它描述的是“现在这份代码真实实现了什么”，而不是产品愿景或未来规划。

## 1. 项目概览

当前项目是一个使用 **JavaFX** 构建的桌面日程管理应用。主工程位于 `todo/` 目录下，采用“应用入口 + 主控制器 + 多视图 + DAO + 模型”的结构。

和旧文档最大的差异是：

- 持久化层当前实际连接 **MySQL**
- 不是 SQLite
- 流程图模块并未实现真实流程图
- 主题切换与时间轴样式切换都位于设置中心，而不是顶部菜单栏或时间轴顶部下拉框

## 2. 工程目录与职责

```text
todo/
├── pom.xml
├── RefactorScript.java
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   ├── com/example/MainApp.java
│   │   │   ├── com/example/controller/MainController.java
│   │   │   ├── com/example/databaseutil/
│   │   │   │   ├── Connectdatabase.java
│   │   │   │   └── ScheduleDAO.java
│   │   │   ├── com/example/model/Schedule.java
│   │   │   └── com/example/view/
│   │   │       ├── View.java
│   │   │       ├── ScheduleListView.java
│   │   │       ├── TimelineView.java
│   │   │       ├── HeatmapView.java
│   │   │       ├── InfoPanelView.java
│   │   │       ├── ScheduleDialog.java
│   │   │       └── FlowchartView.java
│   │   └── resources/
│   │       ├── styles/
│   │       ├── icons/
│   │       ├── database_update.sql
│   │       └── com/example/*.fxml
│   └── test/
│       └── java/com/example/view/
│           ├── TimelineViewTest.java
│           ├── HeatmapViewTest.java
│           └── ThemeCssTest.java
└── target/
```

### 2.1 运行时主链路

真实主链路是：

`MainApp -> MainController -> 当前视图(View) / InfoPanelView -> ScheduleDAO -> MySQL`

### 2.2 不在主链路中的遗留内容

- `RefactorScript.java`：只有占位 `main` 方法，没有实际逻辑
- `primary.fxml` / `secondary.fxml`：未被当前应用入口加载
- `PrimaryController` / `SecondaryController`：源码中不存在
- `database_update.sql`：作为辅助 SQL 文档存在，但运行时不是通过这个文件建表

## 3. 模块与启动流程

## 3.1 `MainApp`

`MainApp` 是 JavaFX 应用入口，主要职责：

- 创建 `MainController`
- 创建主 `Scene`
- 默认加载 `/styles/light-theme.css`
- 设置窗口最小尺寸为 `1200 x 700`
- 设置窗口标题与应用图标
- 注册待办数量监听器，用于更新窗口标题和系统任务栏 badge

与普通 JavaFX 练习项目不同，`MainApp` 还做了这些额外工作：

- 使用 `java.awt.Taskbar` 尝试设置系统任务栏图标与徽标
- 通过 `Canvas + SnapshotParameters` 动态绘制带待办数量角标的图标
- 在 `stop()` 中调用 `mainController.shutdown()`，不过当前 `shutdown()` 仍为空实现

## 3.2 `MainController`

`MainController` 是运行时的真正中枢。它直接组装整个主界面，而不是依赖 FXML。

核心职责包括：

- 构建根节点 `BorderPane`
- 维护左侧侧边栏、中央视图、右侧详情面板
- 初始化 `ScheduleListView`、`TimelineView`、`HeatmapView`、`FlowchartView`
- 管理当前激活视图和当前选中的 `Schedule`
- 打开新建 / 编辑弹窗
- 刷新当前视图与右侧详情面板
- 管理主题、导入主题、时间轴卡片样式偏好
- 维护侧边栏折叠状态与“更多功能”面板展开状态

### 3.2.1 全局状态

`MainController` 中当前比较重要的状态有：

| 状态 | 说明 |
| --- | --- |
| `currentView` | 当前显示在中心区域的视图 |
| `selectedSchedule` | 当前被选中的日程 |
| `currentTheme` | 当前主题 key |
| `currentTimelineCardStyle` | 当前时间轴卡片样式名称 |
| `sidebarCollapsed` | 侧边栏是否折叠 |
| `featurePanelExpanded` | 底部“更多功能”区是否展开 |
| `importedThemeStylesheet` | 外部导入 CSS 的 URI |

### 3.2.2 Preferences 持久化键

当前使用 `java.util.prefs.Preferences` 保存以下偏好：

- `todo.theme`
- `todo.theme.imported.path`
- `todo.timeline.card.style`

### 3.2.3 主题与样式资源

当前内置主题 key 与显示名称：

| key | 名称 |
| --- | --- |
| `light` | 浅色 |
| `mint` | 薄荷 |
| `ocean` | 海洋 |
| `sunset` | 落日 |
| `lavender` | 薰衣草 |
| `forest` | 森林 |
| `slate` | 石板 |
| `macaron` | 马卡龙 |

当前时间轴卡片样式名称：

- 经典实体卡片
- 清新扁平
- 温馨治愈风
- 现代高级极简风
- 新粗野主义
- Material You
- 拟物浮雕风

## 4. 模型层：`Schedule`

`Schedule` 是当前唯一核心业务模型，使用 JavaFX Property 封装字段。

### 4.1 字段盘点

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | `int` | 主键 |
| `name` | `String` | 日程名称 |
| `description` | `String` | 描述 |
| `startDate` | `LocalDate` | 开始日期 |
| `dueDate` | `LocalDate` | 截止日期 |
| `completed` | `boolean` | 是否完成 |
| `priority` | `String` | 优先级，代码按“高 / 中 / 低”理解 |
| `category` | `String` | 分类 |
| `tags` | `String` | 标签，逗号分隔的普通文本 |
| `reminderTime` | `LocalDateTime` | 提醒时间 |
| `color` | `String` | 颜色，HEX 字符串 |
| `createdAt` | `LocalDateTime` | 创建时间 |
| `updatedAt` | `LocalDateTime` | 更新时间 |

### 4.2 模型内置行为

`Schedule` 当前自带一些视图直接依赖的方法：

- `isOverdue()`：未完成且 `dueDate < LocalDate.now()`
- `isUpcoming()`：未完成，且截止日期在未来 7 天内
- `getPriorityValue()`：高 = 3，中 = 2，低 = 1
- 各种字符串 getter / setter：用于兼容旧表结构或字符串格式化需求

### 4.3 注意事项

- 优先级是字符串，不是 enum
- 分类与标签当前没有独立实体模型
- `toString()` 仅返回 `name`

## 5. 持久化层：MySQL + DAO

## 5.1 `Connectdatabase`

当前数据库连接层的实现特点非常明确：

- 数据库类型：MySQL
- 驱动类：`com.mysql.cj.jdbc.Driver`
- URL：`jdbc:mysql://localhost:3306/todo_db`
- 用户名：`root`
- 密码：硬编码在源码中

### 5.1.1 启动时建表/补列

`Connectdatabase.getConnection()` 在获取连接后会执行 `ensureSchema(connection)`。

当前逻辑会：

- 如果 `schedules` 表不存在，则自动创建
- 如果缺少新列，则通过 `ALTER TABLE` 补列

当前关注的字段包括：

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

### 5.1.2 架构初始化方式

运行时真正生效的是 Java 代码中的 `ensureSchema()`，不是 `resources/database_update.sql`。

`database_update.sql` 更像是：

- 手工执行参考
- 文档性 SQL
- 不是应用启动时自动读取的迁移脚本

## 5.2 `ScheduleDAO`

`ScheduleDAO` 承担了当前项目所有数据库访问职责。

### 5.2.1 CRUD

| 方法 | 作用 |
| --- | --- |
| `addSchedule(Schedule)` | 新增日程 |
| `updateSchedule(Schedule)` | 更新日程 |
| `deleteSchedule(int)` | 删除日程 |
| `getScheduleById(int)` | 按 ID 查询 |
| `updateScheduleStatus(int, boolean)` | 单独更新完成状态 |

### 5.2.2 查询与过滤

| 方法 | 作用 |
| --- | --- |
| `getAllSchedules()` | 查询全部日程 |
| `getSchedulesByStatus(boolean)` | 按完成状态查询 |
| `getSchedulesByDateRange(LocalDate, LocalDate)` | 按日期范围查询 |
| `getUpcomingSchedules(int)` | 查询即将到期日程 |
| `getOverdueSchedules()` | 查询逾期未完成日程 |
| `searchSchedules(String)` | 搜索名称或描述 |
| `getSchedulesByCategory(String)` | 按分类查询 |
| `getSchedulesByPriority(String)` | 按优先级查询 |
| `getSchedulesWithPagination(int, int)` | 分页查询 |

### 5.2.3 统计

| 方法 | 作用 |
| --- | --- |
| `getTotalSchedulesCount()` | 总数量 |
| `getCompletedSchedulesCount()` | 已完成数量 |
| `getCompletedCountByDate(LocalDate)` | 某天完成数量 |
| `getDailyCompletionStats(LocalDate, LocalDate)` | 某区间每日完成统计 |
| `getAllCategories()` | 所有分类 |

### 5.2.4 兼容旧表结构

当前 DAO 明显带有“兼容旧 schema”的过渡代码：

- `addSchedule()` 和 `updateSchedule()` 失败时，如果异常信息包含 `Unknown column`，会降级到旧表结构写法
- 分类、优先级、统计等部分查询在旧 schema 下会返回空结果而不是中断
- `mapResultSetToSchedule()` 中对若干列使用 `try/catch` 读取，避免旧表没有对应列时报错

这说明当前数据库层处于“兼容演进中”的状态，而不是稳定完成的迁移体系。

## 6. 视图层现状

所有主视图都实现了 `com.example.view.View`：

```java
public interface View {
    Node getView();
    void refresh();
}
```

## 6.1 `ScheduleListView`

日程列表是当前最完整、最像“主工作台”的视图。

### 已实现行为

- 加载全部日程
- 搜索名称 / 描述
- 排序：
  - 按日期排序
  - 按优先级排序
  - 按分类排序
- 筛选：
  - 全部
  - 未完成
  - 已完成
  - 高优先级
  - 即将到期
- `显示过去日程` 复选框
- 待办 / 已完成分组头折叠
- 点击状态图标切换完成状态
- 点击列表项打开右侧详情面板

### 当前默认过滤逻辑

如果 **不勾选** `显示过去日程`：

- 逾期且未完成的日程会被隐藏
- 截止日期在未来 7 天之后的日程也会被隐藏

因此这个列表默认更像“近期工作面板”，不是完整无限列表。

### 搜索的真实情况

- 搜索只查询 `name` 与 `description`
- 搜索结果直接替换列表显示
- 当前版本没有完善的“清空搜索恢复默认列表”交互闭环

## 6.2 `TimelineView`

时间轴是当前最复杂的自定义渲染视图。

### UI 入口

- 顶部有开始日期、结束日期筛选器
- 有“重置视角”按钮
- 底部有“新建日程”按钮

### 当前真实实现

当前时间轴**不是**“日 / 周 / 月 / 全部”四种可切换模式。

真实实现是：

- 统一绘制一个全量时间轴
- 根据任务持续天数自动分成 3 个垂直分组：
  - 短期：`< 7 天`
  - 中期：`7 - 35 天`
  - 长期：`> 35 天`

### 绘制核心流程

`drawTimeline()` 的主要过程：

1. 拉取全部日程
2. 过滤掉没有有效开始/结束日期的项
3. 计算整体时间范围
4. 应用用户手动选定的日期范围
5. 按持续天数拆分为短 / 中 / 长三组
6. 计算总宽度和面板高度
7. 绘制背景轨道、日期轴、今日高亮、网格线
8. 逐个渲染时间轴卡片
9. 若当前范围包含今天，则自动滚动到今天附近

### 卡片布局规则

每张卡片的 X 坐标由日期偏移决定：

- `ChronoUnit.DAYS.between(minDate, visualStart)` 决定横向偏移

每张卡片的 Y 坐标由分组内冲突检测决定：

- 同组内如果时间区间重叠，就向下分配下一层
- 使用 `CARD_STACK_OFFSET` 形成错落堆叠

### 已实现交互

- 鼠标滚轮控制横向滚动
- 悬停时卡片放大、上浮并提升层级
- 点击卡片：显示右侧详情面板
- 双击卡片：打开编辑弹窗
- 对“今天”所在列进行背景高亮
- 对选中卡片追加 `timeline-schedule-selected` 样式类

### 样式映射

时间轴卡片样式通过 `controller.getCurrentTimelineCardStyle()` 映射到 CSS class：

- `style-classic`
- `style-fresh`
- `style-cozy`
- `style-modern-minimal`
- `style-neo-brutalism`
- `style-material-you`
- `style-neumorphism`

## 6.3 `HeatmapView`

热力图是第二个定制化程度较高的视图。

### 已实现模式

- 周视图
- 月视图
- 年视图

默认打开月视图。

### 核心数据来源

热力图实际使用两套数据：

1. **完成统计数据**
   - 来自 `ScheduleDAO.getDailyCompletionStats(startDate, endDate)`
   - SQL 依据是 `completed = true` 且按 `DATE(updated_at)` 分组
   - 决定热力图格子的颜色深浅

2. **当日日程覆盖数据**
   - 来自 `buildSchedulesByDate(...)`
   - 根据日程的开始 / 截止日期，把跨天任务映射到覆盖区间内的每一天
   - 决定 tooltip 内容和下方“当日日程卡片”面板

### 颜色等级规则

`getLevelForCount(int count)` 当前规则为：

- 0 => level 0
- 1-2 => level 1
- 3-5 => level 2
- 6-8 => level 3
- 9+ => level 4

### 已实现交互

- 切换周 / 月 / 年视图
- 上一周期 / 下一周期
- 回到今天
- 点击单元格选择日期
- 根据所选日期刷新下方日程卡片列表
- 双击下方日程卡片打开编辑弹窗

### 需要特别注意的产品语义

热力图的“颜色深浅”和“下方面板显示的日程数量”并不是同一维度：

- 颜色深浅看的是某天完成了多少任务
- 下方卡片列表看的是有哪些任务覆盖了这一天

这点在用户说明和产品演示时必须说清楚。

## 6.4 `InfoPanelView`

右侧详情面板目前已经比较完整。

### 已实现行为

- 显示标题、状态、开始 / 截止日期
- 显示优先级、分类、标签、提醒时间、描述
- 支持编辑
- 支持删除
- 支持切换完成状态
- 点击面板外区域关闭
- 按 `Esc` 关闭
- 动画显示 / 隐藏

### 状态样式

状态当前分为：

- 已完成
- 已过期
- 即将到期
- 进行中

## 6.5 `ScheduleDialog`

这是当前新增 / 编辑日程的统一弹窗。

### 字段与交互

- 名称
- 描述
- 开始日期
- 截止日期
- 优先级分段按钮
- 分类
- 标签
- 颜色预设板
- 是否开启提醒
- 提醒日期
- 提醒时间

### 已实现校验

- 名称不能为空
- 截止日期不能为空
- 如果开始日期存在，则 `startDate <= dueDate`

### 额外实现细节

- 会在对话框显示时设置窗口图标
- 会尝试替换 `DatePicker` 弹出日历中的左右箭头图标
- 编辑模式下会回填已有数据

## 6.6 `FlowchartView`

当前 `FlowchartView` 只是一个占位页。

真实状态：

- 有标题
- 有“开发中”提示
- `refresh()` 为空实现
- 没有真正的数据读取、布局、节点关系或交互逻辑

## 7. 主题、样式与资源系统

## 7.1 CSS 结构

当前样式采用：

- `base.css`：公共组件样式与大部分详细样式定义
- `light-theme.css`：基础配色变量
- 其他主题文件：局部覆盖变量

主题加载顺序为：

1. `base.css`
2. `light-theme.css`
3. 选中的主题覆盖文件（如 `mint-theme.css`）
4. 若是导入主题，则再追加用户选中的外部 CSS

## 7.2 SVG 图标加载

当前项目没有使用独立 SVG 组件库，而是：

- 通过 DOM 解析 SVG 文件
- 手工创建 `Circle`、`Rectangle`、`SVGPath`、`Text`
- 再缩放并嵌入 JavaFX 组件

`MainController` 和 `ScheduleListView` 各自实现了一套 SVG 解析逻辑，存在重复代码。

## 7.3 `module-info.java`

当前模块声明依赖：

- `javafx.controls`
- `javafx.fxml`
- `javafx.swing`
- `java.sql`
- `java.prefs`
- `java.xml`
- `java.desktop`

其中：

- `javafx.fxml` 当前主要是为遗留 FXML 资源保留
- `java.xml` 主要用于 SVG DOM 解析
- `java.desktop` 主要用于 Taskbar 集成

## 8. 测试现状

## 8.1 当前测试结果

已验证：

- 时间：`2026-04-03 11:04:02 +08:00`
- 命令：`mvn test`
- 结果：`BUILD SUCCESS`
- 汇总：`8 tests, 0 failures, 0 errors, 0 skipped`

## 8.2 当前测试覆盖内容

### `TimelineViewTest`

- 覆盖 `resolveTimelineStart()` / `resolveTimelineEnd()`
- 其中一个测试方法实际上是占位式断言，未真正验证 JavaFX 渲染

### `HeatmapViewTest`

- 覆盖 `buildSchedulesByDate(...)`
- 覆盖 `scheduleOccursOnDate(...)`

### `ThemeCssTest`

- 校验若干 CSS 选择器存在
- 校验新增主题文件存在核心变量

## 8.3 覆盖缺口

当前明显缺少：

- 数据库连接与 schema 初始化的集成测试
- `ScheduleDAO` 的真实数据库 CRUD 测试
- JavaFX UI 自动化测试
- 设置中心、详情面板、对话框交互测试
- 任务栏 badge、窗口图标等平台相关测试

## 9. 当前代码与旧文档的偏差

以下是当前最重要的纠偏点：

| 旧说法 | 当前真实状态 |
| --- | --- |
| 使用 SQLite | 当前代码连接 MySQL |
| 流程图已完成 | 当前仅有占位页 |
| 主题切换在顶部菜单栏，且有暗色模式 | 当前入口在设置中心，内置主题均基于浅色系变量覆盖，没有真正深色主题实现 |
| 时间轴可在顶部下拉框切换卡片风格 | 当前样式切换入口在设置中心的“样式”页 |
| 时间轴提供日 / 周 / 月 / 全部视图 | 当前 UI 没有这套切换，真实行为是按时长自动分组 |
| 可以双击 jar 直接启动 | 仓库当前没有正式打包产物，应从 `todo/` 目录用 Maven 运行源码 |
| FXML 是当前主界面实现基础 | 当前主界面由 `MainController` 直接构建，FXML 资源属于遗留内容 |

## 10. 工程风险与后续建议

按优先级看，当前最值得尽快处理的点是：

1. **去除硬编码数据库凭据**
   当前连接信息直接写在源码中，不适合共享、提交或部署。

2. **把数据库配置外置**
   至少改为环境变量、配置文件或启动参数。

3. **清理遗留资源**
   删除或隔离未接入的 FXML、空脚本和无用目录，避免误导后续维护者。

4. **补上 DAO 集成测试**
   目前数据库相关逻辑完全缺少自动验证。

5. **完善搜索与视图状态恢复**
   当前搜索的交互闭环不完整，用户容易停留在搜索结果态。

6. **统一 SVG 解析逻辑**
   `MainController` 与 `ScheduleListView` 存在重复实现，可抽成工具类。

7. **明确发布方案**
   如果要面向真实用户，需要补齐配置、打包、安装和数据库初始化方案。

## 11. 结论

当前项目已经具备可运行的桌面任务管理骨架，并且：

- 列表、时间轴、热力图、详情面板、设置中心都已有实际功能
- 视觉层和交互层投入较多
- 数据层和工程化层仍处于“可用但不稳”的阶段

如果后续要继续开发，建议优先把“数据库配置外置 + 测试补强 + 清理遗留资源”完成，再继续增加新功能。
