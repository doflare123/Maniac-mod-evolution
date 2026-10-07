package org.example.maniacrevolution.strange.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.world.InteractionHand;
import org.example.maniacrevolution.strange.StrangeEffectEntity;
import org.example.maniacrevolution.strange.WhipMotion;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.example.maniacrevolution.Maniacrev;

@Mod.EventBusSubscriber(modid=Maniacrev.MODID,value=Dist.CLIENT)
public final class StrangeFirstPerson {
    @SubscribeEvent public static void hand(RenderHandEvent event) {
        var mc=Minecraft.getInstance();
        if(mc.player==null || mc.player.isInvisible())return;
        var effect=StrangeClient.effect(mc.player);
        if(effect==null)return;
        if(effect.casting()) {
            event.setCanceled(true);
            if(event.getHand()==InteractionHand.MAIN_HAND) cast(event,effect);
            return;
        }
        boolean empty=mc.player.getMainHandItem().isEmpty() && mc.player.getOffhandItem().isEmpty();
        if(empty)event.setCanceled(true);
        if(event.getHand()!=InteractionHand.MAIN_HAND)return;
        var poses=event.getPoseStack();
        var renderer=(PlayerRenderer)mc.getEntityRenderDispatcher().getRenderer(mc.player);
        for(int side:new int[]{-1,1}) {
            if(effect.kind()!=StrangeEffectEntity.DEFENSE && side==-1)continue;
            boolean drawArm=empty || effect.kind()==StrangeEffectEntity.DEFENSE && side==-1 && mc.player.getOffhandItem().isEmpty();
            Vector3f wrist=new Vector3f(side*.53f,-.38f,-.86f);
            float raise=effect.kind()==StrangeEffectEntity.WHIP?WhipMotion.raise(effect.age()+event.getPartialTick()):0;
            float thrust=effect.kind()==StrangeEffectEntity.WHIP?WhipMotion.thrust(effect.age()+event.getPartialTick()):0;
            if(drawArm) {
                var cameraInverse=new Matrix4f(poses.last().pose()).invert();
                poses.pushPose();
                // Keep the grip inside the camera frustum during both anticipation and impact.
                poses.translate(-.12*raise, .28*raise-.06*thrust, -.35*thrust);
                poses.mulPose(Axis.XP.rotationDegrees(raise*25-thrust*8));
                poses.translate(side*.64,-.6,-.72);
                poses.mulPose(Axis.YP.rotationDegrees(side*45));
                poses.translate(side*-1,3.6,3.5);
                poses.mulPose(Axis.ZP.rotationDegrees(side*120));
                poses.mulPose(Axis.XP.rotationDegrees(200));
                poses.mulPose(Axis.YP.rotationDegrees(side*-135));
                poses.translate(side*5.6,0,0);
                // Model-space fingertip center, including the arm pivot used by renderRight/LeftHand.
                wrist.set(-side*(mc.player.getModelName().equals("slim")?5.5f:6f)/16,12f/16,0);
                cameraInverse.mul(poses.last().pose()).transformPosition(wrist);
                if(side==1)renderer.renderRightHand(poses,event.getMultiBufferSource(),event.getPackedLight(),mc.player);
                else renderer.renderLeftHand(poses,event.getMultiBufferSource(),event.getPackedLight(),mc.player);
                poses.popPose();
            }
            poses.pushPose();
            poses.translate(wrist.x,wrist.y,wrist.z);
            if(effect.kind()==StrangeEffectEntity.DEFENSE) {
                poses.mulPose(Axis.XP.rotationDegrees(90));
                StrangeHandLayer.shield(poses,event.getMultiBufferSource(),.115f,.4f,mc.player.tickCount+event.getPartialTick());
            } else {
                poses.mulPose(Axis.XP.rotationDegrees(-90+raise*125));
                StrangeHandLayer.whip(poses,event.getMultiBufferSource(),effect,event.getPartialTick());
            }
            poses.popPose();
        }
    }
    private static void cast(RenderHandEvent event, StrangeEffectEntity effect) {
        var mc=Minecraft.getInstance();
        var renderer=(PlayerRenderer)mc.getEntityRenderDispatcher().getRenderer(mc.player);
        var poses=event.getPoseStack();
        float angle=org.example.maniacrevolution.strange.StrangeRules.gestureAngle(effect.age()+event.getPartialTick(),effect.kind()==StrangeEffectEntity.PORTAL_CLOSE);
        int drawing=effect.drawingLeft()?-1:1;
        for(int side:new int[]{-1,1}) {
            poses.pushPose();
            poses.translate(side==drawing?-side*.18+Math.cos(angle)*.18:-side*.28,
                    .22+(side==drawing?Math.sin(angle)*.18:0),-.12);
            var cameraInverse=new Matrix4f(poses.last().pose()).invert();
            // Express the grip in this gesture frame, so the hand and amulet share its movement once.
            poses.pushPose();
            poses.translate(side*.64,-.6,-.72);
            poses.mulPose(Axis.YP.rotationDegrees(side*45));
            poses.translate(side*-1,3.6,3.5);
            poses.mulPose(Axis.ZP.rotationDegrees(side*120));
            poses.mulPose(Axis.XP.rotationDegrees(200));
            poses.mulPose(Axis.YP.rotationDegrees(side*-135));
            poses.translate(side*5.6,0,0);
            var wrist=new Vector3f(-side*(mc.player.getModelName().equals("slim")?5.5f:6f)/16,12f/16,0);
            cameraInverse.mul(poses.last().pose()).transformPosition(wrist);
            if(side==1)renderer.renderRightHand(poses,event.getMultiBufferSource(),event.getPackedLight(),mc.player);
            else renderer.renderLeftHand(poses,event.getMultiBufferSource(),event.getPackedLight(),mc.player);
            poses.popPose();
            if(side==drawing) {
                var stack=mc.player.getMainHandItem().is(org.example.maniacrevolution.ModItems.SPACE_AMULET.get())
                        ?mc.player.getMainHandItem():mc.player.getOffhandItem();
                poses.translate(wrist.x,wrist.y,wrist.z);
                mc.getItemRenderer().renderStatic(mc.player,stack,side==1?net.minecraft.world.item.ItemDisplayContext.FIRST_PERSON_RIGHT_HAND
                        :net.minecraft.world.item.ItemDisplayContext.FIRST_PERSON_LEFT_HAND,side==-1,poses,event.getMultiBufferSource(),mc.level,
                        event.getPackedLight(),net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY,mc.player.getId());
            }
            poses.popPose();
        }
    }
}
