package gridfree.math;

/**
 * forcen = press against the obstacle ahead + centre. The old centring step rounds the sideways offset to a whole block,
 * i.e. it centres on the cell grid. An obstacle that itself sits off-centre in its cell (a fence post shifted by 0.25)
 * left the pressed block beside its axis. After the old logic has pressed, the block is now also centred on the
 * pressed box (horizontal sideways axis only, block and push axis-aligned).
 */
public final class Forcen {
    private Forcen() {
    }

    private static boolean unit(double[] v, int i) {
        return Math.abs(v[i] - 1.0) < 1.0E-9 && Math.abs(v[(i + 1) % 3]) < 1.0E-9 && Math.abs(v[(i + 2) % 3]) < 1.0E-9;
    }

    public static boolean arrange(double d, double d2, double d3, double d4, double d5, double d6,
                                  double[] ax, double[] ay, double[] az,
                                  double d7, double d8, double d9, double d10, double d11, double d12,
                                  double d13, double d14, double d15, double d16,
                                  double[][] boxes, int n, int mode, int[] hit, double[] out) {
        boolean r = Move.arrangeOrig(d, d2, d3, d4, d5, d6, ax, ay, az, d7, d8, d9, d10, d11, d12, d13, d14, d15, d16, boxes, n, mode, hit, out);
        if ((mode & 3) != 3 || hit == null || hit.length == 0 || hit[0] < 0 || hit[0] >= n || boxes == null) {
            return r;
        }
        if (!unit(ax, 0) || !unit(ay, 1) || !unit(az, 2)) {
            return r;
        }
        double[] dir = {d13, d14, d15};
        int w = -1;
        for (int i = 0; i < 3; i++) {
            if (Math.abs(dir[i]) > 1.0E-9 && Math.abs(dir[(i + 1) % 3]) < 1.0E-9 && Math.abs(dir[(i + 2) % 3]) < 1.0E-9) {
                w = i;
            }
        }
        if (w < 0 || w == 1) {
            return r;
        }
        int a = w == 0 ? 2 : 0;
        double[] box = boxes[hit[0]];
        double mid = (box[a] + box[a + 3]) * 0.5;
        double cell = a == 0 ? d : d3;
        double shape = a == 0 ? d10 : d12;
        double want = (float) (mid - cell - shape);
        if (Math.abs(want - out[a]) < 1.0E-6) {
            return r;
        }
        double[] keepOut = out.clone();
        int keepHit = hit[0];
        double[] p = {out[0], out[1], out[2]};
        p[a] = want;
        int[] hit2 = new int[hit.length];
        double[] out2 = new double[out.length];
        Move.arrangeOrig(d, d2, d3, p[0], p[1], p[2], ax, ay, az, d7, d8, d9, d10, d11, d12, d13, d14, d15, d16, boxes, n, 2, hit2, out2);
        if (hit2[0] == keepHit && Math.abs(out2[w] - keepOut[w]) < 1.0E-6 && Math.abs(out2[a] - want) < 1.0E-6
            && Move.penetration(d + out2[0] + d10, d2 + out2[1] + d11, d3 + out2[2] + d12, ax, ay, az, d7, d8, d9, boxes, n) <= 1.0E-6) {
            System.arraycopy(out2, 0, out, 0, out.length);
            hit[0] = hit2[0];
            return true;
        }
        System.arraycopy(keepOut, 0, out, 0, keepOut.length);
        hit[0] = keepHit;
        return r;
    }
}
