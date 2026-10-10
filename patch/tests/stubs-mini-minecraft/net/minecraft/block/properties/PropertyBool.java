package net.minecraft.block.properties;
import java.util.*;
public class PropertyBool implements IProperty<Boolean> {
    private final String name;
    private PropertyBool(String n) { name = n; }
    public static PropertyBool func_177716_a(String n) { return new PropertyBool(n); }
    public String func_177701_a() { return name; }
    public Collection<Boolean> func_177700_c() { return Arrays.asList(true, false); }
    public Class<Boolean> func_177699_b() { return Boolean.class; }
    public String func_177702_a(Boolean v) { return v.toString(); }
}
