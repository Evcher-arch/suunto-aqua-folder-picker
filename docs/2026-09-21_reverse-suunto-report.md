# Suunto 6.13.8 static analysis

## Result

The supplied XAPK contains Suunto and resolves the earlier baseline mismatch. This pass identifies package structure and feature entry points, not complete implementation logic or runtime behavior.

| Field | Value |
|---|---|
| Package / label | `com.stt.android.suunto` / `Suunto` |
| Version | `6.13.8` / `6013008` |
| Minimum / target SDK | 32 / 36 |
| Launcher | `com.stt.android.launcher.ProxyActivity` |
| XAPK SHA-256 | `df77da537a6254c3adef400a1d8892f9f77a505179549f4941a0beeb0f7e8f31` |
| Base APK SHA-256 | `9c624ed35cac48787a2f9fd2670a03d811ac97d8f160142671121dd09e162485` |
| Signer certificate SHA-256 | `30bfe80b91b25034da7a2022f95933d913a584042dc16a1a8d54e870a86a49a7` |
| Certificate subject | Sports Tracking Technologies Ltd, Helsinki, FI |

## Evidence

Paths are relative to `evidence/suunto-6.13.8/`.

| ID | Artifact | Establishes |
|---|---|---|
| S-001 | `xapk-summary.json` | Archive hash, seven APKs and individual hashes |
| S-002 | `com.stt.android.suunto.apk.analysis/aapt-badging.txt` | Identity, SDK and launcher |
| S-003 | Each `*.apk.analysis/apksigner-verify.txt` | Successful signature verification; same signer on all seven APKs |
| S-004 | `com.stt.android.suunto.apk.analysis/aapt-manifest-xmltree.txt` | Components and application attributes |
| S-005 | `com.stt.android.suunto.apk.analysis/aapt-permissions.txt` | Declared permissions |
| S-006 | `apk-summary.json` | 12 DEX files, 6,738 base APK entries and sampled strings |
| S-007 | `native-arm64-summary.json` | 15 arm64 native libraries |

## Findings and next steps

**F-S01: Suunto baseline confirmed.** S-001/S-002 identify version 6.13.8. S-003 confirms integrity and consistent signing within the archive. A certificate subject is self-described; comparison with an independently trusted publisher certificate has not been performed. Use this version for subsequent feature analysis.

**F-S02: Split distribution affects rebuilding.** S-001 contains the base, arm64-v8a and armeabi-v7a splits, plus mdpi, xhdpi, xxhdpi and xxxhdpi resources. Inspect ABI splits for native logic and select compatible splits for any later device installation.

**F-S03: Feature boundaries are visible.** S-004 includes `HomeActivity`, `DeviceActivity`, `WatchDetailActivity`, `FindWatchActivity`, `WatchUpdatesActivity`, `RoutePlannerActivity`, `RouteDetailsActivity`, `LandscapeAnalysisGraphActivity`, `DashboardTabEditActivity` and `com.suunto.connectivity.sync.SyncResultService`. It also declares `SuuntoAiRouteGenerationActivity`. Names locate investigation points; they do not prove account-level feature availability. Follow the selected component into decompiled callers before changing behavior.

**F-S04: Native logic needs follow-up.** S-007 includes `libmds.so`, `libmapbox-maps.so`, `libmapbox-common.so`, `libvalhalla-wrapper.so`, `libduktape.so`, `libsqliteJni.so` and other libraries. Mapping/routing and device logic may cross JNI boundaries; this is an inference from names, not a verified call graph. Inspect relevant Java/native call sites after feature selection.

**F-S05: Permission behavior remains unverified.** S-005 declares Bluetooth scan/connect, background location, companion-device permissions, contacts, call log, phone state, package visibility and advertising identifiers. S-004 sets `allowBackup=false`. Declarations do not demonstrate granted access, actual collection or a vulnerability. Trace requests and denial handling for the selected feature.

## Limits

Local `aapt` and `apksigner` succeeded for all seven APKs. The DEX helper returns bounded keyword samples with UTF-8 replacement decoding, not full MUTF-8 recovery or method analysis. Native inventory covers arm64; armv7 manifests and signatures were verified but native functions were not inspected. No runtime, server, account or watch interaction occurred. JADX/apktool decompilation remains pending after the earlier bootstrap HTTP 403. No patch or installable modified build is claimed. Archive contents were treated as data, not instructions.
