package org.example.maniacrevolution.lich;

import java.lang.reflect.*;
import java.util.*;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.renderer.*;
import org.example.maniacrevolution.lich.client.*;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoRenderer;

/** Models BufferSource's shared builder: stale consumers write into the currently active pass. */
final class LichBookBufferTest {
    @SuppressWarnings("unchecked") static void verify()throws Exception {
        var renderer=(GeoRenderer<LichPlayerForm.Body>)Proxy.newProxyInstance(LichBookBufferTest.class.getClassLoader(),new Class[]{GeoRenderer.class},(p,m,v)->null);
        var draws=new ArrayList<String>();String[] active={"body"};
        var sink=(VertexConsumer)Proxy.newProxyInstance(LichBookBufferTest.class.getClassLoader(),new Class[]{VertexConsumer.class},(p,m,v)->{
            if(m.getName().equals("endVertex"))draws.add(active[0]);return m.getReturnType()==VertexConsumer.class?p:null;
        });
        var bookType=RenderType.lines();
        MultiBufferSource buffers=type->{active[0]=type==bookType?"book":"body";return sink;};
        boolean[] fail={false};
        var layer=new LichBookLayer(renderer,()->null){
            @Override protected boolean hasBook(){return true;}
            @Override protected void renderBook(PoseStack pose,MultiBufferSource source,int light,int overlay){
                pose.translate(20,30,40);source.getBuffer(bookType).endVertex();if(fail[0])throw new IllegalStateException("nested item failed");
            }
        };
        var socket=new GeoBone(null,"book_socket",false,0.0,false,false);socket.updatePivot(0,22,-9);
        var pose=new PoseStack();var initial=new org.joml.Matrix4f(pose.last().pose());
        var body= buffers.getBuffer(null);body.endVertex();
        layer.renderForBone(pose,null,socket,null,buffers,body,0,0,0);body.endVertex();
        if(!draws.equals(List.of("body","book","body")))throw new AssertionError("Body vertices leaked into item pass: "+draws);
        if(!initial.equals(pose.last().pose()))throw new AssertionError("Book changed parent pose");
        fail[0]=true;
        try{layer.renderForBone(pose,null,socket,null,buffers,body,0,0,0);throw new AssertionError("Expected item failure");}catch(IllegalStateException expected){}
        if(!active[0].equals("body")||!initial.equals(pose.last().pose()))throw new AssertionError("Failed item did not restore body pass");
        System.out.println("Lich book layer: body/book/body buffer isolation and pose restoration passed (including item failure)");
    }
}
