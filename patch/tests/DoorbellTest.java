import java.util.*;

public class DoorbellTest {
    // meta, canon, facing, powered, clickMeta, clickHandled, thenMeta, thenTicks, powerMeta, unpowerMeta   (taken from the user's furniture:doorbell JSON)
    static final int[][] D = {
        {0, 3, 0, 0, 13, 1, 3, 15, 3, 3},
        {1, 4, 1, 0, 14, 1, 4, 15, 4, 4},
        {2, 2, 2, 0, 12, 1, 2, 15, -1, -1},
        {3, 5, 3, 0, 15, 1, 5, 15, 5, 5},
        {5, 4, 1, 0, 14, 1, 4, 15, 4, 4},
        {10, 13, 0, 1, 13, 0, -1, -1, 13, 13},
        {11, 14, 1, 1, 14, 0, -1, -1, 14, 14},
        {12, 12, 2, 1, -2, 0, -1, -1, -1, -1},
        {13, 15, 3, 1, 15, 0, -1, -1, 15, 15},
    };
    static final String[] F = {"south", "west", "north", "east"};

    static String json(int[][] d) {
        StringBuilder sb = new StringBuilder("{\"reg\":\"furniture:doorbell\",\"states\":[");
        for (int i = 0; i < d.length; i++) {
            int[] r = d[i];
            if (i > 0) sb.append(',');
            sb.append("{\"meta\":").append(r[0]).append(",\"canon\":").append(r[1]);
            if (r[4] != -2) {
                sb.append(",\"beh\":{\"clickHandled\":").append(r[5] == 1)
                  .append(",\"clickMeta\":").append(r[4]).append(",\"clickThenMeta\":").append(r[6])
                  .append(",\"clickThenTicks\":").append(r[7]).append(",\"powerMeta\":").append(r[8])
                  .append(",\"unpowerMeta\":").append(r[9]).append(",\"clickSounds\":[]}");
            }
            sb.append(",\"props\":{\"facing\":\"").append(F[r[2]]).append("\",\"powered\":\"").append(r[3] == 1).append("\"}}");
        }
        return sb.append("]}").toString();
    }

    static String desc(int meta) {
        for (int[] r : D) if (r[0] == meta) return F[r[2]] + (r[3] == 1 ? "+powered" : "");
        return "?" + meta;
    }
    static int facing(int meta) { for (int[] r : D) if (r[0] == meta) return r[2]; return -1; }

    public static void main(String[] a) throws Exception {
        ClassLoader parent = DoorbellTest.class.getClassLoader();
        int failures = 0;
        for (String jar : a) {
            Rig rig = new Rig(jar, parent);
            Object b = rig.block(json(D), null);
            System.out.println("==== " + jar);
            int[] starts = {0, 1, 2, 3};
            for (int s : starts) {
                String c = rig.click(b, s);
                String[] p = c.split("\\|");
                String sets = p[1];
                int m1 = sets.length() > 2 ? Integer.parseInt(sets.substring(1, sets.indexOf(']') > 0 && sets.contains(",") ? sets.indexOf(',') : sets.indexOf(']'))) : -1;
                String t = m1 >= 0 ? rig.tick(b, m1) : "-";
                int m2 = t.length() > 2 ? Integer.parseInt(t.substring(1, t.indexOf(']'))) : -1;
                boolean ok = m1 >= 0 && facing(m1) == facing(s) && m2 >= 0 && facing(m2) == facing(s);
                System.out.printf("click %-6s -> %-14s tick -> %-14s  %s%n", F[facing(s)], m1 < 0 ? "-" : desc(m1), m2 < 0 ? "-" : desc(m2), ok ? "OK" : "FAIL");
                if (!ok) failures++;
            }
            for (int s : new int[]{0, 1, 2, 3, 5, 10, 11, 13}) {
                for (boolean pw : new boolean[]{false, true}) {
                    String n = rig.neighbor(b, s, pw);
                    if (!n.equals("[]")) {
                        int m = Integer.parseInt(n.substring(1, n.indexOf(']')));
                        boolean ok = facing(m) == facing(s);
                        System.out.printf("neighbor(%s) on %-14s -> %-14s %s%n", pw ? "powered" : "unpowered", desc(s), desc(m), ok ? "OK(no facing change)" : "FAIL");
                        if (!ok) failures++;
                    }
                }
            }
            System.out.println("failures so far: " + failures);
        }
    }
}
