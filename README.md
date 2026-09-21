# Suunto Aqua folder picker

Clean-room modernization module for the Suunto Android app's Aqua offline music workflow. It adds folder-aware selection, USB folder indexing through Android's Storage Access Framework, a main-screen command named `Обновить список папок по USB`, and a toggle named `Выделить все`.

The module is designed for authorized personal testing with Suunto Aqua and Suunto 6.13.8. It does not modify music files or headset firmware.

See [USB indexing notes](docs/aqua-usb-index.md) and [Bluetooth investigation](docs/aqua-bluetooth-folders.md). The Java module and smoke fixture are included; APK assembly requires the user's own legally obtained Suunto XAPK and locally decoded smali.

## Contents

| Path | Purpose |
|------|---------|
| `modules/folder-picker/android` | Android bridge, SAF scanner, folder map, and main-screen USB command |
| `modules/folder-picker/src` | Folder selection and ordering logic |
| `modules/folder-picker/smoke` | Deterministic Android fixture tests |
| `scripts/build_folder_diagnostic.ps1` | Build a signed universal diagnostic APK locally |
| `scripts/index_aqua_folders.py` | Correlate USB filenames with the app catalog |
| `docs/` | Reverse-engineering and feature notes |

## Build locally

1. Obtain the official Suunto 6.13.8 XAPK for your authorized test device.
2. Decode it locally with JADX/Apktool and place the decoded project under `reverse/suunto`.
3. Run `scripts/build_folder_diagnostic.ps1` from PowerShell.
4. Install the resulting APK only on a device where the original signing and data migration are understood.

## Scope and provenance

The repository intentionally excludes the original Suunto APK/XAPK, extracted proprietary app sources, phone dumps, personal indexes, signing keys, and build outputs. They remain local-only artifacts. The published code is the modernization layer and its tests.
