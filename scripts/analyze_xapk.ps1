[CmdletBinding()]
param(
    [string]$XapkPath = 'C:\Users\ARCANA\Downloads\suunto-6-13-8.xapk',
    [string]$BuildTools = 'C:\Users\ARCANA\AppData\Local\Android\Sdk\build-tools\37.0.0',
    [string]$OutDir = "$PSScriptRoot\..\evidence\suunto-6.13.8"
)
$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.IO.Compression.FileSystem
$root = [IO.Path]::GetFullPath($OutDir)
New-Item -ItemType Directory -Force -Path $root | Out-Null
$archive = [IO.Compression.ZipFile]::OpenRead($XapkPath)
try {
    $entries = @($archive.Entries | Where-Object { $_.FullName.EndsWith('.apk') })
    foreach ($entry in $entries) {
        if ($entry.FullName -notmatch '^[a-zA-Z0-9_.-]+\.apk$') { throw 'Unsafe APK entry name' }
        $target = Join-Path $root $entry.FullName
        if (Test-Path -LiteralPath $target) { throw "Output exists: $target. Choose a fresh OutDir." }
    }
    $records = foreach ($entry in $entries) {
        $target = Join-Path $root $entry.FullName
        [IO.Compression.ZipFileExtensions]::ExtractToFile($entry, $target, $false)
        $raw = Join-Path $root ($entry.FullName + '.analysis')
        & "$PSScriptRoot\run_static_analysis.ps1" -ApkPath $target -BuildTools $BuildTools -OutDir $raw
        [pscustomobject]@{ name = $entry.FullName; size = $entry.Length; sha256 = (Get-FileHash -LiteralPath $target -Algorithm SHA256).Hash }
    }
    [pscustomobject]@{
        source = $XapkPath
        sha256 = (Get-FileHash -LiteralPath $XapkPath -Algorithm SHA256).Hash
        apks = @($records)
    } | ConvertTo-Json -Depth 5 | Set-Content -LiteralPath (Join-Path $root 'xapk-summary.json') -Encoding UTF8
} finally { $archive.Dispose() }
