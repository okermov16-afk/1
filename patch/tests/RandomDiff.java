import java.util.*;

public class RandomDiff {
    static String rndJson(Random r, boolean identity) {
        int n = 1 + r.nextInt(10);
        List<Integer> metas = new ArrayList<>();
        for (int i = 0; i < 16; i++) metas.add(i);
        Collections.shuffle(metas, r);
        StringBuilder sb = new StringBuilder("{\"reg\":\"t:t\"");
        if (r.nextBoolean()) { int[] pl = new int[24]; for (int i = 0; i < 24; i++) pl[i] = r.nextInt(16); sb.append(",\"placement\":").append(Arrays.toString(pl)); }
        sb.append(",\"states\":[");
        for (int i = 0; i < n; i++) {
            int m = metas.get(i);
            if (i > 0) sb.append(',');
            sb.append("{\"meta\":").append(m);
            if (identity) { if (r.nextBoolean()) sb.append(",\"canon\":").append(m); }
            else sb.append(",\"canon\":").append(r.nextInt(16));
            if (r.nextInt(4) != 0) {
                sb.append(",\"beh\":{\"clickHandled\":").append(r.nextBoolean()).append(",\"clickGui\":false,\"clickMeta\":").append(r.nextInt(17) - 1)
                  .append(",\"clickThenMeta\":").append(r.nextInt(17) - 1).append(",\"clickThenTicks\":").append(r.nextInt(5))
                  .append(",\"powerMeta\":").append(r.nextInt(17) - 1).append(",\"unpowerMeta\":").append(r.nextInt(17) - 1).append(",\"clickSounds\":[]}");
            }
            sb.append("}");
        }
        return sb.append("]}").toString();
    }

    static String trace(Rig rig, Object b, String json, Random r) throws Exception {
        StringBuilder t = new StringBuilder();
        for (int m = 0; m < 16; m++) {
            if (rig.stateOf(b, m) == null) continue;
            t.append(m).append(":").append(rig.click(b, m)).append(";").append(rig.tick(b, m)).append(";")
              .append(rig.neighbor(b, m, false)).append(";").append(rig.neighbor(b, m, true)).append("\n");
        }
        Random pr = new Random(7);
        for (int i = 0; i < 40; i++) t.append(rig.place(b, pr.nextInt(16), pr.nextInt(6), pr.nextFloat() * 720 - 360)).append(',');
        for (int i = 0; i < 8; i++) t.append(rig.place(b, 0, i % 6, i * 90f)).append(',');
        return t.toString();
    }

    public static void main(String[] a) throws Exception {
        ClassLoader parent = RandomDiff.class.getClassLoader();
        Rig o = new Rig(a[0], parent), p = new Rig(a[1], parent);
        String fam = "{\"target\":\"t:t\",\"variants\":[\"t:t\",\"t:u\"],\"k\":8,\"click\":true,\"power\":false}";
        String fam2 = "{\"target\":\"t:t\",\"variants\":[\"t:t\",\"t:u\"],\"k\":8,\"click\":false,\"power\":true}";
        int N = Integer.parseInt(a[2]), diffs = 0, mism = 0, mismChanged = 0;
        Random r = new Random(12345);
        for (int i = 0; i < N; i++) {
            boolean identity = i % 3 != 2;
            String famJson = (i % 5 == 0) ? fam : (i % 7 == 0 ? fam2 : null);
            String json = rndJson(r, identity || famJson != null && i % 2 == 0);
            Object bo = o.block(json, famJson), bp = p.block(json, famJson);
            String to = trace(o, bo, json, r), tp = trace(p, bp, json, r);
            boolean mustMatch = identity || famJson != null;
            if (mustMatch && !to.equals(tp)) { diffs++; if (diffs <= 3) System.out.println("DIFF in identity/family block:\n" + json + "\n--old\n" + to + "\n--new\n" + tp); }
            if (!mustMatch) { mism++; if (!to.equals(tp)) mismChanged++; }
        }
        System.out.println("random blocks: " + N + ", identity/family blocks that must match: " + (N - mism) + ", DIFFS: " + diffs);
        System.out.println("canon!=meta blocks (allowed to change): " + mism + ", of them changed: " + mismChanged);
    }
}
