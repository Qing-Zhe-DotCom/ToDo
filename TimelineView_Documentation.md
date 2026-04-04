# TimelineView 组件说明文档

本文档描述的是当前仓库中 `todo/src/main/java/com/example/view/TimelineView.java` 的真实实现，基线时间为 **2026-04-04**。

## 1. 组件定位

`TimelineView` 是当前项目里的横向时间轴视图，用来把带日期信息的日程按时间范围铺在同一条轴上展示。

它当前已经接入：

- 全局日程卡片样式系统
- 共享完成状态控件
- 主控制器的选中状态与详情面板联动
- 完成状态协同更新机制

## 2. 当前真实能力

### 2.1 日期范围控制

顶部提供：

- 开始日期 `DatePicker`
- 结束日期 `DatePicker`
- `重置视角` 按钮

如果只设置一侧日期，另一侧会按 7 天窗口自动补偿；点击 `重置视角` 会清空两侧筛选并重新计算视图。

### 2.2 自动分组规则

当前时间轴并不是“日 / 周 / 月 / 全部”切换组件。

真实逻辑是先按持续天数把日程分为三组：

- 短期日程：`< 7 天`
- 中期日程：`7 - 35 天`
- 长期日程：`> 35 天`

这三组会自上而下绘制在同一张横向时间轴上。

### 2.3 交互能力

- 鼠标滚轮控制横向滚动
- 今天所在日期列高亮
- 如果当前范围包含今天，视图会尽量自动滚到今天附近
- 点击卡片：选中该日程并打开右侧详情
- 双击卡片：打开编辑弹窗
- 点击卡片中的状态控件：切换完成状态
- 悬停卡片：抬升、放大并提高层级，减少遮挡感

## 3. 结构组成

## 3.1 外层布局

`TimelineView` 当前由这些主要部分构成：

- 顶部工具栏
- 中间 `ScrollPane`
- 底部“新建日程”按钮
- 时间轴状态提示文本

## 3.2 关键字段

| 字段 | 作用 |
| --- | --- |
| `timelinePane` | 真正承载时间轴绘制结果的 Pane |
| `scrollPane` | 承载横向滚动 |
| `startDatePicker` / `endDatePicker` | 日期范围控制 |
| `loadedSchedules` | 当前视图内已加载并已应用乐观变更的日程副本 |
| `timelineEntries` | 渲染前的条目集合 |

## 4. 数据流与刷新流程

## 4.1 `refresh()` 的主流程

当前刷新时会：

1. 从 DAO 拉取全部日程
2. 通过 `controller.applyPendingCompletionMutations(...)` 把正在进行中的完成状态乐观变更合并到当前列表
3. 过滤掉没有可解析时间范围的日程
4. 计算原始最小 / 最大日期
5. 叠加用户选择的起止日期范围
6. 按持续天数拆分成短期、中期、长期三组
7. 计算画布总宽度与总高度
8. 绘制背景轨道、日期轴、今天高亮和网格线
9. 逐条渲染卡片
10. 若当前范围包含今天，则滚动到今天附近

## 4.2 起止日期解析规则

时间轴当前使用两个静态辅助方法统一解析时间范围：

- `resolveTimelineStart(Schedule)`
- `resolveTimelineEnd(Schedule)`

它们会尽量在 `startDate` 和 `dueDate` 之间兜底，保证只有一侧日期时也能参与渲染。

## 5. 卡片渲染规则

## 5.1 坐标计算

每张卡片的横向位置由 `visualStart` 相对于 `minDate` 的偏移决定：

- `startOffset = ChronoUnit.DAYS.between(minDate, visualStart)`
- `startX = LEFT_PADDING + startOffset * DAY_WIDTH + CARD_INSET_X`

宽度由可视范围内的持续天数决定：

- `duration = ChronoUnit.DAYS.between(visualStart, visualEnd) + 1`
- `width = duration * DAY_WIDTH - CARD_INSET_X * 2`

## 5.2 垂直堆叠

同一分组内如果时间区间重叠，卡片会向下分配到新的堆叠层。当前使用：

- `CARD_STACK_OFFSET = 52`

来形成分层错落效果。

## 5.3 卡片组成

当前每张卡片由这些元素组成：

- 背景层
- 左侧强调色条
- 共享状态控件 `ScheduleStatusControl`
- 标题文本
- 日期文本
- 在超短卡片宽度下自动隐藏的文本内容

## 5.4 样式系统接入

当前卡片渲染会调用：

- `ScheduleCardStyleSupport.applyCardPresentation(...)`

因此时间轴卡片会自动附加：

- 基础卡片类
- 时间轴角色类
- 当前选中的全局卡片样式类
- 逾期 / 即将到期 / 高优先级等状态类

## 6. 完成状态交互

## 6.1 当前行为

时间轴中的状态控件会直接调用：

- `controller.updateScheduleCompletion(schedule, targetCompleted)`

因此时间轴已经是当前完成状态协同体系的一部分，而不是只读视图。

## 6.2 与其他视图的同步

`TimelineView` 实现了 `ScheduleCompletionParticipant`，因此它会响应：

- `applyCompletionMutation(...)`
- `confirmCompletionMutation(...)`
- `revertCompletionMutation(...)`

表现上就是：

- 某条日程在别的视图里被乐观标记完成后，时间轴会尽快同步显示完成态
- 如果持久化失败，时间轴会恢复到原状态

## 7. 静态辅助方法

当前类中几个重要的辅助方法有：

- `filterAndSortSchedules(...)`
- `buildTimelineEntries(...)`
- `resolveTimelineStart(...)`
- `resolveTimelineEnd(...)`

这些方法已经被测试或被其他逻辑依赖，属于时间轴当前最稳定的一层纯逻辑代码。

## 8. 已知边界与限制

- 当前不支持真正的“日 / 周 / 月 / 全部”模式切换
- 当前没有虚拟列表或增量渲染机制，仍然是一次性构造当前时间范围内节点
- JavaFX 层面的完整 UI 自动化测试还没有接入
- 卡片过多时虽然有堆叠和 `viewOrder` 处理，但依然可能出现视觉拥挤

## 9. 测试现状

`TimelineViewTest` 当前主要覆盖：

- `resolveTimelineStart()` / `resolveTimelineEnd()`

同时还保留了一个占位式测试，用来说明当前没有引入 TestFX 级别的完整渲染验证。

也就是说，时间轴的纯逻辑辅助方法有基础测试，但完整 JavaFX 渲染与交互仍主要依赖人工验证。

## 10. 结论

当前 `TimelineView` 已经不是早期的概念型时间轴，而是一个：

- 支持日期范围筛选
- 按时长自动分组
- 接入全局卡片样式
- 支持状态切换
- 能参与跨视图完成态同步

的实际可用组件。

如果后续要继续演进，建议优先考虑：

- 增加更系统的 JavaFX UI 自动化测试
- 根据数据量评估是否需要虚拟化或按区间懒绘制
- 决定是否真的要引入旧文档里曾提到的多模式视图切换，而不是继续保持当前的时长分组模型
