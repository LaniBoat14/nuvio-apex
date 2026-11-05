<div align="center">

# Nuvio Apex

A personal build of [NuvioTV](https://github.com/NuvioMedia/NuvioTV) for Android TV: stock Nuvio, plus a better network engine, lossless audio, Dolby Vision handling and a detailed playback stats overlay.

</div>

Apex looks and behaves like stock Nuvio on your TV. It has its own name, icon and package (`com.jeffrey.nuvioapex`), so it installs next to the original and is easy to tell apart.

## What Apex adds to stock Nuvio

- **Network engine** with Automatic speed-up, parallel connections and adjustable chunk size, for steadier throughput on slow or uneven servers.
- **Buffer and Network page** with a single Buffer ahead setting, memory-aware sizing and a Performance preset, on stock's design.
- **Lossless audio** output path.
- **Dolby Vision** profile 7 handling, including conversion for displays that need it.
- **Playback stats overlay**, opened from the analytics icon in the player.

Everything else is stock Nuvio, kept as close to upstream as possible. Every stock file that Apex changes is listed, with the reason, in [PATCHES.md](PATCHES.md), and a check in CI fails if anything else drifts.

The About screen in Settings shows which Nuvio release the current Apex build is based on.

## Install and update

1. Download the latest APK from [Releases](https://github.com/LaniBoat14/nuvio-apex/releases/latest) and install it. The APK is built for 32-bit ARM TVs (`armeabi-v7a`).
2. After that, updates arrive inside the app: Settings, About, check for updates. They install over the app and keep your settings.

Apex is a separate app from Nuvio, so your existing Nuvio install is not touched.

## Credits

Apex stands on other people's work, and this project would not exist without it.

- **[NuvioTV](https://github.com/NuvioMedia/NuvioTV)** by the NuvioMedia maintainers and contributors. Apex is built directly on it.
- **[Nuvio Arc](https://github.com/ysosrs123/NuvioTV-Fork)** by ysosrs123, a fork of NuvioTV. The network engine, the buffer and memory handling, the lossless audio path, the Dolby Vision handling and the stats overlay are ported from Arc.

All upstream copyright notices, licence headers and author attributions are kept.

## Licence

Apex is licensed under the [GNU General Public License v3.0](LICENSE), the same as NuvioTV and Nuvio Arc. The complete source for every release is in this repository.

Apex is an independent, unofficial build. It is not affiliated with or endorsed by NuvioMedia or the Arc author. Nuvio does not provide or host any content, and neither does Apex: you bring your own sources.

## Building and staying current

Apex keeps stock Nuvio on a `stock` branch and carries its own changes on top, so a new Nuvio release is merged in rather than copied over. See [PATCHES.md](PATCHES.md) for the full list of changes and `apex/apex_purity.py` for the check that keeps the list honest.
