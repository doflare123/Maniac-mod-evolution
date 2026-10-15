package org.example.maniacrevolution.scp173.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.InteractionHand;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RenderArmEvent;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.example.maniacrevolution.Maniacrev;
import org.example.maniacrevolution.scp173.Scp173Geometry;

/** Rigid statue arms with an accepted-attack first-person gesture; world bones remain static. */
@Mod.EventBusSubscriber(modid = Maniacrev.MODID, value = Dist.CLIENT)
public final class Scp173FirstPersonArms {
    private static ArmsRenderer renderer;
    private Scp173FirstPersonArms() {}

    @SubscribeEvent
    public static void arm(RenderArmEvent event) {
        if (Scp173PlayerForm.active(event.getPlayer())) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void hand(RenderHandEvent event) {
        var mc = Minecraft.getInstance();
        var player = mc.player;
        if (renderer == null || !Scp173PlayerForm.active(player) || player.isInvisible()
                || mc.options.hideGui || !mc.options.getCameraType().isFirstPerson()) return;
        // Keep native item rendering and item-use feedback when a perk item is held.
        boolean empty = player.getMainHandItem().isEmpty() && player.getOffhandItem().isEmpty();
        if (empty) event.setCanceled(true);
        if (empty && event.getHand() != InteractionHand.MAIN_HAND) return;
        var side = event.getHand() == InteractionHand.MAIN_HAND ? player.getMainArm() : player.getMainArm().getOpposite();
        renderer.arms(player, event.getPoseStack(), event.getMultiBufferSource(), event.getPackedLight(),
                empty ? null : side, event.getPartialTick());
    }

    private static final class ArmsRenderer extends Scp173Renderer {
        private ArmsRenderer(EntityRendererProvider.Context context) { super(context); }

        private void arms(AbstractClientPlayer player, PoseStack pose, MultiBufferSource buffers, int light,
                          net.minecraft.world.entity.HumanoidArm onlyArm, float partial) {
            currentEntity = player;
            var model = getGeoModel().getBakedModel(getGeoModel().getModelResource(getAnimatable()));
            var type = RenderType.entityCutoutNoCull(getTextureLocation(player));
            for (var name : new String[]{"right_arm", "left_arm"}) {
                var bone = model.getBone(name).orElseThrow();
                int side = name.equals("right_arm") ? 1 : -1;
                if (onlyArm != null && (side == 1) != (onlyArm == net.minecraft.world.entity.HumanoidArm.RIGHT)) continue;
                pose.pushPose();
                try {
                    pose.translate(side * 0.28, -0.32, -0.65);
                    float reach = Scp173GameplayClient.strikeReach(partial);
                    pose.translate(-side * 0.10 * reach, 0.06 * reach, -0.32 * reach);
                    pose.mulPose(Axis.XP.rotationDegrees(-32 * reach));
                    pose.mulPose(Axis.YP.rotationDegrees(side * 12 * reach));
                    pose.mulPose(Axis.YP.rotationDegrees(180));
                    pose.scale(Scp173Geometry.SCALE, Scp173Geometry.SCALE, Scp173Geometry.SCALE);
                    pose.translate(-bone.getPivotX() / 16.0, -bone.getPivotY() / 16.0, -bone.getPivotZ() / 16.0);
                    renderRecursively(pose, getAnimatable(), bone, type, buffers, buffers.getBuffer(type),
                            true, 0, light, OverlayTexture.NO_OVERLAY, 1, 1, 1, 1);
                } finally { pose.popPose(); }
            }
        }
    }

    @Mod.EventBusSubscriber(modid = Maniacrev.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class Layers {
        private Layers() {}
        @SubscribeEvent public static void initializeScp173Arms(EntityRenderersEvent.AddLayers event) {
            renderer = new ArmsRenderer(event.getContext());
        }
    }
}
