import gridfree.math.Move;
import java.util.*;

public class Sim3 {
    public static void main(String[] a) {
        double[][] post = {{0.125, 0, 0.375, 0.375, 1, 0.625}};   // post shifted -0.25
        double[] ax = {1, 0, 0}, ay = {0, 1, 0}, az = {0, 0, 1};
        // the lever as it stands now: x 0, z -0.375 (already flush), pressing forcen again with axis -z
        int[] hit = {99}; double[] out = new double[4];
        boolean r = Move.arrange(0.5, 0.5, 1.5, 0, 0, -0.375, ax, ay, az, 0.1875, 0.3125, 0.1875, 0, 0, 0.1875 - 0.5,
            0, 0, -1, 2.0, post, 1, 3, hit, out);
        System.out.printf(Locale.ROOT, "lever already flush, forcen again -> returned %s, hit=%d, out x %+.4f z %+.4f%n", r, hit[0], out[0], out[2]);
    }
}
