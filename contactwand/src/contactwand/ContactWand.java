package contactwand;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

/** Every block is contactable by default; the work is done by the core plugin, nothing is registered. */
@Mod(modid = "contactwand", name = "Contact Wand", version = "1.1", acceptedMinecraftVersions = "[1.12.2]")
public class ContactWand {
    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent e) {
        Cfg.load(e.getSuggestedConfigurationFile());
    }
}
