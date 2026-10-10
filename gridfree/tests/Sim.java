import gridfree.math.Move;
import java.util.*;

public class Sim {
    static String fmt(double[] a) { return String.format(Locale.ROOT, "%+.4f %+.4f %+.4f (dist %.4f)", a[0], a[1], a[2], a[3]); }

    /** lever in the cell south of the fence, attached to its north wall (vanilla LEVER_SOUTH_AABB). */
    static void run(String name, double dx, double dy, double dz, double[] dir, int mode, double[][] boxes) {
        double[] ax = {1, 0, 0}, ay = {0, 1, 0}, az = {0, 0, 1};
        double hx = (0.6875 - 0.3125) / 2, hy = (0.8125 - 0.1875) / 2, hz = 0.375 / 2;
        double ox = 0.0, oy = 0.0, oz = (0.0 + 0.375) / 2 - 0.5;
        int[] hit = new int[1];
        double[] out = new double[4];
        boolean r = Move.arrange(0.5, 0.5, 1.5, dx, dy, dz, ax, ay, az, hx, hy, hz, ox, oy, oz,
            dir[0], dir[1], dir[2], 2.0, boxes, boxes.length, mode, hit, out);
        System.out.printf(Locale.ROOT, "%-46s mode %d -> moved=%s  offset %s hit=%d%n", name, mode, r, fmt(out), hit[0]);
    }

    public static void main(String[] a) {
        // fence cell (0,0,0): vanilla post, clipped to the cell by MoveMode.collect (z of the lever cell is 1)
        double[][] post = {{0.375, 0.0, 0.375, 0.625, 1.0, 0.625}};
        double[] mz = {0, 0, -1};
        for (int mode : new int[]{2, 3}) {
            run("clean lever, axis -z", 0, 0, 0, mz, mode, post);
            run("lever already dx=+0.30", 0.30, 0, 0, mz, mode, post);
            run("lever already dx=+0.60", 0.60, 0, 0, mz, mode, post);
            run("lever already dx=-0.45", -0.45, 0, 0, mz, mode, post);
            run("lever already dy=+0.40", 0, 0.40, 0, mz, mode, post);
            run("lever already dz=-0.20", 0, 0, -0.20, mz, mode, post);
        }
        // direction by view: 20 degrees off axis
        double s = Math.sin(Math.toRadians(20)), c = Math.cos(Math.toRadians(20));
        run("view 20 deg off axis (no axis given)", 0, 0, 0, new double[]{s, 0, -c}, 3, post);
        run("view 20 deg off axis + pitch 15", 0, 0, 0, new double[]{s, 0.26, -c}, 3, post);
    }
}
