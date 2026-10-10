package tst;

import net.minecraft.block.Block;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.IPlantable;

/** Looks like a vanilla block that refuses everything: field names/signatures as in the game. */
public class Shapes extends Block {
    public BlockFaceShape func_193383_a(IBlockAccess w, IBlockState s, BlockPos p, EnumFacing f) {
        if (f == EnumFacing.UP) {
            return BlockFaceShape.BOWL;
        }
        return BlockFaceShape.UNDEFINED;
    }
    public boolean isSideSolid(IBlockState s, IBlockAccess w, BlockPos p, EnumFacing f) { return false; }
    public boolean canPlaceTorchOnTop(IBlockState s, IBlockAccess w, BlockPos p) { return false; }
    public boolean canSustainPlant(IBlockState s, IBlockAccess w, BlockPos p, EnumFacing f, IPlantable pl) { return false; }
    public boolean isNormalCube(IBlockState s, IBlockAccess w, BlockPos p) { return false; }
    public boolean func_176198_a(World w, BlockPos p, EnumFacing f) { return false; }
    public boolean func_176196_c(World w, BlockPos p) { return false; }
    public boolean func_180671_f(World w, BlockPos p, IBlockState s) { return false; }
    /** vanilla pattern: world.getBlockState(pos).isTopSolid() */
    public boolean tops(World w, BlockPos p) {
        return w.func_180495_p(p).func_185896_q();
    }
}
