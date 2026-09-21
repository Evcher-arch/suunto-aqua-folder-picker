# Aqua folder selection core

Status: implemented and JVM-tested selection logic plus native Android picker, wired into a signed Suunto Folders Beta 1 build. Android UI smoke tests passed with a paginated fixture and callback assertions. Full Suunto on an ARM phone and actual Aqua persistence are unverified. No physical-device writes have been performed.

`FolderSelection<T>` builds folder navigation from the device paths already present in Suunto's `SongDetail`. `T` should be the original `SongDetail`, and `index`/`key` must come unchanged from its `SongOffsetKey`. It returns the original objects in explicit selection order.

Supported: direct children, nested folders, natural filename ordering, cross-folder selection, deduplication by device identity, explicit reordering, and catalog reset. Existing playlist tracks should be seeded into selection in their original order. Do not derive a device key from a filename or filesystem enumeration.

The catalog is paginated. Add pages from a single device/catalog snapshot and mark completion only when exhausted. Folder-wide selection rejects an incomplete catalog. On index changes or device disconnect, reset state before loading another snapshot. A failed page load must remain incomplete. The host UI owns coroutine cancellation, process-state restoration and loading/error rendering.

## Integration Design

The beta uses a native mode-choice dialog from the existing add-music sheet, followed by a native folder dialog. `AquaFolders` receives original selection/save callbacks from `AddMusicOptionsScreen`. The bridge is attached only by `OfflineMusicManageActivity`; original Compose rendering remains available. CREATE selection state is ordered before the original confirmation callback. The following design requirements also describe areas for further device verification:

1. In `PlayListDetailScreenKt.AddMusicOptionsScreen`, add a `Tracks / Folders` mode control, preserving the existing track view.
2. In folder mode, show immediate child folders with navigation back to their parent. Offer selection of individual songs and all songs in the current folder; including subfolders must be explicit.
3. Load the complete catalog through the existing `BaseMusicManagerViewModel` flow before enabling whole-folder selection. Display pending/error state, not a misleading empty folder.
4. Share one selection across both modes. Show selected count and allow ordering before confirmation.
5. Original callbacks feed `addSongsToPlayList`. The fixture verifies ordered submission; real device limits, exclusion behavior and existing-playlist order still need testing.
6. Keep the original synchronization protocol. Verify on Aqua after rebuilding: two folders with identical filenames, nested folders, non-ASCII names, more than one page, disconnect/reconnect and reopening the saved playlist.

Missing/invalid device paths must remain available in the original flat view with an explicit unavailable-folder state; do not silently drop songs. USB paths such as `E:\MUSIC` are only inspection paths, never the mobile catalog's path namespace.

## Verify

Run `./verify.ps1` for JVM tests. For Android UI verification, build with `scripts/build_folder_smoke.ps1`, install its APK on `emulator-5554`, then run `scripts/verify_folder_ui.py`. Fixture sources are excluded from the release. These tests do not test Bluetooth or physical headphones.
