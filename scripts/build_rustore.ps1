param(
    [Parameter(Mandatory=$true)][string]$BaseApk,
    [Parameter(Mandatory=$true)][string]$DecodedDir,
    [Parameter(Mandatory=$true)][string]$OutputApk,
    [Parameter(Mandatory=$true)][string]$KeyStore,
    [Parameter(Mandatory=$true)][string]$Python,
    [string]$ApktoolJar = 'C:\Users\ARCANA\Tools\apktool\apktool.jar',
    [string]$BuildTools = 'C:\Users\ARCANA\AppData\Local\Android\Sdk\build-tools\37.0.0',
    [string]$JavaHome = 'C:\Program Files\Android\Android Studio\jbr',
    [string]$KeyAlias = 'folders',
    [switch]$AlreadyDecoded
)
$ErrorActionPreference = 'Stop'
$env:JAVA_HOME = $JavaHome
$env:PATH = "$JavaHome\bin;$env:PATH"
function Check { if ($LASTEXITCODE -ne 0) { throw "Command failed: $LASTEXITCODE" } }
if (!$env:SUUNTO_STORE_PASS -or !$env:SUUNTO_KEY_PASS) {
    throw 'Set SUUNTO_STORE_PASS and SUUNTO_KEY_PASS in the local environment.'
}
if (!(Test-Path -LiteralPath $KeyStore)) { throw 'Use the existing release signing key.' }
if (!$AlreadyDecoded) {
    & "$JavaHome\bin\java.exe" -Xmx3g -jar $ApktoolJar d $BaseApk -o $DecodedDir
    Check
}
& $Python "$PSScriptRoot\patch_rustore_permissions.py" $DecodedDir
Check
$unsigned = "$OutputApk.unsigned"
$aligned = "$OutputApk.aligned"
& "$JavaHome\bin\java.exe" -Xmx3g -jar $ApktoolJar b $DecodedDir -o $unsigned
Check
& "$BuildTools\zipalign.exe" -f -P 16 4 $unsigned $aligned
Check
& "$BuildTools\apksigner.bat" sign --ks $KeyStore --ks-key-alias $KeyAlias --ks-pass env:SUUNTO_STORE_PASS --key-pass env:SUUNTO_KEY_PASS --out $OutputApk $aligned
Check
& "$BuildTools\apksigner.bat" verify --verbose --print-certs $OutputApk
Check
& "$BuildTools\zipalign.exe" -c -P 16 4 $OutputApk
Check
$permissions = & "$BuildTools\aapt.exe" dump permissions $OutputApk
Check
$forbidden = 'android\.permission\.(QUERY_ALL_PACKAGES|READ_CALL_LOG|READ_CONTACTS|ACCESS_BACKGROUND_LOCATION|ACCESS_FINE_LOCATION|ACCESS_COARSE_LOCATION|READ_PHONE_STATE|MANAGE_ONGOING_CALLS|BIND_NOTIFICATION_LISTENER_SERVICE)(?:\x27|\s|$)'
if ($permissions -match $forbidden) { throw 'Forbidden permission remains in the final APK' }
$manifest = & "$BuildTools\aapt.exe" dump xmltree $OutputApk AndroidManifest.xml
Check
if ($manifest -match $forbidden -or $manifest -match 'com.suunto.connectivity.notifications.AncsService') {
    throw 'Forbidden permission reference or notification listener remains in the manifest'
}
$oldCert = & "$BuildTools\apksigner.bat" verify --print-certs $BaseApk
Check
$newCert = & "$BuildTools\apksigner.bat" verify --print-certs $OutputApk
Check
$certPattern = 'certificate SHA-256 digest: ([0-9a-fA-F]{64})'
$oldDigest = [regex]::Match(($oldCert -join "`n"), $certPattern).Groups[1].Value
$newDigest = [regex]::Match(($newCert -join "`n"), $certPattern).Groups[1].Value
if (!$oldDigest -or !$newDigest -or $oldDigest -ne $newDigest) {
    throw 'Signing certificate changed; upgrade would not be possible.'
}
& "$BuildTools\aapt.exe" dump badging $OutputApk | Select-Object -First 4
Check
Get-FileHash -Algorithm SHA256 -LiteralPath $OutputApk
