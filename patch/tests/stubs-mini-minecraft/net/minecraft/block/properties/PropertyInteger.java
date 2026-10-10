package net.minecraft.block.properties;
import java.util.*;
public class PropertyInteger implements IProperty<Integer> {
    private final String name; private final List<Integer> vals = new ArrayList<>();
    private PropertyInteger(String n, int a, int b) { name = n; for (int i = a; i <= b; i++) vals.add(i); }
    public static PropertyInteger func_177719_a(String n, int a, int b) { return new PropertyInteger(n, a, b); }
    public String func_177701_a() { return name; }
    public Collection<Integer> func_177700_c() { return vals; }
    public Class<Integer> func_177699_b() { return Integer.class; }
    public String func_177702_a(Integer v) { return v.toString(); }
}
