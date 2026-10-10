import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;

public class Patch {
    static final String BP = "com/modelport/ported/BlockPorted";
    static final String BH = "com/modelport/ported/BehaviorProbe";
    static final String CM = "com/modelport/ported/CanonMap";
    static final String PB = "com/modelport/ported/PortedBlock";
    static int count;

    static MethodNode find(ClassNode cn, String name, String desc) {
        for (MethodNode m : cn.methods) if (m.name.equals(name) && m.desc.equals(desc)) return m;
        throw new IllegalStateException("no method " + name + desc);
    }

    static InsnList statesAndFamily() {
        InsnList l = new InsnList();
        l.add(new VarInsnNode(Opcodes.ALOAD, 0));
        l.add(new FieldInsnNode(Opcodes.GETFIELD, BP, "data", "L" + PB + ";"));
        l.add(new FieldInsnNode(Opcodes.GETFIELD, PB, "states", "Ljava/util/List;"));
        return l;
    }

    static byte[] patchBlockPorted(byte[] in) {
        ClassReader cr = new ClassReader(in);
        ClassNode cn = new ClassNode();
        cr.accept(cn, 0);

        // 0. getStateFromMeta must be the inverse of getMetaFromState (canon space).
        //    The old body (indexed by the ported meta) becomes private stateByMeta, used by every internal caller.
        String SD = "(I)Lnet/minecraft/block/state/IBlockState;";
        MethodNode orig = find(cn, "func_176203_a", SD);
        MethodNode copy = new MethodNode(Opcodes.ACC_PRIVATE, "stateByMeta", SD, null, null);
        orig.accept(copy);
        cn.methods.add(copy);
        int redirected = 0;
        for (MethodNode m : cn.methods) {
            if (m == orig || m == copy) continue;
            for (AbstractInsnNode i = m.instructions.getFirst(); i != null; i = i.getNext()) {
                if (i instanceof MethodInsnNode mi && mi.getOpcode() == Opcodes.INVOKEVIRTUAL && mi.owner.equals(BP)
                    && mi.name.equals("func_176203_a") && mi.desc.equals(SD)) {
                    mi.setOpcode(Opcodes.INVOKESPECIAL);
                    mi.name = "stateByMeta";
                    redirected++;
                }
            }
        }
        if (redirected < 5) throw new IllegalStateException("redirected only " + redirected);
        orig.instructions.clear();
        orig.tryCatchBlocks.clear();
        orig.localVariables = null;
        InsnList nb = new InsnList();
        nb.add(new VarInsnNode(Opcodes.ALOAD, 0));
        nb.add(statesAndFamily());
        nb.add(new VarInsnNode(Opcodes.ALOAD, 0));
        nb.add(new FieldInsnNode(Opcodes.GETFIELD, BP, "family", "Lcom/modelport/ported/MergeFamily;"));
        nb.add(new VarInsnNode(Opcodes.ILOAD, 1));
        nb.add(new MethodInsnNode(Opcodes.INVOKESTATIC, CM, "fromCanon", "(Ljava/util/List;Ljava/lang/Object;I)I", false));
        nb.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, BP, "stateByMeta", SD, false));
        nb.add(new InsnNode(Opcodes.ARETURN));
        orig.instructions.add(nb);
        count++;
        System.out.println("redirected internal getStateFromMeta calls: " + redirected);

        // 1. targetMeta: family == null branch returns canon target translated to own meta
        MethodNode tm = find(cn, "targetMeta", "(IILjava/lang/String;)I");
        int done = 0;
        for (AbstractInsnNode i = tm.instructions.getFirst(); i != null; i = i.getNext()) {
            if (i instanceof MethodInsnNode mi && mi.owner.equals("java/lang/String") && mi.name.equals("equals")) {
                AbstractInsnNode j = i.getNext();           // IFEQ
                AbstractInsnNode k = j.getNext();           // maybe label/line
                while (k instanceof LabelNode || k instanceof LineNumberNode || k instanceof FrameNode) k = k.getNext();
                if (j.getOpcode() == Opcodes.IFEQ && k.getOpcode() == Opcodes.ILOAD && ((VarInsnNode) k).var == 2) {
                    InsnList repl = new InsnList();
                    repl.add(statesAndFamily());
                    repl.add(new VarInsnNode(Opcodes.ALOAD, 0));
                    repl.add(new FieldInsnNode(Opcodes.GETFIELD, BP, "family", "Lcom/modelport/ported/MergeFamily;"));
                    repl.add(new VarInsnNode(Opcodes.ILOAD, 2));
                    repl.add(new MethodInsnNode(Opcodes.INVOKESTATIC, CM, "fromCanon", "(Ljava/util/List;Ljava/lang/Object;I)I", false));
                    tm.instructions.insert(k, repl);
                    tm.instructions.remove(k);
                    done++;
                    break;
                }
            }
        }
        if (done != 1) throw new IllegalStateException("targetMeta patch applied " + done);
        count++;

        // 2. applyTransition: partner block branch
        MethodNode at = find(cn, "applyTransition", "(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;IILjava/lang/String;)Z");
        done = 0;
        for (AbstractInsnNode i = at.instructions.getFirst(); i != null; i = i.getNext()) {
            if (i instanceof MethodInsnNode mi && mi.getOpcode() == Opcodes.INVOKEVIRTUAL
                && mi.owner.equals("net/minecraft/block/Block") && mi.name.equals("func_176203_a") && mi.desc.equals("(I)Lnet/minecraft/block/state/IBlockState;")) {
                AbstractInsnNode p = mi.getPrevious();
                if (p.getOpcode() == Opcodes.ILOAD && ((VarInsnNode) p).var == 4) {
                    InsnList repl = new InsnList();
                    repl.add(new VarInsnNode(Opcodes.ALOAD, 6));
                    repl.add(new VarInsnNode(Opcodes.ILOAD, 4));
                    repl.add(new MethodInsnNode(Opcodes.INVOKESTATIC, CM, "partner", "(Ljava/lang/Object;I)I", false));
                    at.instructions.insert(p, repl);
                    at.instructions.remove(p);
                    done++;
                }
            }
        }
        if (done != 1) throw new IllegalStateException("applyTransition patch applied " + done);
        count++;

        // 3. getStateForPlacement: table values are canon metas
        MethodNode gp = find(cn, "getStateForPlacement", "(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/util/EnumFacing;FFFILnet/minecraft/entity/EntityLivingBase;Lnet/minecraft/util/EnumHand;)Lnet/minecraft/block/state/IBlockState;");
        done = 0;
        for (AbstractInsnNode i = gp.instructions.getFirst(); i != null; i = i.getNext()) {
            if (i instanceof MethodInsnNode mi && mi.getOpcode() == Opcodes.INVOKESTATIC && mi.owner.equals(BP) && mi.name.equals("placementMeta")) {
                mi.owner = CM;
                mi.name = "placementEnc";
                InsnList add = new InsnList();
                add.add(statesAndFamily());
                add.add(new VarInsnNode(Opcodes.ALOAD, 0));
                add.add(new FieldInsnNode(Opcodes.GETFIELD, BP, "family", "Lcom/modelport/ported/MergeFamily;"));
                add.add(new MethodInsnNode(Opcodes.INVOKESTATIC, CM, "placed", "(ILjava/util/List;Ljava/lang/Object;)I", false));
                gp.instructions.insert(mi, add);
                done++;
                break;
            }
        }
        if (done != 1) throw new IllegalStateException("getStateForPlacement patch applied " + done);
        count++;

        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        cn.accept(cw);
        return cw.toByteArray();
    }

    static byte[] patchProbe(byte[] in) {
        ClassReader cr = new ClassReader(in);
        ClassNode cn = new ClassNode();
        cr.accept(cn, 0);
        MethodNode m = find(cn, "measureOnce", "(Lnet/minecraft/block/Block;ILnet/minecraft/block/state/IBlockState;)Lcom/modelport/ported/BehaviorProbe$Result;");
        InsnList l = new InsnList();
        l.add(new VarInsnNode(Opcodes.ALOAD, 2));
        l.add(new MethodInsnNode(Opcodes.INVOKESTATIC, BH, "metaOf", "(Lnet/minecraft/block/state/IBlockState;)I", false));
        l.add(new VarInsnNode(Opcodes.ISTORE, 1));
        m.instructions.insert(l);
        count++;
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        cn.accept(cw);
        return cw.toByteArray();
    }

    static final String PP = "com/modelport/ported/PortedPlacement";
    static final String PC = "com/modelport/ported/PortedCapture";
    static final String PS = "com/modelport/ported/PlacementSim";

    static byte[] patchPlacement(byte[] in) {
        ClassReader cr = new ClassReader(in);
        ClassNode cn = new ClassNode();
        cr.accept(cn, 0);
        String desc = "(Lnet/minecraft/block/Block;Lnet/minecraft/world/WorldServer;Lnet/minecraft/util/math/BlockPos;Lnet/minecraftforge/common/util/FakePlayer;)[I";
        MethodNode m = find(cn, "probe", desc);
        m.instructions.clear();
        m.tryCatchBlocks.clear();
        m.localVariables = null;
        InsnList l = new InsnList();
        l.add(new VarInsnNode(Opcodes.ALOAD, 0));
        l.add(new VarInsnNode(Opcodes.ALOAD, 1));
        l.add(new VarInsnNode(Opcodes.ALOAD, 2));
        l.add(new VarInsnNode(Opcodes.ALOAD, 3));
        l.add(new MethodInsnNode(Opcodes.INVOKESTATIC, PS, "probe", desc, false));
        l.add(new InsnNode(Opcodes.ARETURN));
        m.instructions.add(l);
        count++;
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        cn.accept(cw);
        return cw.toByteArray();
    }

    static byte[] patchCapture(byte[] in) {
        ClassReader cr = new ClassReader(in);
        ClassNode cn = new ClassNode();
        cr.accept(cn, 0);
        MethodNode run = null;
        for (MethodNode m : cn.methods) if (m.name.equals("run") && m.desc.startsWith("(Ljava/io/File;)")) run = m;
        if (run == null) throw new IllegalStateException("no run(File)");
        int done = 0;
        for (AbstractInsnNode i = run.instructions.getFirst(); i != null; i = i.getNext()) {
            if (i instanceof MethodInsnNode mi && mi.getOpcode() == Opcodes.INVOKESTATIC && mi.owner.equals(BH) && mi.name.equals("release")) {
                run.instructions.insertBefore(mi, new MethodInsnNode(Opcodes.INVOKESTATIC, PS, "refreshAll", "()V", false));
                done++;
            }
        }
        if (done != 1) throw new IllegalStateException("capture patch applied " + done);
        count++;
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        cn.accept(cw);
        return cw.toByteArray();
    }

    static final String PBM = "com/modelport/ported/PortedBakedModel";

    static byte[] patchBakedModel(byte[] in) {
        ClassReader cr = new ClassReader(in);
        ClassNode cn = new ClassNode();
        cr.accept(cn, 0);
        MethodNode m = find(cn, "func_188616_a", "(Lnet/minecraft/block/state/IBlockState;Lnet/minecraft/util/EnumFacing;J)Ljava/util/List;");
        int done = 0;
        for (AbstractInsnNode i = m.instructions.getFirst(); i != null; i = i.getNext()) {
            // pattern:  ALOAD 10 ; IFNONNULL L ; ILOAD 9 ; IFEQ L   (L = end of the fallback block)
            if (i instanceof JumpInsnNode j1 && j1.getOpcode() == Opcodes.IFNONNULL) {
                AbstractInsnNode a = j1.getPrevious();
                AbstractInsnNode b = j1.getNext();
                while (a instanceof LabelNode || a instanceof LineNumberNode || a instanceof FrameNode) a = a.getPrevious();
                while (b instanceof LabelNode || b instanceof LineNumberNode || b instanceof FrameNode) b = b.getNext();
                if (a != null && a.getOpcode() == Opcodes.ALOAD && ((VarInsnNode) a).var == 10
                    && b != null && b.getOpcode() == Opcodes.ILOAD && ((VarInsnNode) b).var == 9
                    && b.getNext() instanceof JumpInsnNode j2 && j2.getOpcode() == Opcodes.IFEQ) {
                    InsnList add = new InsnList();
                    add.add(new VarInsnNode(Opcodes.ALOAD, 0));
                    add.add(new FieldInsnNode(Opcodes.GETFIELD, PBM, "root", "L" + PBM + ";"));
                    add.add(new FieldInsnNode(Opcodes.GETFIELD, PBM, "quads", "Ljava/util/Map;"));
                    add.add(new VarInsnNode(Opcodes.ILOAD, 9));
                    add.add(new VarInsnNode(Opcodes.ILOAD, 5));
                    add.add(new MethodInsnNode(Opcodes.INVOKESTATIC, CM, "variantHasQuads", "(Ljava/util/Map;II)Z", false));
                    add.add(new JumpInsnNode(Opcodes.IFNE, j2.label));
                    m.instructions.insert(j2, add);
                    done++;
                    break;
                }
            }
        }
        if (done != 1) throw new IllegalStateException("baked model patch applied " + done);
        count++;
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        cn.accept(cw);
        return cw.toByteArray();
    }

    public static void main(String[] a) throws Exception {
        String src = a[0], dst = a[1];
        List<String> helpers = new ArrayList<>(Arrays.asList(a).subList(2, a.length));
        try (ZipFile zf = new ZipFile(src); ZipOutputStream zo = new ZipOutputStream(new FileOutputStream(dst))) {
            for (Enumeration<? extends ZipEntry> e = zf.entries(); e.hasMoreElements();) {
                ZipEntry ze = e.nextElement();
                byte[] data = zf.getInputStream(ze).readAllBytes();
                if (ze.getName().equals(BP + ".class")) data = patchBlockPorted(data);
                else if (ze.getName().equals(BH + ".class")) data = patchProbe(data);
                else if (ze.getName().equals(PP + ".class")) data = patchPlacement(data);
                else if (ze.getName().equals(PC + ".class")) data = patchCapture(data);
                else if (ze.getName().equals(PBM + ".class")) data = patchBakedModel(data);
                ZipEntry ne = new ZipEntry(ze.getName());
                ne.setTime(ze.getTime());
                zo.putNextEntry(ne);
                zo.write(data);
                zo.closeEntry();
            }
            for (String h : helpers) {
                Path root = Paths.get(h);
                try (var st = Files.walk(root)) {
                    for (Path f : (Iterable<Path>) st.filter(x -> x.toString().endsWith(".class"))::iterator) {
                        String name = root.relativize(f).toString().replace('\\', '/');
                        ZipEntry ne = new ZipEntry(name);
                        zo.putNextEntry(ne);
                        zo.write(Files.readAllBytes(f));
                        zo.closeEntry();
                    }
                }
            }
        }
        if (count != 8) throw new IllegalStateException("patches " + count);
        System.out.println("patched ok: " + count + " patch points");
    }
}
