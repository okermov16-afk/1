package net.minecraft.block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
public class Block implements net.minecraftforge.registries.IForgeRegistryEntry<Block> {
    public IBlockState defaultState;
    public String name = "t:t";
    public IBlockState func_176203_a(int meta) { throw new UnsupportedOperationException("Block.func_176203_a"); }
    public IBlockState func_176223_P() { return defaultState; }
    public IBlockState getStateForPlacement(World w, BlockPos p, EnumFacing f, float x, float y, float z, int meta, EntityLivingBase placer, EnumHand h) { return func_176203_a(meta); }
    public int func_176201_c(IBlockState s) { throw new UnsupportedOperationException(); }
    public void func_180633_a(World w, BlockPos p, IBlockState s, EntityLivingBase placer, ItemStack st) { }
    public ResourceLocation getRegistryName() { return new ResourceLocation(name); }
}
