# ToDo 开发说明

本文档描述这次现代化升级后的真实基线，而不是旧版实现。

## 1. 技术底座

- Java 目标版本：`21`
- JavaFX 版本：`21.0.7`
- Maven Compiler Plugin：`3.15.0`
- Maven Surefire Plugin：`3.5.5`
- JavaFX Maven Plugin：`0.0.8`
- Maven Toolchains Plugin：`3.2.0`
- Maven Enforcer Plugin：`3.6.2`

关键文件：

- [pom.xml](/C:/Users/12493/Desktop/ToDoUI重制版/ToDo/todo/pom.xml)
- [module-info.java](/C:/Users/12493/Desktop/ToDoUI重制版/ToDo/todo/src/main/java/module-info.java)

## 2. 当前分层

当前项目仍然是单 Maven 模块，但职责已经开始拆开：

```text
config -> data -> application -> ui
```

对应目录：

- `com.example.config`
- `com.example.data`
- `com.example.application`
- `com.example.controller` / `com.example.view`

说明：

- 现在的 UI 还处在过渡期，`MainController` 负责 shell 编排
- 旧的复杂卡片、热力图、时间轴仍然保留为 Java 自定义视图
- 应用启动已经改为 `MainApp -> FXMLLoader -> MainController -> ApplicationContext`

## 3. 组合根

组合根在：
[ApplicationContext.java](/C:/Users/12493/Desktop/ToDoUI重制版/ToDo/todo/src/main/java/com/example/application/ApplicationContext.java)

它负责把下面这些对象接起来：

- `AppProperties`
- `DatabaseProperties`
- `JavaPreferencesStore`
- `JdbcConnectionFactory`
- `SchemaInitializer`
- `JdbcScheduleRepository`
- `ScheduleService`
- `NavigationService`
- `ThemeService`
- `MainViewModel`

## 4. 配置层

默认配置资源：
[application-defaults.properties](/C:/Users/12493/Desktop/ToDoUI重制版/ToDo/todo/src/main/resources/application-defaults.properties)

加载入口：
[ConfigurationLoader.java](/C:/Users/12493/Desktop/ToDoUI重制版/ToDo/todo/src/main/java/com/example/config/ConfigurationLoader.java)

覆盖顺序：

1. classpath 默认值
2. `application.properties`
3. `config/application.properties`
4. `TODO_CONFIG_FILE` 指向的外部 properties
5. 环境变量覆盖单项键值

主题和卡片样式偏好已经从 `MainController` 中抽走到：

- [ThemeService.java](/C:/Users/12493/Desktop/ToDoUI重制版/ToDo/todo/src/main/java/com/example/application/ThemeService.java)
- [JavaPreferencesStore.java](/C:/Users/12493/Desktop/ToDoUI重制版/ToDo/todo/src/main/java/com/example/config/JavaPreferencesStore.java)

## 5. 数据层

数据库连接不再在 UI 层里写死，入口改为：

- [Connectdatabase.java](/C:/Users/12493/Desktop/ToDoUI重制版/ToDo/todo/src/main/java/com/example/databaseutil/Connectdatabase.java)
- [JdbcConnectionFactory.java](/C:/Users/12493/Desktop/ToDoUI重制版/ToDo/todo/src/main/java/com/example/data/JdbcConnectionFactory.java)
- [SchemaInitializer.java](/C:/Users/12493/Desktop/ToDoUI重制版/ToDo/todo/src/main/java/com/example/data/SchemaInitializer.java)

仓库接口：

- [ScheduleRepository.java](/C:/Users/12493/Desktop/ToDoUI重制版/ToDo/todo/src/main/java/com/example/data/ScheduleRepository.java)
- [JdbcScheduleRepository.java](/C:/Users/12493/Desktop/ToDoUI重制版/ToDo/todo/src/main/java/com/example/data/JdbcScheduleRepository.java)

说明：

- `ScheduleDAO` 还保留着，当前作为 JDBC 兼容实现被 repository 包一层
- UI 层已经不再直接 `new ScheduleDAO()`

## 6. UI 层现状

FXML shell：
[main-shell.fxml](/C:/Users/12493/Desktop/ToDoUI重制版/ToDo/todo/src/main/resources/com/example/ui/main-shell.fxml)

控制器：
[MainController.java](/C:/Users/12493/Desktop/ToDoUI重制版/ToDo/todo/src/main/java/com/example/controller/MainController.java)

主视图仍然是这些 Java 组件：

- [ScheduleListView.java](/C:/Users/12493/Desktop/ToDoUI重制版/ToDo/todo/src/main/java/com/example/view/ScheduleListView.java)
- [TimelineView.java](/C:/Users/12493/Desktop/ToDoUI重制版/ToDo/todo/src/main/java/com/example/view/TimelineView.java)
- [HeatmapView.java](/C:/Users/12493/Desktop/ToDoUI重制版/ToDo/todo/src/main/java/com/example/view/HeatmapView.java)
- [InfoPanelView.java](/C:/Users/12493/Desktop/ToDoUI重制版/ToDo/todo/src/main/java/com/example/view/InfoPanelView.java)
- [ScheduleDialog.java](/C:/Users/12493/Desktop/ToDoUI重制版/ToDo/todo/src/main/java/com/example/view/ScheduleDialog.java)

这次改造后的关键变化：

- `MainApp` 不再直接 new 一个完全自建根节点的 controller
- `MainController` 现在从 `ApplicationContext` 取服务
- 列表/时间轴/热力图/详情面板都不再直接持有 DAO

## 7. 测试体系

测试仍然保留在 `src/test/java`，并继续沿用“同包白盒”方式。

当前 Surefire 现实约束：

- `useModulePath=true`
- `forkCount=0`

原因：

- 在当前 Windows 中文路径工作区里，fork JVM 时 Surefire 生成的 `@argfile` 会把模块路径写成 UTF-8
- Java 启动器对该文件的读取在这个环境下出现中文路径乱码，直接导致 `Unknown module: com.example specified to --patch-module`
- 改为 in-process 后，模块化测试可以稳定跑通

如果后续把工作区迁到纯 ASCII 路径，再重新评估是否恢复 fork 模式。

## 8. 工程卫生

已经完成：

- 根目录新增 `.gitignore`
- `todo/target/` 从 Git 索引移除
- 编译产物不再提交
- 删除遗留无效文件：
  [RefactorScript.java](/C:/Users/12493/Desktop/ToDoUI重制版/ToDo/todo/RefactorScript.java)
  [primary.fxml](/C:/Users/12493/Desktop/ToDoUI重制版/ToDo/todo/src/main/resources/com/example/primary.fxml)
  [secondary.fxml](/C:/Users/12493/Desktop/ToDoUI重制版/ToDo/todo/src/main/resources/com/example/secondary.fxml)

建议维护动作：

```powershell
cd todo
mvn clean compile
mvn test
mvn javafx:run
```

如果当前终端还停在旧 JDK，会先切到 21：

```powershell
$env:JAVA_HOME='C:\Program Files\Microsoft\jdk-21.0.10.7-hotspot'
$env:JAVA21_HOME=$env:JAVA_HOME
$env:PATH="$env:JAVA_HOME\bin;$env:PATH"
```
