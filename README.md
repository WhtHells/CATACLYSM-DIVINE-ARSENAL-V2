# CATACLYSM: DIVINE ARSENAL — SOLAR LANCE v0.2

Minecraft Java 1.20.1 / Fabric prototype, designed to be tested on PojavLauncher.

## Changes from v0.1
- More detailed elevated orbital charging structure, additional animated energy rings.
- Layered expanding impact shockwaves.
- Stronger initial explosion and a ~33-block-wide, bowl-shaped crater.
- Terrain excavation spread across ticks to reduce sudden server stalls.
- Beacon activation sound, longer cooldown.

## Build
GitHub Actions builds this mod automatically. Download the **CATACLYSM-SOLAR-LANCE-JAR** artifact from the latest successful workflow run, unzip, and place the non-sources .jar in the Minecraft mods folder with Fabric API 0.92.2+1.20.1.

## Use
Enable cheats and run `/give @s cataclysm:solar_lance`. Aim at the ground and use the item. Test in a disposable world: this weapon destroys terrain.

## Limitations
This is a prototype, not yet at the visual fidelity of The Shooting Star. No custom shaders, textures, synchronized multiplayer effects or cinematic camera system. Android performance and runtime compatibility are not yet verified.
