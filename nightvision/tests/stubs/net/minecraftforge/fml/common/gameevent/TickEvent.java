package net.minecraftforge.fml.common.gameevent;
public class TickEvent {
    public enum Phase { START, END }
    public Phase phase;
    public static class ClientTickEvent extends TickEvent { public ClientTickEvent(Phase p) { phase = p; } }
}
