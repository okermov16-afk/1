import gridfree.math.Move;
import java.util.*;

public class Sim2 {
    static void run(String name, double[][] boxes, double dx) {
        double[] ax = {1, 0, 0}, ay = {0, 1, 0}, az = {0, 0, 1};
        int[] hit = new int[1]; double[] out = new double[4];
        boolean r = Move.arrange(0.5, 0.5, 1.5, dx, 0, 0, ax, ay, az, 0.1875, 0.3125, 0.1875, 0.0, 0.0, 0.1875 - 0.5,
            0, 0, -1, 2.0, boxes, boxes.length, 3, hit, out);
        System.out.printf(Locale.ROOT, "%-44s -> x %+.4f  z %+.4f  pressed=%s%n", name, out[0], out[2], hit[0] >= 0);
    }
    public static void main(String[] a) {
        run("post at cell centre (x .375-.625)", new double[][]{{0.375, 0, 0.375, 0.625, 1, 0.625}}, 0);
        run("YOUR CASE: post shifted -0.25 (x .125-.375)", new double[][]{{0.125, 0, 0.375, 0.375, 1, 0.625}}, 0);
        run("post shifted +0.25", new double[][]{{0.625, 0, 0.375, 0.875, 1, 0.625}}, 0);
        run("shifted post, lever had dx=+0.30", new double[][]{{0.125, 0, 0.375, 0.375, 1, 0.625}}, 0.30);
        run("wide wall (full block)", new double[][]{{0, 0, 0, 1, 1, 0.625}}, 0);
    }
}
