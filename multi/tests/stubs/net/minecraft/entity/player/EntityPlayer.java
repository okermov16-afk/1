package net.minecraft.entity.player;
import net.minecraft.util.text.ITextComponent;
import java.util.*;
public class EntityPlayer extends net.minecraft.entity.EntityLivingBase {
    public final List<String> messages = new ArrayList<>();
    public void func_146105_b(ITextComponent c, boolean b) { messages.add(((net.minecraft.util.text.TextComponentString) c).t + "|" + b); }
}
