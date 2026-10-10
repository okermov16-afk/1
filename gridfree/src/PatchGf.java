import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;

/** java PatchGf <orig.jar> <out.jar> <Forcen.class> : Move.arrange keeps its name and signature, the old body becomes arrangeOrig. */
public class PatchGf {
    static final String MOVE = "gridfree/math/Move";
    static final String FORCEN = "gridfree/math/Forcen";

    static byte[] patchMove(byte[] in) {
        ClassNode cn = new ClassNode();
        new ClassReader(in).accept(cn, 0);
        MethodNode orig = null;
        for (MethodNode m : cn.methods) {
            if (m.name.equals("arrange") && (m.access & Opcodes.ACC_STATIC) != 0 && m.desc.endsWith("[D)Z") && m.desc.startsWith("(DDDDDD")) orig = m;
        }
        if (orig == null) throw new IllegalStateException("arrange not found");
        MethodNode copy = new MethodNode(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "arrangeOrig", orig.desc, null, null);
        orig.accept(copy);
        cn.methods.add(copy);
        orig.instructions.clear();
        orig.tryCatchBlocks.clear();
        orig.localVariables = null;
        InsnList l = new InsnList();
        int slot = 0;
        for (Type t : Type.getArgumentTypes(orig.desc)) {
            l.add(new VarInsnNode(t.getOpcode(Opcodes.ILOAD), slot));
            slot += t.getSize();
        }
        l.add(new MethodInsnNode(Opcodes.INVOKESTATIC, FORCEN, "arrange", orig.desc, false));
        l.add(new InsnNode(Opcodes.IRETURN));
        orig.instructions.add(l);
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        cn.accept(cw);
        return cw.toByteArray();
    }

    public static void main(String[] a) throws Exception {
        int patched = 0;
        try (ZipFile zf = new ZipFile(a[0]); ZipOutputStream zo = new ZipOutputStream(new FileOutputStream(a[1]))) {
            for (Enumeration<? extends ZipEntry> e = zf.entries(); e.hasMoreElements();) {
                ZipEntry ze = e.nextElement();
                byte[] data = zf.getInputStream(ze).readAllBytes();
                if (ze.getName().equals(MOVE + ".class")) { data = patchMove(data); patched++; }
                ZipEntry ne = new ZipEntry(ze.getName());
                ne.setTime(ze.getTime());
                zo.putNextEntry(ne); zo.write(data); zo.closeEntry();
            }
            zo.putNextEntry(new ZipEntry(FORCEN + ".class"));
            zo.write(Files.readAllBytes(Paths.get(a[2])));
            zo.closeEntry();
        }
        if (patched != 1) throw new IllegalStateException("patched " + patched);
        System.out.println("patched ok");
    }
}
