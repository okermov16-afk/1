import com.modelport.ported.*;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import java.util.Map;
public class FakePorted extends Block implements IPorted {
    public static int meta, variant; public static PortedBlock theData;
    public Block block() { return this; }
    public PortedBlock data() { return theData; }
    public Map<String, PortedBlock> sources() { return null; }
    public int metaOf(IBlockState s) { return meta; }
    public int variantIndexOf(IBlockState s) { return variant; }
    public boolean twoHalf() { return false; }
    public IBlockState[] placeDoor(net.minecraft.world.World w, net.minecraft.util.math.BlockPos p, net.minecraft.util.EnumFacing f, float a, float b) { return null; }
}
