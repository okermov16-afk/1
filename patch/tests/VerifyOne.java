public class VerifyOne {
    public static void main(String[] a) throws Exception {
        Rig rig = new Rig(a[0], VerifyOne.class.getClassLoader());
        for (int i = 1; i < a.length; i++) {
            String r;
            try { Class.forName(a[i], true, rig.cl); r = "verified+initialized"; }
            catch (VerifyError e) { r = "VERIFY ERROR: " + e.getMessage().split("\n")[0]; }
            catch (Throwable t) { r = "linked OK (init stopped by stub env: " + t.getClass().getSimpleName() + ")"; }
            System.out.println(a[i].substring(a[i].lastIndexOf('.') + 1) + ": " + r);
        }
    }
}
