public class TestState implements net.minecraft.block.state.IBlockState {
    public final int meta; public final String tag;
    public TestState(int m, String tag) { meta = m; this.tag = tag; }
    public String toString() { return tag + "#" + meta; }
}
