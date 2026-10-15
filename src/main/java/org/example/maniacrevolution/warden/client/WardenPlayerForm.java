package org.example.maniacrevolution.warden.client;

import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.example.maniacrevolution.Maniacrev;
import org.example.maniacrevolution.warden.WardenFormState;

import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

/** Server-authorized form, independent of the local F8 visual test mode. */
@Mod.EventBusSubscriber(modid = Maniacrev.MODID, value = Dist.CLIENT)
public final class WardenPlayerForm {
    private static final WardenFormState STATE = new WardenFormState();
    private static final Map<AbstractClientPlayer, Boolean> PREVIEWS = new WeakHashMap<>();
    private static WardenPlayerRenderer renderer;

    private WardenPlayerForm() {}

    public static void update(UUID player, boolean active) { STATE.update(player, active); if (!active) WardenAnimationsClient.remove(player); }

    public static void setPreviewWarden(AbstractClientPlayer player, boolean warden) { PREVIEWS.put(player, warden); }
    public static boolean isPreview(LivingEntity player) { return PREVIEWS.containsKey(player); }

    @SubscribeEvent
    public static void renderPlayer(RenderPlayerEvent.Pre event) {
        if (renderer == null || !(event.getEntity() instanceof AbstractClientPlayer player)
                || !player.isAlive() || player.isSpectator()) return;
        Boolean preview = PREVIEWS.get(player);
        if (preview != null ? !preview : !STATE.active(player.getUUID())
                || player.level().getPlayerByUUID(player.getUUID()) != player) return;
        event.setCanceled(true);
        // The replacement still posts RenderLivingEvent, so vision can hide or capture it as usual.
        renderer.render(player, player.getYRot(), event.getPartialTick(), event.getPoseStack(),
                event.getMultiBufferSource(), event.getPackedLight());
    }

    @SubscribeEvent
    public static void logout(ClientPlayerNetworkEvent.LoggingOut event) { STATE.clear(); PREVIEWS.clear(); }

    @Mod.EventBusSubscriber(modid = Maniacrev.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class Layers {
        private Layers() {}

        @SubscribeEvent
        public static void add(EntityRenderersEvent.AddLayers event) {
            renderer = new WardenPlayerRenderer(event.getContext());
            WardenFirstPersonArms.setModel(event.getContext().bakeLayer(net.minecraft.client.model.geom.ModelLayers.WARDEN));
        }
        @SubscribeEvent
        public static void setup(net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent event) {
            event.enqueueWork(() -> net.minecraft.client.renderer.ItemBlockRenderTypes.setRenderLayer(
                    org.example.maniacrevolution.block.ModBlocks.WARDEN_SHRIEKER.get(), net.minecraft.client.renderer.RenderType.cutout()));
        }
    }
}
