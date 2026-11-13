package org.example.maniacrevolution.lich.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.*;
import org.example.maniacrevolution.lich.LichUnit;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.*;
import software.bernie.geckolib.renderer.GeoObjectRenderer;
import software.bernie.geckolib.util.GeckoLibUtil;

/** Decorative only; follows the vanilla body's complete animated transform. */
final class LichQuiver implements GeoAnimatable {
    private static final LichQuiver INSTANCE=new LichQuiver();
    private static final GeoObjectRenderer<LichQuiver> RENDERER=new GeoObjectRenderer<>(new LichRenderers.Model<LichQuiver>(q->"lich_skeleton_quiver")) {
        @Override public void preRender(PoseStack pose,LichQuiver quiver,software.bernie.geckolib.cache.object.BakedGeoModel model,
                MultiBufferSource buffers,com.mojang.blaze3d.vertex.VertexConsumer vertices,boolean reRender,float partial,
                int light,int overlay,float red,float green,float blue,float alpha) {
            super.preRender(pose,quiver,model,buffers,vertices,reRender,partial,light,overlay,red,green,blue,alpha);
            // GeoObjectRenderer centers standalone objects in a block. This mesh
            // already has its authored origin and is attached to the body pivot.
            pose.translate(-.5F,-.51F,-.5F);
        }
    };
    // This package's quiver glowmask is intentionally empty. GeckoLib 4.4.7's
    // automatic glowing layer cannot build a texture from an empty mask.
    private final AnimatableInstanceCache cache=GeckoLibUtil.createInstanceCache(this);
    @Override public AnimatableInstanceCache getAnimatableInstanceCache(){return cache;}
    @Override public void registerControllers(AnimatableManager.ControllerRegistrar c){c.add(new AnimationController<>(this,s->s.setAndContinue(RawAnimation.begin().thenLoop("animation.lich_skeleton_quiver.idle"))));}
    @Override public double getTick(Object o){return 0;}
    static void render(LichUnit unit,PoseStack pose,MultiBufferSource buffers,int light){
        var type=RenderType.entityCutoutNoCull(RENDERER.getTextureLocation(INSTANCE));
        RENDERER.render(pose,INSTANCE,buffers,type,buffers.getBuffer(type),light);
    }
}
