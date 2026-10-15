package org.example.maniacrevolution.warden.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.example.maniacrevolution.Maniacrev;
import org.example.maniacrevolution.network.packets.WardenAnimationPacket;
import org.example.maniacrevolution.warden.WardenAnimationTimeline;
import java.util.*;

@Mod.EventBusSubscriber(modid = Maniacrev.MODID, value = Dist.CLIENT)
public final class WardenAnimationsClient {
    private static final Map<UUID, WardenAnimationTimeline.State> STATES = new HashMap<>();
    private static Object level, player;
    private WardenAnimationsClient() {}
    public static void accept(WardenAnimationPacket packet) {
        var mc = Minecraft.getInstance();
        if (mc.level == null || !packet.valid() || !packet.dimension().equals(mc.level.dimension().location())
                || packet.state().tick() - mc.level.getGameTime() > 20 || mc.level.getGameTime() - packet.state().tick() > WardenAnimationTimeline.LEASE) return;
        if (level != mc.level || player != mc.player) { STATES.clear(); level = mc.level; player = mc.player; }
        var old = STATES.get(packet.player());
        if (old != null && old.tick() > packet.state().tick()) return;
        if (packet.state().action() == WardenAnimationTimeline.Action.NONE && packet.state().sniff() < 0) STATES.remove(packet.player());
        else if (STATES.size() < 256 || old != null) STATES.put(packet.player(), packet.state());
    }
    static WardenAnimationTimeline.Frame frame(AbstractClientPlayer target, float partial) {
        if (WardenPlayerForm.isPreview(target)) return WardenAnimationTimeline.Frame.IDLE;
        var state = STATES.get(target.getUUID());
        return state == null || target.level() != level ? WardenAnimationTimeline.Frame.IDLE : state.frame(target.level().getGameTime() + partial);
    }
    static void remove(UUID id) { STATES.remove(id); }
    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut e) { STATES.clear(); level = player = null; }
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        var mc = Minecraft.getInstance();
        if (level != mc.level || player != mc.player) { STATES.clear(); level = mc.level; player = mc.player; }
        if (mc.level == null) return;
        STATES.entrySet().removeIf(entry -> {
            var p = mc.level.getPlayerByUUID(entry.getKey()); var f = entry.getValue().frame(mc.level.getGameTime());
            return p != null && (!p.isAlive() || p.isSpectator()) || f.action() == WardenAnimationTimeline.Action.NONE && f.sniff() < 0;
        });
    }
}
