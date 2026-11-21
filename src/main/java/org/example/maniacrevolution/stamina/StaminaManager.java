package org.example.maniacrevolution.stamina;

import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.example.maniacrevolution.Maniacrev;
import org.example.maniacrevolution.network.ModNetworking;
import org.example.maniacrevolution.network.packets.StaminaSyncPacket;

import java.util.HashMap;
import java.util.Map;

@Mod.EventBusSubscriber(modid = Maniacrev.MODID)
public final class StaminaManager {
    private static final String SAVE_KEY = "maniacrev:stamina";
    private static final int SYNC_INTERVAL = 4;
    private static final int RESYNC_INTERVAL = 20;
    private static final Map<ServerPlayer, State> STATES = new HashMap<>();

    private static final class State {
        final Stamina stamina;
        int lastSent = -1;
        int lastSync;

        State(ServerPlayer player) {
            var data = player.getPersistentData();
            stamina = new Stamina(data.contains(SAVE_KEY, Tag.TAG_INT) ? data.getInt(SAVE_KEY) : Stamina.MAX);
        }
    }

    private StaminaManager() {}

    public static boolean canSprint(ServerPlayer player) {
        return !StaminaMovement.usesStamina(player) || state(player).stamina.amount() > 0;
    }

    public static boolean canJump(ServerPlayer player) {
        return !StaminaMovement.usesStamina(player) || state(player).stamina.amount() >= Stamina.CELL;
    }

    public static boolean tryJump(ServerPlayer player) {
        if (!StaminaMovement.usesStamina(player)) return true;
        State state = state(player);
        if (!state.stamina.spendJump()) return false;
        store(player, state);
        sync(player, state);
        return true;
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void tick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) return;
        State state = state(player);
        int previous = state.stamina.amount();
        if (StaminaMovement.usesStamina(player)) {
            boolean sprinting = player.isSprinting() && !player.isCrouching() && previous > 0;
            state.stamina.tick(sprinting, player.onGround());
            if (player.isSprinting() && (state.stamina.amount() == 0 || previous == 0)) player.setSprinting(false);
            if (previous != state.stamina.amount()) store(player, state);
        }
        int amount = state.stamina.amount();
        int sinceSync = player.tickCount - state.lastSync;
        if (state.lastSent < 0 || sinceSync >= RESYNC_INTERVAL || amount != state.lastSent
                && (amount == 0 || amount == Stamina.MAX || sinceSync >= SYNC_INTERVAL)) {
            sync(player, state);
        }
    }

    @SubscribeEvent
    public static void changeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) state(player).lastSent = -1;
    }

    @SubscribeEvent
    public static void clonePlayer(PlayerEvent.Clone event) {
        STATES.remove(event.getOriginal());
        var oldData = event.getOriginal().getPersistentData();
        if (!event.isWasDeath() && oldData.contains(SAVE_KEY, Tag.TAG_INT)) {
            event.getEntity().getPersistentData().putInt(SAVE_KEY, oldData.getInt(SAVE_KEY));
        }
    }

    @SubscribeEvent
    public static void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        STATES.remove(event.getEntity());
    }

    @SubscribeEvent
    public static void stop(ServerStoppedEvent event) {
        STATES.clear();
    }

    private static State state(ServerPlayer player) {
        return STATES.computeIfAbsent(player, State::new);
    }

    private static void store(ServerPlayer player, State state) {
        player.getPersistentData().putInt(SAVE_KEY, state.stamina.amount());
    }

    private static void sync(ServerPlayer player, State state) {
        if (player.connection == null) return;
        state.lastSync = player.tickCount;
        state.lastSent = state.stamina.amount();
        ModNetworking.sendToPlayer(new StaminaSyncPacket(player.getId(), state.lastSent), player);
    }
}
