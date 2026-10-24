# CATACLYSM: DIVINE ARSENAL — HELIOS ASCENSION v0.3

**Minecraft Java 1.20.1 · Fabric API · Java 17 · PojavLauncher**

This is an actively developed prototype. A successful CI build is not a substitute for in-game visual or performance testing.

## New in v0.3
- Procedural 3D-inspired orbital platform with 16 segmented armored hull plates and pylons.
- Central reactor housing, 8 cooling vanes, 8 moving emitter lenses, three gyroscopic rails.
- Existing energy rings, layered plasma column and ground shockwave retained.
- Charge-up end-rod particles, pulsing beacon audio, impact sparks and explosion audio emitted server-side.
- Limits client-side simultaneous active visual instances to three.
- Terrain excavation skips air and block-entity blocks, retains per-tick block-edit budget.
- No paid asset generators or additional runtime dependencies.

## Build and install
Open [Actions](https://github.com/WhtHells/CATACLYSM-DIVINE-ARSENAL-V2/actions), wait for the latest **green** run, download the `CATACLYSM-SOLAR-LANCE-JAR` artifact and extract the mod JAR. Install with Fabric API 0.92.2+1.20.1 on Minecraft 1.20.1. Remove older CATACLYSM JARs first.

## Use
`/give @s cataclysm:solar_lance` — aim at a block and use the item. The strike destroys terrain; test in a disposable world.

## Engineering limitations
The orbital platform uses procedural mesh quads in the Fabric renderer, **not a Blender-authored rig or imported GLB**. Blender and the optional game-dev CLI are not installed in the execution environment used for this revision. The visual debugging and performance optimization workflows require actual reproducible game captures and telemetry; no runtime FPS measurements or PojavLauncher testing have been performed yet. The v0.3 implementation is an incremental graphical upgrade, not an objectively verified 10x improvement.
