package contactwand.core;

import java.util.Map;
import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;

@IFMLLoadingPlugin.MCVersion("1.12.2")
@IFMLLoadingPlugin.Name("Contact Wand Core")
@IFMLLoadingPlugin.SortingIndex(1103)
@IFMLLoadingPlugin.TransformerExclusions({"contactwand.core"})
public class ContactPlugin implements IFMLLoadingPlugin {
    public String[] getASMTransformerClass() {
        return new String[]{"contactwand.core.ContactTransformer"};
    }

    public String getModContainerClass() {
        return null;
    }

    public String getSetupClass() {
        return null;
    }

    public void injectData(Map<String, Object> data) {
    }

    public String getAccessTransformerClass() {
        return null;
    }
}
