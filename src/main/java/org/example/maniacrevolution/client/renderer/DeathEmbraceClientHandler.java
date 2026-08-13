package org.example.maniacrevolution.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.example.maniacrevolution.Maniacrev;
import org.example.maniacrevolution.client.model.DeathEmbraceModel;
import software.bernie.geckolib.renderer.GeoObjectRenderer;

@Mod.EventBusSubscriber(modid = Maniacrev.MODID, value = Dist.CLIENT)
public final class DeathEmbraceClientHandler {
    private static final GeoObjectRenderer<DeathEmbraceAnimatable> RENDERER =
            new GeoObjectRenderer<>(new DeathEmbraceModel());

    private static DeathEmbraceAnimatable animation;
    private static long endsAtNanos;

    private DeathEmbraceClientHandler() {}

    public static void start(int durationTicks) {
        animation = new DeathEmbraceAnimatable();
        endsAtNanos = System.nanoTime() + Math.max(1, durationTicks) * 50_000_000L;
        if (Minecraft.getInstance().player != null) {
            Minecraft.getInstance().player.playSound(SoundEvents.SOUL_ESCAPE, 0.8F, 0.55F);
        }
    }

    @SubscribeEvent
    public static void onRenderHand(RenderHandEvent event) {
        if (event.getHand() != InteractionHand.MAIN_HAND || animation == null) return;
        if (System.nanoTime() >= endsAtNanos) {
            animation = null;
            return;
        }

        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource buffers = event.getMultiBufferSource();
        ResourceLocation texture = RENDERER.getTextureLocation(animation);
        RenderType renderType = RENDERER.getRenderType(animation, texture, buffers, event.getPartialTick());
        VertexConsumer vertices = buffers.getBuffer(renderType);

        poseStack.pushPose();
        // Blockbench exports this model upright (Y is the arm length). The old
        // 90-degree X rotation put the whole model along the camera depth axis.
        // The model grows from its shoulder roots toward +Y for roughly 33
        // Blockbench pixels. Keep the roots below the camera so the hands close
        // around the centre of the screen instead of disappearing above it.
        // RenderHandEvent inherits the main-hand offset, so compensate it to
        // keep the two-arm effect centred across the whole screen.
        poseStack.translate(-0.56D, -1.34D, -0.50D);
        // Face the palms toward the victim. The previous orientation made the
        // embrace close away from the camera.
        poseStack.mulPose(Axis.YP.rotationDegrees(0.0F));
        // GeckoLib already converts Blockbench pixels to Minecraft units.
        poseStack.scale(0.88F, 0.88F, 0.88F);
        RENDERER.render(poseStack, animation, buffers, renderType, vertices, 0x00F000F0);
        poseStack.popPose();
    }

    @SubscribeEvent
    public static void onRenderPlayer(RenderPlayerEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (animation == null || minecraft.player == null) return;
        if (event.getEntity() != minecraft.player || minecraft.options.getCameraType().isFirstPerson()) return;
        if (System.nanoTime() >= endsAtNanos) {
            animation = null;
            return;
        }
        if (!(event.getEntity() instanceof AbstractClientPlayer)) return;

        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource buffers = event.getMultiBufferSource();
        ResourceLocation texture = RENDERER.getTextureLocation(animation);
        RenderType renderType = RENDERER.getRenderType(animation, texture, buffers, event.getPartialTick());
        VertexConsumer vertices = buffers.getBuffer(renderType);

        poseStack.pushPose();
        // RenderPlayerEvent's origin is at the survivor's feet. The exported
        // arms extend upward from their roots, so the roots belong near the
        // waist; their animated hands then reach the face at ~1.6 blocks.
        poseStack.translate(0.0D, 0.08D, 0.20D);
        poseStack.mulPose(Axis.YP.rotationDegrees(360.0F - event.getEntity().yBodyRot));
        poseStack.scale(0.88F, 0.88F, 0.88F);
        RENDERER.render(poseStack, animation, buffers, renderType, vertices, event.getPackedLight());
        poseStack.popPose();
    }
}
