import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.client.FMLClientHandler;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import multi.Multi;

public class Run {
    static int fails;
    static void check(boolean ok, String what) { System.out.println((ok ? "ok   " : "FAIL ") + what); if (!ok) fails++; }
    static java.lang.reflect.Field f(String n) throws Exception { java.lang.reflect.Field x = Multi.class.getDeclaredField(n); x.setAccessible(true); return x; }

    public static void main(String[] a) throws Exception {
        Multi m = new Multi();
        m.init(null);
        net.minecraft.client.settings.KeyBinding key = (net.minecraft.client.settings.KeyBinding) f("nvKey").get(m);
        check(key != null && net.minecraftforge.fml.client.registry.ClientRegistry.registered == 2, "key registered");
        TickEvent.ClientTickEvent end = new TickEvent.ClientTickEvent(TickEvent.Phase.END);
        EntityPlayer p = new EntityPlayer();
        FMLClientHandler.player = null;
        key.presses = 1; m.onTick(end);
        check(p.effects.isEmpty() && key.presses == 0, "no player: nothing happens, press discarded");
        FMLClientHandler.player = p;
        m.onTick(end);
        check(p.effects.isEmpty(), "idle tick: no effect");
        key.presses = 1; m.onTick(end);
        check(p.effects.size() == 1 && p.effects.get(0).dur == 600 && !p.effects.get(0).part && !p.effects.get(0).amb, "Delete -> night vision on, no particles");
        check(p.messages.size() == 1 && p.messages.get(0).endsWith("|true") && p.messages.get(0).contains("ВКЛ"), "action bar message ON");
        p.effects.clear();
        for (int i = 0; i < 25; i++) m.onTick(end);
        check(p.effects.size() == 1, "refreshed while on (effect lost -> re-added within 20 ticks)");
        key.presses = 1; m.onTick(end);
        check(p.effects.isEmpty(), "Delete again -> off");
        check(p.messages.size() == 2 && p.messages.get(1).contains("ВЫКЛ"), "action bar message OFF");
        for (int i = 0; i < 40; i++) m.onTick(end);
        check(p.effects.isEmpty(), "stays off");
        key.presses = 1; m.onTick(end);
        EntityPlayer p2 = new EntityPlayer(); FMLClientHandler.player = p2;
        m.onTick(end);
        check(p2.effects.size() == 1, "new player object (respawn) gets the effect at once");
        key.presses = 2; m.onTick(end);
        check(p2.effects.size() == 1, "two presses in one tick cancel out (on state kept)");
        TickEvent.ClientTickEvent start = new TickEvent.ClientTickEvent(TickEvent.Phase.START);
        key.presses = 1; m.onTick(start);
        check(key.presses == 1, "START phase ignored");
        // --- mute (Page Down)
        key.presses = 0;
        net.minecraft.client.settings.KeyBinding mkey = (net.minecraft.client.settings.KeyBinding) f("muteKey").get(m);
        net.minecraftforge.client.event.sound.PlaySoundEvent s1 = new net.minecraftforge.client.event.sound.PlaySoundEvent();
        m.onSound(s1);
        check(s1.result != null, "not muted: sound untouched");
        p2.messages.clear();
        mkey.presses = 1; m.onTick(end);
        net.minecraftforge.client.event.sound.PlaySoundEvent s2 = new net.minecraftforge.client.event.sound.PlaySoundEvent();
        m.onSound(s2);
        check(s2.result == null, "Page Down -> new sounds cancelled");
        check(p2.messages.size() == 1 && p2.messages.get(0).contains("\u0417\u0432\u0443\u043a: \u0412\u042b\u041a\u041b"), "message: sound OFF");
        check(p2.effects.size() == 1, "night vision unaffected by mute");
        mkey.presses = 1; m.onTick(end);
        net.minecraftforge.client.event.sound.PlaySoundEvent s3 = new net.minecraftforge.client.event.sound.PlaySoundEvent();
        m.onSound(s3);
        check(s3.result != null, "Page Down again -> sound back");
        System.out.println(fails == 0 ? "ALL OK" : fails + " FAILED");
        System.exit(fails == 0 ? 0 : 1);
    }
}
