import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.client.event.sound.PlaySoundEvent;
import net.minecraftforge.fml.client.FMLClientHandler;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Keyboard;
import multi.Multi;

public class Run {
    static int fails;
    static Multi m;
    static final TickEvent.ClientTickEvent END = new TickEvent.ClientTickEvent(TickEvent.Phase.END);
    static void check(boolean ok, String what) { System.out.println((ok ? "ok   " : "FAIL ") + what); if (!ok) fails++; }
    /** a real key press: down for one tick, up the next */
    static void press(int code) { Keyboard.down.add(code); m.onTick(END); Keyboard.down.remove(code); m.onTick(END); }
    static boolean soundCancelled() { PlaySoundEvent s = new PlaySoundEvent(); new Multi.Mute().onSound(s); return s.result == null; }

    public static void main(String[] a) throws Exception {
        m = new Multi();
        m.init(null);
        check(net.minecraftforge.fml.client.registry.ClientRegistry.registered == 2, "both keys registered in Controls");
        EntityPlayer p = new EntityPlayer();

        // ---- main menu: no player
        FMLClientHandler.player = null;
        press(211);
        check(p.effects.isEmpty(), "main menu: Delete does nothing");
        press(209);
        check(soundCancelled(), "main menu: Page Down mutes");
        check(Minecraft.I.field_147127_av.stops == 1, "main menu: playing sounds stopped");
        press(209);
        check(!soundCancelled(), "main menu: Page Down unmutes");

        // ---- in a world
        FMLClientHandler.player = p;
        for (int i = 0; i < 3; i++) m.onTick(END);
        check(p.effects.isEmpty(), "idle: no effect");
        press(211);
        check(p.effects.size() == 1 && p.effects.get(0).dur == 600 && !p.effects.get(0).part && !p.effects.get(0).amb, "Delete -> night vision on, no particles");
        check(p.messages.size() == 1 && p.messages.get(0).endsWith("|true") && p.messages.get(0).contains("ВКЛ"), "message ON");
        p.effects.clear();
        for (int i = 0; i < 25; i++) m.onTick(END);
        check(p.effects.size() == 1, "refreshed while on");
        press(211);
        check(p.effects.isEmpty() && p.messages.get(1).contains("ВЫКЛ"), "Delete again -> off + message");
        for (int i = 0; i < 40; i++) m.onTick(END);
        check(p.effects.isEmpty(), "stays off");

        // holding the key down toggles only once
        Keyboard.down.add(211); for (int i = 0; i < 10; i++) m.onTick(END); Keyboard.down.remove(211); m.onTick(END);
        check(p.effects.size() == 1, "holding Delete toggles once");

        // chat / inventory open: Delete ignored
        Minecraft.I.field_71462_r = new Object();
        press(211);
        check(p.effects.size() == 1, "screen open: Delete ignored");
        Minecraft.I.field_71462_r = null;

        // respawn
        EntityPlayer p2 = new EntityPlayer(); FMLClientHandler.player = p2;
        m.onTick(END);
        check(p2.effects.size() == 1, "new player object gets the effect at once");

        // mute in the world: message, night vision untouched
        p2.messages.clear();
        press(209);
        check(soundCancelled(), "Page Down in world -> muted");
        check(p2.messages.size() == 1 && p2.messages.get(0).contains("Звук: ВЫКЛ"), "message: sound OFF");
        check(p2.effects.size() == 1, "night vision unaffected by mute");
        press(209);
        check(!soundCancelled(), "Page Down again -> sound back");

        // rebinding in Controls is honoured
        java.lang.reflect.Field f = Multi.class.getDeclaredField("muteKey"); f.setAccessible(true);
        ((net.minecraft.client.settings.KeyBinding) f.get(m)).code = 24;
        press(209);
        check(!soundCancelled(), "old key no longer mutes after rebinding");
        press(24);
        check(soundCancelled(), "rebound key mutes");

        System.out.println(fails == 0 ? "ALL OK" : fails + " FAILED");
        System.exit(fails == 0 ? 0 : 1);
    }
}
