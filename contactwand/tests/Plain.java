package tst;

import net.minecraft.util.math.BlockPos;

/** Mentions BlockPos but none of the hooked methods: must stay byte-identical. */
public class Plain {
    public int x(BlockPos p) { return p.x; }
}
