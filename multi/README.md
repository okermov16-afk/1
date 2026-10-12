# multi 1.0 - small key toggles (Minecraft 1.12.2, Forge, client side only)

| Key | What it does |
|---|---|
| **Delete** | night vision on / off |
| **Page Down** | all sound on / off |

Both are normal key bindings (Controls -> "Multi"), so they can be rebound. A short message appears above the hotbar.

* Night vision: a particle-free night vision effect (600 ticks, refreshed every 20 ticks, no "fading out" flicker). Re-applied after
  respawn or joining another world while on. Off removes the effect.
* Sound: while off, every new sound (effects, music, ambient) is cancelled before it starts (`PlaySoundEvent`), and sounds that are
  already playing are stopped. Nothing is written to options.txt, so closing the game while muted does not leave volume at 0.
* The server is never involved, so it works on servers without the mod.

Install: put `multi-1.0.jar` into `.minecraft/mods`.

Build/test: `./build.sh` (needs only a JDK; compiles against small stubs of the Minecraft/Forge classes and runs `tests/Run.java`).
Members of Minecraft itself are reached by reflection with both MCP and SRG names, so one jar works in the launcher and in a dev environment.
Not verified: a real Minecraft/Forge run (none available in the build environment).
