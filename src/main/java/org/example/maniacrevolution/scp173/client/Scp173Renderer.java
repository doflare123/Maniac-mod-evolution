package org.example.maniacrevolution.scp173.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoReplacedEntityRenderer;

public class Scp173Renderer extends GeoReplacedEntityRenderer<AbstractClientPlayer, Scp173Statue> {
    public Scp173Renderer(EntityRendererProvider.Context context) {
        super(context, new Scp173Model(), new Scp173Statue());
        // Share the visual scale with the server's model-surface queries.
        withScale(org.example.maniacrevolution.scp173.Scp173Geometry.SCALE);
    }

    @Override
    public void render(AbstractClientPlayer player, float yaw, float partialTick, PoseStack pose,
                       MultiBufferSource buffers, int light) {
        // Draw the same baked geometry as the hands. A controller-less static form does not
        // need the replaced-entity animation/visibility pipeline before emitting its cubes.
        currentEntity = player;
        var mc = Minecraft.getInstance();
        boolean visible = !player.isInvisible();
        boolean translucent = !visible && !player.isInvisibleTo(mc.player);
        var type = visible ? RenderType.entityCutoutNoCull(getTextureLocation(player))
                : translucent ? RenderType.itemEntityTranslucentCull(getTextureLocation(player))
                : mc.shouldEntityAppearGlowing(player) ? RenderType.outline(getTextureLocation(player)) : null;
        if (type != null) {
            var baked = getGeoModel().getBakedModel(getGeoModel().getModelResource(getAnimatable()));
            pose.pushPose();
            try {
                pose.mulPose(Axis.YP.rotationDegrees(180 - player.getYRot()));
                float swim = player.getSwimAmount(partialTick);
                pose.translate(0, org.example.maniacrevolution.scp173.Scp173Geometry.swimCenter(swim), 0);
                pose.mulPose(Axis.XP.rotationDegrees(org.example.maniacrevolution.scp173.Scp173Geometry.swimAngle(swim,
                        player.getXRot(), player.isInWater())));
                pose.translate(0, -org.example.maniacrevolution.scp173.Scp173Geometry.CENTER_Y, 0);
                pose.scale(scaleWidth, scaleHeight, scaleWidth);
                for (var bone : baked.topLevelBones())
                    renderRecursively(pose, getAnimatable(), bone, type, buffers, buffers.getBuffer(type), true,
                            partialTick, light, OverlayTexture.pack(OverlayTexture.u(0), OverlayTexture.v(player.hurtTime > 0)),
                            1, 1, 1, translucent ? 0.15F : 1);
            } finally { pose.popPose(); }
        }
        if (shouldShowName(player)) renderNameTag(player, player.getDisplayName(), pose, buffers, light);
    }

    @Override
    protected void applyRotations(Scp173Statue statue, PoseStack pose, float age, float yaw, float partialTick) {
        // Whole-body swimming rotation is applied in render; bones remain static.
        pose.mulPose(Axis.YP.rotationDegrees(180.0F - getCurrentEntity().getYRot()));
    }

    @Override public boolean shouldShowName(AbstractClientPlayer player) {
        return !Scp173PlayerForm.isPreview(player) && super.shouldShowName(player);
    }
}
