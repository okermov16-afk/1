import com.modelport.ported.*;
import java.util.*;

public class RTTest {
    static int fails;
    static void check(boolean c, String m) { if (!c) { fails++; System.out.println("FAIL " + m); } else System.out.println("ok   " + m); }

    static PortedBlock blk(String reg, int[] metas) {
        PortedBlock b = new PortedBlock();
        b.reg = reg;
        for (int m : metas) { PortedBlock.State s = new PortedBlock.State(); s.meta = m; s.canon = m; b.states.add(s); }
        return b;
    }
    static void click(PortedBlock b, int meta, boolean handled, int cm, String cb) {
        for (PortedBlock.State s : b.states) if (s.meta == meta) {
            PortedBlock.Behavior h = new PortedBlock.Behavior();
            h.clickHandled = handled; h.clickMeta = cm; h.clickBlock = cb; s.beh = h;
        }
    }
    static PortedBlock.Behavior beh(PortedBlock b, int meta) { for (PortedBlock.State s : b.states) if (s.meta == meta) return s.beh; return null; }

    public static void main(String[] a) {
        String C = "scpnewblocksandstairs:ap_door", O = "scpnewblocksandstairs:opened_ap_door";
        PortedBlock closed = blk(C, new int[]{2,3,4,5}), open = blk(O, new int[]{2,3,4,5});
        for (int m = 2; m <= 5; m++) { click(closed, m, true, m, O); click(open, m, true, -1, null); }
        Set<String> ch = ReverseToggle.complete(Arrays.asList(closed, open));
        check(ch.size() == 1 && ch.contains(O), "only opened door changed: " + ch);
        for (int m = 2; m <= 5; m++) check(C.equals(beh(open, m).clickBlock) && beh(open, m).clickMeta == m, "opened meta " + m + " -> closed meta " + m);
        check(O.equals(beh(closed, 2).clickBlock) && beh(closed, 2).clickMeta == 2, "closed door untouched");
        check(ReverseToggle.complete(Arrays.asList(closed, open)).isEmpty(), "idempotent");

        // one-way: nothing turns into X -> X untouched
        PortedBlock x = blk("m:x", new int[]{0}); click(x, 0, true, -1, null);
        PortedBlock y = blk("m:y", new int[]{0}); click(y, 0, true, -1, null);
        check(ReverseToggle.complete(Arrays.asList(x, y)).isEmpty(), "no inverse -> nothing invented");

        // ambiguous: two different blocks turn into X
        PortedBlock s1 = blk("m:s1", new int[]{0}), s2 = blk("m:s2", new int[]{0});
        click(s1, 0, true, 0, "m:x"); click(s2, 0, true, 0, "m:x"); click(x, 0, true, -1, null);
        check(ReverseToggle.complete(Arrays.asList(x, s1, s2)).isEmpty() && beh(x, 0).clickBlock == null, "ambiguous sources -> untouched");

        // not handled / gui / state with real result stay as they are
        PortedBlock z = blk("m:z", new int[]{0, 1}); click(z, 0, false, -1, null); click(z, 1, true, 0, "m:s1");
        PortedBlock src = blk("m:src", new int[]{0}); click(src, 0, true, 0, "m:z");
        check(ReverseToggle.complete(Arrays.asList(z, src, s1)).isEmpty(), "unhandled click and already-recorded click untouched");
        PortedBlock g = blk("m:g", new int[]{0}); click(g, 0, true, -1, null); g.states.get(0).beh.clickGui = true;
        PortedBlock gs = blk("m:gs", new int[]{0}); click(gs, 0, true, 0, "m:g");
        check(ReverseToggle.complete(Arrays.asList(g, gs)).isEmpty(), "gui click untouched");
        // meta mismatch: source targets another meta of X
        PortedBlock p = blk("m:p", new int[]{0, 1}); click(p, 0, true, -1, null); click(p, 1, true, -1, null);
        PortedBlock q = blk("m:q", new int[]{0}); click(q, 0, true, 1, "m:p");
        Set<String> r = ReverseToggle.complete(Arrays.asList(p, q));
        check(r.size() == 1 && beh(p, 1).clickBlock.equals("m:q") && beh(p, 0).clickBlock == null, "only the meta that is a click target is completed");
        System.out.println(fails == 0 ? "ALL OK" : "FAILED " + fails);
        System.exit(fails == 0 ? 0 : 1);
    }
}
