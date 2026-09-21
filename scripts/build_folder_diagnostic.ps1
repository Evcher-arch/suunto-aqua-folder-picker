$ErrorActionPreference = 'Stop'
. "$PSScriptRoot\use_reverse_tools.ps1"
$project = [IO.Path]::GetFullPath("$PSScriptRoot\..")
$build = "$project\build\usb-update"
$bt = 'C:\Users\ARCANA\AppData\Local\Android\Sdk\build-tools\37.0.0'
$android = 'C:\Users\ARCANA\AppData\Local\Android\Sdk\platforms\android-36\android.jar'
function Check { if ($LASTEXITCODE -ne 0) { throw "Diagnostic build failed: $LASTEXITCODE" } }
New-Item -ItemType Directory -Force "$build\classes","$build\dex" | Out-Null
& "$env:JAVA_HOME\bin\javac.exe" -cp C:\Users\ARCANA\Tools\apktool\apktool.jar -d "$build\classes" "$PSScriptRoot\AssembleDex.java"
Check
& "$env:JAVA_HOME\bin\java.exe" -Xmx3g -cp "$build\classes;C:\Users\ARCANA\Tools\apktool\apktool.jar" AssembleDex "$project\reverse\suunto\apktool\smali_classes3" "$build\classes3.dex"
Check
& "$env:JAVA_HOME\bin\javac.exe" --release 8 -encoding UTF-8 -cp $android -d "$build\classes" "$project\modules\folder-picker\src\FolderSelection.java" "$project\modules\folder-picker\android\*.java"
Check
& "$env:JAVA_HOME\bin\jar.exe" cf "$build\addon.jar" -C "$build\classes" local
Check
& "$bt\d8.bat" --min-api 32 --lib $android --output "$build\dex" "$build\addon.jar"
Check
& 'C:\Users\ARCANA\.cache\codex-runtimes\codex-primary-runtime\dependencies\python\python.exe' "$PSScriptRoot\pack_apk.py" "$project\release\Suunto-6.13.8-Folders-Beta1-universal.apk" "$build\unsigned.apk" --dex "classes13.dex=$build\dex\classes.dex" --dex "classes3.dex=$build\classes3.dex"
Check
& "$bt\zipalign.exe" -f -P 16 4 "$build\unsigned.apk" "$build\aligned.apk"
Check
$result = "$project\release\Suunto-6.13.8-Folders-Beta5-usb-update.apk"
& "$bt\apksigner.bat" sign --ks "$project\build\suunto-folders.keystore" --ks-key-alias folders --ks-pass pass:android --key-pass pass:android --out $result "$build\aligned.apk"
Check
& "$bt\apksigner.bat" verify --verbose --print-certs $result | Set-Content "$build\signature.txt"
Check
Write-Host $result
