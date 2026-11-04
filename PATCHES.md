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

## Added files

| File | Why |
|---|---|
| `app/src/full/res/values/apex_identity.xml` | Launcher label "Nuvio Apex" (default locale). |
| `apex/`, `apex/apex_purity.py`, `.github/workflows/apex-build.yml` | Guard rails and CI. |
