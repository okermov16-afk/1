package com.modelport.ported;

import com.modelport.ModelPort;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Atypical clicks become the default toggle. When a click on a block is handled but the probe saw no change
 * (e.g. the closing click of a door whose logic lives in a procedure), and exactly one other library block turns
 * into this state when it is clicked, the click on this block is recorded as the way back to that block.
 */
public final class ReverseToggle {
    private ReverseToggle() {
    }

    private static int canonOf(PortedBlock.State s) {
        return s.canon >= 0 ? s.canon : s.meta;
    }

    private static boolean unresolved(PortedBlock.Behavior b) {
        return b != null && b.clickHandled && !b.clickGui && b.clickMeta < 0 && b.clickBlock == null
            && b.clickThenMeta < 0 && b.powerMeta < 0 && b.powerBlock == null && b.unpowerMeta < 0 && b.unpowerBlock == null;
    }

    /** Fills the missing click transitions; returns the registry names of the changed blocks. */
    public static Set<String> complete(Collection<PortedBlock> blocks) {
        Set<String> changed = new LinkedHashSet<String>();
        for (PortedBlock self : blocks) {
            if (self == null || self.reg == null || self.states == null) {
                continue;
            }
            for (PortedBlock.State st : self.states) {
                if (!unresolved(st.beh)) {
                    continue;
                }
                int canon = canonOf(st);
                String srcReg = null;
                int srcMeta = -1;
                boolean ambiguous = false;
                for (PortedBlock other : blocks) {
                    if (other == null || other == self || other.reg == null || other.states == null) {
                        continue;
                    }
                    for (PortedBlock.State os : other.states) {
                        PortedBlock.Behavior ob = os.beh;
                        if (ob == null || ob.clickMeta != canon || !self.reg.equals(ob.clickBlock)) {
                            continue;
                        }
                        int m = canonOf(os);
                        if (srcReg == null) {
                            srcReg = other.reg;
                            srcMeta = m;
                        } else if (!srcReg.equals(other.reg) || srcMeta != m) {
                            ambiguous = true;
                        }
                    }
                }
                if (srcReg == null || ambiguous) {
                    continue;
                }
                st.beh.clickBlock = srcReg;
                st.beh.clickMeta = srcMeta;
                changed.add(self.reg);
            }
        }
        return changed;
    }

    public static void completeAll() {
        try {
            PortedLibrary lib = PortedRegistry.library();
            if (lib == null) {
                return;
            }
            List<PortedBlock> all = new ArrayList<PortedBlock>();
            for (String name : new ArrayList<String>(lib.names())) {
                all.add(lib.get(name));
            }
            for (String reg : complete(all)) {
                lib.rewriteJson(lib.get(reg));
                ModelPort.LOG.info("modelport: обратный клик {} достроен как переключение", (Object) reg);
            }
        } catch (Throwable t) {
            // best effort, stored behaviour stays as it was
        }
    }
}
