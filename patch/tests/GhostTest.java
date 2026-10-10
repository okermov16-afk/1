import java.lang.reflect.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;
import com.google.gson.*;

public class GhostTest {
    static sun.misc.Unsafe U;
    static { try { Field f = sun.misc.Unsafe.class.getDeclaredField("theUnsafe"); f.setAccessible(true); U = (sun.misc.Unsafe) f.get(null); } catch (Exception e) { throw new RuntimeException(e); } }
    static Object get(Object o, String n) throws Exception { Field f = o.getClass().getDeclaredField(n); f.setAccessible(true); return f.get(o); }
    static void set(Object o, String n, Object v) throws Exception { Field f = o.getClass().getDeclaredField(n); f.setAccessible(true); f.set(o, v); }

    public static void main(String[] a) throws Exception {
        String jar = a[0], dir = a[1], blockJson = a[2];
        ClassLoader parent = GhostTest.class.getClassLoader();
        URLClassLoader cl = new URLClassLoader(new URL[]{new java.io.File(jar).toURI().toURL(), new java.io.File("th3/fake/").toURI().toURL()}, parent);
        Class<?> pbmC = cl.loadClass("com.modelport.ported.PortedBakedModel"), qdC = cl.loadClass("com.modelport.ported.QuadData"), bqC = cl.loadClass("net.minecraft.client.renderer.block.model.BakedQuad");
        Class<?> fakeC = cl.loadClass("FakePorted"), stateC = cl.loadClass("net.minecraft.block.state.IBlockState"), facingC = cl.loadClass("net.minecraft.util.EnumFacing"), layerC = cl.loadClass("net.minecraft.util.BlockRenderLayer");
        Class<?> mfc = cl.loadClass("net.minecraftforge.client.MinecraftForgeClient");
        mfc.getField("layer").set(null, Enum.valueOf((Class) layerC, "CUTOUT"));
        JsonObject j = JsonParser.parseString(new String(Files.readAllBytes(Paths.get(blockJson)), "UTF-8").replace("﻿", "")).getAsJsonObject();
        Object model = U.allocateInstance(pbmC);
        Map<Integer, List<Object>> quads = new HashMap<>();
        set(model, "root", model); set(model, "quads", quads); set(model, "itemQuads", new HashMap<>()); set(model, "fixedMeta", 0);
        Map<Object, String> tag = new IdentityHashMap<>();
        Method from = qdC.getMethod("fromBytes", byte[].class);
        int totalQuads = 0;
        for (JsonElement se : j.getAsJsonArray("states")) {
            JsonObject st = se.getAsJsonObject(); int meta = st.get("meta").getAsInt();
            List<String> meshes = new ArrayList<>(); meshes.add(st.get("mesh").getAsString());
            if (st.has("variants")) for (JsonElement v : st.getAsJsonArray("variants")) meshes.add(v.getAsJsonObject().get("mesh").getAsString());
            for (int variant = 0; variant < meshes.size(); variant++) {
                Object qd = from.invoke(null, (Object) Files.readAllBytes(Paths.get(dir, meshes.get(variant))));
                for (Object q : (List<?>) qdC.getField("quads").get(qd)) {
                    Class<?> qc = q.getClass();
                    int layer = qc.getField("layer").getInt(q), side = qc.getField("side").getInt(q);
                    Object bq = bqC.getDeclaredConstructor().newInstance();
                    String t = "meta" + meta + "/v" + variant + " side" + side;
                    bqC.getField("tag").set(bq, t); tag.put(bq, t); totalQuads++;
                    int n5 = side >= 0 && side < 6 ? side : 6, n6 = (variant * 16 + meta) * 64;
                    quads.computeIfAbsent(n6 + layer * 8 + n5, k -> new ArrayList<>()).add(bq);
                    quads.computeIfAbsent(n6 + 32 + n5, k -> new ArrayList<>()).add(bq);
                }
            }
        }
        Object fake = fakeC.getDeclaredConstructor().newInstance();
        Object state = java.lang.reflect.Proxy.newProxyInstance(parent, new Class<?>[]{stateC}, (p, m, args) -> m.getName().equals("func_177230_c") ? fake : null);
        Method getQuads = pbmC.getMethod("func_188616_a", stateC, facingC, long.class);
        Object[] faces = new Object[7]; Object[] all = (Object[]) facingC.getMethod("values").invoke(null);
        for (int i = 0; i < 6; i++) faces[i] = all[i];
        int ghosts = 0, cases = 0;
        for (JsonElement se : j.getAsJsonArray("states")) {
            JsonObject st = se.getAsJsonObject(); int meta = st.get("meta").getAsInt();
            int nv = 1 + (st.has("variants") ? st.getAsJsonArray("variants").size() : 0);
            for (int variant = 0; variant < nv; variant++) {
                fakeC.getField("meta").setInt(null, meta); fakeC.getField("variant").setInt(null, variant);
                List<String> foreign = new ArrayList<>(); int own = 0;
                for (Object face : faces) for (Object q : (List<?>) getQuads.invoke(model, state, face, 0L)) {
                    String t = tag.get(q);
                    if (t.startsWith("meta" + meta + "/v" + variant + " ")) own++; else foreign.add(t);
                }
                cases++; ghosts += foreign.isEmpty() ? 0 : 1;
                if (!foreign.isEmpty() && st.get("meta").getAsInt() == 8 || (!foreign.isEmpty() && meta == 0))
                    System.out.printf("  meta %-2d variant %d: own quads %d, FOREIGN: %s%n", meta, variant, own, foreign);
            }
        }
        System.out.printf("%s: %d state/variant cases, with foreign (ghost) quads: %d%n", jar.substring(jar.lastIndexOf('/') + 1), cases, ghosts);
    }
}
