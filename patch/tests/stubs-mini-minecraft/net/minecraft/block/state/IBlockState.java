package net.minecraft.block.state;
import net.minecraft.block.properties.IProperty;
public interface IBlockState {
    net.minecraft.block.Block func_177230_c();
    <T extends Comparable<T>> T func_177229_b(IProperty<T> p);
    <T extends Comparable<T>, V extends T> IBlockState func_177226_a(IProperty<T> p, V v);
    boolean func_185915_l();
}
