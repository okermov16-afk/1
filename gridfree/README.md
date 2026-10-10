# gridfree-1.1-connect-v4.jar (v2 and v3 are superseded; v3 skipped a lever that was already flush)

Built from the user's original gridfree-1.1-connect.jar. Only `gridfree.math.Move.arrange` is wrapped (old body kept as `arrangeOrig`,
called through `gridfree.math.Forcen.arrange`).

Problem: `/gf forcen` centres the pressed block on the cell grid, not on what it presses against. A fence post shifted by 0.25
(`/gf info`: shift -0.250 / 0.000) left the lever 0.25 beside the post axis.
Change: after the old logic has pressed, forcen also centres the block on the pressed box (horizontal sideways axis only; block and push
axis-aligned; accepted only if the block stays pressed on the same box at the same push-axis position and does not penetrate; otherwise the old result is kept).
`for` and `cen` are untouched.

Tests: `tests/Sim2.java` (the reported scene, old: x 0.000, new: x -0.250), `tests/Reg2.java` (40000 random scenes: modes 1/2 identical,
forcen changes only to the box centre with all invariants, 0 violations).
