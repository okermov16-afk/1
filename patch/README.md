# modelport-1.0.0.jar (patched)

Replaces the original jar 1:1 (same file name, same mod version). 4 classes changed, 4 added, 194 untouched.

## Fixes
1. **Placement tables were probed in a vacuum** (`PortedPlacement.probe` -> `PlacementSim`).
   The old probe called `getStateForPlacement` in empty air, with no block that the player clicked on, and never ran
   `onBlockPlacedBy`. Wall/side-mounted blocks (CFM `BlockDoorBell`/`BlockLightSwitch`:
   `FACING = canPlaceBlockOnSide(..) ? face.getOpposite() : NORTH`) found no solid block behind them and returned
   NORTH for every face and yaw -> constant table, correct for one direction only. Many blocks also set state in `onBlockPlacedBy`.
   The probe now runs in the isolated `ProbeWorld` with a solid support block on the clicked side, then runs `onBlockPlacedBy`.
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

## v4: getStateFromMeta contract
For blocks whose states have `canon != meta` (e.g. `furniture:doorbell`), `getMetaFromState` returns the original mod's meta
(canon) but `getStateFromMeta` was indexed by the ported meta, so `getStateFromMeta(getMetaFromState(s)) != s` for 6 of 8 doorbell
states (pressed south -> pressed east, pressed west -> unpressed south, ...). `BlockPorted.func_176203_a` now decodes canon metas
(inverse of getMetaFromState); every internal caller uses the private `stateByMeta` (the old body). Identity for `canon == meta`
blocks and merge families. Verified with the real BlockPorted constructor (`RealCtor`): all states round-trip, click/tick keeps facing.

## v5: ghost copy of door halves (PortedBakedModel.getQuads)
`getQuads` fell back to the BASE variant whenever a neighbour-dependent variant had no quads for the requested side
(`list == null && variant != 0`). A variant legitimately has quads only on some sides, so base quads of another
orientation were drawn next to the real door: a flat one-sided "copy" of a door half (whole big face or only an edge).
Now the fallback happens only when the variant has no quads at all (`CanonMap.variantHasQuads`).
Verified on real library data of mcwdoors oak/spruce four-panel doors (`GhostTest`): 32 of 64 state/variant cases
had foreign quads before, 0 after.

## v6: GUI icons stay inside the inventory slot (ItemFit)
The GUI item transform copied from the original item model (or the default one) could send parts of a model outside the
slot (large or off-centre models). `PortedBakedModel.handlePerspective` now passes its result through `ItemFit.fit`: the
transformed mesh bounding box is checked against the slot square and, only if it does not fit, scaled/moved just enough.
Icons that already fit, and every non-GUI view, are returned unchanged (`GuiFitTest`).

## v7 — atypical click becomes the default toggle (`ReverseToggle`)
Built on v6 (every v3–v6 fix is unchanged; the jar differs from v6 only in `PlacementSim.class` (one added call) and the new `ReverseToggle.class`).
When a click is handled but no change was recorded and exactly one other library block turns into this state when clicked, the click is
recorded as the way back (e.g. `scpnewblocksandstairs:opened_ap_door` -> `ap_door`). Runs at the end of the capture (same hook as the placement-table refresh),
rewrites only changed block files, is idempotent and never invents a transition when the source is missing or ambiguous. Test: `tests/RTTest.java`.
