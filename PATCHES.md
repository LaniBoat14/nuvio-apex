# Nuvio Apex patch ledger

Base: stock NuvioTV 1.1.0-beta.10 (`99fbd65d8`). Apex = base + four additive features ported from Nuvio Arc
(`ysosrs123/NuvioTV-Fork` @ `ccfd1d7`): network engine, glass analytics overlay, lossless audio, Dolby Vision.

Rule: nothing outside this ledger differs from stock. `python3 apex/apex_purity.py` enforces it.

## Touched stock files

| File | Why |
|---|---|
| `app/build.gradle.kts` | Own application id `com.jeffrey.nuvioapex`, version `1.1.0-beta.10.apex.1` (code 1073001), update feed `LaniBoat14/nuvio-apex`, optional `-PapexArm32Only` ABI split. |
| `.github/workflows/*.yml` (8 stock files) | Removed: stock release, issue-triage and PR-comment automation. They belong to the upstream project (scheduled bots, release publishing) and must not run in Apex. Apex has its own `apex-build.yml`. Listed individually in `apex/touched-stock-files.txt`: android-release, close-stale-issues, close-unlabeled-issues, pr-full-debug-build-comment, pr-full-debug-build, pr-template-check, stale-needs-info, triage-needs-info. |
| `libmpv-android` (gitlink) | Removed: upstream commits a submodule entry with no `.gitmodules`, which breaks `actions/checkout`. Nothing in the build references it (mpv comes from the prebuilt AAR). |
| `app/src/main/java/com/nuvio/tv/data/local/PlayerSettingsDataStore.kt` | Adds the `autoSpeedupEnabled` setting (field, key, reader, setter). |
| `app/src/main/java/com/nuvio/tv/ui/screens/player/PlayerRuntimeController.kt` | Engine state (`bufferMemoryKeeper`, `autoSpeedupLender`, `currentManagedBudgetMb`, `lastSeekWallMs`, `expectedRuntimeMinutes`, `lastAppliedPlayerSettings`), memory-reporter hook, server-source predicate for the media-source factory, prewarm cancel and background buffer trim on exit. |
| `app/src/main/java/com/nuvio/tv/ui/screens/player/PlayerRuntimeControllerInitialization.kt` | Load-control build replaced by the Arc sized-buffer path (budget, top-up, start cushion), `applyMediaSourceFactorySettings`, keeper attach, speed-up hint, seek stamp, first-frame budget tightening. |
| `app/src/main/java/com/nuvio/tv/ui/screens/player/PlayerRuntimeControllerLifecycle.kt` | Resets the byte counter and lanes on release, defers the buffer trim off the main thread, detaches the keeper. |
| `app/src/main/java/com/nuvio/tv/ui/screens/player/PlayerRuntimeControllerPlaybackEvents.kt` | Adds the Automatic speed-up sampler to the progress tick and the keeper tick to the 10 s buffer log. |
| `app/src/main/java/com/nuvio/tv/ui/screens/settings/PlaybackBufferNetworkSettings.kt` | Adds the Automatic speed-up toggle to the Network section. |
| `app/src/main/java/com/nuvio/tv/ui/screens/settings/PlaybackSettingsViewModel.kt` | Adds `setAutoSpeedupEnabled`. |
| `app/src/main/java/com/nuvio/tv/ui/screens/stream/StreamScreen.kt` | Warms the selected stream's connection at the two play call sites. |
| `app/src/main/java/com/nuvio/tv/ui/screens/stream/StreamScreenViewModel.kt` | Adds `prewarmSelectedPlayback`. |
| `app/src/main/java/com/nuvio/tv/core/network/PlaybackSocketFactory.kt` | Replaced by the Arc engine version. |
| `app/src/main/java/com/nuvio/tv/core/player/BitrateAwareLoadControl.kt` | Replaced by the Arc engine version. |
| `app/src/main/java/com/nuvio/tv/ui/screens/player/NuvioExoPlayerPerformanceHelper.kt` | Replaced by the Arc engine version. Keeps stock's `buildScrubbingParams`. |
| `app/src/main/java/com/nuvio/tv/ui/screens/player/ParallelRangeDataSource.kt` | Replaced by the Arc engine version. |
| `app/src/main/java/com/nuvio/tv/ui/screens/player/PlaybackConnectionEvents.kt` | Replaced by the Arc engine version. |
| `app/src/main/java/com/nuvio/tv/ui/screens/player/PlayerMediaSourceFactory.kt` | Replaced by the Arc engine version. |
| `app/src/main/java/com/nuvio/tv/ui/screens/player/PlayerPlaybackNetworking.kt` | Replaced by the Arc engine version. |

## Added files

| File | Why |
|---|---|
| `app/src/full/res/values/apex_identity.xml` | Launcher label "Nuvio Apex" (default locale). |
| `app/src/main/res/values/apex_strings.xml` | Apex-only strings (Automatic speed-up). |
| 44 engine files under `core/` and `ui/screens/player/`, `ui/screens/settings/ArcMemoryBudget.kt` | Arc network engine, copied from Arc; `ArcMemoryBudget` keeps stock `MemoryBudget` untouched. |
| `apex/`, `apex/apex_purity.py`, `.github/workflows/apex-build.yml` | Guard rails and CI. |
