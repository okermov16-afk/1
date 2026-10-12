package net.minecraftforge.fml.common.registry;
public class ForgeRegistries {
    public static final Reg POTIONS = new Reg();
    public static class Reg {
        public Object getValue(int id) { return null; }
        public Object getValue(net.minecraft.util.ResourceLocation r) { return r.s.equals("minecraft:night_vision") ? NV : null; }
    }
    public static final net.minecraft.potion.Potion NV = new net.minecraft.potion.Potion();
}
