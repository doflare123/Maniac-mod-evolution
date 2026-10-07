package org.example.maniacrevolution.strange.client;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.level.block.Block;
import org.example.maniacrevolution.strange.*;
import org.joml.Matrix4f;
import java.util.*;

public final class PortalVisual {
    private static final Map<UUID,Scene> SCENES=new HashMap<>();
    private static final class Scene {
        PortalViewPacket packet;
        RenderTarget target;
        boolean dirty=true;
        long received;
    }
    public static void accept(PortalViewPacket packet) {
        var scene=SCENES.computeIfAbsent(packet.portal(),id->new Scene());
        scene.packet=packet;scene.dirty=true;
        var level=Minecraft.getInstance().level;
        scene.received=level==null?0:level.getGameTime();
    }
    public static void cleanup() {
        var mc=Minecraft.getInstance();
        SCENES.entrySet().removeIf(e->{
            boolean remove=mc.level==null || mc.level.getGameTime()-e.getValue().received>80;
            if(remove&&e.getValue().target!=null)e.getValue().target.destroyBuffers();
            return remove;
        });
    }
    private static void update(Scene scene) {
        var mc=Minecraft.getInstance();
        if(scene.target==null)scene.target=new TextureTarget(256,384,true,Minecraft.ON_OSX);
        scene.target.setClearColor(.09f,.12f,.17f,1);scene.target.clear(Minecraft.ON_OSX);scene.target.bindWrite(true);
        RenderSystem.backupProjectionMatrix();
        PoseStack view=RenderSystem.getModelViewStack();view.pushPose();view.setIdentity();RenderSystem.applyModelViewMatrix();
        RenderSystem.setProjectionMatrix(new Matrix4f().perspective((float)Math.toRadians(70),2f/3,.05f,32),VertexSorting.DISTANCE_TO_ORIGIN);
        PoseStack poses=new PoseStack();poses.mulPose(Axis.YP.rotationDegrees(180-scene.packet.yaw()));poses.translate(-.5,-1.6,-.5);
        var buffers=MultiBufferSource.immediate(new BufferBuilder(262144));
        try {
            for(var cell:scene.packet.cells()) {
                poses.pushPose();var pos=cell.offset();poses.translate(pos.getX(),pos.getY(),pos.getZ());
                mc.getBlockRenderer().renderSingleBlock(Block.stateById(cell.state()),poses,buffers,LightTexture.FULL_BRIGHT,OverlayTexture.NO_OVERLAY);
                poses.popPose();
            }
            buffers.endBatch();scene.dirty=false;
        } finally {
            view.popPose();RenderSystem.applyModelViewMatrix();RenderSystem.restoreProjectionMatrix();mc.getMainRenderTarget().bindWrite(true);
        }
    }
    public static void render(StrangeEffectEntity portal,float partial,PoseStack poses,MultiBufferSource buffers) {
        float scale=portal.portalScale(partial);
        if(scale<=.001)return;
        if(buffers instanceof MultiBufferSource.BufferSource source)source.endBatch();
        Scene scene=SCENES.get(portal.getUUID());
        if(scene!=null&&scene.dirty)update(scene);
        poses.pushPose();poses.mulPose(Axis.YP.rotationDegrees(180-portal.getYRot()));poses.translate(0,1.31,0);poses.scale(scale,scale,scale);
        if(scene!=null&&scene.target!=null) {
            RenderSystem.setShader(GameRenderer::getPositionTexShader);RenderSystem.setShaderTexture(0,scene.target.getColorTextureId());
            RenderSystem.setShaderColor(1,1,1,1);RenderSystem.disableCull();RenderSystem.enableDepthTest();
            var tess=Tesselator.getInstance();var builder=tess.getBuilder();var matrix=poses.last().pose();
            builder.begin(VertexFormat.Mode.TRIANGLE_FAN,DefaultVertexFormat.POSITION_TEX);
            builder.vertex(matrix,0,0,0).uv(.5f,.5f).endVertex();
            for(int i=0;i<=64;i++) {
                double a=i*Math.PI/32;float x=(float)Math.cos(a),y=(float)Math.sin(a);
                builder.vertex(matrix,x*.84f,y*1.26f,0).uv((x+1)/2,(y+1)/2).endVertex();
            }
            tess.end();RenderSystem.enableCull();
        }
        poses.popPose();
    }
    public static void particles(StrangeEffectEntity ring) {
        float scale=ring.portalScale(0);float yaw=(float)Math.toRadians(ring.getYRot());
        var level=ring.level();var random=level.random;
        for(int i=0;i<18;i++) {
            double angle=(i+random.nextDouble())*Math.PI/9+level.getGameTime()*.13;
            double x=Math.cos(angle)*.88*scale,y=1.31+Math.sin(angle)*1.3*scale;
            double dx=x*Math.cos(yaw),dz=x*Math.sin(yaw);
            // Velocity slots carry orbit parameters to this particle's provider.
            level.addParticle(StrangeParticles.PORTAL_SPARK.get(),ring.getX()+dx,ring.getY()+y,ring.getZ()+dz,angle,yaw,scale);
        }
    }
}
