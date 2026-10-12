package nightvision;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import net.minecraft.client.settings.KeyBinding;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Delete toggles night vision (client side only). The key is a normal KeyBinding, so it can be rebound in Controls.
 * <p>
 * Everything that touches Minecraft's own (obfuscated in production) members goes through reflection with both the
 * MCP and the SRG name, so the jar works in a normal launcher and in a dev environment. Only Forge API members and
 * constructors, whose names never change, are linked directly.
 */
@Mod(modid = "nightvision", name = "Night Vision Toggle", version = "1.0", acceptedMinecraftVersions = "[1.12.2]", clientSideOnly = true)
public class NightVision {
    private static final Logger LOG = LogManager.getLogger("nightvision");
    private static final int KEY_DELETE = 211;
    private static final int DURATION = 600;   // ticks; refreshed every REFRESH ticks, far above vanilla's 200-tick flicker
    private static final int REFRESH = 20;

    private KeyBinding key;
    private boolean on;
    private int sinceRefresh;
    private Object lastPlayer;

    private Object potion;                // night_vision
    private Constructor<?> effectCtor;    // PotionEffect(Potion,int,int,boolean,boolean)
    private Object clientHandler;         // FMLClientHandler
    private Method getPlayer, isPressed;

    @Mod.EventHandler
    public void init(FMLInitializationEvent e) {
        try {
            key = new KeyBinding("key.nightvision.toggle", KEY_DELETE, "key.categories.nightvision");
            ClientRegistry.registerKeyBinding(key);
            MinecraftForge.EVENT_BUS.register(this);

            Class<?> fch = Class.forName("net.minecraftforge.fml.client.FMLClientHandler");
            clientHandler = fch.getMethod("instance").invoke(null);
            getPlayer = find(fch, null, 0, "getClientPlayerEntity");
            isPressed = find(KeyBinding.class, null, 0, "isPressed", "func_151468_f");
            potion = lookupPotion();
            Class<?> potionCls = Class.forName("net.minecraft.potion.Potion");
            effectCtor = Class.forName("net.minecraft.potion.PotionEffect")
                    .getConstructor(potionCls, int.class, int.class, boolean.class, boolean.class);
            if (getPlayer == null || isPressed == null || potion == null)
                LOG.error("nightvision: unresolved getPlayer={} isPressed={} potion={}", getPlayer, isPressed, potion);
        } catch (Throwable t) {
            LOG.error("nightvision: setup failed", t);
            key = null;
        }
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END || key == null || getPlayer == null || isPressed == null || potion == null) return;
        try {
            Object p = getPlayer.invoke(clientHandler);
            if (p == null) {   // presses made outside a world must not toggle later
                lastPlayer = null;
                while ((Boolean) isPressed.invoke(key)) { }
                return;
            }
            boolean changed = false;
            while ((Boolean) isPressed.invoke(key)) {
                on = !on;
                changed = true;
                if (on) apply(p); else clear(p);
                message(p);
            }
            if (!on) { lastPlayer = p; return; }
            // a new player object (respawn, new world) has no effect yet
            if (p != lastPlayer && !changed) apply(p);
            else if (++sinceRefresh >= REFRESH) apply(p);
            lastPlayer = p;
        } catch (Throwable t) {
            LOG.error("nightvision: tick failed, disabling", t);
            key = null;
        }
    }

    private void apply(Object player) throws Exception {
        Method add = find(player.getClass(), null, 1, "addPotionEffect", "func_70690_d");
        add.invoke(player, effectCtor.newInstance(potion, DURATION, 0, false, false));
        sinceRefresh = 0;
    }

    private void clear(Object player) throws Exception {
        Method rm = find(player.getClass(), potion.getClass(), 1,
                "removePotionEffect", "func_184596_c", "removeActivePotionEffect", "func_184589_d");
        if (rm != null) rm.invoke(player, potion);
    }

    private void message(Object player) throws Exception {
        Method m = find(player.getClass(), TextComponentString.class, 2, "sendStatusMessage", "func_146105_b");
        if (m != null)
            m.invoke(player, new TextComponentString(on ? "Ночное зрение: ВКЛ"
                    : "Ночное зрение: ВЫКЛ"), true);
    }

    private static Object lookupPotion() throws Exception {
        ResourceLocation id = new ResourceLocation("minecraft:night_vision");
        for (String cn : new String[] {"net.minecraftforge.registries.ForgeRegistries", "net.minecraftforge.fml.common.registry.ForgeRegistries"}) {
            try {
                Object reg = Class.forName(cn).getField("POTIONS").get(null);
                Method gv = find(reg.getClass(), ResourceLocation.class, 1, "getValue");
                Object v = gv == null ? null : gv.invoke(reg, id);
                if (v != null) return v;
            } catch (ClassNotFoundException | NoSuchFieldException ignored) {
            }
        }
        Method byName = find(Class.forName("net.minecraft.potion.Potion"), String.class, 1, "getPotionFromResourceLocation", "func_180142_b");
        return byName == null ? null : byName.invoke(null, "minecraft:night_vision");
    }

    /** First method with one of the names and the given parameter count (and first parameter type, if given), searching superclasses. */
    static Method find(Class<?> c, Class<?> firstParam, int n, String... names) {
        for (String nm : names) {
            for (Class<?> k = c; k != null; k = k.getSuperclass()) {
                for (Method m : k.getDeclaredMethods()) {
                    if (!m.getName().equals(nm) || m.getParameterTypes().length != n || m.isBridge()) continue;
                    if (firstParam != null && !m.getParameterTypes()[0].isAssignableFrom(firstParam)) continue;
                    m.setAccessible(true);
                    return m;
                }
            }
        }
        return null;
    }
}
