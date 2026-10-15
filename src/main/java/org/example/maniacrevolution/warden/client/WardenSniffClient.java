package org.example.maniacrevolution.warden.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.example.maniacrevolution.Maniacrev;
import org.example.maniacrevolution.data.ClientPlayerData;
import org.example.maniacrevolution.data.ClientGameState;
import org.example.maniacrevolution.warden.*;
import org.example.maniacrevolution.network.packets.WardenScentPacket;
import org.example.maniacrevolution.util.PlayerModeUtil;
import java.util.List;

@Mod.EventBusSubscriber(modid = Maniacrev.MODID, value = Dist.CLIENT)
public final class WardenSniffClient {
    private static WardenScentPacket state;
    private static ClientLevel level;
    private static LocalPlayer player;
    private static WardenSurfaceCache cache = new WardenSurfaceCache(20, 8192), next;
    private static final WardenDynamicScene SCENE = new WardenDynamicScene(20);
    private WardenSniffClient() {}
    public static boolean eligible() {
        var mc = Minecraft.getInstance();
        return mc.player != null && WardenMatchRules.active(mc.player.isAlive(), PlayerModeUtil.isSurvivalOrAdventure(mc.player),
                ClientGameState.getPhase(), ClientPlayerData.getManiacClassId(), mc.player.getTeam() == null ? null : mc.player.getTeam().getName());
    }
    private static long age() { return state == null || Minecraft.getInstance().level == null ? 0 : Math.max(0, Minecraft.getInstance().level.getGameTime() - state.tick()); }
    public static int preparation() { return state == null ? 0 : (int) Math.max(0, state.preparation() - age()); }
    public static int duration() { return state == null ? 0 : (int) Math.max(0, state.duration() - age()); }
    public static int cooldown() { return state == null ? 0 : (int) Math.max(0, state.cooldown() - age()); }
    public static void accept(WardenScentPacket packet) {
        var mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || !eligible() || !packet.valid()
                || !packet.dimension().equals(mc.level.dimension().location()) || packet.tick() - mc.level.getGameTime() > 20
                || mc.level.getGameTime() - packet.tick() > WardenSniffCycle.COOLDOWN) return;
        if (level != mc.level || player != mc.player) { reset(); level = mc.level; player = mc.player; }
        if (state == null || packet.tick() >= state.tick()) state = packet;
    }
    static List<WardenScentPoint> points() { return duration() > 0 && state != null ? state.points() : List.of(); }
    static WardenSurfaceCache cache() { return cache; }
    static WardenDynamicScene scene() { return SCENE; }
    static boolean capturing() { return SCENE.capturing(); }
    static boolean readyForCapture() {
        var mc = Minecraft.getInstance(); var center = cache.center();
        return WardenVisionPrototype.inMatch() && duration() > 0 && !points().isEmpty() && cache.ready()
                && center != null && mc.gameRenderer != null
                && Math.abs(mc.gameRenderer.getMainCamera().getPosition().x - center.getX()) <= 19
                && Math.abs(mc.gameRenderer.getMainCamera().getPosition().y - center.getY()) <= 19
                && Math.abs(mc.gameRenderer.getMainCamera().getPosition().z - center.getZ()) <= 19;
    }
    static boolean renderable() { return readyForCapture(); }
    static void clearVisuals() { cache.clear(); next = null; SCENE.clear(); WardenVisionPrototype.clearScentBuffers(); }
    private static void reset() { state = null; level = null; player = null; clearVisuals(); }
    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut event) { reset(); }
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        var mc = Minecraft.getInstance();
        if (level != mc.level || player != mc.player || !eligible()) { reset(); return; }
        if (mc.isPaused()) return;
        if (!WardenVisionPrototype.inMatch() || preparation() == 0 && duration() == 0) { clearVisuals(); return; }
        if (cache.center() == null) cache.start(mc.player.blockPosition());
        cache.tick(mc);
        var center = cache.center();
        if (next == null && (Math.abs(mc.player.getX() - center.getX()) > 2 || Math.abs(mc.player.getY() - center.getY()) > 2
                || Math.abs(mc.player.getZ() - center.getZ()) > 2)) {
            next = new WardenSurfaceCache(20, 8192); next.start(mc.player.blockPosition());
        }
        if (next != null) {
            next.tick(mc);
            if (next.ready()) { cache.clear(); cache = next; next = null; SCENE.clear(); }
        }
    }
}
