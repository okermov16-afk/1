import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class MiniMC {
    public static class St implements IBlockState {
        public final Block b; public final int facing; // 0=S 1=W 2=N 3=E
        public St(Block b, int f) { this.b = b; this.facing = f; }
        public Block func_177230_c() { return b; }
    }
    static int fromYaw(float yaw) { return (int) Math.floor(yaw * 4.0f / 360.0f + 0.5) & 3; }

    public static class Base extends Block {
        public Base(String n) { name = n; defaultState = new St(this, 2); }
        public IBlockState func_176203_a(int meta) { return new St(this, meta & 3); }
        public int func_176201_c(IBlockState s) { return ((St) s).facing; }
    }
    /** CFM-like: placement state ignores yaw, facing set in onBlockPlacedBy */
    public static class Furniture extends Base {
        public Furniture() { super("t:furniture"); }
        public IBlockState getStateForPlacement(World w, BlockPos p, EnumFacing f, float x, float y, float z, int m, EntityLivingBase pl, EnumHand h) { return defaultState; }
        public void func_180633_a(World w, BlockPos p, IBlockState s, EntityLivingBase pl, ItemStack st) { w.func_180501_a(p, new St(this, fromYaw(pl.field_70177_z)), 2); }
    }
    /** PFM-like: facing in getStateForPlacement */
    public static class Normal extends Base {
        public Normal() { super("t:normal"); }
        public IBlockState getStateForPlacement(World w, BlockPos p, EnumFacing f, float x, float y, float z, int m, EntityLivingBase pl, EnumHand h) { return new St(this, fromYaw(pl.field_70177_z)); }
    }
    /** getStateForPlacement fails in the real world (like furniture:* in the user's library) */
    public static class BrokenReal extends Base {
        public BrokenReal() { super("t:brokenreal"); }
        public IBlockState getStateForPlacement(World w, BlockPos p, EnumFacing f, float x, float y, float z, int m, EntityLivingBase pl, EnumHand h) {
            if (w instanceof net.minecraft.world.WorldServer) throw new IllegalStateException("real world");
            return new St(this, fromYaw(pl.field_70177_z));
        }
    }
    public static class Throws extends Normal {
        public void func_180633_a(World w, BlockPos p, IBlockState s, EntityLivingBase pl, ItemStack st) { throw new IllegalStateException("boom"); }
    }
    public static class Hangs extends Normal {
        public void func_180633_a(World w, BlockPos p, IBlockState s, EntityLivingBase pl, ItemStack st) { while (true) { try { Thread.sleep(1000); } catch (InterruptedException e) { } } }
    }
    public static class Removes extends Normal {
        public void func_180633_a(World w, BlockPos p, IBlockState s, EntityLivingBase pl, ItemStack st) { w.func_180501_a(p, net.minecraft.init.Blocks.field_150350_a.func_176223_P(), 2); }
    }
    public static class BadMeta extends Base {
        public BadMeta() { super("t:badmeta"); }
        public IBlockState getStateForPlacement(World w, BlockPos p, EnumFacing f, float x, float y, float z, int m, EntityLivingBase pl, EnumHand h) { return defaultState; }
        public int func_176201_c(IBlockState s) { return 99; }
    }
}
