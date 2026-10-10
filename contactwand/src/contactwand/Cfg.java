package contactwand;

import java.io.File;
import net.minecraftforge.common.config.Configuration;

/** Switches for the global contact rules; read once at start-up, never polled. */
public final class Cfg {
    public static volatile boolean faceShape = true;
    public static volatile boolean sideSolid = true;
    public static volatile boolean placeChecks = true;
    public static volatile boolean placeAnySide = false;
    public static volatile boolean plants = true;
    public static volatile boolean normalCube = false;
    public static volatile boolean liquids = false;

    private Cfg() {
    }

    public static void load(File file) {
        try {
            Configuration c = new Configuration(file);
            c.load();
            String s = "hooks";
            faceShape = c.getBoolean("faceShape", s, true, "Любой блок отдаёт BlockFaceShape.SOLID: факелы, рычаги, кнопки, лестницы, лозы, растяжки. Заборы и панели соединяются с любым блоком.");
            sideSolid = c.getBoolean("sideSolid", s, true, "Любой блок считается твёрдой стороной: картины, рамки, рельсы, факел сверху.");
            placeChecks = c.getBoolean("placeChecks", s, true, "Проверки canPlaceBlockAt/OnSide/canBlockStay проходят рядом с любым блоком (плиты нажимные, редстоун, двери).");
            placeAnySide = c.getBoolean("placeAnySide", s, false, "Считать опорой любого из пяти соседей, а не только блок снизу.");
            plants = c.getBoolean("plants", s, true, "На любом блоке держатся растения (canSustainPlant).");
            normalCube = c.getBoolean("normalCube", s, false, "Любой блок считается полноценным кубом (isNormalCube). ВНИМАНИЕ: влияет на редстоун и pathfinding.");
            liquids = c.getBoolean("liquids", s, false, "Распространять правила и на жидкости. По умолчанию выключено: иначе мобы спавнятся на воде и лаве.");
            if (c.hasChanged()) {
                c.save();
            }
        } catch (Throwable t) {
            // defaults stay
        }
    }
}
