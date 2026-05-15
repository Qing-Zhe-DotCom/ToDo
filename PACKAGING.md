# 打包 / 发布（Windows 安装包）

本仓库的 Windows 安装包由两步生成：

1. `jpackage` 生成 `app-image`
2. Inno Setup 6（`ISCC.exe` / `iscc`）编译安装包，输出 `ToDo-Setup-<version>.exe`

其中 `<version>` 来自 `todo/pom.xml` 的 `<version>`。

## 前提

- Windows 10/11
- JDK 21（需要 `jpackage`）
- Maven（命令行可用 `mvn`）
- Inno Setup 6（命令行可用 `iscc` 或能找到 `ISCC.exe`）

> 说明：仓库内真正的打包逻辑在 `todo/build-installer.ps1`，Python 脚本只是“一键入口”，方便双击/CI 调用。

## 一键打包（推荐）

在仓库根目录执行：

```powershell
python .\build_installer.py
```

默认输出到 `.\dist\`。你也可以指定输出目录：

```powershell
python .\build_installer.py --output-dir .\release\
```

## 直接用 PowerShell 脚本（等价）

```powershell
powershell -ExecutionPolicy Bypass -File .\todo\build-installer.ps1 -OutputDir .\dist
```

## 常见问题

### 1) 找不到 `jpackage`

确认当前终端使用的是 JDK 21，并且 `jpackage` 在 PATH 中：

```powershell
java -version
jpackage --version
```

如果你安装的是 Microsoft OpenJDK 21，通常 `jpackage` 在 `JAVA_HOME\bin` 下。

### 2) 找不到 `iscc` / `ISCC.exe`

需要安装 Inno Setup 6。安装完成后可检查：

```powershell
iscc /?
```

脚本 `todo/build-installer.ps1` 内置了常见路径的 fallback（例如 `%LOCALAPPDATA%` 和 `Program Files`）。

### 3) `mvn` 不存在

安装 Maven 并确保命令行可直接运行：

```powershell
mvn -version
```

