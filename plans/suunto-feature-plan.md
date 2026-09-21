# Suunto feature plan

Baseline: Suunto 6.13.8. User priority is Aqua offline playlist selection by device folders. See [investigation](../docs/aqua-folder-music-investigation.md) and [implementation core](../modules/folder-picker/README.md). Other improvements below remain candidates.

| Area | Observed entry point | Candidate improvement | Acceptance criterion |
|---|---|---|---|
| Watch sync | `com.suunto.connectivity.sync.SyncResultService`, `com.stt.android.watch.DeviceActivity` | Progress, retry and failure details | Interrupted sync resumes without duplicate workouts |
| Maps and routes | `com.stt.android.home.explore.routes.planner.RoutePlannerActivity` | Route validation and offline status | Invalid routes report the cause; lost network does not lose edits |
| Workout analysis | `com.stt.android.workouts.details.analysis.LandscapeAnalysisGraphActivity` | Comparable graphs and configurable metrics | Consistent units and handling of missing samples |
| Notifications | `WatchAppNotificationsPermissionsActivity` | Per-app controls and permission state | Revocation is reflected immediately |
| Dashboard | `com.stt.android.home.dashboardv2.edit.DashboardTabEditActivity` | Saved metric layouts | Layout survives restart and handles empty data |

## Implementation sequence

1. Select a concrete behavior and expected outcome.
2. Restore JADX/apktool availability and inspect the relevant callers, storage and dependencies.
3. Document current behavior before choosing a patch or standalone module. The APK does not contain the original Gradle project.
4. Implement a narrow change and verify the relevant acceptance criterion.
5. For later device verification, select matching ABI/density splits and account for services tied to the original signing certificate.

Current deliverable is offline analysis and reproducible tooling. No modified application build or installation has been performed.
