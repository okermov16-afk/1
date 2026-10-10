package net.minecraft.block.state;
import com.google.common.collect.ImmutableList;
import java.util.*;
import net.minecraft.block.Block;
import net.minecraft.block.properties.IProperty;
public class BlockStateContainer {
    private final Block block; private final IProperty<?>[] props; private final List<State> states = new ArrayList<>();
    public static class State implements IBlockState {
        final BlockStateContainer c; final Map<IProperty<?>, Comparable<?>> vals;
        State(BlockStateContainer c, Map<IProperty<?>, Comparable<?>> v) { this.c = c; vals = v; }
        public Block func_177230_c() { return c.block; }
        @SuppressWarnings("unchecked") public <T extends Comparable<T>> T func_177229_b(IProperty<T> p) { return (T) vals.get(p); }
        public <T extends Comparable<T>, V extends T> IBlockState func_177226_a(IProperty<T> p, V v) {
            Map<IProperty<?>, Comparable<?>> m = new LinkedHashMap<>(vals); m.put(p, v);
            for (State s : c.states) if (s.vals.equals(m)) return s;
            throw new IllegalArgumentException("Cannot set property " + p.func_177701_a() + " to " + v);
        }
        public boolean func_185915_l() { return false; }
        public String toString() { StringBuilder b = new StringBuilder(); for (Map.Entry<IProperty<?>, Comparable<?>> e : vals.entrySet()) b.append(e.getKey().func_177701_a()).append('=').append(e.getValue()).append(','); return b.toString(); }
    }
    public BlockStateContainer(Block b, IProperty<?>... p) {
        block = b; props = p; build(0, new LinkedHashMap<IProperty<?>, Comparable<?>>());
    }
    private void build(int i, Map<IProperty<?>, Comparable<?>> cur) {
        if (i == props.length) { states.add(new State(this, new LinkedHashMap<>(cur))); return; }
        for (Object v : props[i].func_177700_c()) { cur.put(props[i], (Comparable<?>) v); build(i + 1, cur); }
        cur.remove(props[i]);
    }
    public IBlockState func_177621_b() { return states.get(0); }
    public ImmutableList<IBlockState> func_177619_a() { return ImmutableList.<IBlockState>copyOf(states); }
}
