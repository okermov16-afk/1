package multi;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import net.minecraft.client.settings.KeyBinding;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.client.event.sound.PlaySoundEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Multi (client side only): small toggles on keys.
 * <ul>
 * <li><b>Delete</b> - night vision on/off.</li>
 * <li><b>Page Down</b> - all sound on/off.</li>
 * </ul>
 * Both are normal KeyBindings, so they can be rebound in Controls.
 * <p>
 * Everything that touches Minecraft's own (obfuscated in production) members goes through reflection with both the
 * MCP and the SRG name, so the jar works in a normal launcher and in a dev environment. Only Forge API members and
 * constructors, whose names never change, are linked directly.
 */
@Mod(modid = "multi", name = "Multi", version = "1.0", acceptedMinecraftVersions = "[1.12.2]", clientSideOnly = true)
public class Multi {
    private static final Logger LOG = LogManager.getLogger("multi");
    private static final int KEY_DELETE = 211;
    private static final int KEY_PAGE_DOWN = 209;
    private static final int DURATION = 600;   // ticks; refreshed every REFRESH ticks, far above vanilla's 200-tick flicker
    private static final int REFRESH = 20;

    private KeyBinding nvKey, muteKey;
    private boolean nightVision;
    private volatile boolean muted;
    private int sinceRefresh;
    private Object lastPlayer;

    private Object potion;                // night_vision
    private Constructor<?> effectCtor;    // PotionEffect(Potion,int,int,boolean,boolean)
    private Object clientHandler;         // FMLClientHandler
    private Method getPlayer, isPressed;

    @Mod.EventHandler
    public void init(FMLInitializationEvent e) {
        try {
            nvKey = new KeyBinding("key.multi.nightvision", KEY_DELETE, "key.categories.multi");
            muteKey = new KeyBinding("key.multi.mute", KEY_PAGE_DOWN, "key.categories.multi");
            ClientRegistry.registerKeyBinding(nvKey);
            ClientRegistry.registerKeyBinding(muteKey);
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
                LOG.error("multi: unresolved getPlayer={} isPressed={} potion={}", getPlayer, isPressed, potion);
        } catch (Throwable t) {
            LOG.error("multi: setup failed", t);
            nvKey = null;
            muteKey = null;
        }
    }

    /** While muted every new sound (effects, music, ambient) is cancelled before it starts. */
    @SubscribeEvent
    public void onSound(PlaySoundEvent e) {
        if (!muted) return;
        try {
            Method set = find(e.getClass(), null, 1, "setResultSound");
            if (set != null) set.invoke(e, new Object[] {null});
        } catch (Throwable t) {
            LOG.error("multi: could not cancel a sound", t);
        }
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END || nvKey == null || isPressed == null) return;
        try {
            Object p = getPlayer == null ? null : getPlayer.invoke(clientHandler);
            if (p == null) {   // presses made outside a world must not toggle later
                lastPlayer = null;
                while ((Boolean) isPressed.invoke(nvKey)) { }
                while ((Boolean) isPressed.invoke(muteKey)) { }
                return;
            }
            while ((Boolean) isPressed.invoke(muteKey)) {
                muted = !muted;
                if (muted) stopPlayingSounds();
                message(p, muted ? "Звук: ВЫКЛ" : "Звук: ВКЛ");
            }
            if (potion == null) return;
            boolean changed = false;
            while ((Boolean) isPressed.invoke(nvKey)) {
                nightVision = !nightVision;
                changed = true;
                if (nightVision) apply(p); else clear(p);
                message(p, nightVision ? "Ночное зрение: ВКЛ"
                        : "Ночное зрение: ВЫКЛ");
            }
            if (!nightVision) { lastPlayer = p; return; }
            // a new player object (respawn, new world) has no effect yet
            if (p != lastPlayer && !changed) apply(p);
            else if (++sinceRefresh >= REFRESH) apply(p);
            lastPlayer = p;
        } catch (Throwable t) {
            LOG.error("multi: tick failed, disabling", t);
            nvKey = null;
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

    private void message(Object player, String text) throws Exception {
        Method m = find(player.getClass(), TextComponentString.class, 2, "sendStatusMessage", "func_146105_b");
        if (m != null) m.invoke(player, new TextComponentString(text), true);
    }

    /** Best effort: sounds that are already playing (music, long loops) when mute is switched on. */
    private static void stopPlayingSounds() {
        try {
            Class<?> mc = Class.forName("net.minecraft.client.Minecraft");
            Object inst = find(mc, null, 0, "getMinecraft", "func_71410_x").invoke(null);
            java.lang.reflect.Field f = null;
            for (String n : new String[] {"soundHandler", "field_147127_av"}) {
                try { f = mc.getDeclaredField(n); break; } catch (NoSuchFieldException ignored) { }
            }
            if (f == null) return;
            f.setAccessible(true);
            Object handler = f.get(inst);
            Method stop = find(handler.getClass(), null, 0, "stopSounds", "func_147690_c");
            if (stop != null) stop.invoke(handler);
        } catch (Throwable t) {
            LOG.error("multi: could not stop playing sounds", t);
        }
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
