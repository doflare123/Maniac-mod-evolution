package org.example.maniacrevolution.lich;

import java.io.*;
import java.lang.reflect.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import com.google.gson.*;
import com.mojang.blaze3d.vertex.*;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.loading.json.raw.Model;
import software.bernie.geckolib.loading.object.*;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.util.JsonUtil;

/** Bakes the shipping files using the installed GeckoLib, emits and validates their mesh. */
public final class LichModelBakeTest {
    private static Reader resource(String path) {
        var stream=LichModelBakeTest.class.getResourceAsStream("/assets/maniacrev/"+path);
        if(stream==null)throw new AssertionError("Missing resource: "+path);
        return new InputStreamReader(stream,StandardCharsets.UTF_8);
    }
    @SuppressWarnings("unchecked") public static void main(String[] args)throws Exception {
        net.minecraft.SharedConstants.tryDetectVersion();net.minecraft.server.Bootstrap.bootStrap();int total=0;
        var renderer=(GeoRenderer<GeoAnimatable>)Proxy.newProxyInstance(LichModelBakeTest.class.getClassLoader(),new Class[]{GeoRenderer.class},
                (p,m,v)->m.isDefault()?InvocationHandler.invokeDefault(p,m,v):null);
        for(String stem:List.of("lich","lich_ghoul","lich_butcher","lich_corpse","lich_book","lich_phylactery","lich_necromancy_fx","lich_skeleton_quiver")){
            Model raw;try(var reader=resource("geo/"+stem+".geo.json")){raw=JsonUtil.GEO_GSON.fromJson(reader,Model.class);}
            var baked=BakedModelFactory.DEFAULT_FACTORY.constructGeoModel(GeometryTree.fromModel(raw));int[] count={0};
            var consumer=(VertexConsumer)Proxy.newProxyInstance(LichModelBakeTest.class.getClassLoader(),new Class[]{VertexConsumer.class},(p,m,v)->{
                if(m.getName().equals("vertex")&&v.length==3)for(var n:v)if(!Double.isFinite(((Number)n).doubleValue()))throw new AssertionError("Nonfinite "+stem);
                if(m.getName().equals("endVertex"))count[0]++;if(m.isDefault())return InvocationHandler.invokeDefault(p,m,v);return m.getReturnType()==VertexConsumer.class?p:null;
            });
            for(var root:baked.topLevelBones())renderer.renderRecursively(new PoseStack(),null,root,null,null,consumer,true,0,0,0,1,1,1,1);
            if(count[0]==0)throw new AssertionError("Empty mesh "+stem);total+=count[0];
            JsonObject animations;try(var reader=resource("animations/"+stem+".animation.json")){animations=JsonParser.parseReader(reader).getAsJsonObject().getAsJsonObject("animations");}
            BakedAnimations parsed;
            parsed=JsonUtil.GEO_GSON.fromJson(animations,BakedAnimations.class);
            if(parsed.animations().size()!=animations.size())throw new AssertionError("GeckoLib animation parse lost clips: "+stem);
            var bones=new HashSet<String>();for(var geometry:raw.minecraftGeometry())for(var bone:geometry.bones())bones.add(bone.name());
            for(var entry:animations.entrySet()){
                if(!entry.getKey().startsWith("animation."+stem+"."))throw new AssertionError("Animation namespace "+entry.getKey());
                double duration=entry.getValue().getAsJsonObject().get("animation_length").getAsDouble();
                if(entry.getKey().endsWith(".emerge")&&Math.abs(duration-5)>.001)throw new AssertionError("Emerge duration");
                if(entry.getKey().contains(".cast_")&&Math.abs(duration-.5)>.001)throw new AssertionError("Cast duration");
                for(var bone:entry.getValue().getAsJsonObject().getAsJsonObject("bones").keySet())if(!bones.contains(bone))throw new AssertionError("Missing animated bone "+stem+":"+bone);
            }
            String folder=stem.equals("lich_book")||stem.equals("lich_phylactery")?"item":"entity";
            for(String suffix:List.of("","_glowmask"))try(var stream=LichModelBakeTest.class.getResourceAsStream("/assets/maniacrev/textures/"+folder+"/"+stem+suffix+".png")){
                if(stream==null)throw new AssertionError("Missing texture "+stem+suffix);
                var texture=javax.imageio.ImageIO.read(stream);if(texture==null)throw new AssertionError("Invalid texture "+stem+suffix);
                boolean visible=false;
                for(int y=0;y<texture.getHeight();y++)for(int x=0;x<texture.getWidth();x++)visible|=(texture.getRGB(x,y)>>>24)!=0;
                boolean deliberatelyEmpty=suffix.equals("_glowmask")&&(stem.equals("lich_corpse")||stem.equals("lich_skeleton_quiver"));
                if(visible==deliberatelyEmpty)throw new AssertionError("Unexpected texture alpha: "+stem+suffix);
            }
            System.out.println(stem+": "+count[0]+" finite vertices, "+animations.size()+" animations, texture and glowmask decoded");
        }
        verifyVanillaPoses();
        LichRuntimePoseTest.verify();
        LichBookBufferTest.verify();
        LichHeldItemLayerTest.verify();
        LichCorpseLayoutTest.verify();
        LichQuiverAttachmentTest.verify();
        LichRitualInputTest.verify();
        LichHudLayoutTest.verify();
        LichArmyLayoutTest.verify();
        LichArmyRenderTest.verify();
        LichAbilityMenuKeyTest.verify();
        LichReticleTest.verify();
        org.example.maniacrevolution.lich.client.LichWorldMarkersTest.verify();
        JsonObject sounds;try(var reader=resource("sounds.json")){sounds=JsonParser.parseReader(reader).getAsJsonObject();}
        int files=0;
        for(var entry:sounds.entrySet())if(entry.getKey().startsWith("lich."))for(var sound:entry.getValue().getAsJsonObject().getAsJsonArray("sounds")){
            String id=sound.getAsJsonObject().get("name").getAsString();String path="sounds/"+id.substring(id.indexOf(':')+1)+".ogg";
            try(var stream=LichModelBakeTest.class.getResourceAsStream("/assets/maniacrev/"+path)){if(stream==null||!new String(stream.readNBytes(4),StandardCharsets.US_ASCII).equals("OggS"))throw new AssertionError("Missing/invalid OGG "+path);}files++;
        }
        if(files!=29)throw new AssertionError("Expected 29 Lich OGG files, got "+files);
        System.out.println("Lich resources verified: "+total+" vertices, 29 OGG references.");
    }
    private static void verifyVanillaPoses()throws Exception {
        for(String stem:List.of("lich_zombie","lich_skeleton")) {
            org.example.maniacrevolution.lich.client.LichVanillaModel.Rig rig;
            try(var geometry=resource("lich_motion/"+stem+".rig.json");var motion=resource("lich_motion/"+stem+".motion.json")){
                rig=org.example.maniacrevolution.lich.client.LichVanillaModel.read(geometry,motion);
            }
            var model=new org.example.maniacrevolution.lich.client.LichVanillaModel(rig);
            if (stem.equals("lich_skeleton")) {
                float[] maximum = {0, 0};
                var uvSink = (VertexConsumer)Proxy.newProxyInstance(LichModelBakeTest.class.getClassLoader(), new Class[]{VertexConsumer.class}, (p,m,v)->{
                    if (m.getName().equals("uv")) {
                        maximum[0] = Math.max(maximum[0], ((Number)v[0]).floatValue());
                        maximum[1] = Math.max(maximum[1], ((Number)v[1]).floatValue());
                    }
                    if(m.isDefault())return InvocationHandler.invokeDefault(p,m,v);
                    return m.getReturnType()==VertexConsumer.class?p:null;
                });
                rig.parts().get("head").render(new PoseStack(), uvSink, 0, 0);
                if (Math.abs(maximum[0]-.5f)>.0001 || Math.abs(maximum[1]-.5f)>.0001)
                    throw new AssertionError("Skeleton head must sample 32x16 pixels of the 64x32 vanilla skin: " + Arrays.toString(maximum));
                System.out.println("Skeleton vanilla skin UV coverage verified (64x32, independent of quiver atlas).");
            }
            int[] vertices={0};double[] minimum={Double.POSITIVE_INFINITY};
            var sink=(VertexConsumer)Proxy.newProxyInstance(LichModelBakeTest.class.getClassLoader(),new Class[]{VertexConsumer.class},(p,m,v)->{
                if(m.getName().equals("vertex")&&v.length>=3){
                    for(int i=0;i<3;i++)if(!Double.isFinite(((Number)v[i]).doubleValue()))throw new AssertionError("Nonfinite vanilla pose "+stem);
                    minimum[0]=Math.min(minimum[0],((Number)v[1]).doubleValue());vertices[0]++;
                }
                if(m.isDefault())return InvocationHandler.invokeDefault(p,m,v);return m.getReturnType()==VertexConsumer.class?p:null;
            });
            int samples=0;
            for(var entry:rig.clips().entrySet()) {
                var animation=entry.getValue().getAsJsonObject();String clip=entry.getKey().substring(entry.getKey().lastIndexOf('.')+1);
                int frames=(int)Math.ceil(animation.get("animation_length").getAsDouble()*60);
                for(int frame=0;frame<=frames;frame++) {
                    model.pose(clip,frame/60.0);minimum[0]=Double.POSITIVE_INFINITY;
                    var pose=new PoseStack();pose.scale(-1,-1,1);pose.translate(0,-1.5,0);
                    model.renderToBuffer(pose,sink,0,0,1,1,1,1);samples++;
                    if(minimum[0]<-.015)throw new AssertionError(stem+" "+clip+" t="+frame/60.0+" sinks below floor: "+minimum[0]);
                }
            }
            model.pose("emerge",2);model.pose("idle",0);
            if(stem.equals("lich_skeleton")) {
                float idle=model.rightArm.xRot;model.pose("shoot",.30);
                if(model.rightArm.xRot> -1||Math.abs(model.rightArm.xRot-idle)<.5)
                    throw new AssertionError("Skeleton must hold its bow raised at the server release phase");
                model.pose("idle",0);
            }
            if(Math.abs(rig.parts().get("right_forearm").xRot)>.001)throw new AssertionError("Joint did not reset after emerge");
            if(vertices[0]==0)throw new AssertionError("No vanilla mesh");
            System.out.println(stem+": "+rig.clips().size()+" clips, "+samples+" ModelPart poses emitted, finite vertices and floor support verified");
        }
        for(boolean swim:List.of(false,true))for(boolean crouch:List.of(false,true))for(boolean sprint:List.of(false,true))for(boolean move:List.of(false,true)){
            String clip=org.example.maniacrevolution.lich.client.LichPlayerForm.locomotion(swim,crouch,sprint,move);
            try(var reader=resource("animations/lich.animation.json")){
                var animations=JsonParser.parseReader(reader).getAsJsonObject().getAsJsonObject("animations");
                if(!animations.has("animation.lich."+clip)||!animations.has("animation.lich.book_"+clip))throw new AssertionError("Missing Lich locomotion/book clip "+clip);
            }
        }
    }
}
