# Aqua Bluetooth Folder Investigation

Date: 2026-09-21. Scope: user's Suunto Aqua, read-only filesystem inspection and static analysis of Suunto 6.13.8. No firmware, music, or playlist changes.

## Findings

The app's exported 196-song catalog supplies bare filenames, not folder paths. A USB snapshot now maps 191 songs uniquely. Four duplicate-name records and one unmatched filename are intentionally unresolved. The new map was uploaded without rebuilding or reinstalling the APK and verified by downloading it again.

The inspected client protocol exposes song metadata and named service-file transfer. No remote directory enumeration command was found in the inspected `com/suunto/soa` code. This is not proof that the firmware has no undocumented directory support. No direct PC GATT connection or Bluetooth request/response capture was performed in this investigation.

Windows reported Aqua Bluetooth/AVRCP devices. This does not establish a file-transfer profile or an active proprietary BLE connection. The USB volume became available separately as E:.

## Protocol Evidence

Sources below are relative to `reverse/suunto/jadx/sources/com/suunto/soa/`.

| Source | Observation |
| --- | --- |
| `ble/device/HeadsetConfig.java` | BLE service BF00, write BF05, notify BF06, all in the Bluetooth base UUID namespace |
| `ble/request/GetMusicInfoRequest.java` | Opcode 0x22, payload: four-byte song index |
| `ble/response/MusicInfoResponse.java` | Decodes response data as UTF-8 JSON into OfflineMusicInfo; no folder stripping in this parser |
| `ble/request/GetFileInfoRequest.java` | Opcode 0x32, file type + two-byte filename byte length + UTF-8 filename; not a directory listing |
| `ble/request/NotifyDeviceStartSendFileRequest.java` | Opcode 0x33, named-file transfer request; not evidence of arbitrary filesystem browsing |
| `data/SyncedFileType.java` | Lists playlist, all-song, sorting, customization, and activity/log file types; no directory type |
| `command/Request.java`, `IntKtxKt.java`, `DataConstants.java` | Frame: AA BB CC, command, opcode, uint16 big-endian content length, sequence byte, payload, DD EE FF. Content length includes sequence byte. Response-request command is C0. |

USB `E:/SYSTEM/allsong.lst` is JSON with playListId, playListName, musicNum and musicList. musicList contains index/key pairs rather than filesystem paths. Its musicNum is hex 000000c4 (196). Reproducible read-only inspection:

```powershell
$list = Get-Content -LiteralPath 'E:\SYSTEM\allsong.lst' -Raw -Encoding UTF8 | ConvertFrom-Json
$list | Select-Object playListName,musicNum
$list.musicList | Select-Object -First 3
```

This USB file is supporting evidence only; it is not a captured Bluetooth payload. The on-disk filename differs from the client ALL_SONG constant, so they must not be assumed byte-identical without capture.

## Current Result And Remaining Work

Snapshot: `build/indexes/20260921-165841-225/`. Map SHA256: `83f73a434d6be97b8ca4717c20329d9cbcabe1ccb4e1934e6b619e591685d23a`.

Mappings: MUSIC/2025 = 124, MUSIC/2026 = 56, MUSIC = 1, SYSTEM = 10. The phone file is verified; appearance after reconnecting Aqua remains a user UI check.

An automatic Bluetooth folder refresh is not implemented or confirmed. Next evidence needed: live BF00 service discovery and capture of the normal app's metadata/file-transfer exchange, followed by analysis of whether any returned field or service file retains paths. Do not infer directory support from the term musicPath, nor guess unsupported opcodes or modify firmware to test it.
