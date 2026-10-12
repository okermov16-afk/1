package net.minecraftforge.fml.client;
public class FMLClientHandler {
    public static net.minecraft.entity.player.EntityPlayer player;
    private static final FMLClientHandler I = new FMLClientHandler();
    public static FMLClientHandler instance() { return I; }
    public net.minecraft.entity.player.EntityPlayer getClientPlayerEntity() { return player; }
}
