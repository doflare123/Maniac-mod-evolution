package org.example.maniacrevolution.warden;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.example.maniacrevolution.Maniacrev;
import org.example.maniacrevolution.data.PlayerDataManager;
import org.example.maniacrevolution.game.GameManager;
import org.example.maniacrevolution.network.ModNetworking;
import org.example.maniacrevolution.network.packets.WardenFormPacket;

/** Server decides the form, including the game mode of remote players. No attributes are changed. */
@Mod.EventBusSubscriber(modid = Maniacrev.MODID)
public final class WardenFormManager {
    private static final WardenFormState STATE = new WardenFormState();

    private WardenFormManager() {}

    private static boolean active(ServerPlayer player) {
        GameType mode = player.gameMode.getGameModeForPlayer();
        return WardenMatchRules.active(player.isAlive(), mode == GameType.SURVIVAL || mode == GameType.ADVENTURE,
                GameManager.getPhaseValue(), PlayerDataManager.get(player).getManiacClassId(),
                player.getTeam() == null ? null : player.getTeam().getName());
    }

    @SubscribeEvent
    public static void tick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        var server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;
        for (var player : server.getPlayerList().getPlayers()) {
            boolean form = active(player);
            if (STATE.update(player.getUUID(), form))
                ModNetworking.sendToAllPlayers(new WardenFormPacket(player.getUUID(), form));
        }
    }

    @SubscribeEvent
    public static void login(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer receiver)) return;
        // Snapshot independent of class-packet order and of whether the form changed this tick.
        for (var player : receiver.getServer().getPlayerList().getPlayers())
            ModNetworking.sendToPlayer(new WardenFormPacket(player.getUUID(), active(player)), receiver);
    }

    @SubscribeEvent
    public static void tracking(PlayerEvent.StartTracking event) {
        if (event.getEntity() instanceof ServerPlayer receiver && event.getTarget() instanceof ServerPlayer target)
            ModNetworking.sendToPlayer(new WardenFormPacket(target.getUUID(), active(target)), receiver);
    }

    @SubscribeEvent
    public static void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && STATE.update(player.getUUID(), false))
            ModNetworking.sendToAllPlayers(new WardenFormPacket(player.getUUID(), false));
    }

    @SubscribeEvent
    public static void stopped(ServerStoppedEvent event) { STATE.clear(); }
}
