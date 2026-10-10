# contactwand 1.1 - contact for every block, no wand

Rebuilt from the user's contactwand 1.0 (kept: the core plugin that prepends hooks to the block methods).
Removed: the wand item, mark store and world data, network packets, events, GUI, renderer, snap (it only worked together with marks).
The hooks no longer look anything up in a store: for the methods that receive the block state the answer is read from that state
(not air, not a liquid); the placement checks look at the neighbour state once. Nothing is polled by a timer.

Config: `config/contactwand.cfg`, category `hooks` (the old keys are kept; new key `liquids`, default false).
Test: `tests/Run.java` (transforms real classes, loads them with -Xverify:all, calls the hooked methods).
