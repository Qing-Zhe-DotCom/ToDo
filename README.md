# ToDo

当前仓库的实际工程根目录在 [todo/pom.xml](/C:/Users/12493/Desktop/ToDoUI重制版/ToDo/todo/pom.xml)。  
这次升级后，项目已经切到 `Java 21 + JavaFX 21.0.7 + Maven 新插件链路`，并完成了配置外置、测试修复、`target` 去跟踪和一版分层收口。

## 当前结构

- 启动入口仍然是 `com.example.MainApp`
- 模块名仍然是 `com.example`
- UI 入口改为 `FXML shell`：
  [main-shell.fxml](/C:/Users/12493/Desktop/ToDoUI重制版/ToDo/todo/src/main/resources/com/example/ui/main-shell.fxml)
- 运行时组合根在：
  [ApplicationContext.java](/C:/Users/12493/Desktop/ToDoUI重制版/ToDo/todo/src/main/java/com/example/application/ApplicationContext.java)
- 配置层：
  [ConfigurationLoader.java](/C:/Users/12493/Desktop/ToDoUI重制版/ToDo/todo/src/main/java/com/example/config/ConfigurationLoader.java)
- 数据层：
  [JdbcScheduleRepository.java](/C:/Users/12493/Desktop/ToDoUI重制版/ToDo/todo/src/main/java/com/example/data/JdbcScheduleRepository.java)
- 应用层：
  [ScheduleService.java](/C:/Users/12493/Desktop/ToDoUI重制版/ToDo/todo/src/main/java/com/example/application/ScheduleService.java)
  [ThemeService.java](/C:/Users/12493/Desktop/ToDoUI重制版/ToDo/todo/src/main/java/com/example/application/ThemeService.java)

## JDK 21

本机已经安装 `Microsoft OpenJDK 21`，并把用户级 `JAVA_HOME`/`JAVA21_HOME` 指到了：

`C:\Program Files\Microsoft\jdk-21.0.10.7-hotspot`

如果你当前终端还是老环境，重新开一个 PowerShell；或者临时执行：

```powershell
$env:JAVA_HOME='C:\Program Files\Microsoft\jdk-21.0.10.7-hotspot'
$env:JAVA21_HOME=$env:JAVA_HOME
$env:PATH="$env:JAVA_HOME\bin;$env:PATH"
```

## 配置

默认配置在：
[application-defaults.properties](/C:/Users/12493/Desktop/ToDoUI重制版/ToDo/todo/src/main/resources/application-defaults.properties)

支持两种覆盖方式：

1. 在工程根或 `config/` 下放 `application.properties`
2. 使用环境变量覆盖

支持的数据库变量：

- `TODO_DB_DRIVER`
- `TODO_DB_URL`
- `TODO_DB_USER`
- `TODO_DB_PASSWORD`
- `TODO_CONFIG_FILE`

## 构建与运行

在仓库根进入 Maven 模块：

```powershell
cd todo
```

编译：

```powershell
mvn clean compile
```

测试：

```powershell
mvn test
```

运行桌面应用：

```powershell
mvn javafx:run
```

## 测试说明

`mvn test` 现在已经恢复可运行。  
为了兼容当前 Windows 中文路径工作区，Surefire 被配置成 `useModulePath=true + forkCount=0`，避免 fork JVM 时 `@argfile` 对中文路径做错误编码，仍然保持模块化测试执行。

## 工程卫生

- 根目录已经新增 [.gitignore](/C:/Users/12493/Desktop/ToDoUI重制版/ToDo/.gitignore)
- `todo/target/` 不再跟踪
- 旧遗留文件已删除：
  [RefactorScript.java](/C:/Users/12493/Desktop/ToDoUI重制版/ToDo/todo/RefactorScript.java)
  [primary.fxml](/C:/Users/12493/Desktop/ToDoUI重制版/ToDo/todo/src/main/resources/com/example/primary.fxml)
  [secondary.fxml](/C:/Users/12493/Desktop/ToDoUI重制版/ToDo/todo/src/main/resources/com/example/secondary.fxml)

## 仍在进行中的方向

- `MainController` 已经从直接持有 DAO/Preferences 收口到应用服务，但仍然是过渡期 shell controller
- 现有复杂自定义视图还保留在 `com.example.view`，后续可以继续往更细的 `ui/controller/viewmodel` 迁移
- 数据访问已经进入 `config -> data -> application -> ui` 方向，但 `ScheduleDAO` 仍作为底层 JDBC 兼容实现存在
