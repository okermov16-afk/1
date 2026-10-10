package net.minecraft.block;
import net.minecraft.block.state.*;
import net.minecraft.block.material.Material;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
public class Block implements net.minecraftforge.registries.IForgeRegistryEntry<Block> {
    public IBlockState defaultState;
    public String name = "t:t";
    public BlockStateContainer field_176227_L;
    public boolean field_149787_q, field_149785_s;
    public Block() { }
    public Block(Material m) { field_176227_L = func_180661_e(); if (field_176227_L != null) defaultState = field_176227_L.func_177621_b(); }
    protected BlockStateContainer func_180661_e() { return null; }
    public net.minecraftforge.registries.IForgeRegistryEntry setRegistryName(String ns, String n) { name = ns + ":" + n; return this; }
    public Block func_149663_c(String s) { return this; }
    public Block func_149711_c(float f) { return this; }
    public Block func_149752_b(float f) { return this; }
    public Block func_149672_a(SoundType s) { return this; }
    public Block func_149647_a(net.minecraft.creativetab.CreativeTabs t) { return this; }
    public void func_180632_j(IBlockState s) { defaultState = s; }
    public IBlockState func_176223_P() { return defaultState; }
    public IBlockState func_176203_a(int meta) { throw new UnsupportedOperationException("Block.func_176203_a"); }
    public IBlockState getStateForPlacement(World w, BlockPos p, EnumFacing f, float x, float y, float z, int meta, EntityLivingBase placer, EnumHand h) { return func_176203_a(meta); }
    public int func_176201_c(IBlockState s) { throw new UnsupportedOperationException(); }
    public void func_180633_a(World w, BlockPos p, IBlockState s, EntityLivingBase placer, ItemStack st) { }
    public ResourceLocation getRegistryName() { return new ResourceLocation(name); }
}
