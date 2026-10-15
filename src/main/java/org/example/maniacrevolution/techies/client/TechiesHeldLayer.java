package org.example.maniacrevolution.techies.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemDisplayContext;
import org.example.maniacrevolution.techies.MineType;

/** Replace only the visual main-hand stack; never write to any player inventory. */
public final class TechiesHeldLayer extends ItemInHandLayer<AbstractClientPlayer,PlayerModel<AbstractClientPlayer>> {
    private final net.minecraft.client.renderer.entity.layers.RenderLayer<AbstractClientPlayer,PlayerModel<AbstractClientPlayer>> original;
    public TechiesHeldLayer(PlayerRenderer renderer,net.minecraft.client.renderer.entity.layers.RenderLayer<AbstractClientPlayer,PlayerModel<AbstractClientPlayer>> original) {
        super(renderer,Minecraft.getInstance().getEntityRenderDispatcher().getItemInHandRenderer()); this.original=original;
    }
    private static void add(ModelPart arm,float[] angles,boolean left) {
        arm.xRot+=(float)Math.toRadians(angles[0]); arm.yRot+=(float)Math.toRadians(angles[1])*(left?-1:1); arm.zRot+=(float)Math.toRadians(angles[2])*(left?-1:1);
    }
    @Override public void render(PoseStack pose,MultiBufferSource buffer,int light,AbstractClientPlayer player,
            float limb,float amount,float partial,float age,float yaw,float pitch) {
        var state=TechiesHands.pose(player); var model=getParentModel();
        if(!TechiesHands.displays(state) || player.isInvisible()) {
            if(state!=null && TechiesHands.clock()-state.press<=6) super.render(pose,buffer,light,player,limb,amount,partial,age,yaw,pitch);
            else original.render(pose,buffer,light,player,limb,amount,partial,age,yaw,pitch);
            return;
        }
        boolean left=player.getMainArm()==HumanoidArm.LEFT;
        ModelPart main=left?model.leftArm:model.rightArm,support=left?model.rightArm:model.leftArm;
        ModelPart mainSleeve=left?model.leftSleeve:model.rightSleeve,supportSleeve=left?model.rightSleeve:model.leftSleeve;
        float mx=main.xRot,my=main.yRot,mz=main.zRot,sx=support.xRot,sy=support.yRot,sz=support.zRot;
        add(main,TechiesHands.sample(state,"main_arm_deg"),left); add(support,TechiesHands.sample(state,"support_arm_deg"),left);
        mainSleeve.copyFrom(main); supportSleeve.copyFrom(support);
        main.visible=support.visible=true;
        var consumer=buffer.getBuffer(RenderType.entityTranslucent(player.getSkinTextureLocation()));
        int overlay=net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY;
        main.render(pose,consumer,light,overlay); support.render(pose,consumer,light,overlay);
        if(player.isModelPartShown(net.minecraft.world.entity.player.PlayerModelPart.LEFT_SLEEVE)) { model.leftSleeve.visible=true; model.leftSleeve.render(pose,consumer,light,overlay); }
        if(player.isModelPartShown(net.minecraft.world.entity.player.PlayerModelPart.RIGHT_SLEEVE)) { model.rightSleeve.visible=true; model.rightSleeve.render(pose,consumer,light,overlay); }
        super.render(pose,buffer,light,player,limb,amount,partial,age,yaw,pitch);
        main.xRot=mx; main.yRot=my; main.zRot=mz; support.xRot=sx; support.yRot=sy; support.zRot=sz;
        mainSleeve.copyFrom(main); supportSleeve.copyFrom(support);
        main.visible=support.visible=model.leftSleeve.visible=model.rightSleeve.visible=false;
    }
    @Override protected void renderArmWithItem(net.minecraft.world.entity.LivingEntity entity,ItemStack stack,ItemDisplayContext context,
            HumanoidArm arm,PoseStack pose,MultiBufferSource buffer,int light) {
        if(entity instanceof AbstractClientPlayer player) {
            var state=TechiesHands.pose(player);
            if(arm==player.getMainArm() && TechiesHands.displays(state)) {
                stack=new ItemStack(MineType.values()[TechiesHands.type(state)].item());
                stack.getOrCreateTag().putDouble("TechiesVisualSeconds",TechiesHands.elapsed(state));
                stack.getOrCreateTag().putFloat("TechiesVisualWeight",state.start>=0 || state.type<0?TechiesHands.weight(state):0);
            } else if(stack.is(org.example.maniacrevolution.ModItems.TECHIES_REMOTE.get()) && state!=null) {
                stack=stack.copy(); stack.getOrCreateTag().putDouble("TechiesPressSeconds",(TechiesHands.clock()-state.press)/20);
            }
        }
        super.renderArmWithItem(entity,stack,context,arm,pose,buffer,light);
    }
    @net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid=org.example.maniacrevolution.Maniacrev.MODID,value=net.minecraftforge.api.distmarker.Dist.CLIENT)
    public static final class Events {
        @net.minecraftforge.eventbus.api.SubscribeEvent public static void hide(net.minecraftforge.client.event.RenderLivingEvent.Pre<?,?> e) {
            if(e.getEntity() instanceof AbstractClientPlayer player && e.getRenderer() instanceof PlayerRenderer renderer
                    && TechiesHands.displays(TechiesHands.pose(player)) && !player.isInvisible()) {
                var m=renderer.getModel(); m.leftArm.visible=m.rightArm.visible=m.leftSleeve.visible=m.rightSleeve.visible=false;
            }
        }
        @net.minecraftforge.eventbus.api.SubscribeEvent public static void restore(net.minecraftforge.client.event.RenderLivingEvent.Post<?,?> e) {
            if(e.getEntity() instanceof AbstractClientPlayer player && e.getRenderer() instanceof PlayerRenderer renderer
                    && TechiesHands.displays(TechiesHands.pose(player))) {
                var m=renderer.getModel(); m.leftArm.visible=m.rightArm.visible=true;
                m.leftSleeve.visible=player.isModelPartShown(net.minecraft.world.entity.player.PlayerModelPart.LEFT_SLEEVE);
                m.rightSleeve.visible=player.isModelPartShown(net.minecraft.world.entity.player.PlayerModelPart.RIGHT_SLEEVE);
            }
        }
    }
}
