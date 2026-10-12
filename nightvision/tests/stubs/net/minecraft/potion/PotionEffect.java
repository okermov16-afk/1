package net.minecraft.potion;
public class PotionEffect { public final Potion p; public final int dur, amp; public final boolean amb, part;
    public PotionEffect(Potion p, int d, int a, boolean amb, boolean part) { this.p = p; dur = d; amp = a; this.amb = amb; this.part = part; } }
