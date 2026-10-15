package org.example.maniacrevolution.techies.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import software.bernie.geckolib.renderer.GeoObjectRenderer;
import software.bernie.geckolib.cache.object.BakedGeoModel;

public final class TechiesWorldRenderer extends GeoObjectRenderer<TechiesVisual> {
    public TechiesWorldRenderer() { super(new TechiesModel()); }
    @Override public void preRender(PoseStack pose,TechiesVisual v,BakedGeoModel model,MultiBufferSource buffer,
            VertexConsumer consumer,boolean reRender,float partial,int light,int overlay,float r,float g,float b,float a) {
        pose.translate(-.5,-.51,-.5);
        super.preRender(pose,v,model,buffer,consumer,reRender,partial,light,overlay,r,g,b,a);
    }
    @Override public void actuallyRender(PoseStack pose,TechiesVisual v,BakedGeoModel model,net.minecraft.client.renderer.RenderType type,
            MultiBufferSource buffer,VertexConsumer consumer,boolean reRender,float partial,int light,int overlay,float r,float g,float b,float a) {
        super.actuallyRender(pose,v,model,type,buffer,consumer,reRender,partial,light,overlay,v.red,v.green,v.blue,v.alpha);
    }
}
