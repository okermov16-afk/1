package contactwand.core;

import contactwand.Cfg;
import net.minecraft.block.BlockLiquid;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.fluids.IFluidBlock;

/**
 * Called from the injected prologues of the block methods. Stateless: no store, no lookups where the state is
 * already at hand, nothing runs unless the game itself asks the question.
 */
public final class Hooks {
    private Hooks() {
    }

    /** A block counts as contactable unless it is air (or a liquid while Cfg.liquids is off). */
    private static boolean solid(IBlockState s) {
        if (s == null || s.func_185904_a() == Material.field_151579_a) {
            return false;
        }
        if (Cfg.liquids) {
            return true;
        }
        Object b = s.func_177230_c();
        return !(b instanceof BlockLiquid) && !(b instanceof IFluidBlock);
    }

    private static boolean at(IBlockAccess a, BlockPos p) {
        try {
            return a != null && p != null && solid(a.func_180495_p(p));
        } catch (Throwable t) {
            return false;
        }
    }

    public static boolean faceShape(IBlockState s) {
        return Cfg.faceShape && solid(s);
    }

    public static boolean sideSolid(IBlockState s) {
        return Cfg.sideSolid && solid(s);
    }

    public static boolean plantSoil(IBlockState s) {
        return Cfg.plants && solid(s);
    }

    public static boolean normalCube(IBlockState s) {
        return Cfg.normalCube && solid(s);
    }

    public static boolean topSolid(IBlockAccess a, BlockPos p, IBlockState s) {
        if (Cfg.sideSolid && solid(s)) {
            return true;
        }
        return s != null && s.func_185896_q();
    }

    public static boolean supportSide(IBlockAccess a, BlockPos p, EnumFacing side) {
        if (!Cfg.placeChecks || p == null || side == null) {
            return false;
        }
        return at(a, p.func_177972_a(side.func_176734_d()));
    }

    public static boolean supportAround(IBlockAccess a, BlockPos p) {
        if (!Cfg.placeChecks || a == null || p == null) {
            return false;
        }
        if (at(a, p.func_177977_b())) {
            return true;
        }
        if (!Cfg.placeAnySide) {
            return false;
        }
        EnumFacing[] all = EnumFacing.field_82609_l;
        for (int i = 0; i < all.length; i++) {
            if (all[i] != EnumFacing.DOWN && at(a, p.func_177972_a(all[i]))) {
                return true;
            }
        }
        return false;
    }
}
