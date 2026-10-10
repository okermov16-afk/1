package gridfree.math;

/**
 * forcen = press against the obstacle ahead + centre. When the centring step rounded a leftover sideways offset into
 * a column with nothing ahead, nothing was pressed and the block ended up a whole block to the side. In that case
 * (and only then) the block is put in the centre of its own cell and pressed once more.
 */
public final class Forcen {
    private Forcen() {
    }

    public static boolean arrange(double d, double d2, double d3, double d4, double d5, double d6,
                                  double[] ax, double[] ay, double[] az,
                                  double d7, double d8, double d9, double d10, double d11, double d12,
                                  double d13, double d14, double d15, double d16,
                                  double[][] boxes, int n, int mode, int[] hit, double[] out) {
        boolean r = Move.arrangeOrig(d, d2, d3, d4, d5, d6, ax, ay, az, d7, d8, d9, d10, d11, d12, d13, d14, d15, d16, boxes, n, mode, hit, out);
        if ((mode & 3) != 3 || hit == null || hit.length == 0 || hit[0] >= 0) {
            return r;
        }
        double a = Math.abs(d13), b = Math.abs(d14), c = Math.abs(d15);
        int w = a >= b && a >= c ? 0 : (b >= c ? 1 : 2);
        double e4 = w == 0 ? d4 : 0.0, e5 = w == 1 ? d5 : 0.0, e6 = w == 2 ? d6 : 0.0;
        if (e4 == d4 && e5 == d5 && e6 == d6) {
            return r;
        }
        double[] keepOut = out.clone();
        int keepHit = hit[0];
        boolean r2 = Move.arrangeOrig(d, d2, d3, e4, e5, e6, ax, ay, az, d7, d8, d9, d10, d11, d12, d13, d14, d15, d16, boxes, n, mode, hit, out);
        if (hit[0] >= 0) {
            return r2;
        }
        System.arraycopy(keepOut, 0, out, 0, keepOut.length);
        hit[0] = keepHit;
        return r;
    }
}
