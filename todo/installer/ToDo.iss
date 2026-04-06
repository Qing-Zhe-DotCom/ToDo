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
#ifdef ChineseSimplifiedFile
ShowLanguageDialog=auto
#else
ShowLanguageDialog=no
#endif
SetupIconFile={#AppIconFile}
UninstallDisplayIcon={app}\ToDo.exe
OutputDir={#OutputDir}
OutputBaseFilename=ToDo-Setup-{#AppVersion}

[Languages]
Name: "english"; MessagesFile: "compiler:Default.isl"
#ifdef ChineseSimplifiedFile
Name: "chinesesimplified"; MessagesFile: "{#ChineseSimplifiedFile}"
#endif

[Tasks]
Name: "desktopicon"; Description: "{cm:CreateDesktopIcon}"; GroupDescription: "{cm:AdditionalIcons}"

[Files]
Source: "{#AppSourceDir}\*"; DestDir: "{app}"; Flags: ignoreversion recursesubdirs createallsubdirs

[Icons]
Name: "{autoprograms}\ToDo"; Filename: "{app}\ToDo.exe"
Name: "{autodesktop}\ToDo"; Filename: "{app}\ToDo.exe"; Tasks: desktopicon

[Run]
Filename: "{app}\ToDo.exe"; Description: "{cm:LaunchProgram,ToDo}"; Flags: nowait postinstall skipifsilent
