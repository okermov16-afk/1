package net.minecraft.entity;
import net.minecraft.potion.*;
import java.util.*;
public class EntityLivingBase {
    public final List<PotionEffect> effects = new ArrayList<>();
    public void func_70690_d(PotionEffect e) { effects.removeIf(x -> x.p == e.p); effects.add(e); }
    public void func_184596_c(Potion p) { effects.removeIf(x -> x.p == p); }
}
