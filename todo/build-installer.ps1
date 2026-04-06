param(
    [string]$OutputDir = (Split-Path $PSScriptRoot -Parent)
)

$ErrorActionPreference = "Stop"

function Get-RequiredCommandPath {
    param(
        [Parameter(Mandatory = $true)]
        [string]$CommandName,
        [string[]]$FallbackPaths = @()
    )

    $command = Get-Command $CommandName -ErrorAction SilentlyContinue
    if ($command) {
        return $command.Source
    }

    foreach ($fallback in $FallbackPaths) {
        if (Test-Path $fallback) {
            return $fallback
        }
    }

    throw "Could not find required command: $CommandName"
}

function Invoke-ExternalCommand {
    param(
        [Parameter(Mandatory = $true)]
        [string]$FilePath,
        [string[]]$Arguments = @()
    )

    & $FilePath @Arguments
    if ($LASTEXITCODE -ne 0) {
        throw ("Command failed with exit code {0}: {1}" -f $LASTEXITCODE, $FilePath)
    }
}

function Stop-ProjectProcesses {
    param(
        [Parameter(Mandatory = $true)]
        [string]$ProjectPath
    )

    $escapedProjectPath = [Regex]::Escape($ProjectPath)
    $candidates = Get-CimInstance Win32_Process |
        Where-Object {
            $_.Name -in @("java.exe", "javaw.exe", "ToDo.exe") -and
            (
                ($_.CommandLine -and $_.CommandLine -match $escapedProjectPath) -or
                ($_.ExecutablePath -and $_.ExecutablePath -match $escapedProjectPath)
            )
        }

    foreach ($candidate in $candidates) {
        try {
            Stop-Process -Id $candidate.ProcessId -Force -ErrorAction Stop
        } catch {
        }
    }
}

$projectDir = (Resolve-Path $PSScriptRoot).Path
$repoRoot = (Split-Path $projectDir -Parent)
$resolvedOutputDir = $OutputDir
if (-not [System.IO.Path]::IsPathRooted($resolvedOutputDir)) {
    $resolvedOutputDir = Join-Path $repoRoot $resolvedOutputDir
}
$resolvedOutputDir = [System.IO.Path]::GetFullPath($resolvedOutputDir)

[xml]$pom = Get-Content (Join-Path $projectDir "pom.xml")
$version = $pom.project.version
if ([string]::IsNullOrWhiteSpace($version)) {
    throw "Could not read project version from pom.xml"
}

$artifactName = "todo-$version.jar"
$iconPath = Join-Path $projectDir "src\main\resources\icons\todo.ico"
$issPath = Join-Path $projectDir "installer\ToDo.iss"
$targetDir = Join-Path $projectDir "target"
$legacyWorkDir = Join-Path $targetDir "installer-work"
$workDir = Join-Path $projectDir ".installer-work"
$inputDir = Join-Path $workDir "input"
$appImageDest = Join-Path $workDir "app-image"
$appImageDir = Join-Path $appImageDest "ToDo"
$chineseSimplifiedFile = Join-Path $env:LOCALAPPDATA "Programs\Inno Setup 6\Languages\ChineseSimplified.isl"

if (-not (Test-Path $iconPath)) {
    throw "Icon file not found: $iconPath"
}

if (-not (Test-Path $issPath)) {
    throw "Inno Setup script not found: $issPath"
}

$jpackage = Get-RequiredCommandPath -CommandName "jpackage" -FallbackPaths @(
    "C:\Program Files\Microsoft\jdk-21.0.10.7-hotspot\bin\jpackage.exe",
    "C:\Program Files\Java\jdk-21\bin\jpackage.exe"
)

$iscc = Get-RequiredCommandPath -CommandName "iscc" -FallbackPaths @(
    (Join-Path $env:LOCALAPPDATA "Programs\Inno Setup 6\ISCC.exe"),
    (Join-Path ${env:ProgramFiles(x86)} "Inno Setup 6\ISCC.exe"),
    (Join-Path $env:ProgramFiles "Inno Setup 6\ISCC.exe"),
    "D:\Users\12493\AppData\Local\Programs\Inno Setup 6\ISCC.exe"
)

New-Item -ItemType Directory -Force -Path $resolvedOutputDir | Out-Null

Write-Host "==> Stopping project processes that may lock target files"
Stop-ProjectProcesses -ProjectPath $projectDir

if (Test-Path $legacyWorkDir) {
    Write-Host "==> Removing legacy target\\installer-work"
    attrib -R (Join-Path $legacyWorkDir "*") /S /D | Out-Null
    Remove-Item -Recurse -Force $legacyWorkDir -ErrorAction SilentlyContinue
}

Write-Host "==> Building Maven package"
Invoke-ExternalCommand -FilePath "mvn" -Arguments @(
    "clean",
    "package",
    "dependency:copy-dependencies",
    "-DincludeScope=runtime"
)

$jarPath = Join-Path $targetDir $artifactName
if (-not (Test-Path $jarPath)) {
    throw "Application jar not found: $jarPath"
}

Write-Host "==> Preparing jpackage input"
Remove-Item -Recurse -Force $workDir -ErrorAction SilentlyContinue
New-Item -ItemType Directory -Force -Path $inputDir | Out-Null
Copy-Item $jarPath -Destination (Join-Path $inputDir $artifactName)
Copy-Item (Join-Path $targetDir "dependency\*.jar") -Destination $inputDir

Write-Host "==> Building app image with jpackage"
Invoke-ExternalCommand -FilePath $jpackage -Arguments @(
    "--type",
    "app-image",
    "--dest",
    $appImageDest,
    "--name",
    "ToDo",
    "--app-version",
    $version,
    "--vendor",
    "ToDo",
    "--icon",
    $iconPath,
    "--input",
    $inputDir,
    "--main-jar",
    $artifactName,
    "--main-class",
    "com.example.PackagingLauncher",
    "--add-modules",
    "java.se,jdk.unsupported,jdk.crypto.ec"
)

if (-not (Test-Path (Join-Path $appImageDir "ToDo.exe"))) {
    throw "jpackage did not produce ToDo.exe"
}

Write-Host "==> Compiling Inno Setup installer"
$isccArguments = @(
    "/DAppVersion=$version",
    "/DAppSourceDir=$appImageDir",
    "/DOutputDir=$resolvedOutputDir",
    "/DAppIconFile=$iconPath"
)

if (Test-Path $chineseSimplifiedFile) {
    $isccArguments += "/DChineseSimplifiedFile=$chineseSimplifiedFile"
}

$isccArguments += $issPath
Invoke-ExternalCommand -FilePath $iscc -Arguments $isccArguments

$installerPath = Join-Path $resolvedOutputDir "ToDo-Setup-$version.exe"
if (-not (Test-Path $installerPath)) {
    throw "Installer was not created: $installerPath"
}

Write-Host ""
Write-Host "Installer created successfully:"
Write-Host $installerPath
