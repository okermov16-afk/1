import com.google.gson.*;
import java.lang.reflect.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Rig {
    public final ClassLoader cl;
    public final Class<?> bpC, pbC, mfC, worldC, facingC, entityC, posC;
    static final Gson GSON = new Gson();
    static Unsafe U;
    static sun.misc.Unsafe unsafe;
    static {
        try { Field f = sun.misc.Unsafe.class.getDeclaredField("theUnsafe"); f.setAccessible(true); unsafe = (sun.misc.Unsafe) f.get(null); } catch (Exception e) { throw new RuntimeException(e); }
    }
    interface Unsafe {}

    public Rig(String jar, ClassLoader parent) throws Exception {
        cl = new URLClassLoader(new URL[]{new java.io.File(jar).toURI().toURL()}, parent);
        bpC = cl.loadClass("com.modelport.ported.BlockPorted");
        pbC = cl.loadClass("com.modelport.ported.PortedBlock");
        mfC = cl.loadClass("com.modelport.ported.MergeFamily");
        worldC = cl.loadClass("net.minecraft.world.World");
        facingC = cl.loadClass("net.minecraft.util.EnumFacing");
        entityC = cl.loadClass("net.minecraft.entity.EntityLivingBase");
        posC = cl.loadClass("net.minecraft.util.math.BlockPos");
    }

    static void set(Object o, Class<?> c, String name, Object v) throws Exception { Field f = c.getDeclaredField(name); f.setAccessible(true); f.set(o, v); }
    static Object get(Object o, Class<?> c, String name) throws Exception { Field f = c.getDeclaredField(name); f.setAccessible(true); return f.get(o); }

    public Object block(String json, String familyJson) throws Exception {
        Object data = GSON.fromJson(json, pbC);
        Object fam = familyJson == null ? null : GSON.fromJson(familyJson, mfC);
        Object b = unsafe.allocateInstance(bpC);
        List<?> states = (List<?>) pbC.getField("states").get(data);
        Class<?> stC = cl.loadClass("com.modelport.ported.PortedBlock$State");
        Object byMeta = Array.newInstance(stC, 16);
        Object[] som = new Object[16];
        Map<Object, Integer> metaOfState = new HashMap<>();
        Map<Object, Integer> idMeta = new HashMap<>();
        int first = -1;
        for (Object s : states) {
            int m = stC.getField("meta").getInt(s);
            int canon = stC.getField("canon").getInt(s);
            Array.set(byMeta, m, s);
            TestState ts = new TestState(m, "S");
            som[m] = ts;
            metaOfState.put(ts, m);
            idMeta.put(ts, canon >= 0 ? canon : m);
            if (first < 0 || m < first) first = m;
        }
        set(b, bpC, "data", data);
        set(b, bpC, "byMeta", byMeta);
        Class<?> iprop = cl.loadClass("net.minecraft.block.properties.IProperty");
        set(b, bpC, "props", Array.newInstance(iprop, 0));
        set(b, bpC, "byName", new HashMap<>());
        set(b, bpC, "variantCache", new ConcurrentHashMap<>());
        Class<?> ibs = cl.loadClass("net.minecraft.block.state.IBlockState");
        Object arr = Array.newInstance(ibs, 16);
        for (int i = 0; i < 16; i++) Array.set(arr, i, som[i]);
        set(b, bpC, "stateOfMeta", arr);
        set(b, bpC, "metaOfState", metaOfState);
        set(b, bpC, "idMetaOfState", idMeta);
        set(b, bpC, "family", fam);
        Field ds = cl.loadClass("net.minecraft.block.Block").getDeclaredField("defaultState");
        ds.set(b, som[first]);
        return b;
    }

    public Object newWorld() throws Exception { return worldC.getDeclaredConstructor().newInstance(); }

    Method m(String name) { for (Method x : bpC.getDeclaredMethods()) if (x.getName().equals(name)) { x.setAccessible(true); return x; } throw new RuntimeException(name); }

    public Object stateOf(Object b, int meta) throws Exception {
        Object arr = get(b, bpC, "stateOfMeta");
        return Array.get(arr, meta);
    }

    @SuppressWarnings("unchecked")
    public List<String> worldSets(Object w) throws Exception {
        List<Object[]> sets = (List<Object[]>) worldC.getField("sets").get(w);
        List<String> r = new ArrayList<>();
        for (Object[] s : sets) r.add(String.valueOf(((TestState) s[0]).meta));
        return r;
    }
    @SuppressWarnings("unchecked")
    public List<Integer> worldTicks(Object w) throws Exception {
        List<Object[]> t = (List<Object[]>) worldC.getField("ticks").get(w);
        List<Integer> r = new ArrayList<>();
        for (Object[] s : t) r.add((Integer) s[0]);
        return r;
    }

    /** click: returns "ret|sets|ticks" */
    public String click(Object b, int meta) throws Exception {
        Object w = newWorld();
        try {
            Object r = m("func_180639_a").invoke(b, w, null, stateOf(b, meta), null, null, null, 0f, 0f, 0f);
            return r + "|" + worldSets(w) + "|" + worldTicks(w);
        } catch (InvocationTargetException e) { return "EX:" + e.getCause().getClass().getSimpleName(); }
    }
    public String tick(Object b, int meta) throws Exception {
        Object w = newWorld();
        try {
            m("func_180650_b").invoke(b, w, null, stateOf(b, meta), new Random(1));
            return worldSets(w).toString();
        } catch (InvocationTargetException e) { return "EX:" + e.getCause().getClass().getSimpleName(); }
    }
    public String neighbor(Object b, int meta, boolean powered) throws Exception {
        Object w = newWorld();
        worldC.getField("powered").setBoolean(w, powered);
        try {
            m("func_189540_a").invoke(b, stateOf(b, meta), w, null, null, null);
            return worldSets(w).toString();
        } catch (InvocationTargetException e) { return "EX:" + e.getCause().getClass().getSimpleName(); }
    }
    public String place(Object b, int itemMeta, int face, float yaw) throws Exception {
        Object ent = entityC.getDeclaredConstructor().newInstance();
        entityC.getField("field_70177_z").setFloat(ent, yaw);
        Object f = facingC.getDeclaredConstructor(int.class).newInstance(face);
        try {
            Object r = m("getStateForPlacement").invoke(b, null, null, f, 0f, 0f, 0f, itemMeta, ent, null);
            return String.valueOf(((TestState) r).meta);
        } catch (InvocationTargetException e) { return "EX:" + e.getCause().getClass().getSimpleName(); }
    }
}
