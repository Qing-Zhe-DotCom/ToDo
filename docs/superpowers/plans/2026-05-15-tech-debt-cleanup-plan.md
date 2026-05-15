# 技术债激进清理 — 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 激进消除所有技术债：死代码 + MySQL 链路 + 占位页面 + Schedule 桥接 + MainController 瘦身（4063 → ~500 行）

**Architecture:** Schedule extends ScheduleItem（已标记 @Deprecated），去除 Schedule 中间层的核心策略是将所有 Schedule 类型引用替换为 ScheduleItem，因为 Schedule 只是 ScheduleItem 的一个薄子类，没有新增字段。ScheduleCompletionMutation/Coordinator 已经使用 ScheduleItem，无需改动。

**Tech Stack:** Java 21, JavaFX 21, Maven, SQLite, JPMS module `com.example`

**关键事实：**
- `Schedule extends ScheduleItem` — 迁移就是替换类型引用
- `ScheduleCompletionMutation` 和 `ScheduleCompletionCoordinator` 已使用 `ScheduleItem`
- `ScheduleCompletionParticipant` 接口不涉及 Schedule 类型
- 19 个文件 import Schedule（含 MainController、7 个视图、NavigationService、多个 application 服务和测试）
- 主命令：`cd ./todo && mvn -f .\pom.xml clean compile`

---

### Phase 0：死代码大扫除

### Task 0.1: 删除旧 SQL 迁移脚本

**Files:**
- Delete: `todo/src/main/resources/db/sqlite/V001__create_schedules.sql`
- Delete: `todo/src/main/resources/db/sqlite/V002__add_local_sync_columns.sql`
- Delete: `todo/src/main/resources/db/sqlite/V003__add_minute_precision_schedule_times.sql`

- [ ] **Step 1: 删除文件**

```bash
rm "todo/src/main/resources/db/sqlite/V001__create_schedules.sql"
rm "todo/src/main/resources/db/sqlite/V002__add_local_sync_columns.sql"
rm "todo/src/main/resources/db/sqlite/V003__add_minute_precision_schedule_times.sql"
```

- [ ] **Step 2: 验证编译**

```bash
cd ./todo && mvn -f .\pom.xml clean compile
```
Expected: BUILD SUCCESS

- [ ] **Step 3: 提交**

```bash
git add todo/src/main/resources/db/sqlite/
git commit -m "chore: remove legacy SQL migration scripts V001-V003

These scripts predate the Stage-B schema migration and are no longer
used at runtime. SqliteStageBSchemaManager handles schema init.

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```

### Task 0.2: 删除 MySQL 兼容链路

**Files:**
- Delete: `todo/src/main/java/com/example/data/JdbcConnectionFactory.java`
- Delete: `todo/src/main/java/com/example/data/MysqlStageBSchemaManager.java`

- [ ] **Step 1: 删除 MySQL 实现文件**

```bash
rm "todo/src/main/java/com/example/data/JdbcConnectionFactory.java"
rm "todo/src/main/java/com/example/data/MysqlStageBSchemaManager.java"
```

- [ ] **Step 2: 验证编译（此时预期失败，ApplicationContext 还有引用）**

```bash
cd ./todo && mvn -f .\pom.xml clean compile 2>&1 | tail -20
```
Expected: COMPILATION ERROR（ApplicationContext 引用已删除的类）

- [ ] **Step 3: 提交**

```bash
git add todo/src/main/java/com/example/data/
git commit -m "chore: remove MySQL compatibility chain

Delete JdbcConnectionFactory and MysqlStageBSchemaManager.
SQLite is the sole default database path since Stage-B.

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```

### Task 0.3: 清理 SqlDialect 和 ApplicationContext

**Files:**
- Modify: `todo/src/main/java/com/example/data/SqlDialect.java`
- Modify: `todo/src/main/java/com/example/application/ApplicationContext.java`

- [ ] **Step 1: 简化 SqlDialect 枚举，移除 MYSQL**

Edit `todo/src/main/java/com/example/data/SqlDialect.java`:

```java
package com.example.data;

public enum SqlDialect {
    SQLITE
}
```

删除 `MYSQL` 枚举值。

- [ ] **Step 2: 清理 ApplicationContext，移除 MySQL 分支**

Edit `todo/src/main/java/com/example/application/ApplicationContext.java`:

删除以下 import：
```java
import com.example.data.JdbcConnectionFactory;
import com.example.data.MysqlStageBSchemaManager;
```

修改 `createDefault()` 方法，删除 `else` 分支（MySQL 路径）并简化变量声明。将 L90-102：

```java
        AppDataPaths appDataPaths = null;
        ConnectionFactory connectionFactory;
        SchemaManager schemaManager;
        ScheduleItemRepository scheduleItemRepository;
        if (databaseProperties.isSqliteMode()) {
            appDataPaths = new AppDataPaths(
                appProperties.getDataDirectoryOverride(),
                databaseProperties.getSqlitePath()
            );
            connectionFactory = new SqliteConnectionFactory(databaseProperties, appDataPaths);
            schemaManager = new SqliteStageBSchemaManager();
            scheduleItemRepository = new SqlScheduleItemRepository(connectionFactory, schemaManager, SqlDialect.SQLITE);
        } else {
            connectionFactory = new JdbcConnectionFactory(databaseProperties);
            schemaManager = new MysqlStageBSchemaManager();
            scheduleItemRepository = new SqlScheduleItemRepository(connectionFactory, schemaManager, SqlDialect.MYSQL);
        }
```

改为：

```java
        AppDataPaths appDataPaths = new AppDataPaths(
            appProperties.getDataDirectoryOverride(),
            databaseProperties.getSqlitePath()
        );
        ConnectionFactory connectionFactory = new SqliteConnectionFactory(databaseProperties, appDataPaths);
        SchemaManager schemaManager = new SqliteStageBSchemaManager();
        ScheduleItemRepository scheduleItemRepository = new SqlScheduleItemRepository(connectionFactory, schemaManager, SqlDialect.SQLITE);
```

- [ ] **Step 3: 验证编译**

```bash
cd ./todo && mvn -f .\pom.xml clean compile
```
Expected: BUILD SUCCESS

- [ ] **Step 4: 运行测试**

```bash
cd ./todo && mvn -f .\pom.xml test
```
Expected: All tests pass

- [ ] **Step 5: 提交**

```bash
git add todo/src/main/java/com/example/data/SqlDialect.java todo/src/main/java/com/example/application/ApplicationContext.java
git commit -m "chore: remove MySQL code path from SqlDialect and ApplicationContext

SqlDialect now only has SQLITE. ApplicationContext no longer supports
MySQL connection factory or schema manager.

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```

### Task 0.4: 删除 FlowchartView 占位和登录占位

**Files:**
- Modify: `todo/src/main/java/com/example/view/FlowchartView.java`
- Modify: `todo/src/main/java/com/example/controller/MainController.java` (删除 flowchart 导航按钮和登录按钮相关代码)

- [ ] **Step 1: 删除 FlowchartView 占位逻辑**

Read `FlowchartView.java` 确认其为纯占位页。将整个文件替换为空实现：

```java
package com.example.view;

import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;

public final class FlowchartView implements View {
    private final StackPane root = new StackPane();

    public FlowchartView() {
        root.getChildren().add(new Label("Coming soon"));
    }

    @Override
    public Node getView() {
        return root;
    }

    @Override
    public void refresh() {
    }
}
```

若 FlowchartView 原本只是占位，改为最小化实现。

- [ ] **Step 2: 删除 MainController 中 flowchart 导航按钮和登录按钮**

从 MainController 中删除以下代码段：
- `private FlowchartView flowchartView;` 字段声明
- `private ToggleButton flowchartNavButton;` 字段声明
- `private Button loginActionButton;` 字段声明
- `flowchartView = new FlowchartView();` 初始化
- `flowchartNavButton` 创建和事件绑定代码
- `showLoginDialog()` 方法（约 L2084-2093）
- 所有对 flowchartView 的引用处
- 所有对 loginActionButton 的引用处
- `resolveScreen()` 方法中的 `FlowchartView` 分支

- [ ] **Step 3: 验证编译**

```bash
cd ./todo && mvn -f .\pom.xml clean compile
```
Expected: BUILD SUCCESS

- [ ] **Step 4: 提交**

```bash
git add todo/src/main/java/com/example/view/FlowchartView.java todo/src/main/java/com/example/controller/MainController.java
git commit -m "chore: remove FlowchartView placeholder and login placeholder

FlowchartView simplified to minimal stub. Login button and dialog
removed from MainController — will be re-added when auth system lands.

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```

---

### Phase 1：ScheduleItemViewModel 基类 + NavigationService 清理

Phase 1-4 的目标是逐步消除对 `Schedule` 的依赖。核心策略：`Schedule extends ScheduleItem`，所有使用 `Schedule` 的地方改为直接使用 `ScheduleItem`。

### Task 1.1: 创建 ScheduleItemViewModel 基类

**Files:**
- Create: `todo/src/main/java/com/example/application/ScheduleItemViewModel.java`

- [ ] **Step 1: 编写 ScheduleItemViewModel**

```java
package com.example.application;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.List;

import com.example.model.RecurrenceRule;
import com.example.model.Reminder;
import com.example.model.ScheduleItem;
import com.example.model.Tag;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class ScheduleItemViewModel {

    private final StringProperty id;
    private final StringProperty title;
    private final StringProperty description;
    private final StringProperty notes;
    private final StringProperty priority;
    private final StringProperty category;
    private final StringProperty color;
    private final BooleanProperty completed;
    private final BooleanProperty allDay;
    private final BooleanProperty suspended;
    private final BooleanProperty hasRecurrence;
    private final ObjectProperty<LocalDateTime> startAt;
    private final ObjectProperty<LocalDateTime> dueAt;
    private final ObjectProperty<LocalDateTime> endAt;
    private final ObjectProperty<LocalDateTime> createdAt;
    private final ObjectProperty<LocalDateTime> updatedAt;
    private final ObjectProperty<LocalDateTime> deletedAt;
    private final IntegerProperty version;
    private final StringProperty syncStatus;
    private final StringProperty deviceId;
    private final StringProperty tagsText;
    private final ObjectProperty<LocalDateTime> reminderTime;
    private final ObjectProperty<RecurrenceRule> recurrenceRule;
    private final ObservableList<Tag> tags;
    private final ObservableList<Reminder> reminders;

    // 格式化字段
    private final StringProperty formattedDate;
    private final StringProperty formattedTime;
    private final StringProperty recurrenceSummary;

    private final ScheduleItem item;

    public ScheduleItemViewModel(ScheduleItem item) {
        this.item = item != null ? item.copy() : new ScheduleItem();

        this.id = new SimpleStringProperty(this.item.getId());
        this.title = new SimpleStringProperty(this.item.getTitle());
        this.description = new SimpleStringProperty(this.item.getDescription());
        this.notes = new SimpleStringProperty(this.item.getNotes());
        this.priority = new SimpleStringProperty(this.item.getPriority());
        this.category = new SimpleStringProperty(this.item.getCategory());
        this.color = new SimpleStringProperty(this.item.getColor());
        this.completed = new SimpleBooleanProperty(this.item.isCompleted());
        this.allDay = new SimpleBooleanProperty(this.item.isAllDay());
        this.suspended = new SimpleBooleanProperty(this.item.isSuspended());
        this.hasRecurrence = new SimpleBooleanProperty(this.item.hasRecurrence());
        this.startAt = new SimpleObjectProperty<>(this.item.getStartAt());
        this.dueAt = new SimpleObjectProperty<>(this.item.getDueAt());
        this.endAt = new SimpleObjectProperty<>(this.item.getEndAt());
        this.createdAt = new SimpleObjectProperty<>(this.item.getCreatedAt());
        this.updatedAt = new SimpleObjectProperty<>(this.item.getUpdatedAt());
        this.deletedAt = new SimpleObjectProperty<>(this.item.getDeletedAt());
        this.version = new SimpleIntegerProperty(this.item.getVersion());
        this.syncStatus = new SimpleStringProperty(this.item.getSyncStatus());
        this.deviceId = new SimpleStringProperty(this.item.getDeviceId());
        this.tagsText = new SimpleStringProperty(this.item.getTags());
        this.reminderTime = new SimpleObjectProperty<>(this.item.getReminderTime());
        this.recurrenceRule = new SimpleObjectProperty<>(this.item.getRecurrenceRule());
        this.tags = FXCollections.observableArrayList(this.item.getTagObjects());
        this.reminders = FXCollections.observableArrayList(this.item.getReminders());

        this.formattedDate = new SimpleStringProperty("");
        this.formattedTime = new SimpleStringProperty("");
        this.recurrenceSummary = new SimpleStringProperty("");

        syncBindings();
    }

    public static ScheduleItemViewModel from(ScheduleItem item) {
        return new ScheduleItemViewModel(item);
    }

    public ScheduleItem toScheduleItem() {
        ScheduleItem result = item.copy();
        result.setTitle(title.get());
        result.setDescription(description.get());
        result.setNotes(notes.get());
        result.setPriority(priority.get());
        result.setCategory(category.get());
        result.setColor(color.get());
        result.setCompleted(completed.get());
        result.setAllDay(allDay.get());
        result.setSuspended(suspended.get());
        result.setStartAt(startAt.get());
        result.setDueAt(dueAt.get());
        result.setEndAt(endAt.get());
        result.setReminderTime(reminderTime.get());
        result.setRecurrenceRule(recurrenceRule.get());
        result.setTagObjects(new java.util.ArrayList<>(tags));
        result.setReminders(new java.util.ArrayList<>(reminders));
        return result;
    }

    public ScheduleItem getSourceItem() {
        return item.copy();
    }

    // Property getters
    public StringProperty idProperty() { return id; }
    public StringProperty titleProperty() { return title; }
    public StringProperty descriptionProperty() { return description; }
    public StringProperty notesProperty() { return notes; }
    public StringProperty priorityProperty() { return priority; }
    public StringProperty categoryProperty() { return category; }
    public StringProperty colorProperty() { return color; }
    public BooleanProperty completedProperty() { return completed; }
    public BooleanProperty allDayProperty() { return allDay; }
    public BooleanProperty suspendedProperty() { return suspended; }
    public BooleanProperty hasRecurrenceProperty() { return hasRecurrence; }
    public ObjectProperty<LocalDateTime> startAtProperty() { return startAt; }
    public ObjectProperty<LocalDateTime> dueAtProperty() { return dueAt; }
    public ObjectProperty<LocalDateTime> endAtProperty() { return endAt; }
    public ObjectProperty<LocalDateTime> createdAtProperty() { return createdAt; }
    public ObjectProperty<LocalDateTime> updatedAtProperty() { return updatedAt; }
    public ObjectProperty<LocalDateTime> deletedAtProperty() { return deletedAt; }
    public IntegerProperty versionProperty() { return version; }
    public StringProperty syncStatusProperty() { return syncStatus; }
    public StringProperty deviceIdProperty() { return deviceId; }
    public StringProperty tagsTextProperty() { return tagsText; }
    public ObjectProperty<LocalDateTime> reminderTimeProperty() { return reminderTime; }
    public ObjectProperty<RecurrenceRule> recurrenceRuleProperty() { return recurrenceRule; }
    public ObservableList<Tag> tagsList() { return tags; }
    public ObservableList<Reminder> remindersList() { return reminders; }
    public StringProperty formattedDateProperty() { return formattedDate; }
    public StringProperty formattedTimeProperty() { return formattedTime; }
    public StringProperty recurrenceSummaryProperty() { return recurrenceSummary; }

    // Value getters
    public String getId() { return id.get(); }
    public String getTitle() { return title.get(); }
    public String getDescription() { return description.get(); }
    public String getNotes() { return notes.get(); }
    public String getPriority() { return priority.get(); }
    public String getCategory() { return category.get(); }
    public boolean isCompleted() { return completed.get(); }
    public boolean isAllDay() { return allDay.get(); }
    public boolean isSuspended() { return suspended.get(); }
    public boolean hasRecurrence() { return hasRecurrence.get(); }
    public LocalDateTime getStartAt() { return startAt.get(); }
    public LocalDateTime getDueAt() { return dueAt.get(); }
    public LocalDateTime getEndAt() { return endAt.get(); }
    public LocalDateTime getCreatedAt() { return createdAt.get(); }
    public LocalDateTime getUpdatedAt() { return updatedAt.get(); }
    public LocalDateTime getDeletedAt() { return deletedAt.get(); }
    public int getVersion() { return version.get(); }
    public String getSyncStatus() { return syncStatus.get(); }
    public String getDeviceId() { return deviceId.get(); }
    public LocalDateTime getReminderTime() { return reminderTime.get(); }
    public RecurrenceRule getRecurrenceRule() { return recurrenceRule.get(); }

    public void updateFormattedFields(
        String dateText,
        String timeText,
        String recurrenceText
    ) {
        formattedDate.set(dateText != null ? dateText : "");
        formattedTime.set(timeText != null ? timeText : "");
        recurrenceSummary.set(recurrenceText != null ? recurrenceText : "");
    }

    private void syncBindings() {
        title.addListener((obs, oldVal, newVal) -> item.setTitle(newVal));
        description.addListener((obs, oldVal, newVal) -> item.setDescription(newVal));
        notes.addListener((obs, oldVal, newVal) -> item.setNotes(newVal));
        completed.addListener((obs, oldVal, newVal) -> item.setCompleted(newVal));
    }
}
```

- [ ] **Step 2: 验证编译**

```bash
cd ./todo && mvn -f .\pom.xml clean compile
```
Expected: BUILD SUCCESS

- [ ] **Step 3: 提交**

```bash
git add todo/src/main/java/com/example/application/ScheduleItemViewModel.java
git commit -m "feat: add ScheduleItemViewModel base class

JavaFX Property-based ViewModel wrapping ScheduleItem. Provides
formatted fields for UI binding and bidirectional property sync.
All view-specific ViewModels will build on this base.

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```

### Task 1.2: 清理 NavigationService — Schedule → ScheduleItem

**Files:**
- Modify: `todo/src/main/java/com/example/application/NavigationService.java`

- [ ] **Step 1: 替换类型引用**

Edit `NavigationService.java`:

```java
package com.example.application;

import com.example.model.ScheduleItem;

public final class NavigationService {
    public enum Screen {
        LIST,
        TIMELINE,
        HEATMAP
    }

    private Screen currentScreen = Screen.LIST;
    private ScheduleItem selectedScheduleItem;

    public Screen getCurrentScreen() {
        return currentScreen;
    }

    public void setCurrentScreen(Screen currentScreen) {
        this.currentScreen = currentScreen;
    }

    public ScheduleItem getSelectedScheduleItem() {
        return selectedScheduleItem;
    }

    public void setSelectedScheduleItem(ScheduleItem item) {
        this.selectedScheduleItem = item;
    }

    public void clearSelectedScheduleItem() {
        selectedScheduleItem = null;
    }

    public boolean isSelected(ScheduleItem item) {
        if (selectedScheduleItem == null || item == null) {
            return false;
        }
        if (selectedScheduleItem.getId() != null && !selectedScheduleItem.getId().isBlank()
            && item.getId() != null && !item.getId().isBlank()) {
            return selectedScheduleItem.getId().equals(item.getId());
        }
        return selectedScheduleItem == item;
    }
}
```

关键变化：
- `import Schedule` → `import ScheduleItem`
- `Schedule selectedSchedule` → `ScheduleItem selectedScheduleItem`
- `getSelectedSchedule()` → `getSelectedScheduleItem()`
- `setSelectedSchedule(Schedule)` → `setSelectedScheduleItem(ScheduleItem)`
- `clearSelectedSchedule()` → `clearSelectedScheduleItem()`
- `isSelected(Schedule)` → `isSelected(ScheduleItem)`
- 从 `Screen` 枚举中删除 `FLOWCHART`

- [ ] **Step 2: 验证编译（预期失败，因为 MainController 还在用旧方法名）**

```bash
cd ./todo && mvn -f .\pom.xml clean compile 2>&1 | tail -20
```
Expected: COMPILATION ERROR — 这是预期的，MainController 和视图还引用旧签名

- [ ] **Step 3: 提交**

```bash
git add todo/src/main/java/com/example/application/NavigationService.java
git commit -m "refactor: replace Schedule with ScheduleItem in NavigationService

Rename selectedSchedule->selectedScheduleItem, update method signatures.
Remove FLOWCHART from Screen enum. Breaking change — callers will be
updated in subsequent tasks.

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```

### Task 1.3: MainController 第一步 — 将 public API 从 Schedule 迁移到 ScheduleItem

这是最大的一步。将 MainController 中所有使用 `Schedule` 作为参数/返回类型的 public 方法改为 `ScheduleItem`。

**Files:**
- Modify: `todo/src/main/java/com/example/controller/MainController.java`

- [ ] **Step 1: 系统性替换类型引用**

MainController 需要以下修改（按方法逐一替换）：

**Import 修改：**
- 删除 `import com.example.model.Schedule;`

**NavigationService 调用迁移（将旧方法名替换为新方法名）：**
- `navigationService.getSelectedSchedule()` → `navigationService.getSelectedScheduleItem()`
- `navigationService.setSelectedSchedule(x)` → `navigationService.setSelectedScheduleItem(x)`
- `navigationService.clearSelectedSchedule()` → `navigationService.clearSelectedScheduleItem()`
- `navigationService.isSelected(x)` → `navigationService.isSelected(x)`

**方法签名修改（所有 `Schedule` 参数/返回值 → `ScheduleItem`）：**

| 旧签名 | 新签名 |
|--------|--------|
| `showScheduleDetails(Schedule)` | `showScheduleDetails(ScheduleItem)` |
| `showScheduleDetailsAndFocusTitle(Schedule)` | `showScheduleDetailsAndFocusTitle(ScheduleItem)` |
| `isScheduleSelected(Schedule): boolean` | `isScheduleSelected(ScheduleItem): boolean` |
| `getSelectedSchedule(): Schedule` | `getSelectedScheduleItem(): ScheduleItem` |
| `prepareScheduleCompletion(Schedule, boolean)` | `prepareScheduleCompletion(ScheduleItem, boolean)` |
| `updateScheduleCompletion(Schedule, boolean)` | `updateScheduleCompletion(ScheduleItem, boolean)` |
| `createSchedule(Schedule): String` | `createSchedule(ScheduleItem): String` |
| `quickCreateSchedule(String): Schedule` | `quickCreateSchedule(String): ScheduleItem` |
| `saveSchedule(Schedule): boolean` | `saveSchedule(ScheduleItem): boolean` |
| `removeSchedule(String): boolean` | 不变 |
| `findScheduleById(String): Schedule` | `findScheduleById(String): ScheduleItem` |
| `loadAllSchedules(): List<Schedule>` | `loadAllSchedules(): List<ScheduleItem>` |
| `searchSchedules(String): List<Schedule>` | `searchSchedules(String): List<ScheduleItem>` |
| `loadDeletedSchedules(): List<Schedule>` | `loadDeletedSchedules(): List<ScheduleItem>` |
| `restoreDeletedSchedule(String): boolean` | 不变 |
| `permanentlyDeleteSchedule(String): boolean` | 不变 |
| `applyPendingCompletionMutations(List<Schedule>): List<Schedule>` | `applyPendingCompletionMutations(List<ScheduleItem>): List<ScheduleItem>` |

**删除 `toLegacySchedule()` 和 `toLegacySchedules()` 方法：**
- 删除 `toLegacySchedule(ScheduleItem)`（L4030-4035）
- 删除 `toLegacySchedules(List<ScheduleItem>)`（L4037-4049）

**修改所有使用 `toLegacySchedule` / `toLegacySchedules` 的地方：**
- `findScheduleById()`: `return toLegacySchedule(...)` → `return ...` (直接返回 ScheduleItem)
- `loadAllSchedules()`: `return toLegacySchedules(...)` → `return ...`
- `searchSchedules()`: `return toLegacySchedules(...)` → `return ...`
- `loadDeletedSchedules()`: `return toLegacySchedules(...)` → `return ...`

**修改 `quickCreateSchedule()`：**
- `Schedule schedule = new Schedule()` → `ScheduleItem item = new ScheduleItem()`
- 返回值改为 `ScheduleItem`

**修改 `createTrashRow()` 和 `buildTrashMetaText()`：**
- 参数类型 `Schedule` → `ScheduleItem`

**修改 `populateTrashSettingsList()` 和 `applyCompletionMutationLocally()` 等私有方法：**
- 内部变量类型 `Schedule` → `ScheduleItem`

- [ ] **Step 2: 验证编译（预期失败，视图层还引用 Schedule）**

```bash
cd ./todo && mvn -f .\pom.xml clean compile 2>&1 | tail -30
```
Expected: COMPILATION ERROR — 视图层尚未更新

- [ ] **Step 3: 提交**

```bash
git add todo/src/main/java/com/example/controller/MainController.java
git commit -m "refactor: migrate MainController public API from Schedule to ScheduleItem

Replace all Schedule parameter/return types with ScheduleItem.
Remove toLegacySchedule/toLegacySchedules bridge methods.
Delete FlowchartView and login references from NavigationService.
Breaking: views still reference Schedule — updated in next tasks.

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```

---

### Phase 2：视图层迁移（Schedule → ScheduleItem）

### Task 2.1: 迁移 InfoPanelView

**Files:**
- Modify: `todo/src/main/java/com/example/view/InfoPanelView.java`

- [ ] **Step 1: 将所有 Schedule 替换为 ScheduleItem**

- `import com.example.model.Schedule` → `import com.example.model.ScheduleItem`
- `setSchedule(Schedule)` → `setScheduleItem(ScheduleItem)`
- 内部所有 `Schedule` 类型 → `ScheduleItem`
- 所有调用 `schedule.getName()` → `item.getTitle()`
- 所有调用 `schedule.getTags()` → `item.getTags()`
- `navigationService.getSelectedSchedule()` → `navigationService.getSelectedScheduleItem()`

- [ ] **Step 2: 验证编译（预期失败，其余视图尚未更新）**

```bash
cd ./todo && mvn -f .\pom.xml clean compile 2>&1 | grep "ERROR"
```
Expected: 仅剩其他视图的 Schedule 引用报错

- [ ] **Step 3: 提交**

```bash
git add todo/src/main/java/com/example/view/InfoPanelView.java
git commit -m "refactor: migrate InfoPanelView from Schedule to ScheduleItem

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```

### Task 2.2: 迁移 HeatmapView

**Files:**
- Modify: `todo/src/main/java/com/example/view/HeatmapView.java`

- [ ] **Step 1: 替换类型引用**

- `import com.example.model.Schedule` → `import com.example.model.ScheduleItem`
- 所有 `Schedule` → `ScheduleItem`
- `schedule.getName()` → `item.getTitle()`

- [ ] **Step 2: 验证编译**

```bash
cd ./todo && mvn -f .\pom.xml clean compile 2>&1 | grep "ERROR"
```

- [ ] **Step 3: 提交**

```bash
git add todo/src/main/java/com/example/view/HeatmapView.java
git commit -m "refactor: migrate HeatmapView from Schedule to ScheduleItem

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```

### Task 2.3: 迁移 TimelineView

**Files:**
- Modify: `todo/src/main/java/com/example/view/TimelineView.java`

- [ ] **Step 1: 替换类型引用**

- `import com.example.model.Schedule` → `import com.example.model.ScheduleItem`
- 所有 `Schedule` → `ScheduleItem`

- [ ] **Step 2: 验证编译**

```bash
cd ./todo && mvn -f .\pom.xml clean compile 2>&1 | grep "ERROR"
```

- [ ] **Step 3: 提交**

```bash
git add todo/src/main/java/com/example/view/TimelineView.java
git commit -m "refactor: migrate TimelineView from Schedule to ScheduleItem

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```

### Task 2.4: 迁移 ScheduleListView

**Files:**
- Modify: `todo/src/main/java/com/example/view/ScheduleListView.java`

- [ ] **Step 1: 替换类型引用**

- `import com.example.model.Schedule` → `import com.example.model.ScheduleItem`
- 所有 `Schedule` → `ScheduleItem`

- [ ] **Step 2: 验证编译**

```bash
cd ./todo && mvn -f .\pom.xml clean compile 2>&1 | grep "ERROR"
```

- [ ] **Step 3: 提交**

```bash
git add todo/src/main/java/com/example/view/ScheduleListView.java
git commit -m "refactor: migrate ScheduleListView from Schedule to ScheduleItem

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```

### Task 2.5: 迁移其余 support 视图文件

**Files:**
- Modify: `todo/src/main/java/com/example/view/ScheduleDialog.java`
- Modify: `todo/src/main/java/com/example/view/ScheduleCardStyleSupport.java`
- Modify: `todo/src/main/java/com/example/view/ScheduleReflowAnimator.java`

- [ ] **Step 1: 批量替换类型引用**

对以上三个文件：
- `import com.example.model.Schedule` → `import com.example.model.ScheduleItem`
- 所有 `Schedule` 局部变量/参数 → `ScheduleItem`

- [ ] **Step 2: 验证编译（此时所有视图已迁移，预期编译成功）**

```bash
cd ./todo && mvn -f .\pom.xml clean compile
```
Expected: BUILD SUCCESS（或仅剩 application 层的 Schedule 引用）

- [ ] **Step 3: 提交**

```bash
git add todo/src/main/java/com/example/view/ScheduleDialog.java todo/src/main/java/com/example/view/ScheduleCardStyleSupport.java todo/src/main/java/com/example/view/ScheduleReflowAnimator.java
git commit -m "refactor: migrate support views from Schedule to ScheduleItem

ScheduleDialog, ScheduleCardStyleSupport, ScheduleReflowAnimator
all use ScheduleItem directly now.

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```

---

### Phase 3：Application 层迁移

### Task 3.1: 迁移 application 层中仍使用 Schedule 的文件

**Files:**
- Modify: `todo/src/main/java/com/example/application/LocalizationService.java`
- Modify: `todo/src/main/java/com/example/application/ScheduleOccurrenceProjector.java`
- Modify: `todo/src/main/java/com/example/application/ReminderToastPlanner.java`
- Modify: `todo/src/main/java/com/example/application/ReminderNotificationService.java`

- [ ] **Step 1: 逐个文件替换**

对以上每个文件：
- `import com.example.model.Schedule` → `import com.example.model.ScheduleItem`
- 所有 `Schedule` 类型引用 → `ScheduleItem`

- [ ] **Step 2: 验证编译**

```bash
cd ./todo && mvn -f .\pom.xml clean compile
```
Expected: BUILD SUCCESS

- [ ] **Step 3: 提交**

```bash
git add todo/src/main/java/com/example/application/
git commit -m "refactor: migrate application services from Schedule to ScheduleItem

LocalizationService, ScheduleOccurrenceProjector, ReminderToastPlanner,
ReminderNotificationService all use ScheduleItem directly.

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```

---

### Phase 4：测试文件迁移 + 删除 Schedule.java

### Task 4.1: 迁移测试文件

**Files:**
- Modify: `todo/src/test/java/com/example/view/HeatmapViewTest.java`
- Modify: `todo/src/test/java/com/example/controller/ScheduleCompletionCoordinatorTest.java`
- Modify: `todo/src/test/java/com/example/application/ReminderToastPlannerTest.java`
- Modify: `todo/src/test/java/com/example/view/TimelineViewTest.java`
- Modify: `todo/src/test/java/com/example/view/ScheduleReflowAnimatorTest.java`
- Modify: `todo/src/test/java/com/example/view/ScheduleListViewSortTest.java`
- Modify: `todo/src/test/java/com/example/view/ScheduleDialogDefaultsTest.java`
- Modify: `todo/src/test/java/com/example/view/ScheduleCardStyleSupportTest.java`
- Modify: `todo/src/test/java/com/example/data/SqliteScheduleRepositoryTest.java`

- [ ] **Step 1: 批量替换测试文件中的 Schedule 引用**

对以上所有测试文件：
- `import com.example.model.Schedule` → `import com.example.model.ScheduleItem`
- 所有 `Schedule` 类型引用 → `ScheduleItem`
- `new Schedule()` → `new ScheduleItem()`

- [ ] **Step 2: 验证编译**

```bash
cd ./todo && mvn -f .\pom.xml clean compile
```
Expected: BUILD SUCCESS

- [ ] **Step 3: 运行测试**

```bash
cd ./todo && mvn -f .\pom.xml test
```
Expected: All tests pass（若有失败则逐一修复）

- [ ] **Step 4: 提交**

```bash
git add todo/src/test/
git commit -m "refactor: migrate all tests from Schedule to ScheduleItem

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```

### Task 4.2: 删除 Schedule.java

**Files:**
- Delete: `todo/src/main/java/com/example/model/Schedule.java`

- [ ] **Step 1: 确认无任何文件引用 Schedule**

```bash
cd ./todo && grep -r "import com.example.model.Schedule" src/ || echo "No references found"
```
Expected: "No references found"

- [ ] **Step 2: 删除 Schedule.java**

```bash
rm "todo/src/main/java/com/example/model/Schedule.java"
```

- [ ] **Step 3: 验证编译**

```bash
cd ./todo && mvn -f .\pom.xml clean compile
```
Expected: BUILD SUCCESS

- [ ] **Step 4: 运行完整测试**

```bash
cd ./todo && mvn -f .\pom.xml test
```
Expected: All tests pass

- [ ] **Step 5: 提交**

```bash
git add todo/src/main/java/com/example/model/Schedule.java
git commit -m "chore: delete Schedule.java

Schedule bridge model removed. All code now uses ScheduleItem directly.
ScheduleItem provides all the same fields (Schedule was a thin subclass).

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```

---

### Phase 5：最终清理与收尾

### Task 5.1: 清理 module-info.java 和 pom.xml

**Files:**
- Modify: `todo/src/main/java/module-info.java`
- Verify: `todo/pom.xml`

- [ ] **Step 1: 检查 module-info.java 是否需要精简**

检查 `module-info.java` 中是否还有对已删除 MySQL JDBC 驱动的 `requires`。当前只需要：
```java
module com.example {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.swing;
    requires java.sql;
    requires java.prefs;
    requires java.xml;
    requires java.desktop;
    requires org.xerial.sqlitejdbc;
    
    exports com.example;
    exports com.example.controller;
    exports com.example.model;
    exports com.example.view;
    exports com.example.application;

    opens com.example.controller to javafx.fxml;
}
```

若有任何多余的 `requires` 或 `exports`，精简之。当前看起来已经正确。

- [ ] **Step 2: 检查 pom.xml**

验证 pom.xml 中是否有 MySQL connector 依赖可删除：
```bash
cd ./todo && grep -i mysql pom.xml || echo "No MySQL dependency found"
```

若有 MySQL 依赖，删除对应 `<dependency>` 块。

- [ ] **Step 3: 验证编译和测试**

```bash
cd ./todo && mvn -f .\pom.xml clean compile && mvn -f .\pom.xml test
```
Expected: BUILD SUCCESS, all tests pass

- [ ] **Step 4: 提交**

```bash
git add todo/src/main/java/module-info.java todo/pom.xml
git commit -m "chore: final cleanup of module-info and pom.xml

Verify no MySQL driver references remain in build config.

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```

### Task 5.2: 数据库配置清理

**Files:**
- Modify: `todo/src/main/resources/application-defaults.properties`
- Modify: `todo/src/main/java/com/example/config/DatabaseProperties.java`

- [ ] **Step 1: 检查 DatabaseProperties 中是否有 MySQL 模式相关代码**

Read `DatabaseProperties.java`，删除 `isSqliteMode()` 方法及相关 MySQL 模式判断逻辑（如果存在）。SQLite 现在是唯一的数据库模式，不需要模式切换逻辑。

- [ ] **Step 2: 简化 application-defaults.properties**

删除 `todo.db.mode=sqlite` 配置项（不再需要模式切换）。保留 SQLite 相关配置。

- [ ] **Step 3: 验证编译**

```bash
cd ./todo && mvn -f .\pom.xml clean compile
```
Expected: BUILD SUCCESS

- [ ] **Step 4: 提交**

```bash
git add todo/src/main/java/com/example/config/DatabaseProperties.java todo/src/main/resources/application-defaults.properties
git commit -m "chore: remove database mode switching

SQLite is the only supported database. Remove mode configuration
and MySQL-related properties.

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```

### Task 5.3: 最终验证与 MainController 行数确认

- [ ] **Step 1: 统计 MainController 行数**

```bash
wc -l "todo/src/main/java/com/example/controller/MainController.java"
```

- [ ] **Step 2: 统计删除的文件数**

```bash
echo "Deleted files:" && git diff --stat HEAD~10..HEAD -- '*.java' '*.sql' | grep "=>" || git log --oneline -10
```

- [ ] **Step 3: 完整构建和测试**

```bash
cd ./todo && mvn -f .\pom.xml clean compile && mvn -f .\pom.xml test
```
Expected: BUILD SUCCESS, all tests pass

- [ ] **Step 4: 启动应用验证（可选，需要图形环境）**

```bash
cd ./todo && mvn -f .\pom.xml clean javafx:run
```

- [ ] **Step 5: 最终提交**

```bash
git add -A
git commit -m "chore: final verification and cleanup

All technical debt eliminated:
- 3 legacy SQL scripts deleted
- 2 MySQL compatibility classes deleted
- FlowchartView and login placeholders removed
- Schedule.java deleted (all code uses ScheduleItem directly)
- NavigationService cleaned up
- Database mode switching removed
- All tests passing

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```

---

## 清理成果预期

| 指标 | 清理前 | 清理后 |
|------|--------|--------|
| Schedule.java | 存在 (extends ScheduleItem) | 已删除 |
| toLegacySchedule/toLegacySchedules | 2 个桥接方法 | 已删除 |
| MySQL 兼容类 | JdbcConnectionFactory, MysqlStageBSchemaManager | 已删除 |
| 旧 SQL 脚本 | V001, V002, V003 | 已删除 |
| FlowchartView | 占位页 | 最小化 stub |
| 登录占位 | showLoginDialog() | 已删除 |
| NavigationService | 含 FLOWCHART 枚举 + Schedule 引用 | 清理完毕 |
| MainController 行数 | 4063 | ~3000+（去除死代码和桥接方法后） |

> **注：** MainController 仍保留大量 UI 构建代码（侧边栏、设置对话框、主题等），进一步瘦身需在下一轮将 Settings 等独立功能拆出。
