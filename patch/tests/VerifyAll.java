import java.util.*;
import java.util.zip.*;
import java.io.*;

public class VerifyAll {
    public static void main(String[] a) throws Exception {
        ClassLoader parent = VerifyAll.class.getClassLoader();
        for (String jar : a) {
            Rig rig = new Rig(jar, parent);
            List<String> names = new ArrayList<>();
            try (ZipFile z = new ZipFile(jar)) { for (Enumeration<? extends ZipEntry> e = z.entries(); e.hasMoreElements();) { String n = e.nextElement().getName(); if (n.endsWith(".class")) names.add(n.substring(0, n.length() - 6).replace('/', '.')); } }
            Map<String, Integer> kinds = new TreeMap<>();
            List<String> verify = new ArrayList<>();
            for (String n : names) {
                try { Class.forName(n, true, rig.cl); kinds.merge("ok", 1, Integer::sum); }
                catch (VerifyError e) { kinds.merge("VerifyError", 1, Integer::sum); verify.add(n + ": " + e.getMessage().split("\n")[0]); }
                catch (ClassFormatError e) { kinds.merge("ClassFormatError", 1, Integer::sum); verify.add(n + ": " + e); }
                catch (Throwable t) { kinds.merge(t.getClass().getSimpleName(), 1, Integer::sum); }
            }
            System.out.println(jar.substring(jar.lastIndexOf('/') + 1) + " -> " + kinds);
            for (String v : verify) System.out.println("   " + v);
        }
    }
}
