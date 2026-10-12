# nightvision 1.0 - Delete toggles night vision (Minecraft 1.12.2, Forge)

Client-side only. Press **Delete** to turn night vision on, press again to turn it off. A short message appears above the hotbar.
The key is a normal key binding (Controls -> "Night Vision"), so it can be rebound.

How it works: while on, the client gives the player a particle-free night vision effect (600 ticks, refreshed every 20 ticks, so the
vanilla "fading out" flicker never shows). Turning it off removes the effect. After a respawn or joining another world it is re-applied
while the toggle is on. The server is never involved, so it also works on servers without the mod.

Install: put `nightvision-1.0.jar` into `.minecraft/mods`.

Build/test: `./build.sh` (needs only a JDK; compiles against small stubs of the Minecraft/Forge classes and runs `tests/Run.java`).
Members of Minecraft itself are reached by reflection with both MCP and SRG names, so one jar works in the launcher and in a dev environment.
Not verified: a real Minecraft/Forge run (none available in the build environment).
