import gridfree.math.Move;
import java.lang.reflect.Method;
import java.net.*;
import java.util.*;

public class Reg2 {
    static Method arr(ClassLoader cl) throws Exception {
        for (Method x : cl.loadClass("gridfree.math.Move").getMethods()) if (x.getName().equals("arrange")) return x;
        throw new IllegalStateException();
    }
    public static void main(String[] a) throws Exception {
        URLClassLoader oldL = new URLClassLoader(new URL[]{new java.io.File(a[0]).toURI().toURL()}, null);
        Method mo = arr(oldL), mn = arr(Reg2.class.getClassLoader());
        Random r = new Random(11);
        int total = 0, identical = 0, centredSame = 0, changedOk = 0, bad = 0, notMode3Changed = 0;
        for (int it = 0; it < 40000; it++) {
            int mode = new int[]{1, 2, 3, 3, 3, 3}[r.nextInt(6)];
            int w = new int[]{0, 2, 1}[r.nextInt(3)], sgn = r.nextBoolean() ? 1 : -1;
            double[] dir = new double[3]; dir[w] = sgn;
            if (r.nextInt(5) == 0) dir[(w + 1) % 3] = (r.nextDouble() - 0.5) * 0.6;
            int nb = 1 + r.nextInt(4);
            double[][] boxes = new double[nb][];
            for (int i = 0; i < nb; i++) {
                double x = r.nextInt(5) - 2 + r.nextDouble() * 0.6, y = r.nextInt(3) - 1 + r.nextDouble() * 0.6, z = r.nextInt(5) - 2 + r.nextDouble() * 0.6;
                if (r.nextInt(3) == 0) { x = Math.floor(x) + 0.375; z = Math.floor(z) + 0.375; boxes[i] = new double[]{x, y, z, x + 0.25, y + 1, z + 0.25}; }
                else boxes[i] = new double[]{x, y, z, x + 0.1 + r.nextDouble() * 0.5, y + 0.1 + r.nextDouble() * 0.7, z + 0.1 + r.nextDouble() * 0.5};
            }
            double[] off = {(r.nextDouble() - 0.5) * 1.0, (r.nextDouble() - 0.5) * 1.0, (r.nextDouble() - 0.5) * 1.0};
            double hx = 0.1 + r.nextDouble() * 0.3, hy = 0.1 + r.nextDouble() * 0.3, hz = 0.1 + r.nextDouble() * 0.3;
            double[] cen = {(r.nextDouble() - 0.5) * 0.5, (r.nextDouble() - 0.5) * 0.5, (r.nextDouble() - 0.5) * 0.5};
            double[] ux = {1, 0, 0}, uy = {0, 1, 0}, uz = {0, 0, 1};
            Object[] args = {0.5, 0.5, 1.5, off[0], off[1], off[2], ux, uy, uz, hx, hy, hz, cen[0], cen[1], cen[2], dir[0], dir[1], dir[2], 2.0, boxes, boxes.length, mode, null, null};
            int[] h1 = new int[1], h2 = new int[1]; double[] o1 = new double[4], o2 = new double[4];
            args[22] = h1; args[23] = o1; boolean r1 = (Boolean) mo.invoke(null, args);
            args[22] = h2; args[23] = o2; boolean r2 = (Boolean) mn.invoke(null, args);
            total++;
            if (r1 == r2 && Arrays.equals(o1, o2) && h1[0] == h2[0]) { identical++; continue; }
            if (mode != 3) { notMode3Changed++; continue; }
            // a change must keep the press, keep the push-axis position, put the block on the box centre, and not penetrate
            boolean ok = h1[0] >= 0 && h2[0] >= 0 && r2 && Math.abs(o1[w] - o2[w]) < 1e-6;
            int la = w == 0 ? 2 : 0;
            double[] b = boxes[h2[0]];
            double want = (float) ((b[la] + b[la + 3]) * 0.5 - (la == 0 ? 0.5 : 1.5) - cen[la]);
            ok = ok && Math.abs(o2[la] - want) < 1e-5 && w != 1;
            double pen = Move.penetration(0.5 + o2[0] + cen[0], 0.5 + o2[1] + cen[1], 1.5 + o2[2] + cen[2], ux, uy, uz, hx, hy, hz, boxes, boxes.length);
            ok = ok && pen <= 1e-6;
            if (ok) changedOk++; else { bad++; if (bad <= 3) System.out.println("BAD " + Arrays.toString(o1) + h1[0] + " -> " + Arrays.toString(o2) + h2[0] + " w=" + w); }
        }
        System.out.printf("total %d | identical %d | changed in modes 1/2 %d (must be 0) | forcen changed, all invariants hold %d | forcen changed, INVARIANT BROKEN %d%n", total, identical, notMode3Changed, changedOk, bad);
        System.exit(bad == 0 && notMode3Changed == 0 ? 0 : 1);
    }
}
