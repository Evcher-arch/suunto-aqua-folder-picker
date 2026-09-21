# Aqua folders beta patch inventory

Baseline SHA-256: `9c624ed35cac48787a2f9fd2670a03d811ac97d8f160142671121dd09e162485`.

Only three original classes are manually changed, all in classes3.dex:

- `com/suunto/headset/ui/OfflineMusicManageActivity`: add `onCreate(Bundle)` override calling the original superclass then `AquaFolders.attach(Activity)`. Add `onDestroy()` override calling `AquaFolders.detach()` then the original superclass.
- `com/suunto/music/viewmodel/BaseMusicManagerViewModel.loadSelectableSongs()`: insert `AquaFolders.begin(Object)` at entry. The bridge checks the current Aqua view model identity.
- `com/suunto/music/components/PlayListDetailScreenKt.AddMusicOptionsScreen(...)`: insert `invoke-static/range {p0 .. p4}, Llocal/suunto/music/AquaFolders;->bind(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)V` at entry.

The native Android bridge and selection core compile to classes13.dex. All other DEX entries, binary resources, assets and ABI libraries remain byte-identical; `payload-audit.json` verifies this. Old signatures/source stamp are removed and every split gets the same local development signature.

Re-decoding the source APK overwrites the working smali hooks, so preserve/reapply these three modifications before rebuilding. `build_folder_release.ps1` assembles the changed DEX with Apktool's SmaliBuilder and compiles the additive Java DEX using D8. It does not rebuild Android resources, which avoids altering split resource references and preserves files whose names cannot be extracted on Windows.

The release is an experimental locally signed artifact, not an official Suunto update. See release notes for installation and test scope.
