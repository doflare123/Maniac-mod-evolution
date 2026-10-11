package org.example.maniacrevolution.techies.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.world.entity.EquipmentSlot;
import org.example.maniacrevolution.Maniacrev;
import org.example.maniacrevolution.ModItems;

public final class TechiesHelmetLayer extends RenderLayer<AbstractClientPlayer,PlayerModel<AbstractClientPlayer>> {
    private final TechiesVisual helmet=new TechiesVisual("techies_helmet");
    public TechiesHelmetLayer(RenderLayerParent<AbstractClientPlayer,PlayerModel<AbstractClientPlayer>> parent) { super(parent); }
    @Override public void render(PoseStack pose,MultiBufferSource buffer,int light,AbstractClientPlayer p,
            float limb,float amount,float partial,float age,float yaw,float pitch) {
        if(p.isInvisible() || !p.getItemBySlot(EquipmentSlot.HEAD).is(ModItems.TECHIES_HELMET.get())) return;
        pose.pushPose(); getParentModel().head.translateAndRotate(pose); pose.scale(1,-1,1);
        TechiesClient.RENDERER.render(pose,helmet,buffer,RenderType.entityCutoutNoCull(Maniacrev.loc("textures/item/techies_helmet.png")),null,light);
        pose.popPose();
    }
    @net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid=Maniacrev.MODID,bus=net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus.MOD,value=net.minecraftforge.api.distmarker.Dist.CLIENT)
    public static final class Registration {
        @net.minecraftforge.eventbus.api.SubscribeEvent public static void layers(net.minecraftforge.client.event.EntityRenderersEvent.AddLayers e) {
            for(String skin:e.getSkins()) {
                var r=e.getSkin(skin);
                if(r instanceof net.minecraft.client.renderer.entity.player.PlayerRenderer p) {
                    p.addLayer(new TechiesHelmetLayer(p));
                    // Forge exposes no layer replacement API; use its remapped field access once at registration.
                    java.util.List<net.minecraft.client.renderer.entity.layers.RenderLayer<net.minecraft.client.player.AbstractClientPlayer,net.minecraft.client.model.PlayerModel<net.minecraft.client.player.AbstractClientPlayer>>> layers=
                            net.minecraftforge.fml.util.ObfuscationReflectionHelper.getPrivateValue(net.minecraft.client.renderer.entity.LivingEntityRenderer.class,p,"f_115291_");
                    for(int i=0;i<layers.size();i++) if(layers.get(i) instanceof net.minecraft.client.renderer.entity.layers.ItemInHandLayer) {
                        layers.set(i,new TechiesHeldLayer(p,layers.get(i))); break;
                    }
                    for(int i=0;i<layers.size();i++) if(layers.get(i) instanceof net.minecraft.client.renderer.entity.layers.CustomHeadLayer) {
                        var original=layers.get(i);
                        layers.set(i,new RenderLayer<>(p) {
                            @Override public void render(PoseStack pose,MultiBufferSource buffer,int light,AbstractClientPlayer player,
                                    float limb,float amount,float partial,float age,float yaw,float pitch) {
                                if(!player.getItemBySlot(EquipmentSlot.HEAD).is(ModItems.TECHIES_HELMET.get()))
                                    original.render(pose,buffer,light,player,limb,amount,partial,age,yaw,pitch);
                            }
                        });
                        break;
                    }
                }
            }
        }
    }
}
