import gridfree.math.Move;
import java.lang.reflect.Method;
import java.net.*;
import java.util.*;

/** Old jar vs new jar on random scenes: every case where the old forcen pressed something must come out identical. */
public class Reg {
    static Method arr(ClassLoader cl) throws Exception {
        Class<?> m = cl.loadClass("gridfree.math.Move");
        for (Method x : m.getMethods()) if (x.getName().equals("arrange")) return x;
        throw new IllegalStateException();
    }
    public static void main(String[] a) throws Exception {
        URLClassLoader oldL = new URLClassLoader(new URL[]{new java.io.File(a[0]).toURI().toURL()}, null);
        URLClassLoader newL = new URLClassLoader(new URL[]{new java.io.File(a[1]).toURI().toURL()}, null);
        Method mo = arr(oldL), mn = arr(newL);
        Random r = new Random(7);
        int same = 0, fixed = 0, changedBad = 0, unchangedNothing = 0, total = 0;
        for (int it = 0; it < 20000; it++) {
            int mode = new int[]{1, 2, 3, 3, 3}[r.nextInt(5)];
            int w = r.nextInt(3), sgn = r.nextBoolean() ? 1 : -1;
            double[] dir = new double[3]; dir[w] = sgn;
            if (r.nextInt(4) == 0) { dir[(w + 1) % 3] = (r.nextDouble() - 0.5) * 0.6; }
            double[][] boxes;
            int nb = 1 + r.nextInt(4);
            boxes = new double[nb][];
            for (int i = 0; i < nb; i++) {
                double x = r.nextInt(5) - 2 + r.nextDouble() * 0.6, y = r.nextInt(3) - 1 + r.nextDouble() * 0.6, z = r.nextInt(5) - 2 + r.nextDouble() * 0.6;
                boxes[i] = new double[]{x, y, z, x + 0.1 + r.nextDouble() * 0.5, y + 0.1 + r.nextDouble() * 0.7, z + 0.1 + r.nextDouble() * 0.5};
            }
            double[] off = {(r.nextDouble() - 0.5) * 2.2, (r.nextDouble() - 0.5) * 1.2, (r.nextDouble() - 0.5) * 2.2};
            if (r.nextBoolean()) { off[(w + 1) % 3] = 0; off[(w + 2) % 3] = 0; }
            double hx = 0.1 + r.nextDouble() * 0.4, hy = 0.1 + r.nextDouble() * 0.4, hz = 0.1 + r.nextDouble() * 0.4;
            double cx = 0.5, cy = 0.5, cz = 0.5;
            double[] cen = {(r.nextDouble() - 0.5) * 0.6, (r.nextDouble() - 0.5) * 0.6, (r.nextDouble() - 0.5) * 0.6};
            Object[] args = {cx, cy, cz, off[0], off[1], off[2], new double[]{1, 0, 0}, new double[]{0, 1, 0}, new double[]{0, 0, 1},
                hx, hy, hz, cen[0], cen[1], cen[2], dir[0], dir[1], dir[2], 2.0, boxes, boxes.length, mode, null, null};
            int[] h1 = new int[1], h2 = new int[1]; double[] o1 = new double[4], o2 = new double[4];
            args[22] = h1; args[23] = o1;
            boolean r1 = (Boolean) mo.invoke(null, args);
            args[22] = h2; args[23] = o2;
            boolean r2 = (Boolean) mn.invoke(null, args);
            total++;
            boolean eq = r1 == r2 && Arrays.equals(o1, o2) && h1[0] == h2[0];
            if (eq) { same++; if (mode == 3 && h1[0] < 0) unchangedNothing++; continue; }
            if (mode == 3 && h1[0] < 0 && h2[0] >= 0) fixed++; else { changedBad++; if (changedBad <= 3) System.out.println("BAD mode " + mode + " " + Arrays.toString(o1) + " " + h1[0] + " -> " + Arrays.toString(o2) + " " + h2[0]); }
        }
        System.out.printf("total %d  identical %d  fixed (old pressed nothing, new presses) %d  still nothing (no obstacle reachable) %d  CHANGED WHERE OLD WORKED %d%n", total, same, fixed, unchangedNothing, changedBad);
        System.exit(changedBad == 0 ? 0 : 1);
    }
}
