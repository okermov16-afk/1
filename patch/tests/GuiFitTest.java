import java.lang.reflect.*;
import java.net.*;
import java.util.*;

public class GuiFitTest {
    static sun.misc.Unsafe U;
    static { try { Field f = sun.misc.Unsafe.class.getDeclaredField("theUnsafe"); f.setAccessible(true); U = (sun.misc.Unsafe) f.get(null); } catch (Exception e) { throw new RuntimeException(e); } }
    static void set(Object o, String n, Object v) throws Exception { Field f = o.getClass().getDeclaredField(n); f.setAccessible(true); f.set(o, v); }

    static float[] mat(Object m) throws Exception {
        float[] r = new float[16]; String[] n = {"m00","m01","m02","m03","m10","m11","m12","m13","m20","m21","m22","m23","m30","m31","m32","m33"};
        for (int i = 0; i < 16; i++) r[i] = m.getClass().getField(n[i]).getFloat(m);
        return r;
    }
    static float[] extents(float[] M, float[][] pts) {
        float minX = 9, maxX = -9, minY = 9, maxY = -9;
        for (float[] p : pts) {
            float x = p[0] - .5f, y = p[1] - .5f, z = p[2] - .5f;
            float X = M[0] * x + M[1] * y + M[2] * z + M[3], Y = M[4] * x + M[5] * y + M[6] * z + M[7];
            minX = Math.min(minX, X); maxX = Math.max(maxX, X); minY = Math.min(minY, Y); maxY = Math.max(maxY, Y);
        }
        return new float[]{minX, maxX, minY, maxY};
    }
    static float[][] box(float x0, float x1, float y0, float y1, float z0, float z1) {
        List<float[]> l = new ArrayList<>();
        for (float x : new float[]{x0, x1}) for (float y : new float[]{y0, y1}) for (float z : new float[]{z0, z1}) l.add(new float[]{x, y, z});
        return l.toArray(new float[0][]);
    }

    public static void main(String[] a) throws Exception {
        String jar = a[0];
        ClassLoader parent = GuiFitTest.class.getClassLoader();
        URLClassLoader cl = new URLClassLoader(new URL[]{new java.io.File(jar).toURI().toURL(), new java.io.File("th3/fake/").toURI().toURL()}, parent);
        Class<?> pbmC = cl.loadClass("com.modelport.ported.PortedBakedModel"), pbC = cl.loadClass("com.modelport.ported.PortedBlock"), bqC = cl.loadClass("net.minecraft.client.renderer.block.model.BakedQuad");
        Class<?> fakeC = cl.loadClass("FakePorted"), ttC = cl.loadClass("net.minecraft.client.renderer.block.model.ItemCameraTransforms$TransformType");
        Object GUI = Enum.valueOf((Class) ttC, "GUI"), GROUND = Enum.valueOf((Class) ttC, "GROUND");
        String[] names = {"крупная модель (как decocraft:dumbell_rack)", "модель сдвинута за границу ячейки", "нормальная иконка (дверь MCW, настоящая матрица)", "маленькая модель, стандартная матрица"};
        float[][] gui = {
            {-0.39009142f, -8.22056E-9f, -0.3900913f, 0, -0.19504565f, 0.47776234f, 0.19504568f, 0, 0.337829f, 0.27583623f, -0.33782908f, 0, 0, 0, 0, 1},
            {1, 0, 0, 0.45f, 0, 1, 0, 0.1f, 0, 0, 1, 0, 0, 0, 0, 1},
            {1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1},
            {0.625f * 0.7071f, 0, 0.625f * 0.7071f, 0, -0.625f * 0.35f, 0.625f * 0.866f, 0.625f * 0.35f, 0, -0.625f * 0.61f, 0.625f * 0.5f, 0.625f * 0.61f, 0, 0, 0, 0, 1}};
        float[][][] boxes = {box(-0.2127f, 1.2127f, 0, 1.13f, 0.0015f, 1.0015f), box(0, 1, 0, 1, 0.47f, 0.53f), box(0, 1, 0, 1, 0.469f, 0.531f), box(0.3f, 0.7f, 0.0f, 0.4f, 0.3f, 0.7f)};
        int bad = 0;
        for (int c = 0; c < 4; c++) {
            Object model = U.allocateInstance(pbmC);
            Map<Integer, List<Object>> itemQuads = new HashMap<>(), quads = new HashMap<>();
            for (int s = 0; s < 6; s++) {
                Object bq = bqC.getDeclaredConstructor().newInstance();
                int[] d = new int[28];
                for (int k = 0; k < 4; k++) { float[] p = boxes[c][(s + k) % 8]; d[k * 7] = Float.floatToRawIntBits(p[0]); d[k * 7 + 1] = Float.floatToRawIntBits(p[1]); d[k * 7 + 2] = Float.floatToRawIntBits(p[2]); }
                if (s == 0) { int[] d2 = new int[28]; for (int k = 0; k < 4; k++) for (int t = 0; t < 3; t++) d2[k * 7 + t] = Float.floatToRawIntBits(boxes[c][k * 2][t]); d = d2; }
                bqC.getField("data").set(bq, d);
                quads.computeIfAbsent(0 * 64 + 32 + s, k -> new ArrayList<>()).add(bq);
            }
            // ensure the full bbox is covered: one quad per corner set
            Object bqAll = bqC.getDeclaredConstructor().newInstance(); int[] da = new int[28];
            float[][] bx = boxes[c]; for (int k = 0; k < 4; k++) for (int t = 0; t < 3; t++) da[k * 7 + t] = Float.floatToRawIntBits(bx[k][t]);
            bqC.getField("data").set(bqAll, da); quads.get(32).add(bqAll);
            Object bqAll2 = bqC.getDeclaredConstructor().newInstance(); int[] db = new int[28];
            for (int k = 0; k < 4; k++) for (int t = 0; t < 3; t++) db[k * 7 + t] = Float.floatToRawIntBits(bx[4 + k][t]);
            bqC.getField("data").set(bqAll2, db); quads.get(32).add(bqAll2);
            set(model, "root", model); set(model, "quads", quads); set(model, "itemQuads", itemQuads); set(model, "fixedMeta", 0);
            Object data = pbC.getDeclaredConstructor().newInstance();
            float[][] mats = new float[9][]; for (int i = 0; i < 9; i++) mats[i] = gui[c];
            pbC.getField("itemMatrices").set(data, mats);
            fakeC.getField("theData").set(null, data);
            set(model, "block", fakeC.getDeclaredConstructor().newInstance());
            Method hp = pbmC.getMethod("handlePerspective", ttC);
            Object pr = hp.invoke(model, GUI); Object m = pr.getClass().getMethod("getRight").invoke(pr);
            float[] M = mat(m); float[] e = extents(M, boxes[c]), e0 = extents(gui[c], boxes[c]);
            boolean fits = e[0] >= -0.5005 && e[1] <= 0.5005 && e[2] >= -0.5005 && e[3] <= 0.5005;
            boolean unchanged = Arrays.equals(M, gui[c]);
            Object pg = hp.invoke(model, GROUND); boolean groundSame = Arrays.equals(mat(pg.getClass().getMethod("getRight").invoke(pg)), gui[c]);
            System.out.printf("%-52s было x[%.2f..%.2f] y[%.2f..%.2f] -> стало x[%.2f..%.2f] y[%.2f..%.2f]  %s%s%n", names[c], e0[0], e0[1], e0[2], e0[3], e[0], e[1], e[2], e[3],
                fits ? "ПОМЕЩАЕТСЯ" : "ВЫХОДИТ ЗА ЯЧЕЙКУ", unchanged ? " (матрица не менялась)" : " (скорректирована)");
            if (!fits) bad++;
            if (c == 2 || c == 3) { if (!unchanged) { System.out.println("   ОШИБКА: подходящая иконка изменена"); bad += 100; } }
            if (!groundSame) { System.out.println("   ОШИБКА: другой режим показа изменён"); bad += 100; }
        }
        System.out.println(jar.substring(jar.lastIndexOf('/') + 1) + ": проблем " + bad);
    }
}
