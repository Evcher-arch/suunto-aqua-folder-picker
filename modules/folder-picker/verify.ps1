$ErrorActionPreference = 'Stop'
$jdk = 'C:\Program Files\Android\Android Studio\jbr\bin'
$output = Join-Path $PSScriptRoot 'build'
New-Item -ItemType Directory -Force -Path $output | Out-Null
& "$jdk\javac.exe" --release 8 -d $output "$PSScriptRoot\src\FolderSelection.java" "$PSScriptRoot\test\FolderSelectionTest.java"
if ($LASTEXITCODE -ne 0) { throw 'Compilation failed' }
& "$jdk\java.exe" -cp $output local.suunto.music.FolderSelectionTest
if ($LASTEXITCODE -ne 0) { throw 'Tests failed' }
