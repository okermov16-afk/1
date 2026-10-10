import org.objectweb.asm.*;
import org.objectweb.asm.commons.*;
import org.objectweb.asm.tree.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.jar.*;

public class StubGen {
    public static void main(String[] a) throws Exception {
        Set<String> inJar = new HashSet<>();
        Set<String> refs = new TreeSet<>();
        Set<String> ifaces = new HashSet<>();
        List<byte[]> classes = new ArrayList<>();
        try (JarFile jf = new JarFile(a[0])) {
            for (Enumeration<JarEntry> e = jf.entries(); e.hasMoreElements();) {
                JarEntry je = e.nextElement();
                if (!je.getName().endsWith(".class")) continue;
                inJar.add(je.getName().substring(0, je.getName().length() - 6));
                try (InputStream in = jf.getInputStream(je)) { classes.add(in.readAllBytes()); }
            }
        }
        Remapper rec = new Remapper() {
            public String map(String n) { refs.add(n); return n; }
        };
        for (byte[] b : classes) {
            ClassReader cr = new ClassReader(b);
            ClassNode cn = new ClassNode();
            cr.accept(cn, 0);
            for (String i : cn.interfaces) ifaces.add(i);
            for (MethodNode m : cn.methods) for (AbstractInsnNode in : m.instructions)
                if (in instanceof MethodInsnNode mi && mi.getOpcode() == Opcodes.INVOKEINTERFACE) ifaces.add(mi.owner);
            ClassWriter cw = new ClassWriter(0);
            cr.accept(new ClassRemapper(cw, rec), 0);
        }
        Path out = Paths.get(a[1]);
        int n = 0;
        for (String r : refs) {
            if (r.startsWith("[") || inJar.contains(r) || r.startsWith("java/") || r.startsWith("javax/")
                || r.startsWith("com/google/gson") || r.startsWith("com/google/common") || r.startsWith("org/apache") || r.startsWith("org/objectweb")) continue;
            boolean iface = ifaces.contains(r);
            ClassWriter cw = new ClassWriter(0);
            cw.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC | (iface ? Opcodes.ACC_INTERFACE | Opcodes.ACC_ABSTRACT : Opcodes.ACC_SUPER), r, null, "java/lang/Object", null);
            cw.visitEnd();
            Path p = out.resolve(r + ".class");
            Files.createDirectories(p.getParent());
            Files.write(p, cw.toByteArray());
            n++;
        }
        System.out.println("stubs: " + n);
    }
}
