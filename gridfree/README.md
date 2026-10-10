# gridfree-1.1-connect-v2.jar

Built from the user's gridfree-1.1-connect.jar. The only change: `gridfree.math.Move.arrange` (used by `/gf forcen`, `/gf for`, `/gf cen`)
keeps its name and signature; its old body is now `arrangeOrig`, called through `gridfree.math.Forcen.arrange`.
Behaviour differs only for `forcen` (centre + press) when the old code pressed against nothing (the centring step had rounded a
leftover sideways offset into a column with no obstacle): the block is then put in the centre of its own cell and pressed once more.
Test: `tests/Reg.java` (old vs new on 20000 random scenes: 0 changes where the old code pressed something, 291 fixed), `tests/Sim.java` (the lever at a fence post).
