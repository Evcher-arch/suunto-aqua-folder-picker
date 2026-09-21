$ErrorActionPreference = 'Stop'
. "$PSScriptRoot\use_reverse_tools.ps1"
$project = [IO.Path]::GetFullPath("$PSScriptRoot\..")
$build = "$project\build\smoke"
$module = "$project\modules\folder-picker"
$bt = 'C:\Users\ARCANA\AppData\Local\Android\Sdk\build-tools\37.0.0'
$android = 'C:\Users\ARCANA\AppData\Local\Android\Sdk\platforms\android-36\android.jar'
function Check { if ($LASTEXITCODE -ne 0) { throw "Smoke build failed: $LASTEXITCODE" } }
New-Item -ItemType Directory -Force "$build\classes","$build\dex" | Out-Null
& "$env:JAVA_HOME\bin\javac.exe" --release 8 -encoding UTF-8 -cp $android -d "$build\classes" "$module\src\FolderSelection.java" "$module\android\*.java" "$module\smoke\*.java"
Check
& "$env:JAVA_HOME\bin\jar.exe" cf "$build\smoke.jar" -C "$build\classes" .
Check
& "$bt\d8.bat" --min-api 32 --lib $android --output "$build\dex" "$build\smoke.jar"
Check
& "$bt\aapt.exe" package -f -M "$module\smoke\AndroidManifest.xml" -I $android -F "$build\resources.apk"
Check
& 'C:\Users\ARCANA\.cache\codex-runtimes\codex-primary-runtime\dependencies\python\python.exe' "$PSScriptRoot\pack_apk.py" "$build\resources.apk" "$build\unsigned.apk" --dex "classes.dex=$build\dex\classes.dex"
Check
& "$bt\zipalign.exe" -f 4 "$build\unsigned.apk" "$build\aligned.apk"
Check
& "$bt\apksigner.bat" sign --ks "$project\build\suunto-folders.keystore" --ks-key-alias folders --ks-pass pass:android --key-pass pass:android --out "$build\smoke.apk" "$build\aligned.apk"
Check
