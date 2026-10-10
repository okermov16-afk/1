import java.lang.reflect.*;
import java.util.*;
import net.minecraft.block.Block;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.util.FakePlayer;

public class PlacementTest {
    static int[] run(Rig rig, Block b) throws Exception {
        Class<?> pp = rig.cl.loadClass("com.modelport.ported.PortedPlacement");
        Method m = pp.getDeclaredMethod("probe", Block.class, WorldServer.class, BlockPos.class, FakePlayer.class);
        m.setAccessible(true);
        return (int[]) m.invoke(null, b, new WorldServer(), new BlockPos(100, 80, 100), new FakePlayer());
    }
    static String fmt(int[] t) { return t == null ? "null" : Arrays.toString(t); }
    static int[] expectedByYaw() { int[] e = new int[24]; for (int i = 0; i < 24; i++) e[i] = i % 4; return e; }

    public static void main(String[] a) throws Exception {
        ClassLoader parent = PlacementTest.class.getClassLoader();
        int fail = 0;
        Object[][] cases = {
            {"CFM-like (onBlockPlacedBy sets facing)", new MiniMC.Furniture(), "yaw", "const2"},
            {"PFM-like (getStateForPlacement sets facing)", new MiniMC.Normal(), "yaw", "yaw"},
            {"getStateForPlacement fails in real world", new MiniMC.BrokenReal(), "yaw", "null"},
            {"onBlockPlacedBy throws", new MiniMC.Throws(), "yaw", "yaw"},
            {"onBlockPlacedBy removes the block", new MiniMC.Removes(), "yaw", "yaw"},
            {"getMetaFromState out of range", new MiniMC.BadMeta(), "null", "null"},
        };
        Rig oldRig = new Rig(a[0], parent), newRig = new Rig(a[1], parent);
        for (Object[] c : cases) {
            Block b = (Block) c[1];
            int[] o = run(oldRig, b), n = run(newRig, b);
            String eo = (String) c[3], en = (String) c[2];
            boolean okOld = eo.equals("null") ? o == null : eo.equals("yaw") ? Arrays.equals(o, expectedByYaw()) : (o != null && Arrays.stream(o).allMatch(x -> x == 2));
            boolean okNew = en.equals("null") ? n == null : Arrays.equals(n, expectedByYaw());
            System.out.printf("%-46s old=%-5s new=%-5s%n", c[0], okOld ? "as-expected" : "UNEXPECTED(" + fmt(o) + ")", okNew ? "OK" : "FAIL(" + fmt(n) + ")");
            if (!okNew) fail++;
        }
        // CFM wall-mounted (BlockDoorBell/BlockLightSwitch logic): depends on the clicked face, needs a solid block behind
        {
            Block b = new MiniMC.WallMounted();
            int[] expect = new int[24];
            for (int f = 0; f < 6; f++) for (int i = 0; i < 4; i++) {
                net.minecraft.util.EnumFacing ef = net.minecraft.util.EnumFacing.values()[f];
                expect[f * 4 + i] = ef.hIndex() >= 0 ? ef.func_176734_d().hIndex() : 2;
            }
            int[] o = run(oldRig, b), n = run(newRig, b);
            boolean okOld = o != null && Arrays.stream(o).allMatch(x -> x == 2);
            boolean okNew = Arrays.equals(n, expect);
            System.out.printf("%-46s old=%-5s new=%-5s%n", "CFM wall-mounted (doorbell/switch logic)", okOld ? "const-2(bug)" : "UNEXPECTED(" + fmt(o) + ")", okNew ? "OK" : "FAIL(" + fmt(n) + ")");
            if (!okNew) fail++;
        }
        // HBTM doorbell/switch: throws for UP/DOWN, valid for the horizontal faces
        {
            Block b = new MiniMC.HbtmLike();
            int[] expect = new int[24];
            for (int f = 0; f < 6; f++) for (int i = 0; i < 4; i++) expect[f * 4 + i] = f < 2 ? 2 : (f ^ 1);
            int[] o = run(oldRig, b), n = run(newRig, b);
            boolean okNew = Arrays.equals(n, expect);
            System.out.printf("%-46s old=%-5s new=%-5s%n", "HBTM-like (throws for UP/DOWN)", o == null ? "null(bug)" : "UNEXPECTED(" + fmt(o) + ")", okNew ? "OK" : "FAIL(" + fmt(n) + ")");
            if (!okNew || o != null) fail++;
            int[] n2 = run(newRig, new MiniMC.AlwaysThrows());
            boolean ok2 = n2 == null;
            System.out.printf("%-46s new=%-5s%n", "block that refuses every face stays null", ok2 ? "OK" : "FAIL(" + fmt(n2) + ")");
            if (!ok2) fail++;
        }
        // hang: must fall back to old values within ~LIMIT, and the block must not block the rest
        long t0 = System.currentTimeMillis();
        int[] n = run(newRig, new MiniMC.Hangs());
        long dt = System.currentTimeMillis() - t0;
        boolean ok = Arrays.equals(n, expectedByYaw()) && dt < 8000;
        System.out.printf("%-46s new=%s (%d ms)%n", "onBlockPlacedBy hangs (5 s limit, falls back)", ok ? "OK" : "FAIL(" + fmt(n) + ")", dt);
        if (!ok) fail++;
        // after a hang the machinery must still work
        int[] again = run(newRig, new MiniMC.Furniture());
        ok = Arrays.equals(again, expectedByYaw());
        System.out.printf("%-46s new=%s%n", "CFM-like again after the hang", ok ? "OK" : "FAIL(" + fmt(again) + ")");
        if (!ok) fail++;
        System.out.println(fail == 0 ? "ALL OK" : "FAILURES: " + fail);
    }
}
