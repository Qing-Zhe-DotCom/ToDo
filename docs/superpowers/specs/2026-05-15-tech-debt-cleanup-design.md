# 技术债激进清理 — 设计文档

## 目标

激进消除所有技术债：死代码、遗留兼容链路、占位页面、Schedule 桥接模型、MainController 臃肿。

## 清理清单

### Phase 0：死代码大扫除
- 删除 `V001-V003` 旧 SQL 迁移脚本
- 删除 `JdbcConnectionFactory`、`MysqlStageBSchemaManager`
- 删除 `SqlDialect.MYSQL` 分支
- 删除 `FlowchartView` 占位逻辑
- 删除登录占位提示
- 清理 `ApplicationContext` 中 MySQL 链路引用

### Phase 1-4：逐视图迁移
- **Phase 1** InfoPanel：创建 ViewModel → 改造 View → 拆出 Controller
- **Phase 2** HeatmapView：同上模式
- **Phase 3** TimelineView：同上模式
- **Phase 4** ScheduleListView + ScheduleDialog：同上模式，最复杂

### Phase 5：最终清理
- 删除 `Schedule.java`
- 清理 `NavigationService` 中 Schedule 引用
- 最终瘦身 `MainController`（4063 → ~500 行）
- 更新 `module-info.java`

## 架构模式

### ViewModel
- `ScheduleItemViewModel` 基类，直接包装 `ScheduleItem`
- 暴露 JavaFX Property 供视图绑定
- 格式化逻辑在 ViewModel 内

### Controller
- 每个视图一个独立 Controller
- 职责：参数收集 → 调用 Service → 更新 View
- 禁止 SQL、禁止阻塞 UI

## 设计原则
- 每个 Phase 独立可编译、可测试、可回滚
- 渐进式迁移，不改行为只改结构
