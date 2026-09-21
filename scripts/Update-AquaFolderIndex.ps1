[CmdletBinding()]
param(
    [Parameter(Mandatory=$true)][ValidatePattern('^[a-zA-Z0-9_.:-]+$')][string]$Serial,
    [string]$UsbRoot = 'E:\',
    [switch]$AllowPartial
)
$ErrorActionPreference = 'Stop'
$project = [IO.Path]::GetFullPath("$PSScriptRoot\..")
$adb = 'C:\Users\ARCANA\AppData\Local\Android\Sdk\platform-tools\adb.exe'
$python = 'C:\Users\ARCANA\.cache\codex-runtimes\codex-primary-runtime\dependencies\python\python.exe'
$work = Join-Path "$project\build\indexes" (Get-Date -Format 'yyyyMMdd-HHmmss-fff')
$remote = '/sdcard/Android/data/com.stt.android.suunto/files'
if (!(Test-Path -LiteralPath (Join-Path $UsbRoot 'MUSIC') -PathType Container)) { throw 'Aqua MUSIC directory was not found.' }
New-Item -ItemType Directory -Force $work | Out-Null
& $adb -s $Serial pull "$remote/aqua-folder-catalog.json" "$work\catalog.json"
if ($LASTEXITCODE -ne 0) { throw 'Open Folders in Suunto while Aqua is connected by Bluetooth first.' }
& $python "$PSScriptRoot\index_aqua_folders.py" "$work\catalog.json" $UsbRoot "$work\aqua-folder-map.json"
if ($LASTEXITCODE -ne 0) { throw 'Folder indexing failed.' }
$index = Get-Content -LiteralPath "$work\aqua-folder-map.json" -Raw -Encoding UTF8 | ConvertFrom-Json
$matched = @($index.entries | Where-Object status -eq 'matched').Count
$total = @($index.entries).Count
if ($matched -eq 0 -or ($matched -ne $total -and !$AllowPartial)) {
    throw "Only $matched of $total songs matched uniquely. No phone index was replaced. Inspect $work."
}
if ($matched -ne $total) {
    Write-Warning "Only $matched of $total songs matched. Unresolved songs will remain without folders."
}
& $adb -s $Serial push "$work\aqua-folder-map.json" "$remote/aqua-folder-map.pending.json"
if ($LASTEXITCODE -ne 0) { throw 'Could not upload the new index.' }
& $adb -s $Serial shell mv "$remote/aqua-folder-map.pending.json" "$remote/aqua-folder-map.json"
if ($LASTEXITCODE -ne 0) { throw 'Could not activate the new index.' }
Write-Host "Indexed $matched songs. Reconnect Aqua by Bluetooth and reopen Folders. Snapshot: $work"
