#ifndef AppVersion
  #error AppVersion is not defined.
#endif

#ifndef AppSourceDir
  #error AppSourceDir is not defined.
#endif

#ifndef OutputDir
  #error OutputDir is not defined.
#endif

#ifndef AppIconFile
  #error AppIconFile is not defined.
#endif

#ifndef ChineseSimplifiedFile
  #error ChineseSimplifiedFile is not defined.
#endif

#ifndef ChineseTraditionalFile
  #error ChineseTraditionalFile is not defined.
#endif

[Setup]
AppId={{D2232C75-43A9-4C4D-B1A9-92E2A67B6631}
AppName=ToDo
AppVersion={#AppVersion}
AppVerName=ToDo v{#AppVersion}
AppPublisher=ToDo
DefaultDirName={autopf}\ToDo
DefaultGroupName=ToDo
DisableProgramGroupPage=yes
PrivilegesRequired=lowest
PrivilegesRequiredOverridesAllowed=dialog
ArchitecturesAllowed=x64compatible
ArchitecturesInstallIn64BitMode=x64compatible
Compression=lzma
SolidCompression=yes
WizardStyle=modern
ShowLanguageDialog=yes
SetupIconFile={#AppIconFile}
UninstallDisplayIcon={app}\ToDo.exe
OutputDir={#OutputDir}
OutputBaseFilename=ToDo-Setup-{#AppVersion}

[Languages]
Name: "english"; MessagesFile: "compiler:Default.isl"
Name: "chinesesimplified"; MessagesFile: "{#ChineseSimplifiedFile}"
Name: "chinesetraditional"; MessagesFile: "{#ChineseTraditionalFile}"

[Tasks]
Name: "desktopicon"; Description: "{cm:CreateDesktopIcon}"; GroupDescription: "{cm:AdditionalIcons}"

[Files]
Source: "{#AppSourceDir}\*"; DestDir: "{app}"; Flags: ignoreversion recursesubdirs createallsubdirs

[Icons]
Name: "{autoprograms}\ToDo"; Filename: "{app}\ToDo.exe"
Name: "{autodesktop}\ToDo"; Filename: "{app}\ToDo.exe"; Tasks: desktopicon

[Run]
Filename: "{app}\ToDo.exe"; Description: "{cm:LaunchProgram,ToDo}"; Flags: nowait postinstall skipifsilent

[Code]
function ResolveDefaultLanguageId: string;
begin
  if ActiveLanguage = 'chinesetraditional' then begin
    Result := 'zh-TW';
  end else if ActiveLanguage = 'chinesesimplified' then begin
    Result := 'zh-CN';
  end else begin
    Result := 'en';
  end;
end;

procedure WriteDefaultLanguageConfig;
var
  configDir: string;
  configPath: string;
  configContent: string;
begin
  configDir := ExpandConstant('{userappdata}\ToDo\config');
  if not ForceDirectories(configDir) then begin
    Log(Format('Failed to create config directory: %s', [configDir]));
    Exit;
  end;

  configPath := AddBackslash(configDir) + 'application.properties';
  configContent := 'todo.app.default-language=' + ResolveDefaultLanguageId() + #13#10;
  if not SaveStringToFile(configPath, configContent, False) then begin
    Log(Format('Failed to write default language config: %s', [configPath]));
  end;
end;

procedure CurStepChanged(CurStep: TSetupStep);
begin
  if CurStep = ssPostInstall then begin
    WriteDefaultLanguageConfig;
  end;
end;
