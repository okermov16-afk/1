package contactwand.core;

import java.util.HashSet;
import java.util.Set;
import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FrameNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LineNumberNode;
import org.objectweb.asm.tree.LookupSwitchInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TableSwitchInsnNode;
import org.objectweb.asm.tree.TryCatchBlockNode;
import org.objectweb.asm.tree.VarInsnNode;

/**
 * Prepends a hook call to the block methods that decide whether something can be attached to / stand on a block.
 * Applied once per class at load time; nothing is polled afterwards.
 */
public class ContactTransformer implements IClassTransformer {
    private static final String HOOKS = "contactwand/core/Hooks";
    private static final String BFS = "net/minecraft/block/state/BlockFaceShape";
    private static final String STATE = "net/minecraft/block/state/IBlockState";
    private static final String ACCESS = "net/minecraft/world/IBlockAccess";
    private static final String POS = "net/minecraft/util/math/BlockPos";
    private static final String FACING = "net/minecraft/util/EnumFacing";
    private static final String D_STATE = "(L" + STATE + ";)Z";
    private static final String D_ACCESS_POS = "(L" + ACCESS + ";L" + POS + ";)Z";
    private static final String D_ACCESS_POS_SIDE = "(L" + ACCESS + ";L" + POS + ";L" + FACING + ";)Z";
    private static final int RET_TRUE = 0;
    private static final int RET_SOLID = 1;

    private static final Target[] BLOCK_TARGETS = new Target[]{
        new Target("func_193383_a", "(L" + ACCESS + ";L" + STATE + ";L" + POS + ";L" + FACING + ";)L" + BFS + ";", new int[]{2}, "faceShape", D_STATE, RET_SOLID),
        new Target("isSideSolid", "(L" + STATE + ";L" + ACCESS + ";L" + POS + ";L" + FACING + ";)Z", new int[]{1}, "sideSolid", D_STATE, RET_TRUE),
        new Target("canPlaceTorchOnTop", "(L" + STATE + ";L" + ACCESS + ";L" + POS + ";)Z", new int[]{1}, "sideSolid", D_STATE, RET_TRUE),
        new Target("canSustainPlant", "(L" + STATE + ";L" + ACCESS + ";L" + POS + ";L" + FACING + ";Lnet/minecraftforge/common/IPlantable;)Z", new int[]{1}, "plantSoil", D_STATE, RET_TRUE),
        new Target("isNormalCube", "(L" + STATE + ";L" + ACCESS + ";L" + POS + ";)Z", new int[]{1}, "normalCube", D_STATE, RET_TRUE),
        new Target("func_176198_a", "(Lnet/minecraft/world/World;L" + POS + ";L" + FACING + ";)Z", new int[]{1, 2, 3}, "supportSide", D_ACCESS_POS_SIDE, RET_TRUE),
        new Target("func_176196_c", "(Lnet/minecraft/world/World;L" + POS + ";)Z", new int[]{1, 2}, "supportAround", D_ACCESS_POS, RET_TRUE),
        new Target("func_180671_f", "(Lnet/minecraft/world/World;L" + POS + ";L" + STATE + ";)Z", new int[]{1, 2}, "supportAround", D_ACCESS_POS, RET_TRUE)
    };

    private static final String GETSTATE = "func_180495_p";
    private static final String GETSTATE_DESC = "(L" + POS + ";)L" + STATE + ";";
    private static final String ISTOPSOLID = "func_185896_q";
    private static final String TOPSOLID_HOOK_DESC = "(L" + ACCESS + ";L" + POS + ";L" + STATE + ";)Z";
    private static final byte[] P_ISTOPSOLID = toAscii(ISTOPSOLID);
    private static final byte[] P_BLOCKPOS = toAscii(POS);
    private static final byte[][] P_NAMES = new byte[BLOCK_TARGETS.length][];

    static {
        for (int i = 0; i < BLOCK_TARGETS.length; i++) {
            P_NAMES[i] = toAscii(BLOCK_TARGETS[i].name);
        }
    }

    public byte[] transform(String name, String transformedName, byte[] bytes) {
        if (bytes == null || transformedName == null) {
            return bytes;
        }
        try {
            if (skip(transformedName) || !contains(bytes, P_BLOCKPOS)) {
                return bytes;
            }
            if (!anyName(bytes) && !contains(bytes, P_ISTOPSOLID)) {
                return bytes;
            }
            return patch(bytes);
        } catch (Throwable t) {
            System.out.println("[ContactWand] transform failed for " + transformedName + ": " + t);
            return bytes;
        }
    }

    private static boolean skip(String n) {
        return n.startsWith("java.") || n.startsWith("javax.") || n.startsWith("sun.") || n.startsWith("com.google.")
            || n.startsWith("org.objectweb.") || n.startsWith("org.apache.") || n.startsWith("io.netty.")
            || n.startsWith("org.lwjgl.") || n.startsWith("scala.") || n.startsWith("contactwand.");
    }

    private static boolean anyName(byte[] b) {
        for (int i = 0; i < P_NAMES.length; i++) {
            if (contains(b, P_NAMES[i])) {
                return true;
            }
        }
        return false;
    }

    private static byte[] patch(byte[] bytes) {
        ClassNode cn = new ClassNode();
        new ClassReader(bytes).accept(cn, 0);
        boolean changed = false;
        for (int i = 0; i < cn.methods.size(); i++) {
            MethodNode m = (MethodNode) cn.methods.get(i);
            if ((m.access & 0x500) != 0) {
                continue;
            }
            if ((m.access & Opcodes.ACC_STATIC) == 0) {
                for (int j = 0; j < BLOCK_TARGETS.length; j++) {
                    Target t = BLOCK_TARGETS[j];
                    if (t.name.equals(m.name) && t.desc.equals(m.desc)) {
                        inject(m, t);
                        changed = true;
                        break;
                    }
                }
            }
            if (patchTopSolid(m)) {
                changed = true;
            }
        }
        if (!changed) {
            return bytes;
        }
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        cn.accept(cw);
        return cw.toByteArray();
    }

    private static void inject(MethodNode m, Target t) {
        LabelNode skip = new LabelNode();
        InsnList l = new InsnList();
        for (int i = 0; i < t.args.length; i++) {
            l.add(new VarInsnNode(Opcodes.ALOAD, t.args[i]));
        }
        l.add(new MethodInsnNode(Opcodes.INVOKESTATIC, HOOKS, t.hook, t.hookDesc, false));
        l.add(new JumpInsnNode(Opcodes.IFEQ, skip));
        if (t.ret == RET_SOLID) {
            l.add(new FieldInsnNode(Opcodes.GETSTATIC, BFS, "SOLID", "L" + BFS + ";"));
            l.add(new InsnNode(Opcodes.ARETURN));
        } else {
            l.add(new InsnNode(Opcodes.ICONST_1));
            l.add(new InsnNode(Opcodes.IRETURN));
        }
        l.add(skip);
        l.add(new FrameNode(Opcodes.F_SAME, 0, null, 0, null));
        m.instructions.insert(l);
    }

    /** world.getBlockState(pos).isTopSolid() becomes Hooks.topSolid(world, pos, state). */
    private static boolean patchTopSolid(MethodNode m) {
        if (m.instructions == null || m.instructions.size() == 0) {
            return false;
        }
        Set<LabelNode> targets = jumpTargets(m);
        boolean changed = false;
        AbstractInsnNode[] all = m.instructions.toArray();
        for (int i = 0; i < all.length; i++) {
            if (!(all[i] instanceof MethodInsnNode)) {
                continue;
            }
            MethodInsnNode get = (MethodInsnNode) all[i];
            if (!GETSTATE.equals(get.name) || !GETSTATE_DESC.equals(get.desc)) {
                continue;
            }
            boolean straight = true;
            AbstractInsnNode n = get.getNext();
            for (; n != null && !(n instanceof MethodInsnNode); n = n.getNext()) {
                if (n instanceof LabelNode && targets.contains(n)) {
                    straight = false;
                    break;
                }
                if (n instanceof FrameNode) {
                    straight = false;
                    break;
                }
                if (n instanceof LabelNode || n instanceof LineNumberNode) {
                    continue;
                }
                straight = false;
                break;
            }
            if (!straight || !(n instanceof MethodInsnNode)) {
                continue;
            }
            MethodInsnNode top = (MethodInsnNode) n;
            if (!ISTOPSOLID.equals(top.name) || !"()Z".equals(top.desc)) {
                continue;
            }
            m.instructions.insertBefore(get, new InsnNode(Opcodes.DUP2));
            m.instructions.set(top, new MethodInsnNode(Opcodes.INVOKESTATIC, HOOKS, "topSolid", TOPSOLID_HOOK_DESC, false));
            changed = true;
        }
        return changed;
    }

    private static Set<LabelNode> jumpTargets(MethodNode m) {
        Set<LabelNode> set = new HashSet<LabelNode>();
        for (AbstractInsnNode a = m.instructions.getFirst(); a != null; a = a.getNext()) {
            if (a instanceof JumpInsnNode) {
                set.add(((JumpInsnNode) a).label);
            } else if (a instanceof TableSwitchInsnNode) {
                TableSwitchInsnNode t = (TableSwitchInsnNode) a;
                set.add(t.dflt);
                set.addAll(t.labels);
            } else if (a instanceof LookupSwitchInsnNode) {
                LookupSwitchInsnNode t = (LookupSwitchInsnNode) a;
                set.add(t.dflt);
                set.addAll(t.labels);
            }
        }
        if (m.tryCatchBlocks != null) {
            for (int i = 0; i < m.tryCatchBlocks.size(); i++) {
                TryCatchBlockNode t = (TryCatchBlockNode) m.tryCatchBlocks.get(i);
                set.add(t.start);
                set.add(t.end);
                set.add(t.handler);
            }
        }
        return set;
    }

    private static byte[] toAscii(String s) {
        byte[] b = new byte[s.length()];
        for (int i = 0; i < s.length(); i++) {
            b[i] = (byte) s.charAt(i);
        }
        return b;
    }

    private static boolean contains(byte[] b, byte[] p) {
        int n = b.length - p.length;
        for (int i = 0; i <= n; i++) {
            int k = 0;
            while (k < p.length && b[i + k] == p[k]) {
                k++;
            }
            if (k == p.length) {
                return true;
            }
        }
        return false;
    }

    private static final class Target {
        final String name;
        final String desc;
        final int[] args;
        final String hook;
        final String hookDesc;
        final int ret;

        Target(String name, String desc, int[] args, String hook, String hookDesc, int ret) {
            this.name = name;
            this.desc = desc;
            this.args = args;
            this.hook = hook;
            this.hookDesc = hookDesc;
            this.ret = ret;
        }
    }
}
