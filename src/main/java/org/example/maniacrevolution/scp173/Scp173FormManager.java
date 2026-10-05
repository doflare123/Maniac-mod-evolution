package org.example.maniacrevolution.scp173;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.example.maniacrevolution.Maniacrev;
import org.example.maniacrevolution.network.ModNetworking;
import org.example.maniacrevolution.network.packets.Scp173FormPacket;

/** Visual form follows the same server eligibility as gaze restrictions. */
@Mod.EventBusSubscriber(modid = Maniacrev.MODID)
public final class Scp173FormManager {
    private static final Set<UUID> ACTIVE = new HashSet<>();
    private Scp173FormManager() {}

    @SubscribeEvent
    public static void tick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        var server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;
        for (var player : server.getPlayerList().getPlayers()) {
            boolean active = Scp173Manager.active(player);
            boolean changed = active ? ACTIVE.add(player.getUUID()) : ACTIVE.remove(player.getUUID());
            if (changed) ModNetworking.sendToAllPlayers(new Scp173FormPacket(player.getUUID(), active));
        }
    }

    @SubscribeEvent
    public static void login(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer receiver)) return;
        for (var player : receiver.getServer().getPlayerList().getPlayers())
            ModNetworking.sendToPlayer(new Scp173FormPacket(player.getUUID(), Scp173Manager.active(player)), receiver);
    }

    @SubscribeEvent
    public static void tracking(PlayerEvent.StartTracking event) {
        if (event.getEntity() instanceof ServerPlayer receiver && event.getTarget() instanceof ServerPlayer target)
            ModNetworking.sendToPlayer(new Scp173FormPacket(target.getUUID(), Scp173Manager.active(target)), receiver);
    }

    @SubscribeEvent
    public static void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && ACTIVE.remove(player.getUUID()))
            ModNetworking.sendToAllPlayers(new Scp173FormPacket(player.getUUID(), false));
    }

    @SubscribeEvent
    public static void stopped(ServerStoppedEvent event) { ACTIVE.clear(); }
}
