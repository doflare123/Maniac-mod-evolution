package org.example.maniacrevolution.lich;

import java.awt.image.BufferedImage;
import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import javax.imageio.ImageIO;
import com.mojang.blaze3d.vertex.*;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoRenderer;

/** CPU diagnostic of vertices emitted by the installed renderer, no game window or OpenGL. */
final class LichMeshPreview {
    @SuppressWarnings("unchecked") static void write(BakedGeoModel model,String file)throws Exception {
        var points=new ArrayList<double[]>();double[] pending=new double[5];
        var sink=(VertexConsumer)Proxy.newProxyInstance(LichMeshPreview.class.getClassLoader(),new Class[]{VertexConsumer.class},(p,m,v)->{
            if(m.getName().equals("vertex")&&v.length>=3)for(int i=0;i<3;i++)pending[i]=((Number)v[i]).doubleValue();
            if(m.getName().equals("uv")){pending[3]=((Number)v[0]).doubleValue();pending[4]=((Number)v[1]).doubleValue();}
            if(m.getName().equals("endVertex"))points.add(pending.clone());
            if(m.isDefault())return InvocationHandler.invokeDefault(p,m,v);return m.getReturnType()==VertexConsumer.class?p:null;
        });
        var renderer=(GeoRenderer<GeoAnimatable>)Proxy.newProxyInstance(LichMeshPreview.class.getClassLoader(),new Class[]{GeoRenderer.class},(p,m,v)->m.isDefault()?InvocationHandler.invokeDefault(p,m,v):null);
        for(var bone:model.topLevelBones())renderer.renderRecursively(new PoseStack(),null,bone,null,null,sink,true,0,0,0,1,1,1,1);
        if(points.isEmpty())throw new AssertionError("No vertices in Lich runtime preview");
        BufferedImage texture;try(var input=LichMeshPreview.class.getResourceAsStream("/assets/maniacrev/textures/entity/lich.png")){texture=ImageIO.read(input);}
        int width=700,height=850;var output=new BufferedImage(width,height,BufferedImage.TYPE_INT_RGB);var depth=new double[width*height];Arrays.fill(depth,Double.NEGATIVE_INFINITY);
        for(int y=0;y<height;y++)for(int x=0;x<width;x++)output.setRGB(x,y,0x222838);
        double angle=Math.toRadians(15);
        for(var point:points){double x=point[0],z=point[2];point[0]=width/2.0+240*(x*Math.cos(angle)+z*Math.sin(angle));point[1]=height-40-240*point[1];point[2]=x*Math.sin(angle)-z*Math.cos(angle);}
        for(int i=0;i<points.size();i+=4)for(int[] ids:List.of(new int[]{0,1,2},new int[]{0,2,3})) {
            var a=points.get(i+ids[0]);var b=points.get(i+ids[1]);var c=points.get(i+ids[2]);
            double denominator=(b[1]-c[1])*(a[0]-c[0])+(c[0]-b[0])*(a[1]-c[1]);if(Math.abs(denominator)<1e-9)continue;
            int x0=Math.max(0,(int)Math.floor(Math.min(a[0],Math.min(b[0],c[0])))),x1=Math.min(width-1,(int)Math.ceil(Math.max(a[0],Math.max(b[0],c[0]))));
            int y0=Math.max(0,(int)Math.floor(Math.min(a[1],Math.min(b[1],c[1])))),y1=Math.min(height-1,(int)Math.ceil(Math.max(a[1],Math.max(b[1],c[1]))));
            for(int y=y0;y<=y1;y++)for(int x=x0;x<=x1;x++){
                double wa=((b[1]-c[1])*(x+.5-c[0])+(c[0]-b[0])*(y+.5-c[1]))/denominator;
                double wb=((c[1]-a[1])*(x+.5-c[0])+(a[0]-c[0])*(y+.5-c[1]))/denominator,wc=1-wa-wb;if(wa<0||wb<0||wc<0)continue;
                double z=wa*a[2]+wb*b[2]+wc*c[2];if(z<=depth[y*width+x])continue;
                int tx=Math.max(0,Math.min(texture.getWidth()-1,(int)((wa*a[3]+wb*b[3]+wc*c[3])*texture.getWidth())));
                int ty=Math.max(0,Math.min(texture.getHeight()-1,(int)((wa*a[4]+wb*b[4]+wc*c[4])*texture.getHeight())));
                int color=texture.getRGB(tx,ty);if((color>>>24)==0)continue;
                output.setRGB(x,y,color);depth[y*width+x]=z;
            }
        }
        ImageIO.write(output,"png",new File(file));
    }
}
