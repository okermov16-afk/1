import java.lang.reflect.*;
import java.util.*;

public class PartnerTest {
    static String st(int meta, int canon, int clickMeta, String clickBlock, String facing) {
        return "{\"meta\":" + meta + ",\"canon\":" + canon + ",\"beh\":{\"clickHandled\":true,\"clickMeta\":" + clickMeta
            + (clickBlock == null ? "" : ",\"clickBlock\":\"" + clickBlock + "\"") + ",\"clickThenMeta\":-1,\"clickSounds\":[]},\"props\":{\"facing\":\"" + facing + "\"}}";
    }
    static void registry(Rig rig, Map<String, Object> m) throws Exception {
        Class<?> fr = rig.cl.loadClass("net.minecraftforge.fml.common.registry.ForgeRegistries");
        Class<?> ir = rig.cl.loadClass("net.minecraftforge.registries.IForgeRegistry");
        Object proxy = Proxy.newProxyInstance(rig.cl, new Class<?>[]{ir}, (p, me, args) -> m.get(String.valueOf(args[0])));
        fr.getField("BLOCKS").set(null, proxy);
    }
    public static void main(String[] a) throws Exception {
        ClassLoader parent = PartnerTest.class.getClassLoader();
        // A: identity, click -> partner B meta 2 (canon space == meta space for B identity)
        String A = "{\"reg\":\"t:a\",\"states\":[" + st(0, 0, 2, "t:b", "south") + "," + st(1, 1, 3, "t:b", "west") + "]}";
        String Bid = "{\"reg\":\"t:b\",\"states\":[" + st(0, 0, -1, null, "south") + "," + st(1, 1, -1, null, "west") + "," + st(2, 2, -1, null, "north") + "," + st(3, 3, -1, null, "east") + "]}";
        // B mismatching (furniture-like): canon 3 lives at meta 0, canon 5 at meta 3 ...
        String Bmm = "{\"reg\":\"t:b\",\"states\":[" + st(0, 3, -1, null, "south") + "," + st(1, 4, -1, null, "west") + "," + st(2, 2, -1, null, "north") + "," + st(3, 5, -1, null, "east") + "]}";
        for (String jar : a) {
            Rig rig = new Rig(jar, parent);
            for (String[] c : new String[][]{{"identity partner", Bid}, {"mismatching partner", Bmm}}) {
                Object ba = rig.block(A, null), bb = rig.block(c[1], null);
                Map<String, Object> m = new HashMap<>(); m.put("t:b", bb); m.put("t:a", ba);
                registry(rig, m);
                // click on A meta 0 asks B to become canon 2 ; on A meta 1 canon 3
                System.out.println(jar.substring(jar.lastIndexOf('/') + 1) + " | " + c[0] + " | click A0 -> " + rig.click(ba, 0) + " | click A1 -> " + rig.click(ba, 1));
            }
        }
    }
}
