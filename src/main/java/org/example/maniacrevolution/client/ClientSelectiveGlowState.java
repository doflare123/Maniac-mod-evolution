package org.example.maniacrevolution.client;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.example.maniacrevolution.Maniacrev;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = Maniacrev.MODID, value = Dist.CLIENT)
public final class ClientSelectiveGlowState {
    private static final Map<TargetKey, ActiveGlow> ACTIVE = new HashMap<>();
    private static int tickCounter;

    private ClientSelectiveGlowState() {
    }

    public static void setGlow(ResourceLocation dimension, UUID entityUuid, int entityId, boolean enabled) {
        TargetKey key = new TargetKey(dimension, entityUuid);
        if (enabled) {
            ActiveGlow state = ACTIVE.computeIfAbsent(key, ignored -> new ActiveGlow(entityId));
            state.entityId = entityId;
            apply(key, state);
            return;
        }

        ActiveGlow state = ACTIVE.remove(key);
        if (state != null) {
            restore(key, state);
        }
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || ACTIVE.isEmpty() || ++tickCounter % 10 != 0) {
            return;
        }

        ACTIVE.forEach(ClientSelectiveGlowState::apply);
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        ACTIVE.forEach(ClientSelectiveGlowState::restore);
        ACTIVE.clear();
        tickCounter = 0;
    }

    private static void apply(TargetKey key, ActiveGlow state) {
        Entity entity = findEntity(key, state.entityId);
        if (entity == null) {
            return;
        }

        if (state.originalGlowing == null) {
            state.originalGlowing = entity.hasGlowingTag();
        }
        if (!entity.hasGlowingTag()) {
            entity.setGlowingTag(true);
        }
    }

    private static void restore(TargetKey key, ActiveGlow state) {
        Entity entity = findEntity(key, state.entityId);
        if (entity != null && state.originalGlowing != null) {
            entity.setGlowingTag(state.originalGlowing);
        }
    }

    private static Entity findEntity(TargetKey key, int entityId) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || !minecraft.level.dimension().location().equals(key.dimension)) {
            return null;
        }

        Entity entity = minecraft.level.getEntity(entityId);
        return entity != null && entity.getUUID().equals(key.entityUuid) ? entity : null;
    }

    private record TargetKey(ResourceLocation dimension, UUID entityUuid) {
    }

    private static final class ActiveGlow {
        private int entityId;
        private Boolean originalGlowing;

        private ActiveGlow(int entityId) {
            this.entityId = entityId;
        }
    }
}
