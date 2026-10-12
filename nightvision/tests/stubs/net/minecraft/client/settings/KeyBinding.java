package net.minecraft.client.settings;
public class KeyBinding {
    public int presses;
    public KeyBinding(String d, int k, String c) {}
    public boolean func_151468_f() { if (presses > 0) { presses--; return true; } return false; }
}
