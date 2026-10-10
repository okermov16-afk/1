package net.minecraft.util;
public class EnumFacing implements Comparable<EnumFacing> {
    public final int idx;
    private static final EnumFacing[] ALL = new EnumFacing[6];
    private static final String[] NAMES = {"down", "up", "north", "south", "west", "east"};
    private static final int[] DX = {0, 0, 0, 0, -1, 1}, DY = {-1, 1, 0, 0, 0, 0}, DZ = {0, 0, -1, 1, 0, 0};
    static { for (int i = 0; i < 6; i++) ALL[i] = new EnumFacing(i); }
    public EnumFacing(int i) { idx = i; }
    public static EnumFacing[] values() { return ALL.clone(); }
    public static EnumFacing func_176739_a(String n) { for (int i = 0; i < 6; i++) if (NAMES[i].equals(n)) return ALL[i]; return null; }
    public String func_176610_l() { return NAMES[idx]; }
    public int func_176745_a() { return idx; }
    public EnumFacing func_176734_d() { return ALL[idx ^ 1]; }
    public int dx() { return DX[idx]; }
    public int dy() { return DY[idx]; }
    public int dz() { return DZ[idx]; }
    public int hIndex() { switch (idx) { case 3: return 0; case 4: return 1; case 2: return 2; case 5: return 3; default: return -1; } }
    public int compareTo(EnumFacing o) { return idx - o.idx; }
    public String toString() { return NAMES[idx]; }
}
