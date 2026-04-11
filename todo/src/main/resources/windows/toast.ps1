param(
    [Parameter(Mandatory = $true)]
    [string]$InputPath,

    [Parameter(Mandatory = $true)]
    [string]$AppId,

    [Parameter(Mandatory = $true)]
    [string]$AppName,

    [string]$ExePath = "",

    [string]$ShortcutName = "ToDo"
)

$ErrorActionPreference = "Stop"

function Ensure-AppUserModelIdRegistry {
    param(
        [Parameter(Mandatory = $true)][string]$AppId,
        [Parameter(Mandatory = $true)][string]$AppName,
        [string]$ExePath
    )

    $regPath = "HKCU:\Software\Classes\AppUserModelId\$AppId"
    New-Item -Path $regPath -Force | Out-Null
    New-ItemProperty -Path $regPath -Name "DisplayName" -Value $AppName -PropertyType String -Force | Out-Null
    if ($ExePath -and (Test-Path $ExePath)) {
        New-ItemProperty -Path $regPath -Name "IconUri" -Value $ExePath -PropertyType String -Force | Out-Null
    }
}

function Ensure-ShortcutWithAumid {
    param(
        [Parameter(Mandatory = $true)][string]$ShortcutPath,
        [Parameter(Mandatory = $true)][string]$TargetPath,
        [Parameter(Mandatory = $true)][string]$AppId
    )

    $shortcutDir = Split-Path $ShortcutPath -Parent
    if (-not (Test-Path $shortcutDir)) {
        New-Item -ItemType Directory -Path $shortcutDir -Force | Out-Null
    }

    if (-not (Test-Path $ShortcutPath)) {
        $wsh = New-Object -ComObject WScript.Shell
        $shortcut = $wsh.CreateShortcut($ShortcutPath)
        $shortcut.TargetPath = $TargetPath
        $shortcut.WorkingDirectory = Split-Path $TargetPath -Parent
        $shortcut.IconLocation = "$TargetPath,0"
        $shortcut.Save()
    }

    $typeName = "ToDoToast.ShortcutHelper"
    if (-not ($typeName -as [type])) {
        $source = @"
using System;
using System.Runtime.InteropServices;
using System.Runtime.InteropServices.ComTypes;

namespace ToDoToast {
    [ComImport]
    [Guid("00021401-0000-0000-C000-000000000046")]
    internal class ShellLink { }

    [ComImport]
    [Guid("886D8EEB-8CF2-4446-8D02-CDBA1DBDCF99")]
    [InterfaceType(ComInterfaceType.InterfaceIsIUnknown)]
    internal interface IPropertyStore {
        uint GetCount(out uint cProps);
        uint GetAt(uint iProp, out PROPERTYKEY pkey);
        uint GetValue(ref PROPERTYKEY key, out PROPVARIANT pv);
        uint SetValue(ref PROPERTYKEY key, ref PROPVARIANT pv);
        uint Commit();
    }

    [StructLayout(LayoutKind.Sequential, Pack = 4)]
    internal struct PROPERTYKEY {
        public Guid fmtid;
        public uint pid;
    }

    [StructLayout(LayoutKind.Sequential)]
    internal struct PROPVARIANT {
        public ushort vt;
        public ushort wReserved1;
        public ushort wReserved2;
        public ushort wReserved3;
        public IntPtr p;
        public int p2;
    }

    internal static class NativeMethods {
        [DllImport("Ole32.dll", PreserveSig = false)]
        internal static extern void PropVariantClear(ref PROPVARIANT pvar);
    }

    public static class ShortcutHelper {
        private static readonly PROPERTYKEY PKEY_AppUserModel_ID = new PROPERTYKEY {
            fmtid = new Guid("9F4C2855-9F79-4B39-A8D0-E1D42DE1D5F3"),
            pid = 5
        };

        public static void SetAppUserModelId(string shortcutPath, string appId) {
            if (string.IsNullOrWhiteSpace(shortcutPath)) throw new ArgumentNullException(nameof(shortcutPath));
            if (string.IsNullOrWhiteSpace(appId)) throw new ArgumentNullException(nameof(appId));

            object link = new ShellLink();
            var persist = (IPersistFile)link;
            persist.Load(shortcutPath, 0x00000002); // STGM_READWRITE

            var store = (IPropertyStore)link;
            var pv = new PROPVARIANT();
            pv.vt = (ushort)VarEnum.VT_LPWSTR;
            pv.p = Marshal.StringToCoTaskMemUni(appId);
            store.SetValue(ref PKEY_AppUserModel_ID, ref pv);
            store.Commit();
            persist.Save(shortcutPath, true);
            NativeMethods.PropVariantClear(ref pv);
        }
    }
}
"@

        Add-Type -TypeDefinition $source -Language CSharp -ErrorAction Stop | Out-Null
    }

    [ToDoToast.ShortcutHelper]::SetAppUserModelId($ShortcutPath, $AppId)
}

function Ensure-ToastRegistration {
    param(
        [Parameter(Mandatory = $true)][string]$AppId,
        [Parameter(Mandatory = $true)][string]$AppName,
        [string]$ExePath,
        [Parameter(Mandatory = $true)][string]$ShortcutName
    )

    Ensure-AppUserModelIdRegistry -AppId $AppId -AppName $AppName -ExePath $ExePath

    $startMenuPrograms = Join-Path ([Environment]::GetFolderPath("StartMenu")) "Programs"
    $shortcutPath = Join-Path $startMenuPrograms ("$ShortcutName.lnk")
    if ($ExePath -and (Test-Path $ExePath)) {
        Ensure-ShortcutWithAumid -ShortcutPath $shortcutPath -TargetPath $ExePath -AppId $AppId
    } elseif (Test-Path $shortcutPath) {
        Ensure-ShortcutWithAumid -ShortcutPath $shortcutPath -TargetPath "" -AppId $AppId
    }
}

function Read-PlannedToasts {
    param([Parameter(Mandatory = $true)][string]$InputPath)

    if (-not (Test-Path $InputPath)) {
        throw "Input file not found: $InputPath"
    }

    $raw = Get-Content -Path $InputPath -Raw
    if (-not $raw) {
        return @()
    }

    $parsed = $raw | ConvertFrom-Json
    if ($null -eq $parsed) {
        return @()
    }

    if ($parsed -is [System.Collections.IEnumerable]) {
        return @($parsed)
    }

    return @($parsed)
}

function Sync-ScheduledToasts {
    param(
        [Parameter(Mandatory = $true)][string]$AppId,
        [Parameter(Mandatory = $true)][object[]]$PlannedToasts
    )

    [Windows.UI.Notifications.ToastNotificationManager, Windows.UI.Notifications, ContentType = WindowsRuntime] | Out-Null

    $notifier = [Windows.UI.Notifications.ToastNotificationManager]::CreateToastNotifier($AppId)

    $expected = @{}
    foreach ($toast in $PlannedToasts) {
        if ($toast -and $toast.id -and $toast.dueEpochMillis) {
            $expected[$toast.id] = $toast
        }
    }

    $existing = $notifier.GetScheduledToastNotifications()
    foreach ($scheduled in $existing) {
        $id = $scheduled.Id
        if (-not $expected.ContainsKey($id)) {
            $notifier.RemoveFromSchedule($scheduled)
            continue
        }

        $want = $expected[$id]
        $wantTime = [DateTimeOffset]::FromUnixTimeMilliseconds([long]$want.dueEpochMillis)
        $currentTime = [DateTimeOffset]$scheduled.DeliveryTime
        if ($currentTime.ToUnixTimeMilliseconds() -ne $wantTime.ToUnixTimeMilliseconds()) {
            $notifier.RemoveFromSchedule($scheduled)
        }
    }

    $after = $notifier.GetScheduledToastNotifications()
    $existingIds = New-Object 'System.Collections.Generic.HashSet[string]'
    foreach ($scheduled in $after) {
        if ($scheduled -and $scheduled.Id) {
            $existingIds.Add($scheduled.Id) | Out-Null
        }
    }

    foreach ($toast in $PlannedToasts) {
        if (-not $toast -or -not $toast.id -or -not $toast.dueEpochMillis) {
            continue
        }
        if ($existingIds.Contains([string]$toast.id)) {
            continue
        }

        $deliveryTime = [DateTimeOffset]::FromUnixTimeMilliseconds([long]$toast.dueEpochMillis)
        if ($deliveryTime -lt [DateTimeOffset]::Now) {
            continue
        }

        $template = [Windows.UI.Notifications.ToastTemplateType]::ToastText02
        $xml = [Windows.UI.Notifications.ToastNotificationManager]::GetTemplateContent($template)
        $textNodes = $xml.GetElementsByTagName("text")
        $null = $textNodes.Item(0).AppendChild($xml.CreateTextNode([string]$toast.title))
        $null = $textNodes.Item(1).AppendChild($xml.CreateTextNode([string]$toast.body))

        $scheduledToast = [Windows.UI.Notifications.ScheduledToastNotification]::new($xml, $deliveryTime)
        $scheduledToast.Id = [string]$toast.id
        $notifier.AddToSchedule($scheduledToast)
    }
}

Ensure-ToastRegistration -AppId $AppId -AppName $AppName -ExePath $ExePath -ShortcutName $ShortcutName
$planned = Read-PlannedToasts -InputPath $InputPath
Sync-ScheduledToasts -AppId $AppId -PlannedToasts $planned
