package net.minecraft.init;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
public class Blocks {
    public static final Block field_150350_a = mk();
    public static final Block field_150348_b = mk();
    private static Block mk() {
        final Block b = new Block();
        b.defaultState = new IBlockState() { public Block func_177230_c() { return b; } };
        return b;
    }
}
