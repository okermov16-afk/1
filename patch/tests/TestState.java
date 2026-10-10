public class TestState implements net.minecraft.block.state.IBlockState {
    public final int meta; public final String tag;
    public TestState(int m, String tag) { meta = m; this.tag = tag; }
    public net.minecraft.block.Block func_177230_c() { return null; }
    public String toString() { return tag + "#" + meta; }
}
