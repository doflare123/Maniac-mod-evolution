package org.example.maniacrevolution.lich.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.*;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.example.maniacrevolution.Maniacrev;
import org.example.maniacrevolution.entity.ModEntities;
import org.example.maniacrevolution.lich.*;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.*;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

@Mod.EventBusSubscriber(modid = Maniacrev.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class LichRenderers {
    private static String unitStem(LichUnit u) { return u.kind() == LichRules.Kind.GHOUL ? "lich_ghoul" : "lich_butcher"; }
    public static class Model<T extends GeoAnimatable> extends GeoModel<T> {
        private final java.util.function.Function<T,String> stem;
        public Model(java.util.function.Function<T,String> stem) { this.stem=stem; }
        @Override public ResourceLocation getModelResource(T a){return Maniacrev.loc("geo/"+stem.apply(a)+".geo.json");}
        @Override public ResourceLocation getTextureResource(T a){String s=stem.apply(a);return Maniacrev.loc("textures/"+(s.equals("lich_book")||s.equals("lich_phylactery")?"item/":"entity/")+s+".png");}
        @Override public ResourceLocation getAnimationResource(T a){return Maniacrev.loc("animations/"+stem.apply(a)+".animation.json");}
    }
    public static final class ItemRenderer extends GeoItemRenderer<LichItem> {
        public ItemRenderer(){super(new Model<>(i->i.role==LichItem.Role.BOOK?"lich_book":"lich_phylactery"));addRenderLayer(new AutoGlowingGeoLayer<>(this));}
    }
    private static final class VanillaUnit extends MobRenderer<LichUnit,HumanoidModel<LichUnit>> {
        private final boolean skeleton;
        VanillaUnit(EntityRendererProvider.Context c,boolean skeleton){super(c,new LichVanillaModel(skeleton),.4F);this.skeleton=skeleton;addLayer(new ItemInHandLayer<>(this,c.getItemInHandRenderer()));
            addLayer(new net.minecraft.client.renderer.entity.layers.RenderLayer<>(this){
                @Override public void render(PoseStack pose,MultiBufferSource buffers,int light,LichUnit unit,float swing,float amount,float partial,float age,float yaw,float pitch){
                    if(!skeleton)getParentModel().renderToBuffer(pose,buffers.getBuffer(RenderType.eyes(Maniacrev.loc("textures/entity/lich_zombie_glowmask.png"))),LightTexture.FULL_BRIGHT,net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY,1,1,1,1);
                    else {
                        pose.pushPose();try {
                            ((LichVanillaModel)getParentModel()).bodyTransform(pose);
                            pose.scale(-1,-1,1);pose.translate(0,-1.5,0);
                            LichQuiver.render(unit,pose,buffers,light);
                        }finally{pose.popPose();}
                    }
                }
            });
        }
        @Override public ResourceLocation getTextureLocation(LichUnit u){return skeleton?new ResourceLocation("minecraft","textures/entity/skeleton/skeleton.png"):Maniacrev.loc("textures/entity/lich_zombie.png");}
        @Override protected float getFlipDegrees(LichUnit unit){return 0;}
    }
    private static final class UnitRenderer extends EntityRenderer<LichUnit> {
        private final VanillaUnit zombie,skeleton;
        private final GeoEntityRenderer<LichUnit> geo;
        UnitRenderer(EntityRendererProvider.Context c){super(c);zombie=new VanillaUnit(c,false);skeleton=new VanillaUnit(c,true);geo=new GeoEntityRenderer<>(c,new Model<>(LichRenderers::unitStem)){
            @Override protected float getDeathMaxRotation(LichUnit unit){return 0;}
        };geo.addRenderLayer(new AutoGlowingGeoLayer<>(geo));}
        @Override public ResourceLocation getTextureLocation(LichUnit u){return u.kind()==LichRules.Kind.SKELETON?skeleton.getTextureLocation(u):u.kind()==LichRules.Kind.ZOMBIE?zombie.getTextureLocation(u):geo.getTextureLocation(u);}
        @Override public void render(LichUnit u,float yaw,float partial,PoseStack p,MultiBufferSource b,int light){
            if(u.kind()==LichRules.Kind.ZOMBIE)zombie.render(u,yaw,partial,p,b,light);
            else if(u.kind()==LichRules.Kind.SKELETON)skeleton.render(u,yaw,partial,p,b,light);
            else geo.render(u,yaw,partial,p,b,light);
            if(u.emerging()>0&&!u.isDeadOrDying()){p.pushPose();if(u.kind()==LichRules.Kind.BUTCHER)p.scale(1.3F,1.3F,1.3F);LichMagic.render(u,"emerge",p,b,light,partial);p.popPose();}
            else if(u.swinging){p.pushPose();p.translate(0,1,-.4);p.scale(.3F,.3F,.3F);LichMagic.render(u,u.kind()==LichRules.Kind.SKELETON?"shoot":"attack",p,b,light,partial);p.popPose();}
            else if(u.kind()==LichRules.Kind.GHOUL&&u.detecting()){p.pushPose();p.translate(0,1.2,0);p.scale(.2F,.2F,.2F);LichMagic.render(u,"scout",p,b,light,partial);p.popPose();}
        }
    }
    private static final class ObjectRenderer extends GeoEntityRenderer<LichObject> {
        @Override public void render(LichObject object,float yaw,float partial,PoseStack pose,MultiBufferSource buffers,int light) {
            super.render(object,yaw,partial,pose,buffers,light);
            String action=object.visualAction();
            if(action.equals("raise")||action.equals("absorb")||action.equals("repair"))
                LichMagic.render(object,"cast_"+action,pose,buffers,light,partial);
        }
        ObjectRenderer(EntityRendererProvider.Context c){
            super(c,new Model<>(o->o.phylactery()?"lich_phylactery":"lich_corpse"));
            addRenderLayer(new AutoGlowingGeoLayer<LichObject>(this){
                @Override public void render(PoseStack pose,LichObject object,software.bernie.geckolib.cache.object.BakedGeoModel model,RenderType type,MultiBufferSource buffers,com.mojang.blaze3d.vertex.VertexConsumer vertices,float partial,int light,int overlay){
                    // The asset contract intentionally provides an empty corpse glowmask.
                    if(object.phylactery())super.render(pose,object,model,type,buffers,vertices,partial,light,overlay);
                }
            });
        }
    }
    @SubscribeEvent public static void register(EntityRenderersEvent.RegisterRenderers e){
        e.registerEntityRenderer(ModEntities.LICH_UNIT.get(),UnitRenderer::new);
        e.registerEntityRenderer(ModEntities.LICH_OBJECT.get(),ObjectRenderer::new);
        e.registerEntityRenderer(ModEntities.LICH_BOLT.get(),LichBoltRenderer::new);
    }
}
