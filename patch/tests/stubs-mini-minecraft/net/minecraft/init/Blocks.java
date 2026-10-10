package net.minecraft.init;
import net.minecraft.block.Block;
public class Blocks {
    public static final Block field_150350_a = mk();
    public static final Block field_150348_b = mk();
    private static Block mk() {
        final Block b = new Block();
        b.field_176227_L = new net.minecraft.block.state.BlockStateContainer(b);
        b.defaultState = b.field_176227_L.func_177621_b();
        return b;
    }
}
