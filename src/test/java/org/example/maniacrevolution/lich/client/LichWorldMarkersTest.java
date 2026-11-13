package org.example.maniacrevolution.lich.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public final class LichWorldMarkersTest {
    private LichWorldMarkersTest() {}

    public static void verify() {
        List<Vec3> vertices=new ArrayList<>();
        var sink=(VertexConsumer)Proxy.newProxyInstance(LichWorldMarkersTest.class.getClassLoader(),
                new Class<?>[]{VertexConsumer.class},(proxy,method,args)->{
                    if(method.isDefault())return InvocationHandler.invokeDefault(proxy,method,args);
                    if(method.getName().equals("vertex"))
                        vertices.add(new Vec3(((Number)args[0]).doubleValue(),((Number)args[1]).doubleValue(),((Number)args[2]).doubleValue()));
                    return method.getReturnType()==VertexConsumer.class?proxy:null;
                });
        var block=new BlockPos(29_000_001,64,-29_000_001);
        var camera=Vec3.atLowerCornerOf(block);
        var lines=new LichWorldMarkers.Lines(new PoseStack().last(),sink,camera);
        var bounds=new AABB(camera,camera.add(1,.5,1));
        lines.face(bounds,new BlockHitResult(camera.add(.5,.5,.5),Direction.UP,block,false),.48f);
        require(vertices.size()==16&&vertices.stream().allMatch(v->Math.abs(v.y-.508)<.00001),
                "four face corners stay on the slab's selected surface");
        require(vertices.stream().anyMatch(v->Math.abs(v.x-.24)<.00001),"camera-relative coordinates retain sub-block precision");
        vertices.clear();
        lines.face(bounds,new BlockHitResult(camera.add(1,.25,.5),Direction.EAST,block,false),.48f);
        require(vertices.size()==16&&vertices.stream().allMatch(v->Math.abs(v.x-1.008)<.00001),"a side hit draws in the vertical plane");
        vertices.clear();
        lines.corners(bounds,.48f);
        require(vertices.size()==48,"the hover volume uses three short edges per corner");
        vertices.clear();
        lines.arc(camera,.4,0,.48f,32);
        require(vertices.isEmpty(),"empty ritual progress draws no foreground arc");
        lines.arc(camera,.4,.5,.48f,32);
        require(vertices.size()==64&&Math.abs(vertices.get(63).x+.4)<.00001,"ritual progress draws the correct fraction");
        vertices.clear();
        lines.cross(camera,Direction.Axis.Y,.14,.48f,false);
        lines.cross(camera,Direction.Axis.Y,.14,.48f,true);
        require(vertices.size()==6,"remove and add signs use one and two strokes");
        require(LichWorldMarkers.fade(0)>LichWorldMarkers.fade(12)&&LichWorldMarkers.fade(16)==0,"accepted cues fade to zero");
        System.out.println("Lich world markers: slab/side corners, camera precision, ritual arc, editor signs and cue fade.");
    }
    private static void require(boolean condition,String message) {
        if(!condition)throw new AssertionError(message);
    }
}
