# Modernization roadmap

Superseded for current work by [Suunto feature plan](suunto-feature-plan.md). The Suunto 6.13.8 XAPK resolves the baseline mismatch. Everything below records the earlier Uptodown assessment, not the current Suunto roadmap.

## Decision gate

The current APK is `com.uptodown`, not a Suunto app. Choose the correct branch before implementation:

1. **Suunto modernization**: obtain the correct Suunto APK or source baseline, then rerun the same evidence pipeline.
2. **Uptodown-style app modernization**: continue with this APK, but treat the work as app-store modernization rather than Suunto modernization.
3. **Clean-room replacement**: use the observed feature map only as requirements input and build a new app with original code and assets.

## Recommended project track

Use a clean-room Kotlin/Android project for new feature work. Avoid binary patching unless there is explicit ownership or redistribution authorization for the target app.

```text
suunto-modernization/
  docs/       analysis reports and decisions
  evidence/   raw command output and normalized summaries
  plans/      product and engineering roadmap
  patches/    authorized patch notes only
  scripts/    reproducible analysis helpers
```

## Feature backlog

| Priority | Feature area | Proposed improvement | Evidence basis |
|---|---|---|---|
| P0 | Baseline validation | Replace this sample with the intended Suunto APK/source before Suunto-specific work | F-001 |
| P0 | Privacy and permissions | Permission-by-feature onboarding, package visibility explanation, ad ID controls | F-002 |
| P1 | Update/install UX | Clear install/delete state machine, rollback confirmation, failed install diagnostics | F-002 |
| P1 | Downloads | Queue, pause/resume, checksum display, storage pressure warnings | Manifest components: downloads, updates, file explorer |
| P1 | Account and trust | Modern Credential Manager flow, explicit account sync controls, signer/certificate display where relevant | Permissions and DEX strings |
| P2 | TV/mobile split | Shared domain module with separate Compose mobile and TV presentation modules | Launch + TV activities |
| P2 | Observability | Local diagnostic export with redaction for install/download failures | Firebase/Crashlytics and WorkManager presence |

## Next engineering steps

1. Install or unblock `jadx` and `apktool`.
2. Rerun `apk-reverse/scripts/decode.ps1` into `reverse/suunto-apk/`.
3. Identify concrete Java/Kotlin classes for download, install, update, account, and privacy flows.
4. Decide whether modernization is a clean-room app or an authorized fork/patch.
5. Create Android project skeleton only after the baseline is corrected.

## Safety and legal boundary

Do not remove licensing checks, bypass signature validation, bypass SSL pinning, or redistribute a modified third-party APK without authorization. This workspace is set up for offline analysis and clean-room planning.
