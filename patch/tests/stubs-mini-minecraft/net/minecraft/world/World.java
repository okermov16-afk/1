package net.minecraft.world;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import java.util.*;
public class World {
    public boolean field_72995_K;
    public boolean powered;
    public List<Object[]> sets = new ArrayList<>();
    public List<Object[]> ticks = new ArrayList<>();
    public Map<BlockPos, IBlockState> blocks = new HashMap<>();
    public boolean func_180501_a(BlockPos p, IBlockState s, int f) { sets.add(new Object[]{s, f}); blocks.put(p, s); return true; }
    public void func_175684_a(BlockPos p, Block b, int t) { ticks.add(new Object[]{t}); }
    public boolean func_175640_z(BlockPos p) { return powered; }
    public IBlockState func_180495_p(BlockPos p) { IBlockState s = blocks.get(p); return s != null ? s : net.minecraft.init.Blocks.field_150350_a.func_176223_P(); }
}
