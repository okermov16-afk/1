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
        if (count != 6) throw new IllegalStateException("patches " + count);
        System.out.println("patched ok: " + count + " patch points");
    }
}
