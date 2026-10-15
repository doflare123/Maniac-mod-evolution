package org.example.maniacrevolution.scp173.client;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.example.maniacrevolution.Maniacrev;

@Mod.EventBusSubscriber(modid = Maniacrev.MODID, value = Dist.CLIENT)
public final class Scp173PlayerForm {
    private static final Set<UUID> ACTIVE = new HashSet<>();
    private static final Map<AbstractClientPlayer, Boolean> PREVIEWS = new WeakHashMap<>();
    private static Scp173Renderer renderer;
    private Scp173PlayerForm() {}

    public static void update(UUID player, boolean active) {
        if (active) ACTIVE.add(player); else ACTIVE.remove(player);
    }
    public static void setPreviewScp173(AbstractClientPlayer player, boolean statue) { PREVIEWS.put(player, statue); }
    public static boolean isPreview(AbstractClientPlayer player) { return PREVIEWS.containsKey(player); }
    public static boolean active(AbstractClientPlayer player) {
        return player != null && !PREVIEWS.containsKey(player) && ACTIVE.contains(player.getUUID())
                && player.isAlive() && !player.isSpectator()
                && player.level().getPlayerByUUID(player.getUUID()) == player;
    }

    @SubscribeEvent
    public static void renderPlayer(RenderPlayerEvent.Pre event) {
        if (renderer == null || !(event.getEntity() instanceof AbstractClientPlayer player)
                || !player.isAlive() || player.isSpectator()) return;
        Boolean preview = PREVIEWS.get(player);
        if (preview != null ? !preview : !ACTIVE.contains(player.getUUID())
                || player.level().getPlayerByUUID(player.getUUID()) != player) return;
        event.setCanceled(true);
        var pose = event.getPoseStack();
        pose.pushPose();
        try {
            // The dispatcher already applied the vanilla player's crouch offset. Static geometry
            // must stay at the feet, matching the server samples even when the player hitbox changes.
            var offset = event.getRenderer().getRenderOffset(player, event.getPartialTick());
            pose.translate(-offset.x, -offset.y, -offset.z);
            // The rigid renderer emits the baked bones at the feet, without an animation offset.
            // Retain Forge living-render hooks: Warden vision hides/captures this replacement as well.
            if (MinecraftForge.EVENT_BUS.post(new RenderLivingEvent.Pre<>(player, event.getRenderer(),
                    event.getPartialTick(), pose, event.getMultiBufferSource(), event.getPackedLight()))) return;
            renderer.render(player, player.getYRot(), event.getPartialTick(), pose,
                    event.getMultiBufferSource(), event.getPackedLight());
            MinecraftForge.EVENT_BUS.post(new RenderLivingEvent.Post<>(player, event.getRenderer(),
                    event.getPartialTick(), pose, event.getMultiBufferSource(), event.getPackedLight()));
        } finally { pose.popPose(); }
    }

    @SubscribeEvent
    public static void logout(ClientPlayerNetworkEvent.LoggingOut event) { ACTIVE.clear(); PREVIEWS.clear(); }

    @Mod.EventBusSubscriber(modid = Maniacrev.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class Layers {
        private Layers() {}
        @SubscribeEvent public static void initializeScp173Body(EntityRenderersEvent.AddLayers event) {
            renderer = new Scp173Renderer(event.getContext());
        }
    }
}
