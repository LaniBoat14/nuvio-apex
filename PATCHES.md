# Nuvio Apex patch ledger

Base: stock NuvioTV 1.1.0-beta.10 (`99fbd65d8`). Apex = base + four additive features ported from Nuvio Arc
(`ysosrs123/NuvioTV-Fork` @ `ccfd1d7`): network engine, glass analytics overlay, lossless audio, Dolby Vision.

Rule: nothing outside this ledger differs from stock. `python3 apex/apex_purity.py` enforces it.

## Touched stock files

| File | Why |
|---|---|
| `app/build.gradle.kts` | Own application id `com.jeffrey.nuvioapex`, version `1.1.0-beta.10.apex.1` (code 1073001), update feed `LaniBoat14/nuvio-apex`, optional `-PapexArm32Only` ABI split. |

## Added files

| File | Why |
|---|---|
| `app/src/full/res/values/apex_identity.xml` | Launcher label "Nuvio Apex" (default locale). |
| `apex/`, `apex/apex_purity.py`, `.github/workflows/apex-build.yml` | Guard rails and CI. |
