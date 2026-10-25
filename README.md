# CATACLYSM: DIVINE ARSENAL — PROJECT APOCALYPSE v0.4

Minecraft Java **1.20.1**, **Fabric**, Java 17, mobile-oriented OpenGL compatibility.

## Implemented in v0.4
- Dedicated client-side `StrikeCinematicHud`: animated targeting reticle, top/bottom cinematic bars, target coordinates, phase status, charge meter and impact flash.
- Server strike continues to resolve at 64 ticks (3.2 seconds), with staged audio, particles, 9-strength explosion and bounded crater edits.
- Red plasma shells and expanding impact column layered over the previous cyan beam.
- v0.3 armored orbital platform retained: segmented plates, pylons, core, emitters and gyroscopic rails.
- No mandatory post-processing shaders or RGBA16F framebuffer support.
- Mod version bumped to 0.4.0.

## Important scope
This is **the first playable code milestone** of a larger cinematic overhaul, **not** a reproduction of The Shooting Star. Full space cutscenes, camera travel, a separately authored station asset, and a high-fidelity planet are **not implemented**. No mobile runtime testing or benchmark has been completed yet. Gameplay must be verified before describing the release as stable.

## Installation
1. Open [GitHub Actions](https://github.com/WhtHells/CATACLYSM-DIVINE-ARSENAL-V2/actions) and confirm the latest workflow is green.
2. Download the `CATACLYSM-SOLAR-LANCE-JAR` artifact; extract the mod JAR.
3. Remove previous CATACLYSM versions from `mods`, install the new JAR and Fabric API 0.92.2+1.20.1.
4. Create a disposable world and use `/give @s cataclysm:solar_lance`.

**Warning:** The attack permanently destroys blocks. Test in a disposable world.
