# Installed reverse tools

Verified on 2026-09-21:

| Tool | Version | Location |
|---|---|---|
| JADX | 1.5.5 | `C:\Users\ARCANA\Tools\jadx\bin\jadx.bat` |
| Apktool | 3.0.2 | `C:\Users\ARCANA\Tools\apktool\apktool.bat` |
| Java | 21.0.10 | `C:\Program Files\Android\Android Studio\jbr` |

Apktool installed through reverse-skill bootstrap with a matching manifest SHA-256. JADX downloaded directly from the official GitHub release after the bootstrap encountered a GitHub API rate limit. Both version commands succeeded using Java 21. The reverse-skill tool index was refreshed.

From the workspace root, prepare a PowerShell session:

```powershell
. .\suunto-modernization\scripts\use_reverse_tools.ps1
jadx --version
apktool --version
```

This only changes the current session environment. Global Java defaults remain untouched. Earlier reports describing unavailable tools are historical; installation is now resolved. APK decompilation, UI integration and modified build verification are separate pending steps.
