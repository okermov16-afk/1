import com.google.gson.*;
import java.lang.reflect.*;
import java.net.*;
import java.util.*;

public class RealCtor {
    static String json() {
        // furniture:doorbell exactly as listed by the user's library (meta, canon, facing, powered, clickMeta, clickThen)
        int[][] D = {
            {0, 3, 0, 0, 13, 3}, {1, 4, 1, 0, 14, 4}, {2, 2, 2, 0, 12, 2}, {3, 5, 3, 0, 15, 5}, {4, 3, 0, 0, 13, 3}, {5, 4, 1, 0, 14, 4},
            {10, 13, 0, 1, -2, 0}, {11, 14, 1, 1, -2, 0}, {12, 12, 2, 1, -2, 0}, {13, 15, 3, 1, -2, 0}};
        String[] F = {"south", "west", "north", "east"};
        StringBuilder sb = new StringBuilder("{\"reg\":\"furniture:doorbell\",\"material\":\"wood\",\"sound\":\"wood\",\"creativeHidden\":true,\"placement\":[2,2,2,2,2,2,2,2,3,3,3,3,2,2,2,2,5,5,5,5,4,4,4,4],\"states\":[");
        for (int i = 0; i < D.length; i++) {
            int[] r = D[i];
            if (i > 0) sb.append(',');
            sb.append("{\"meta\":").append(r[0]).append(",\"canon\":").append(r[1]);
            if (r[4] != -2) sb.append(",\"beh\":{\"clickHandled\":true,\"clickMeta\":").append(r[4]).append(",\"clickThenMeta\":").append(r[5]).append(",\"clickThenTicks\":15,\"clickSounds\":[]}");
            sb.append(",\"props\":{\"facing\":\"").append(F[r[2]]).append("\",\"powered\":\"").append(r[3] == 1).append("\"}}");
        }
        return sb.append("]}").toString();
    }

    public static void main(String[] a) throws Exception {
        ClassLoader parent = RealCtor.class.getClassLoader();
        for (String jar : a) {
            ClassLoader cl = new URLClassLoader(new URL[]{new java.io.File(jar).toURI().toURL()}, parent);
            Class<?> bpC = cl.loadClass("com.modelport.ported.BlockPorted"), pbC = cl.loadClass("com.modelport.ported.PortedBlock");
            Object data = new Gson().fromJson(json(), pbC);
            Object b = bpC.getConstructor(pbC, Map.class).newInstance(data, new HashMap<String, Object>());
            System.out.println("==== " + jar.substring(jar.lastIndexOf('/') + 1));
            Class<?> blockC = cl.loadClass("net.minecraft.block.Block");
            Method metaOfM = bpC.getMethod("metaOf", cl.loadClass("net.minecraft.block.state.IBlockState"));
            Method getMeta = bpC.getMethod("func_176201_c", cl.loadClass("net.minecraft.block.state.IBlockState"));
            Method fromMeta = bpC.getMethod("func_176203_a", int.class);
            Object container = blockC.getField("field_176227_L").get(b);
            List<?> valid = (List<?>) container.getClass().getMethod("func_177619_a").invoke(container);
            Set<Integer> ids = new TreeSet<>();
            System.out.println("valid states: " + valid.size());
            for (Object st : valid) {
                int meta = (Integer) metaOfM.invoke(b, st), canon = (Integer) getMeta.invoke(b, st);
                Object back = fromMeta.invoke(b, canon);
                ids.add(canon);
                System.out.printf("  %-30s metaOf=%-2d getMetaFromState(canon)=%-2d  getStateFromMeta(canon)=%s%s%n", st, meta, canon, back, back == st ? "" : "   <-- NOT the same state");
            }
            System.out.println("distinct ids: " + ids.size() + " of " + valid.size());
            // full click -> tick flow on the real states
            Class<?> worldC = cl.loadClass("net.minecraft.world.World"), stC = cl.loadClass("net.minecraft.block.state.IBlockState");
            Method click = null, tick = null;
            for (Method m : bpC.getDeclaredMethods()) { if (m.getName().equals("func_180639_a")) click = m; if (m.getName().equals("func_180650_b")) tick = m; }
            click.setAccessible(true); tick.setAccessible(true);
            int bad = 0;
            for (Object st : valid) {
                if (st.toString().contains("powered=true")) continue;
                Object w = worldC.getDeclaredConstructor().newInstance();
                click.invoke(b, w, null, st, null, null, null, 0f, 0f, 0f);
                List<?> sets = (List<?>) worldC.getField("sets").get(w);
                Object pressed = sets.isEmpty() ? null : ((Object[]) sets.get(sets.size() - 1))[0];
                Object w2 = worldC.getDeclaredConstructor().newInstance();
                String back = "-";
                if (pressed != null) { tick.invoke(b, w2, null, pressed, new Random(1)); List<?> s2 = (List<?>) worldC.getField("sets").get(w2); back = s2.isEmpty() ? "-" : String.valueOf(((Object[]) s2.get(s2.size() - 1))[0]); }
                String face = st.toString().split(",")[0];
                boolean ok = pressed != null && String.valueOf(pressed).startsWith(face) && String.valueOf(pressed).contains("powered=true") && back.equals(st.toString());
                if (!ok) bad++;
                System.out.printf("  click %-18s -> %-34s tick -> %-34s %s%n", face, pressed, back, ok ? "OK" : "FAIL");
            }
            System.out.println("click/tick failures: " + bad);
        }
    }
}
