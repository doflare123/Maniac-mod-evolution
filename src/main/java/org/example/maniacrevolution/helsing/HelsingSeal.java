package org.example.maniacrevolution.helsing;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import org.example.maniacrevolution.Maniacrev;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.*;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoObjectRenderer;
import software.bernie.geckolib.util.GeckoLibUtil;

public final class HelsingSeal implements GeoAnimatable {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final GeoObjectRenderer<HelsingSeal> renderer = new GeoObjectRenderer<>(new GeoModel<>() {
        public ResourceLocation getModelResource(HelsingSeal seal) { return Maniacrev.loc("geo/helsing_seal.geo.json"); }
        public ResourceLocation getTextureResource(HelsingSeal seal) { return Maniacrev.loc("textures/effect/helsing_seal.png"); }
        public ResourceLocation getAnimationResource(HelsingSeal seal) { return Maniacrev.loc("animations/helsing_seal.animation.json"); }
    });
    public void render(PoseStack pose, MultiBufferSource buffers, Player player, float partial) {
        var type = renderer.getRenderType(this, renderer.getTextureLocation(this), buffers, partial);
        pose.pushPose(); pose.translate(0, player.isCrouching() ? 0.7 : 0.95, 0);
        pose.mulPose(Axis.YP.rotationDegrees(-net.minecraft.util.Mth.rotLerp(partial, player.yBodyRotO, player.yBodyRot)));
        // GeoObjectRenderer places block objects at their centre; this effect uses the player origin.
        pose.translate(-0.5, -0.51, -0.5);
        renderer.render(pose, this, buffers, type, buffers.getBuffer(type), 0x00F000F0);
        pose.popPose();
    }
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "seal", 0, state -> state.setAndContinue(RawAnimation.begin().thenLoop("protect"))));
    }
    public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }
    public double getTick(Object object) { return Minecraft.getInstance().level == null ? 0 : Minecraft.getInstance().level.getGameTime(); }
}
