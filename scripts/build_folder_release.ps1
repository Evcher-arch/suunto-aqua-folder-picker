$ErrorActionPreference = 'Stop'
. "$PSScriptRoot\use_reverse_tools.ps1"
$project = [IO.Path]::GetFullPath("$PSScriptRoot\..")
$build = "$project\build"
$release = "$project\release\suunto-6.13.8-folders-beta1"
$sdk = 'C:\Users\ARCANA\AppData\Local\Android\Sdk'
$bt = "$sdk\build-tools\37.0.0"
$android = "$sdk\platforms\android-36\android.jar"
$python = 'C:\Users\ARCANA\.cache\codex-runtimes\codex-primary-runtime\dependencies\python\python.exe'
function Check { if ($LASTEXITCODE -ne 0) { throw "Command failed: $LASTEXITCODE" } }
New-Item -ItemType Directory -Force "$build\addon-classes","$build\addon",$release | Out-Null
& "$env:JAVA_HOME\bin\javac.exe" -cp C:\Users\ARCANA\Tools\apktool\apktool.jar -d "$build\addon-classes" "$PSScriptRoot\AssembleDex.java"
Check
& "$env:JAVA_HOME\bin\java.exe" -Xmx3g -cp "$build\addon-classes;C:\Users\ARCANA\Tools\apktool\apktool.jar" AssembleDex "$project\reverse\suunto\apktool\smali_classes3" "$build\classes3.dex"
Check
& "$env:JAVA_HOME\bin\javac.exe" --release 8 -encoding UTF-8 -cp $android -d "$build\addon-classes" "$project\modules\folder-picker\src\FolderSelection.java" "$project\modules\folder-picker\android\AquaFolders.java" "$project\modules\folder-picker\android\UsbFolderMap.java"
Check
& "$env:JAVA_HOME\bin\jar.exe" cf "$build\folder-picker.jar" -C "$build\addon-classes" local
Check
& "$bt\d8.bat" --min-api 32 --lib $android --output "$build\addon" "$build\folder-picker.jar"
Check
$keystore = "$build\suunto-folders.keystore"
if (!(Test-Path $keystore)) {
    & "$env:JAVA_HOME\bin\keytool.exe" -genkeypair -keystore $keystore -storepass android -keypass android -alias folders -keyalg RSA -keysize 2048 -validity 3650 -dname 'CN=Local Suunto Folders Development'
    Check
}
Get-ChildItem "$project\evidence\suunto-6.13.8" -Filter '*.apk' | ForEach-Object {
    $unsigned = "$build\$($_.Name).unsigned"
    if ($_.Name -eq 'com.stt.android.suunto.apk') {
        & $python "$PSScriptRoot\pack_apk.py" $_.FullName $unsigned --dex "classes3.dex=$build\classes3.dex" --dex "classes13.dex=$build\addon\classes.dex"
    } else { & $python "$PSScriptRoot\pack_apk.py" $_.FullName $unsigned }
    Check
    $aligned = "$build\$($_.Name).aligned"
    & "$bt\zipalign.exe" -f -P 16 4 $unsigned $aligned
    Check
    & "$bt\apksigner.bat" sign --ks $keystore --ks-key-alias folders --ks-pass pass:android --key-pass pass:android --out "$release\$($_.Name)" $aligned
    Check
    & "$bt\apksigner.bat" verify --verbose --print-certs "$release\$($_.Name)" | Set-Content "$release\$($_.Name).verify.txt"
    Check
}
Get-ChildItem $release -Filter '*.apk' | Get-FileHash -Algorithm SHA256 | ConvertTo-Json | Set-Content "$release\sha256.json"
Write-Host "Release: $release"
