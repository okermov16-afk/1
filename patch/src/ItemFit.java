package com.modelport.ported;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.vecmath.Matrix4f;
import net.minecraft.client.renderer.block.model.BakedQuad;
import org.apache.commons.lang3.tuple.Pair;

/**
 * Keeps the GUI icon of a ported block inside its inventory slot. The model is drawn as
 * S * M * T(-0.5) * v, so after M the slot is x,y in [-0.5, 0.5]. If the transform copied from the original
 * item model sends part of the model outside that square, the icon is scaled down / moved just enough to fit.
 * Icons that already fit are returned untouched.
 */
public final class ItemFit {
    private static final int GUI = 6;
    private static final float LIMIT = 0.5f;
    private static final float EPS = 0.0005f;

    private ItemFit() {
    }

    public static Pair fit(Pair p, int type, Map<Integer, List<BakedQuad>> itemQuads, Map<Integer, List<BakedQuad>> quads, int meta) {
        try {
            if (type != GUI || p == null || !(p.getRight() instanceof Matrix4f)) {
                return p;
            }
            Matrix4f m = (Matrix4f) p.getRight();
            List<BakedQuad> all = collect(itemQuads, quads, meta);
            if (all.isEmpty()) {
                return p;
            }
            float minX = Float.MAX_VALUE, maxX = -Float.MAX_VALUE, minY = Float.MAX_VALUE, maxY = -Float.MAX_VALUE;
            boolean any = false;
            for (BakedQuad q : all) {
                int[] d = q.func_178209_a();
                if (d == null || d.length == 0 || d.length % 4 != 0) {
                    continue;
                }
                int stride = d.length / 4;
                for (int k = 0; k < 4; k++) {
                    float x = Float.intBitsToFloat(d[k * stride]) - 0.5f;
                    float y = Float.intBitsToFloat(d[k * stride + 1]) - 0.5f;
                    float z = Float.intBitsToFloat(d[k * stride + 2]) - 0.5f;
                    float px = m.m00 * x + m.m01 * y + m.m02 * z + m.m03;
                    float py = m.m10 * x + m.m11 * y + m.m12 * z + m.m13;
                    if (Float.isNaN(px) || Float.isNaN(py)) {
                        return p;
                    }
                    minX = Math.min(minX, px);
                    maxX = Math.max(maxX, px);
                    minY = Math.min(minY, py);
                    maxY = Math.max(maxY, py);
                    any = true;
                }
            }
            if (!any) {
                return p;
            }
            if (minX >= -LIMIT - EPS && maxX <= LIMIT + EPS && minY >= -LIMIT - EPS && maxY <= LIMIT + EPS) {
                return p;
            }
            float w = maxX - minX, h = maxY - minY;
            float f = Math.min(1.0f, Math.min(2 * LIMIT / Math.max(w, 1.0e-6f), 2 * LIMIT / Math.max(h, 1.0e-6f)));
            float cx = (minX + maxX) / 2, cy = (minY + maxY) / 2;
            float roomX = LIMIT - f * w / 2, roomY = LIMIT - f * h / 2;
            float nx = Math.max(-roomX, Math.min(roomX, cx)), ny = Math.max(-roomY, Math.min(roomY, cy));
            float tx = nx - f * cx, ty = ny - f * cy;
            float[] a = new float[]{
                f * m.m00, f * m.m01, f * m.m02, f * m.m03 + tx,
                f * m.m10, f * m.m11, f * m.m12, f * m.m13 + ty,
                f * m.m20, f * m.m21, f * m.m22, f * m.m23,
                m.m30, m.m31, m.m32, m.m33};
            return Pair.of(p.getLeft(), new Matrix4f(a));
        } catch (Throwable t) {
            return p;
        }
    }

    private static List<BakedQuad> collect(Map<Integer, List<BakedQuad>> itemQuads, Map<Integer, List<BakedQuad>> quads, int meta) {
        List<BakedQuad> all = new ArrayList<BakedQuad>();
        for (int side = 0; side <= 6; side++) {
            List<BakedQuad> l = itemQuads.get(meta * 8 + side);
            if (l != null) {
                all.addAll(l);
            }
        }
        if (!all.isEmpty()) {
            return all;
        }
        for (int i = -1; i < 16 && all.isEmpty(); i++) {
            int mt = i < 0 ? meta : i;
            for (int side = 0; side <= 6; side++) {
                List<BakedQuad> l = quads.get(mt * 64 + 32 + side);
                if (l != null) {
                    all.addAll(l);
                }
            }
        }
        return all;
    }
}
