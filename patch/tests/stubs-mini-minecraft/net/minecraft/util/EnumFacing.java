package net.minecraft.util;
public class EnumFacing {
    public final int idx;
    private static final EnumFacing[] ALL = new EnumFacing[6];
    private static final int[] DX = {0, 0, 0, 0, -1, 1}, DY = {-1, 1, 0, 0, 0, 0}, DZ = {0, 0, -1, 1, 0, 0};
    static { for (int i = 0; i < 6; i++) ALL[i] = new EnumFacing(i); }
    public EnumFacing(int i) { idx = i; }
    public static EnumFacing[] values() { return ALL.clone(); }
    public int func_176745_a() { return idx; }
    public EnumFacing func_176734_d() { return ALL[idx ^ 1]; }
    public int dx() { return DX[idx]; }
    public int dy() { return DY[idx]; }
    public int dz() { return DZ[idx]; }
    /** DOWN0 UP1 NORTH2 SOUTH3 WEST4 EAST5 -> horizontal index S0 W1 N2 E3 (or -1) */
    public int hIndex() { switch (idx) { case 3: return 0; case 4: return 1; case 2: return 2; case 5: return 3; default: return -1; } }
}
