package org.example.maniacrevolution.client;

import net.minecraft.client.Minecraft;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;
import org.example.maniacrevolution.Maniacrev;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = Maniacrev.MODID, value = Dist.CLIENT)
public final class ClientSelectiveGlowState {
    private static final Map<TargetKey, ActiveGlow> ACTIVE = new HashMap<>();
    private static final int GLOWING_MASK = 1 << 6;
    private static final EntityDataAccessor<Byte> SHARED_FLAGS = sharedFlags();
    private static final Map<Entity, Boolean> FRAME_FLAGS = new HashMap<>();

    @SuppressWarnings("unchecked")
    private static EntityDataAccessor<Byte> sharedFlags() {
        // Forge remaps this SRG name in development and in a packaged mod.
        return (EntityDataAccessor<Byte>) ObfuscationReflectionHelper.getPrivateValue(
                Entity.class, null, "f_19805_");
    }

    private ClientSelectiveGlowState() {
    }

    public static void setGlow(ResourceLocation dimension, UUID entityUuid, int entityId, boolean enabled) {
        TargetKey key = new TargetKey(dimension, entityUuid);
        ACTIVE.keySet().removeIf(existing -> existing.entityUuid.equals(entityUuid));
        if (enabled) {
            ACTIVE.put(key, new ActiveGlow(entityId));
        }
    }

    @SubscribeEvent
    public static void onRenderTick(TickEvent.RenderTickEvent event) {
        restoreFrame();
        if (event.phase == TickEvent.Phase.START) ACTIVE.forEach(ClientSelectiveGlowState::apply);
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        restoreFrame();
        ACTIVE.clear();
    }

    private static void apply(TargetKey key, ActiveGlow state) {
        Entity entity = findEntity(key, state.entityId);
        if (entity == null) {
            return;
        }

        // setGlowingTag only changes the server-side source of this flag.
        // Override the actual render bit for this frame, then restore vanilla
        // metadata before the client processes the next server update.
        byte flags = entity.getEntityData().get(SHARED_FLAGS);
        FRAME_FLAGS.put(entity, (flags & GLOWING_MASK) != 0);
        entity.getEntityData().set(SHARED_FLAGS, (byte) (flags | GLOWING_MASK));
    }

    private static void restoreFrame() {
        FRAME_FLAGS.forEach((entity, wasGlowing) -> {
            byte flags = entity.getEntityData().get(SHARED_FLAGS);
            entity.getEntityData().set(SHARED_FLAGS,
                    (byte) (wasGlowing ? flags | GLOWING_MASK : flags & ~GLOWING_MASK));
        });
        FRAME_FLAGS.clear();
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
        private final int entityId;

        private ActiveGlow(int entityId) {
            this.entityId = entityId;
        }
    }
}
