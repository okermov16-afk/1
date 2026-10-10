# Baseline for all further changes: modelport-1.0.0-v6.jar

* file: patch/modelport-1.0.0-v6.jar
* size: 438277 bytes
* MD5: 864e858ad48f43e0fb904915d8bff4a3
* confirmed by the player as working ("версия 6 в норме")

Rules for every following version (versions are cumulative: vN+1 = vN + one new change):
1. Patch.java keeps ALL earlier patch items; the new fix is added as one more item. Nothing is removed.
2. Before shipping, compare every class of the candidate with the previous version: only the classes changed on purpose may differ.
3. Run all suites in `patch/tests` (placement, doorbell, ghost doors, GUI fit, RandomDiff) on the candidate. Any failure = not shipped.
4. A shipped jar is never overwritten; each gets a new name, size and MD5.

Fixes contained in v6 (all must stay): placement probe (support block, onBlockPlacedBy, per-face fallback),
canon/meta translation, getStateFromMeta contract, door ghost quads (variantHasQuads), GUI icon fit (ItemFit).
