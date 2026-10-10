package com.modelport.ported;

import java.util.List;

/**
 * Translates a meta recorded in the original block's getMetaFromState space ("canon")
 * into the ported block's own meta index (the index its states are stored under).
 * Identity for blocks whose states all have canon == meta, and for merged families.
 */
public final class CanonMap {
    private CanonMap() {
    }

    public static int fromCanon(List<PortedBlock.State> states, Object family, int c) {
        if (family != null || states == null || c < 0) {
            return c;
        }
        int best = -1;
        for (int i = 0; i < states.size(); i++) {
            PortedBlock.State s = states.get(i);
            if (s == null) {
                continue;
            }
            int cn = s.canon >= 0 ? s.canon : s.meta;
            if (cn != c) {
                continue;
            }
            if (s.meta == c) {
                return c;
            }
            if (best < 0 || s.meta < best) {
                best = s.meta;
            }
        }
        return best >= 0 ? best : c;
    }

    /** Partner blocks decode canon metas themselves (BlockPorted.func_176203_a), nothing to translate. */
    public static int partner(Object block, int c) {
        return c;
    }

    public static int placed(int enc, List<PortedBlock.State> states, Object family) {
        int m = enc & 0xFF;
        return (enc & 0x100) != 0 ? fromCanon(states, family, m) : m;
    }

    public static int placementEnc(int[] table, int n, int face, float yaw) {
        int m = BlockPorted.placementMeta(table, n, face, yaw);
        boolean applied = table != null && table.length == 24 && n == 0 && face >= 0 && face <= 5;
        return applied ? (m | 0x100) : m;
    }
}
