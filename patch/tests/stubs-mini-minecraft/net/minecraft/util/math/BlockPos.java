package net.minecraft.util.math;
public class BlockPos {
    public final int x, y, z;
    public BlockPos(int x, int y, int z) { this.x = x; this.y = y; this.z = z; }
    public int func_177958_n() { return x; }
    public int func_177956_o() { return y; }
    public int func_177952_p() { return z; }
    public BlockPos func_177972_a(net.minecraft.util.EnumFacing f) { return new BlockPos(x + f.dx(), y + f.dy(), z + f.dz()); }
    public boolean equals(Object o) { return o instanceof BlockPos && ((BlockPos) o).x == x && ((BlockPos) o).y == y && ((BlockPos) o).z == z; }
    public int hashCode() { return x * 31 * 31 + y * 31 + z; }
}
