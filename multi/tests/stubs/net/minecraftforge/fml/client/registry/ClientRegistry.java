package net.minecraftforge.fml.client.registry;
public class ClientRegistry { public static int registered; public static void registerKeyBinding(net.minecraft.client.settings.KeyBinding k) { registered++; } }
