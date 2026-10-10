package tst;

import contactwand.Cfg;
import contactwand.core.ContactTransformer;
import java.lang.reflect.*;
import java.nio.file.*;
import java.util.*;
import net.minecraft.block.Block;
import net.minecraft.block.BlockLiquid;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

public class Run {
    static int fails;
    static void check(boolean c, String m) { System.out.println((c ? "ok   " : "FAIL ") + m); if (!c) fails++; }

    static class St implements IBlockState {
        final Material m; final Block b; final boolean top;
        St(Material m, Block b, boolean top) { this.m = m; this.b = b; this.top = top; }
        public Material func_185904_a() { return m; }
        public Block func_177230_c() { return b; }
        public boolean func_185896_q() { return top; }
    }
    static class W extends World {
        final Map<String, IBlockState> map = new HashMap<>();
        static String k(BlockPos p) { return p.x + "," + p.y + "," + p.z; }
        public IBlockState func_180495_p(BlockPos p) { IBlockState s = map.get(k(p)); return s != null ? s : AIR; }
    }
    static final IBlockState AIR = new St(Material.field_151579_a, new Block(), false);
    static final IBlockState STONE = new St(Material.STONE, new Block(), false);
    static final IBlockState WATER = new St(Material.STONE, new BlockLiquid(), false);

    static class L extends ClassLoader {
        L() { super(Run.class.getClassLoader()); }
        Class<?> def(String n, byte[] b) { return defineClass(n, b, 0, b.length); }
    }

    public static void main(String[] a) throws Exception {
        ContactTransformer t = new ContactTransformer();
        byte[] orig = Files.readAllBytes(Paths.get(a[0], "tst/Shapes.class"));
        byte[] done = t.transform("tst.Shapes", "tst.Shapes", orig);
        check(!Arrays.equals(orig, done), "hooked class was changed");
        byte[] plain = Files.readAllBytes(Paths.get(a[0], "tst/Plain.class"));
        check(Arrays.equals(plain, t.transform("tst.Plain", "tst.Plain", plain)), "class without hooked methods is untouched byte for byte");
        byte[] lib = orig;
        check(Arrays.equals(lib, t.transform("java.util.X", "java.util.X", lib)), "java.* is skipped");
        check(t.transform("x", "x", null) == null, "null bytes are returned as is");

        Class<?> c = new L().def("tst.Shapes", done);
        Object o = c.newInstance();
        W w = new W();
        BlockPos p = new BlockPos(0, 10, 0);
        EnumFacing N = EnumFacing.NORTH, U = EnumFacing.UP;
        Method shape = c.getMethod("func_193383_a", IBlockAccess.class, IBlockState.class, BlockPos.class, EnumFacing.class);
        Method side = c.getMethod("isSideSolid", IBlockState.class, IBlockAccess.class, BlockPos.class, EnumFacing.class);
        Method torch = c.getMethod("canPlaceTorchOnTop", IBlockState.class, IBlockAccess.class, BlockPos.class);
        Method plant = c.getMethod("canSustainPlant", IBlockState.class, IBlockAccess.class, BlockPos.class, EnumFacing.class, net.minecraftforge.common.IPlantable.class);
        Method cube = c.getMethod("isNormalCube", IBlockState.class, IBlockAccess.class, BlockPos.class);
        Method onSide = c.getMethod("func_176198_a", World.class, BlockPos.class, EnumFacing.class);
        Method at = c.getMethod("func_176196_c", World.class, BlockPos.class);
        Method stay = c.getMethod("func_180671_f", World.class, BlockPos.class, IBlockState.class);
        Method tops = c.getMethod("tops", World.class, BlockPos.class);

        System.out.println("-- stone, default config");
        check(shape.invoke(o, w, STONE, p, N) == BlockFaceShape.SOLID, "face shape SOLID");
        check(shape.invoke(o, w, STONE, p, U) == BlockFaceShape.SOLID, "face shape SOLID also where the block said BOWL");
        check((Boolean) side.invoke(o, STONE, w, p, N), "isSideSolid");
        check((Boolean) torch.invoke(o, STONE, w, p), "torch on top");
        check((Boolean) plant.invoke(o, STONE, w, p, U, null), "plants");
        check(!(Boolean) cube.invoke(o, STONE, w, p), "isNormalCube stays off by default");

        System.out.println("-- air and liquids fall through to the block's own answer");
        check(shape.invoke(o, w, AIR, p, N) == BlockFaceShape.UNDEFINED, "air: UNDEFINED");
        check(shape.invoke(o, w, AIR, p, U) == BlockFaceShape.BOWL, "air: BOWL kept");
        check(!(Boolean) side.invoke(o, AIR, w, p, N), "air: not solid");
        check(shape.invoke(o, w, WATER, p, N) == BlockFaceShape.UNDEFINED, "liquid: UNDEFINED");
        check(!(Boolean) plant.invoke(o, WATER, w, p, U, null), "liquid: no plants");
        check(shape.invoke(o, w, null, p, N) == BlockFaceShape.UNDEFINED, "null state does not crash");

        System.out.println("-- placement checks");
        w.map.put(W.k(p.func_177977_b()), STONE);
        check((Boolean) at.invoke(o, w, p), "canPlaceBlockAt: solid block below");
        check((Boolean) stay.invoke(o, w, p, AIR), "canBlockStay: solid block below");
        w.map.clear();
        check(!(Boolean) at.invoke(o, w, p), "canPlaceBlockAt: nothing below -> original (false)");
        w.map.put(W.k(p.func_177977_b()), WATER);
        check(!(Boolean) at.invoke(o, w, p), "water below is not a support");
        w.map.clear();
        w.map.put(W.k(p.func_177972_a(N.func_176734_d())), STONE);
        check((Boolean) onSide.invoke(o, w, p, N), "canPlaceBlockOnSide: block behind");
        check(!(Boolean) onSide.invoke(o, w, p, U), "canPlaceBlockOnSide: nothing under (UP)");
        w.map.clear();
        w.map.put(W.k(p.func_177972_a(EnumFacing.EAST)), STONE);
        check(!(Boolean) at.invoke(o, w, p), "side neighbour is not a support unless placeAnySide");
        Cfg.placeAnySide = true;
        check((Boolean) at.invoke(o, w, p), "placeAnySide: side neighbour counts");
        Cfg.placeAnySide = false;

        System.out.println("-- isTopSolid() call site");
        w.map.put(W.k(p), STONE);
        check((Boolean) tops.invoke(o, w, p), "world.getBlockState(pos).isTopSolid() is true for a solid block");
        w.map.clear();
        check(!(Boolean) tops.invoke(o, w, p), "...and still false for air");
        w.map.put(W.k(p), new St(Material.STONE, new Block(), true));
        check((Boolean) tops.invoke(o, w, p), "...and the block's own true answer is kept");

        System.out.println("-- switches");
        Cfg.faceShape = false;
        check(shape.invoke(o, w, STONE, p, N) == BlockFaceShape.UNDEFINED, "faceShape=false -> original");
        Cfg.faceShape = true; Cfg.normalCube = true;
        check((Boolean) cube.invoke(o, STONE, w, p), "normalCube=true -> on");
        Cfg.normalCube = false; Cfg.liquids = true;
        check(shape.invoke(o, w, WATER, p, N) == BlockFaceShape.SOLID, "liquids=true -> liquid counts");
        Cfg.liquids = false; Cfg.placeChecks = false;
        w.map.put(W.k(p.func_177977_b()), STONE);
        check(!(Boolean) at.invoke(o, w, p), "placeChecks=false -> original");

        System.out.println(fails == 0 ? "ALL OK" : "FAILED " + fails);
        System.exit(fails == 0 ? 0 : 1);
    }
}
