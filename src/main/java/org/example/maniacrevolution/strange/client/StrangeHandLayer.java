package org.example.maniacrevolution.strange.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import org.example.maniacrevolution.strange.StrangeEffectEntity;
import org.joml.Matrix4f;

/** Effects share the actual animated arm transform, including PlayerAnimator and slim skins. */
public final class StrangeHandLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    public StrangeHandLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) { super(parent); }
    @Override public void render(PoseStack poses, MultiBufferSource buffers, int light, AbstractClientPlayer player,
            float swing, float amount, float partial, float age, float yaw, float pitch) {
        var effect = StrangeClient.effect(player);
        if (effect == null || player.isInvisible() || effect.casting()) return;
        for (int side : new int[]{-1,1}) {
            if (effect.kind() != StrangeEffectEntity.DEFENSE && side != 1) continue;
            poses.pushPose();
            (side == 1 ? getParentModel().rightArm : getParentModel().leftArm).translateAndRotate(poses);
            float center = player.getModelName().equals("slim") ? .03125f : .0625f;
            poses.translate(side == 1 ? -center : center, .625, 0);
            if (effect.kind() == StrangeEffectEntity.DEFENSE) shield(poses, buffers, .27f, .7f, age);
            else {
                float extension = effect.kind() == StrangeEffectEntity.WHIP
                        ? Math.max(0, Math.min(1, ((effect.age()+partial)/20f-.5f)/.15f)) : 0;
                poses.mulPose(com.mojang.math.Axis.XP.rotationDegrees((player.getXRot()-15)*extension));
                whip(poses, buffers, effect, partial);
            }
            poses.popPose();
        }
    }
    public static void shield(PoseStack poses, MultiBufferSource buffers, float radius, float alpha, float age) {
        var consumer = buffers.getBuffer(RenderType.lightning());
        for (float r : new float[]{radius,radius*.8f,radius*.45f})
            for (int i=0;i<32;i++) {
                double a=i*Math.PI/16+age*.012, b=(i+1)*Math.PI/16+age*.012;
                segment(consumer,poses.last().pose(),(float)(r*Math.cos(a)),0,(float)(r*Math.sin(a)),
                        (float)(r*Math.cos(b)),0,(float)(r*Math.sin(b)),.0025f,alpha);
            }
    }
    public static void whip(PoseStack poses, MultiBufferSource buffers, StrangeEffectEntity effect, float partial) {
        var consumer = buffers.getBuffer(RenderType.lightning());
        float time = (effect.age()+partial)/20f;
        boolean striking = effect.kind() == StrangeEffectEntity.WHIP;
        float extension = striking ? Math.max(0,Math.min(1,(time-.5f)/.15f)) : 0;
        float recovery = striking ? Math.max(0,Math.min(1,(time-.85f)/.35f)) : 0;
        float length = striking ? (1.1f+extension*4.9f)*(1-recovery)+.75f*recovery : .75f;
        float px=0,py=0,pz=0;
        for(int i=1;i<=36;i++) {
            float f=i/36f, a=f*(float)Math.PI*4-time*8;
            float radius=(striking?.3f:.15f)*f*(1-extension)*(1-recovery);
            float x=(float)Math.sin(a)*radius, y=length*f, z=(float)Math.cos(a)*radius;
            segment(consumer,poses.last().pose(),px,py,pz,x,y,z,striking?.018f:.012f,.9f);
            px=x;py=y;pz=z;
        }
    }
    public static void segment(VertexConsumer c, Matrix4f m,float x,float y,float z,float X,float Y,float Z,float width,float alpha) {
        for(int plane=0;plane<2;plane++) {
            float wx=plane==0?width:0,wz=plane==1?width:0;
            c.vertex(m,x-wx,y,z-wz).color(1f,.68f,.12f,alpha).endVertex();
            c.vertex(m,X-wx,Y,Z-wz).color(1f,.88f,.35f,alpha).endVertex();
            c.vertex(m,X+wx,Y,Z+wz).color(1f,.88f,.35f,alpha).endVertex();
            c.vertex(m,x+wx,y,z+wz).color(1f,.68f,.12f,alpha).endVertex();
        }
    }
}
