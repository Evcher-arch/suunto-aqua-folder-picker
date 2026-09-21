# Aqua offline music: folder selection

Update: tools were installed and Folders Beta 1 is now built. See [release notes](../release/suunto-6.13.8-folders-beta1/README.md). Original tool-blocking statements below describe the earlier investigation stage.

Request: add an optional folder browser to the mobile playlist editor for music stored on Suunto Aqua. User identified connected USB storage as `E:\MUSIC`.

## Confirmed implementation facts

Evidence files are under `evidence/suunto-6.13.8/` and were obtained with Android SDK `dexdump`, without running the APK.

| Evidence | Finding | Engineering consequence |
|---|---|---|
| `headset-disassembly.txt`, `OfflineMusicUseCase.getAllSongPlayListInfo`, offset `466336` onward | Reads `SyncedFileType.ALL_SONG` from device and parses `SoaPlayListInfo` using Moshi | Use the existing device catalog flow |
| `headset-disassembly.txt`, `OfflineMusicMapperKt.toSongDetail`, offset `3bf368` onward | Calls `OfflineMusicInfo.getMusicPath`; retains it and separately extracts filename using `/` | Path information survives conversion into the app model |
| `music-disassembly.txt`, `SongDetail` fields and `getPath`, offset `49b394` | Song model contains path, name and `SongOffsetKey` | Group by path while retaining original device identity |
| `music-disassembly.txt`, `PlayListDetailScreenKt` / `AddMusicOptionsScreen` | Existing selection screen is in the shared music components | Implement the optional mode here, scoped to Aqua until other devices are tested |
| `music-disassembly.txt`, `BaseMusicManagerViewModel.loadSelectableSongs`, `loadMoreSelectableSongs` | Selection data is loaded in pages | Do not build a supposedly complete folder list from the first page |

This confirms sufficient model support for a folder view. It does not establish a completed UI patch, exact runtime path prefixes, order preservation in the final submission flow, or firmware support for every proposed operation.

## USB observations

Read-only inspection found one file directly in `E:\MUSIC` and 58 files in `E:\MUSIC\2026`. `E:\SYSTEM\allsong.lst` is JSON with `musicNum=00000045` (69) and index/key entries. `PL.TXT` lists the all-songs entry and a `2026.lst` playlist with `musicNum=00000039` (57). `CL.TXT` contains a playlist object and index/key array. The counts refer to different scopes and do not by themselves prove corruption or stale data. Do not assume Windows directory order maps to these keys.

No files on the headphones were changed. Audio content was not copied. USB observations do not replace testing Bluetooth catalog data.

## Implemented artifact and remaining work

`modules/folder-picker` contains a Java 8 compatible, JVM-tested folder and selection core. Tests passed for nested folders, pagination guards, cross-folder selection, duplicate filenames, natural ordering, explicit reordering and reset. This is preparatory implementation, not an installed fix.

The original app's Compose UI integration, rebuild/signing and device tests remain outstanding. Downloading JADX was blocked: sandbox network access failed; the escalated request was not executed because automatic approval review reported an incompatible Guardian compaction checkpoint. This is a technical review failure, not a safety rejection. The review must be repaired or the tools supplied locally before that download can proceed. No attempt was made to bypass it.
