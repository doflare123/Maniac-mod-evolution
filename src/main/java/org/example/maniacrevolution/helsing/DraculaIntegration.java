package org.example.maniacrevolution.helsing;

import net.minecraft.server.level.ServerPlayer;

/** Install an adapter when Dracula is introduced; Helsing never guesses a future class ID. */
public final class DraculaIntegration {
    public interface Adapter {
        boolean isDracula(ServerPlayer player);
        float blood(ServerPlayer player);
        float maxBlood(ServerPlayer player);
        void reduceBloodFromNextHits(ServerPlayer player, int hits, float reduction);
    }
    private static Adapter adapter;
    public static void install(Adapter value) { adapter = value; }
    public static Adapter get(ServerPlayer player) {
        return adapter != null && adapter.isDracula(player) ? adapter : null;
    }
    private DraculaIntegration() {}
}
