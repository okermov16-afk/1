package com.modelport.ported;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.BlockPos;
public final class ProbeWorld extends net.minecraft.world.World {
    public static int created;
    public ProbeWorld() { created++; }
    public EntityPlayer player() { return new EntityPlayer(); }
    public static BlockPos center() { return new BlockPos(8, 64, 8); }
}
