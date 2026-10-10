# Baseline for all further changes: modelport-1.0.0-v6.jar

* file: patch/modelport-1.0.0-v6.jar
* size: 438277 bytes
* MD5: 864e858ad48f43e0fb904915d8bff4a3
* confirmed by the player as working ("версия 6 в норме")

Rules for every following version:
1. Build it from this jar (apply only the new change on top). Never rebuild from the original upload.
2. Never drop an earlier fix. Before shipping run all suites in `patch/tests` against the candidate and diff its entries against v6:
   only the classes changed on purpose may differ.
3. v6 itself is never overwritten.

Fixes contained in v6 (all must stay): placement probe (support block, onBlockPlacedBy, per-face fallback),
canon/meta translation, getStateFromMeta contract, door ghost quads (variantHasQuads), GUI icon fit (ItemFit).
