$ErrorActionPreference = 'Stop'
. "$PSScriptRoot\use_reverse_tools.ps1"
$project = [IO.Path]::GetFullPath("$PSScriptRoot\..")
$bt = 'C:\Users\ARCANA\AppData\Local\Android\Sdk\build-tools\37.0.0'
$result = "$project\release\Suunto-6.13.8-Folders-Beta1-universal.apk"
function Check { if ($LASTEXITCODE -ne 0) { throw "Universal build failed: $LASTEXITCODE" } }
& "$env:JAVA_HOME\bin\java.exe" -Xmx3g -jar "$project\build\APKEditor-1.4.9.jar" m -i "$project\release\suunto-6.13.8-folders-beta1.xapk" -o "$project\build\universal-unsigned.apk" -validate-modules -f
Check
& 'C:\Users\ARCANA\.cache\codex-runtimes\codex-primary-runtime\dependencies\python\python.exe' "$PSScriptRoot\pack_apk.py" "$project\build\universal-unsigned.apk" "$project\build\universal-normalized.apk"
Check
& "$bt\zipalign.exe" -f -P 16 4 "$project\build\universal-normalized.apk" "$project\build\universal-aligned.apk"
Check
& "$bt\apksigner.bat" sign --ks "$project\build\suunto-folders.keystore" --ks-key-alias folders --ks-pass pass:android --key-pass pass:android --out $result "$project\build\universal-aligned.apk"
Check
& "$bt\apksigner.bat" verify --verbose --print-certs $result | Set-Content "$project\release\universal-signature.txt"
Check
& "$bt\zipalign.exe" -c -P 16 4 $result
Check
& "$bt\aapt.exe" dump badging $result | Set-Content "$project\release\universal-badging.txt"
Check
& "$bt\aapt.exe" dump xmltree $result AndroidManifest.xml | Set-Content "$project\release\universal-manifest.txt"
Check
Get-FileHash $result -Algorithm SHA256 | ConvertTo-Json | Set-Content "$project\release\universal-sha256.json"
Write-Host $result
