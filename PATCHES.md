# Nuvio Apex patch ledger

Base: stock NuvioTV 1.1.0-beta.11 (`1206736fd`). Apex = base + four additive features ported from Nuvio Arc
(`ysosrs123/NuvioTV-Fork` @ `ccfd1d7`): network engine, glass analytics overlay, lossless audio, Dolby Vision.

Rule: nothing outside this ledger differs from stock. `python3 apex/apex_purity.py` enforces it.

## Touched stock files

| File | Why |
|---|---|
| `app/build.gradle.kts` | Own application id `com.jeffrey.nuvioapex`, Apex version scheme (`versionName` 0.1.x, `versionCode` = stock code x 1000 + n), `NUVIO_BASE` field naming the Nuvio release this build is rebased on, update feed `LaniBoat14/nuvio-apex-releases` (public releases repo; source stays private), optional `-PapexArm32Only` ABI split. |
| `app/src/main/java/com/nuvio/tv/ui/screens/settings/AboutScreen.kt` | Adds one About line, "Nuvio Apex · based on Nuvio `NUVIO_BASE`". |
| `app/src/main/res/drawable/ic_launcher.png`, `app/src/main/res/drawable/tv_banner.png`, `app/src/main/res/drawable-nodpi/app_logo_mark.png`, `app/src/main/res/drawable-nodpi/tv_banner.png`, `app/src/main/res/mipmap-xhdpi/banner.png`, `app/src/main/res/mipmap-{mdpi,hdpi,xhdpi,xxhdpi,xxxhdpi}/ic_launcher.png` | Apex icon and banner: stock's mark recolored (gold, orange, coral) so Apex is told apart from stock and Flow on the same box. |
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
| `app/libs/lib-decoder-ffmpeg-release.aar` | Replaced by the Arc ffmpeg decoder aar (denied-transcode planning, surround, IEC passthrough support). |
| `app/proguard-rules.pro` | Keeps `DefaultAudioSink.audioTrack` for the stats overlay. |
| `app/src/main/cpp/dovi_bridge.cpp` | Dolby Vision edition: Arc DV7 conversion, RPU/HDR10 handling and stats. |
| `app/src/main/java/com/nuvio/tv/core/network/StreamSpeedTester.kt` | Lossless audio: surround resolution, IEC/system passthrough, denied-codec handling, audio settings and tracks. |
| `app/src/main/java/com/nuvio/tv/core/player/DolbyVisionExtractorsFactory.kt` | Dolby Vision edition: Arc DV7 conversion, RPU/HDR10 handling and stats. |
| `app/src/main/java/com/nuvio/tv/core/player/DolbyVisionMatroskaTransformer.kt` | Dolby Vision edition: Arc DV7 conversion, RPU/HDR10 handling and stats. |
| `app/src/main/java/com/nuvio/tv/core/player/DoviBridge.kt` | Dolby Vision edition: Arc DV7 conversion, RPU/HDR10 handling and stats. |
| `app/src/main/java/com/nuvio/tv/core/player/HevcDvRpuStripper.kt` | Dolby Vision edition: Arc DV7 conversion, RPU/HDR10 handling and stats. |
| `app/src/main/java/com/nuvio/tv/core/player/HevcHdr10PlusStripper.kt` | Dolby Vision edition: Arc DV7 conversion, RPU/HDR10 handling and stats. |
| `app/src/main/java/com/nuvio/tv/core/player/dvmkv/MatroskaExtractor.java` | Dolby Vision edition: Arc DV7 conversion, RPU/HDR10 handling and stats. |
| `app/src/main/java/com/nuvio/tv/ui/screens/player/AudioOutputRouteDetector.kt` | Lossless audio: surround resolution, IEC/system passthrough, denied-codec handling, audio settings and tracks. |
| `app/src/main/java/com/nuvio/tv/ui/screens/player/DolbyVisionBaseLayerPolicy.kt` | Dolby Vision edition: Arc DV7 conversion, RPU/HDR10 handling and stats. |
| `app/src/main/java/com/nuvio/tv/ui/screens/player/LastPlaybackDiagnostics.kt` | Dolby Vision edition: Arc DV7 conversion, RPU/HDR10 handling and stats. |
| `app/src/main/java/com/nuvio/tv/ui/screens/player/PlaybackSpeedAwareAudioRenderer.kt` | Lossless audio: surround resolution, IEC/system passthrough, denied-codec handling, audio settings and tracks. |
| `app/src/main/java/com/nuvio/tv/ui/screens/player/PlaybackSpeedAwareAudioSink.kt` | Lossless audio: surround resolution, IEC/system passthrough, denied-codec handling, audio settings and tracks. |
| `app/src/main/java/com/nuvio/tv/ui/screens/player/PlayerMediaInfo.kt` | Sectioned stats overlay: analytics control button, polling, view-model sampling, diagnostics. |
| `app/src/main/java/com/nuvio/tv/ui/screens/player/PlayerPlaybackAnalyticsDiagnostics.kt` | Sectioned stats overlay: analytics control button, polling, view-model sampling, diagnostics. |
| `app/src/main/java/com/nuvio/tv/ui/screens/player/PlayerRuntimeControllerAudioDelayRoutes.kt` | Lossless audio: surround resolution, IEC/system passthrough, denied-codec handling, audio settings and tracks. |
| `app/src/main/java/com/nuvio/tv/ui/screens/player/PlayerRuntimeControllerMetadata.kt` | Lossless audio: surround resolution, IEC/system passthrough, denied-codec handling, audio settings and tracks. |
| `app/src/main/java/com/nuvio/tv/ui/screens/player/PlayerRuntimeControllerMpv.kt` | Lossless audio: surround resolution, IEC/system passthrough, denied-codec handling, audio settings and tracks. |
| `app/src/main/java/com/nuvio/tv/ui/screens/player/PlayerRuntimeControllerStreams.kt` | Lossless audio: surround resolution, IEC/system passthrough, denied-codec handling, audio settings and tracks. |
| `app/src/main/java/com/nuvio/tv/ui/screens/player/PlayerRuntimeControllerTrackSelection.kt` | Lossless audio: surround resolution, IEC/system passthrough, denied-codec handling, audio settings and tracks. |
| `app/src/main/java/com/nuvio/tv/ui/screens/player/PlayerRuntimeControllerTracks.kt` | Lossless audio: surround resolution, IEC/system passthrough, denied-codec handling, audio settings and tracks. |
| `app/src/main/java/com/nuvio/tv/ui/screens/player/PlayerScreen.kt` | Sectioned stats overlay: analytics control button, polling, view-model sampling, diagnostics. |
| `app/src/main/java/com/nuvio/tv/ui/screens/player/PlayerUiState.kt` | Lossless audio: surround resolution, IEC/system passthrough, denied-codec handling, audio settings and tracks. |
| `app/src/main/java/com/nuvio/tv/ui/screens/player/PlayerViewModel.kt` | Sectioned stats overlay: analytics control button, polling, view-model sampling, diagnostics. |
| `app/src/main/java/com/nuvio/tv/ui/screens/settings/NetworkSettingsScreen.kt` | Lossless audio: surround resolution, IEC/system passthrough, denied-codec handling, audio settings and tracks. |
| `app/src/main/java/com/nuvio/tv/ui/screens/settings/PlaybackAudioSettings.kt` | Lossless audio: surround resolution, IEC/system passthrough, denied-codec handling, audio settings and tracks. |
| `app/src/main/java/com/nuvio/tv/ui/screens/settings/PlaybackSettingsModel.kt` | Lossless audio: surround resolution, IEC/system passthrough, denied-codec handling, audio settings and tracks. |
| `app/src/main/java/com/nuvio/tv/ui/screens/settings/PlaybackVideoSettings.kt` | Lossless audio: surround resolution, IEC/system passthrough, denied-codec handling, audio settings and tracks. |
| `app/src/main/baseline-prof.txt` | Wildcard rules for the playback path (engine, network, IEC, stats) so Arc code is precompiled like the stock player. |
| `app/src/test/java/com/nuvio/tv/core/player/DolbyVisionBaseLayerPolicyTest.kt` | Test updated to match the replaced code (Dolby Vision policy, Arc engine) or to run without CI Trakt credentials. |
| `app/src/test/java/com/nuvio/tv/data/repository/TraktAuthServiceTest.kt` | Test updated to match the replaced code (Dolby Vision policy, Arc engine) or to run without CI Trakt credentials. |
| `app/src/test/java/com/nuvio/tv/ui/screens/player/NuvioExoPlayerPerformanceHelperTest.kt` | Test updated to match the replaced code (Dolby Vision policy, Arc engine) or to run without CI Trakt credentials. |
| `app/src/test/java/com/nuvio/tv/ui/screens/player/ParallelRangeChunkLeaseTest.kt` | Test updated to match the replaced code (Dolby Vision policy, Arc engine) or to run without CI Trakt credentials. |

## Added files

| File | Why |
|---|---|
| `app/src/main/java/com/nuvio/tv/ui/screens/settings/ArcMemoryBudget.kt` (added) | Full Arc memory budget (native-aware sizing, connection-based chunk ceilings), alongside stock's untouched `MemoryBudget`. The Buffer and Network screen, view model, saved settings and engine all use it so the numbers agree. |
| `app/src/full/res/values/apex_identity.xml` | Launcher label "Nuvio Apex" (default locale). |
| `app/src/main/res/values/apex_strings.xml` | Apex-only strings (Automatic speed-up). |
| 44 engine files under `core/` and `ui/screens/player/`, `ui/screens/settings/ArcMemoryBudget.kt` | Arc network engine, copied from Arc; `ArcMemoryBudget` keeps stock `MemoryBudget` untouched. |
| `apex/`, `apex/apex_purity.py`, `.github/workflows/apex-build.yml` | Guard rails and CI. |
