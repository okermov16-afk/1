package net.minecraft.block.properties;
import java.util.*;
import net.minecraft.util.EnumFacing;
public class PropertyDirection implements IProperty<EnumFacing> {
    private final String name; private final List<EnumFacing> allowed = new ArrayList<>();
    private PropertyDirection(String n, com.google.common.base.Predicate<EnumFacing> f) { name = n; for (EnumFacing e : EnumFacing.values()) if (f.apply(e)) allowed.add(e); }
    public static PropertyDirection func_177712_a(String n, com.google.common.base.Predicate<EnumFacing> f) { return new PropertyDirection(n, f); }
    public String func_177701_a() { return name; }
    public Collection<EnumFacing> func_177700_c() { return allowed; }
    public Class<EnumFacing> func_177699_b() { return EnumFacing.class; }
    public String func_177702_a(EnumFacing v) { return v.func_176610_l(); }
}
