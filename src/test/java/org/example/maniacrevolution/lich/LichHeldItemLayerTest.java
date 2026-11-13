package org.example.maniacrevolution.lich;

import java.lang.reflect.*;
import java.util.*;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.renderer.*;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.*;
import org.example.maniacrevolution.lich.client.*;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoRenderer;

final class LichHeldItemLayerTest {
    @SuppressWarnings("unchecked") static void verify()throws Exception {
        var main=new ItemStack(Items.DIAMOND_SWORD);var off=new ItemStack(Items.SHIELD);
        for(var arm:HumanoidArm.values())for(boolean left:List.of(false,true)) {
            String socket=left?"left_palm_socket":"right_palm_socket";
            boolean primary=(arm==HumanoidArm.LEFT)==left;
            var item=LichHeldItemLayer.held(socket,arm,main,off,false);
            if(item.stack()!=(primary?main:off)||item.left()!=left)throw new AssertionError("Wrong physical hand: "+arm+" "+socket);
            item=LichHeldItemLayer.held(socket,arm,main,off,true);
            if(primary?!item.stack().isEmpty():item.stack()!=off)throw new AssertionError("Book duplicated or offhand hidden");
            if(!LichHeldItemLayer.held(socket,arm,ItemStack.EMPTY,ItemStack.EMPTY,false).stack().isEmpty())throw new AssertionError("Empty hand renders an item");
        }
        if(!LichHeldItemLayer.held("book_socket",HumanoidArm.RIGHT,main,off,false).stack().isEmpty())throw new AssertionError("Ordinary item on special book socket");
        var renderer=(GeoRenderer<LichPlayerForm.Body>)Proxy.newProxyInstance(LichHeldItemLayerTest.class.getClassLoader(),new Class[]{GeoRenderer.class},(p,m,v)->null);
        String[] active={"body"};var draws=new ArrayList<String>();
        var sink=(VertexConsumer)Proxy.newProxyInstance(LichHeldItemLayerTest.class.getClassLoader(),new Class[]{VertexConsumer.class},(p,m,v)->{
            if(m.getName().equals("endVertex"))draws.add(active[0]);return m.getReturnType()==VertexConsumer.class?p:null;
        });
        var itemType=RenderType.lines();MultiBufferSource buffers=type->{active[0]=type==itemType?"item":"body";return sink;};
        boolean[] fail={false};
        var layer=new LichHeldItemLayer(renderer,()->null){
            @Override protected Held held(String bone){return new Held(main,false);}
            @Override protected void renderItem(Held item,PoseStack pose,MultiBufferSource source,int light,int overlay){
                var vanillaEquivalent=new org.joml.Matrix4f().translate(5.25F/16,18.1F/16,-1F/16)
                        .scale(-1,-1,1).rotateX(-(float)Math.PI/2).rotateY((float)Math.PI);
                if(!vanillaEquivalent.equals(pose.last().pose(),.00001F))throw new AssertionError("Item orientation differs from vanilla hand axes");
                pose.translate(12,13,14);source.getBuffer(itemType).endVertex();if(fail[0])throw new IllegalStateException("item failed");
            }
        };
        var socket=new GeoBone(null,"right_palm_socket",false,0.0,false,false);socket.updatePivot(5.25F,18.1F,-1);
        var pose=new PoseStack();var initial=new org.joml.Matrix4f(pose.last().pose());var body=buffers.getBuffer(null);body.endVertex();
        layer.renderForBone(pose,null,socket,null,buffers,body,0,0,0);body.endVertex();
        if(!draws.equals(List.of("body","item","body"))||!initial.equals(pose.last().pose()))throw new AssertionError("Held item corrupts parent pass/pose");
        fail[0]=true;
        try{layer.renderForBone(pose,null,socket,null,buffers,body,0,0,0);throw new AssertionError("Expected item failure");}catch(IllegalStateException expected){}
        if(!active[0].equals("body")||!initial.equals(pose.last().pose()))throw new AssertionError("Item failure corrupts parent pass/pose");
        System.out.println("Lich held items: both dominant hands, offhand, book exclusion, empty slots and buffer/pose restoration passed");
    }
}
