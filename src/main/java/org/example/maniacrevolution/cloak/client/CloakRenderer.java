package org.example.maniacrevolution.cloak.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import org.example.maniacrevolution.Maniacrev;
import org.example.maniacrevolution.cloak.CloakEntity;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public final class CloakRenderer extends GeoEntityRenderer<CloakEntity> {
    @Override protected void applyRotations(CloakEntity entity, PoseStack poses, float age, float yaw, float partial) {
        // GeoEntityRenderer's default body yaw is zero for non-living entities.
        float facing = Mth.rotLerp(partial, entity.yRotO, entity.getYRot());
        if (entity.stage().attached() && entity.level().getEntity(entity.ownerId()) instanceof Player player) {
            facing = Mth.rotLerp(partial, player.yBodyRotO, player.yBodyRot);
        }
        poses.mulPose(com.mojang.math.Axis.YP.rotationDegrees(180 - facing));
    }
    public CloakRenderer(EntityRendererProvider.Context context) {
        super(context, new GeoModel<>() {
            @Override public ResourceLocation getModelResource(CloakEntity entity) { return Maniacrev.loc("geo/cloak_of_levitation.geo.json"); }
            @Override public ResourceLocation getTextureResource(CloakEntity entity) { return Maniacrev.loc("textures/entity/cloak_of_levitation.png"); }
            @Override public ResourceLocation getAnimationResource(CloakEntity entity) { return Maniacrev.loc("animations/cloak_of_levitation.animation.json"); }
        });
        shadowRadius = 0;
    }

    @Override public void render(CloakEntity entity, float yaw, float partial, PoseStack poses, MultiBufferSource buffers, int light) {
        var owner = entity.level().getEntity(entity.ownerId());
        if (entity.stage().attached() && owner != null && owner.isInvisible()) return;
        if (entity.stage().attached() && !entity.stage().hovering() && owner == Minecraft.getInstance().player
                && Minecraft.getInstance().options.getCameraType().isFirstPerson()) return;
        poses.pushPose();
        if (entity.stage().attached() && owner instanceof Player player) {
            // Follow the interpolated player directly: entity tracking otherwise trails by several ticks.
            poses.translate(Mth.lerp(partial, player.xo, player.getX()) - Mth.lerp(partial, entity.xo, entity.getX()),
                    Mth.lerp(partial, player.yo, player.getY()) - Mth.lerp(partial, entity.yo, entity.getY()),
                    Mth.lerp(partial, player.zo, player.getZ()) - Mth.lerp(partial, entity.zo, entity.getZ()));
            yaw = Mth.rotLerp(partial, player.yBodyRotO, player.yBodyRot);
        }
        super.render(entity, yaw, partial, poses, buffers, light);
        poses.popPose();
    }
}
