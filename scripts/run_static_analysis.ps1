#requires -Version 5

[CmdletBinding()]
param(
    [string]$ApkPath = "C:\Users\ARCANA\Downloads\uptodown-com.stt.android.suunto.apk",
    [string]$BuildTools = "C:\Users\ARCANA\AppData\Local\Android\Sdk\build-tools\37.0.0",
    [string]$OutDir = "suunto-modernization\evidence\raw"
)

$ErrorActionPreference = "Stop"

New-Item -ItemType Directory -Force -Path $OutDir | Out-Null

$aapt = Join-Path $BuildTools "aapt.exe"
$apksigner = Join-Path $BuildTools "apksigner.bat"

& $aapt dump badging $ApkPath |
    Set-Content -LiteralPath (Join-Path $OutDir "aapt-badging.txt") -Encoding UTF8
if ($LASTEXITCODE -ne 0) { throw 'aapt badging failed' }

& $aapt dump permissions $ApkPath |
    Set-Content -LiteralPath (Join-Path $OutDir "aapt-permissions.txt") -Encoding UTF8
if ($LASTEXITCODE -ne 0) { throw 'aapt permissions failed' }

& $aapt dump xmltree $ApkPath AndroidManifest.xml |
    Set-Content -LiteralPath (Join-Path $OutDir "aapt-manifest-xmltree.txt") -Encoding UTF8
if ($LASTEXITCODE -ne 0) { throw 'aapt manifest failed' }

& $apksigner verify --print-certs $ApkPath |
    Set-Content -LiteralPath (Join-Path $OutDir "apksigner-verify.txt") -Encoding UTF8
if ($LASTEXITCODE -ne 0) { throw 'APK signature verification failed' }

Write-Host "Static Android evidence written to $OutDir"
