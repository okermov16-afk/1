# modelport-1.0.0.jar (patched)

Replaces the original jar 1:1 (same file name, same mod version). 4 classes changed, 4 added, 194 untouched.

## Fixes
1. **Placement tables ignored `onBlockPlacedBy`** (`PortedPlacement.probe` -> `PlacementSim`).
   Many mods (CFM `BlockFurniture`) set the facing in `onBlockPlacedBy`; the old probe only called
   `getStateForPlacement`, so the table was constant ("always north") and the block was only correct
   for one player direction. The probe now also runs `onBlockPlacedBy` in the isolated `ProbeWorld`.
   Any failure of the extra step (exception, 5 s hang, block removed, bad meta) falls back to exactly
   the value the old probe produced. After a capture run, `PlacementSim.refreshAll()` rewrites the stored
   table of every library block whose fresh table differs (old entries were skipped before).
2. **canon / meta mix-up** (`CanonMap`, used in `BlockPorted.targetMeta`, `applyTransition`, `getStateForPlacement`).
   Recorded transitions (click / power / unpower / click-then, placement table) are in the original mod's
   `getMetaFromState` space ("canon"), but were applied as ported meta indices. For blocks where
   `canon != meta` (e.g. `furniture:doorbell`) this turned south into east on click, etc.
   Translation is the identity for blocks whose states all have `canon == meta` and for merge families.
3. `BehaviorProbe.measureOnce` compares against the canon of the probed state (was: meta index), which
   produced spurious transitions at capture time for `canon != meta` blocks.

## Verification (no Minecraft available in the build environment)
* Real patched bytecode run under `-Xverify:all` against stub Minecraft classes.
* `DoorbellTest`: original jar reproduces the reported bug, patched jar keeps facing.
* `RandomDiff`: 8000 random blocks, old vs new jar: 0 differences for all blocks where behaviour must not change.
* `PartnerTest`, `PlacementTest` (CFM-like, PFM-like, throwing, hanging, removing, bad meta).
Not verified: real Minecraft/Forge runtime (SRG name `func_180633_a`, real `ProbeWorld` with real mods).
