package org.example.maniacrevolution.lich.client;

import java.util.*;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.*;
import net.minecraft.world.entity.Entity;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.*;
import software.bernie.geckolib.util.GeckoLibUtil;
import software.bernie.geckolib.renderer.GeoObjectRenderer;

/** A weak per-entity visual instance; no world state or gameplay callbacks in animation clips. */
public final class LichMagic implements GeoAnimatable {
    private static final Map<Entity,LichMagic> INSTANCES=new WeakHashMap<>();
    private static final GeoObjectRenderer<LichMagic> RENDERER=new GeoObjectRenderer<>(new LichRenderers.Model<LichMagic>(a->"lich_necromancy_fx")) {
        @Override public void preRender(PoseStack pose,LichMagic effect,software.bernie.geckolib.cache.object.BakedGeoModel model,
                MultiBufferSource buffers,com.mojang.blaze3d.vertex.VertexConsumer vertices,boolean reRender,float partial,
                int light,int overlay,float red,float green,float blue,float alpha) {
            super.preRender(pose,effect,model,buffers,vertices,reRender,partial,light,overlay,red,green,blue,alpha);
            pose.translate(-.5F,-.51F,-.5F);
        }
    };
    private final AnimatableInstanceCache cache=GeckoLibUtil.createInstanceCache(this);
    private String clip="hidden";private double time;private int lastTick=-100;
    @Override public AnimatableInstanceCache getAnimatableInstanceCache(){return cache;}
    @Override public double getTick(Object o){return time;}
    @Override public void registerControllers(AnimatableManager.ControllerRegistrar c){c.add(new AnimationController<>(this,"fx",0,s->s.setAndContinue(RawAnimation.begin().thenPlayAndHold("animation.lich_necromancy_fx."+clip))));}
    public static void render(Entity entity,String clip,PoseStack pose,MultiBufferSource buffers,int light){
        render(entity,clip,pose,buffers,light,0);
    }
    public static void render(Entity entity,String clip,PoseStack pose,MultiBufferSource buffers,int light,float partial){
        var effect=INSTANCES.get(entity);
        if(effect==null||!clip.equals(effect.clip)||entity.tickCount-effect.lastTick>3){effect=new LichMagic();INSTANCES.put(entity,effect);}
        effect.clip=clip;effect.time=entity.tickCount+partial;effect.lastTick=entity.tickCount;
        var type=RenderType.entityTranslucent(RENDERER.getTextureLocation(effect));pose.pushPose();
        try{RENDERER.render(pose,effect,buffers,type,buffers.getBuffer(type),LightTexture.FULL_BRIGHT);}finally{pose.popPose();}
    }
}
