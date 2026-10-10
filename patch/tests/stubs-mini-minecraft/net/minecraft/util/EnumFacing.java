package net.minecraft.util;
public class EnumFacing {
    public final int idx;
    private static final EnumFacing[] ALL = new EnumFacing[6];
    static { for (int i = 0; i < 6; i++) ALL[i] = new EnumFacing(i); }
    public EnumFacing(int i) { idx = i; }
    public static EnumFacing[] values() { return ALL.clone(); }
    public int func_176745_a() { return idx; }
}
