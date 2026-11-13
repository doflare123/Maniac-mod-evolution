package org.example.maniacrevolution.lich.client;

import java.io.*;
import java.util.*;
import com.google.gson.*;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.*;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.HumanoidArm;
import org.example.maniacrevolution.Maniacrev;
import org.example.maniacrevolution.lich.*;

/** The vanilla humanoid mesh, split at elbows/knees, with the authored vanilla-rig poses. */
public final class LichVanillaModel extends HumanoidModel<LichUnit> {
    public record Rig(ModelPart root, Map<String,ModelPart> parts, JsonObject clips) {}
    private final Rig rig;
    private record Channel(double[] times,float[][] values) {
        float[] at(double time) {
            int index=Arrays.binarySearch(times,time);
            if(index>=0)return values[index];int high=-index-1;
            if(high==0)return values[0];if(high==times.length)return values[high-1];
            float weight=(float)((time-times[high-1])/(times[high]-times[high-1]));var result=new float[3];
            for(int i=0;i<3;i++)result[i]=values[high-1][i]+(values[high][i]-values[high-1][i])*weight;return result;
        }
    }
    private record BonePose(Channel rotation,Channel position,Channel scale) {}
    private record Clip(double length,boolean loop,Map<String,BonePose> bones) {}
    private final Map<String,Clip> clips=new HashMap<>();
    public LichVanillaModel(boolean skeleton) { this(load(skeleton)); }
    public LichVanillaModel(Rig rig) {
        super(rig.root());this.rig=rig;
        for(var entry:rig.clips().entrySet()) {
            var animation=entry.getValue().getAsJsonObject();var bones=new LinkedHashMap<String,BonePose>();
            for(var bone:animation.getAsJsonObject("bones").entrySet()) {
                if(!rig.parts().containsKey(bone.getKey()))throw new IllegalArgumentException("Missing vanilla joint: "+bone.getKey());
                var channels=bone.getValue().getAsJsonObject();bones.put(bone.getKey(),new BonePose(channel(channels.get("rotation"),0),channel(channels.get("position"),0),channel(channels.get("scale"),1)));
            }
            clips.put(entry.getKey().substring(entry.getKey().lastIndexOf('.')+1),new Clip(animation.get("animation_length").getAsDouble(),animation.has("loop")&&animation.get("loop").getAsString().equals("true"),bones));
        }
    }
    private static Rig load(boolean skeleton) {
        String stem=skeleton?"lich_skeleton":"lich_zombie";
        var resources=Minecraft.getInstance().getResourceManager();
        try(var geometry=resources.openAsReader(Maniacrev.loc("lich_motion/"+stem+".rig.json"));
            var motion=resources.openAsReader(Maniacrev.loc("lich_motion/"+stem+".motion.json"))) {
            return read(geometry,motion);
        } catch(IOException e) { throw new IllegalStateException("Cannot load Lich vanilla rig: "+stem,e); }
    }
    public static Rig read(Reader geometry,Reader motion) {
        var model=JsonParser.parseReader(geometry).getAsJsonObject().getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject();
        var description=model.getAsJsonObject("description");
        float width=description.get("texture_width").getAsFloat(),height=description.get("texture_height").getAsFloat();
        // The skeleton reference packs the vanilla skin and decorative quiver in
        // one 256x128 atlas. Runtime draws the body with Minecraft's 64x32 skin;
        // only the separately rendered quiver uses its own texture dimensions.
        if(description.get("identifier").getAsString().equals("geometry.lich_skeleton_reference")){width=64;height=32;}
        var bones=new LinkedHashMap<String,JsonObject>();
        for(var value:model.getAsJsonArray("bones")) {
            var bone=value.getAsJsonObject();String name=bone.get("name").getAsString();
            // The quiver is a separately textured Geo layer attached to body.
            bones.put(name,bone);
        }
        var parts=new LinkedHashMap<String,ModelPart>();
        var children=new HashMap<String,Map<String,ModelPart>>();
        for(var entry:bones.entrySet()) {
            String name=entry.getKey();var bone=entry.getValue();float[] pivot=vector(bone.get("pivot"));
            var cubes=new ArrayList<ModelPart.Cube>();
            if(!name.equals("quiver")&&bone.has("cubes"))for(var value:bone.getAsJsonArray("cubes")) {
                var cube=value.getAsJsonObject();float[] origin=vector(cube.get("origin")),size=vector(cube.get("size"));
                float x=origin[0]-pivot[0],y=pivot[1]-origin[1]-size[1],z=origin[2]-pivot[2];
                var uv=cube.getAsJsonObject("uv");
                for(var face:uv.entrySet()) {
                    var data=face.getValue().getAsJsonObject();float[] start=vector2(data.getAsJsonArray("uv"));
                    Direction direction=Direction.valueOf(face.getKey().toUpperCase(Locale.ROOT));
                    float u=start[0],v=start[1];
                    switch(direction) {
                        case WEST -> v-=size[2];
                        case NORTH -> {u-=size[2];v-=size[2];}
                        case EAST -> {u-=size[2]+size[0];v-=size[2];}
                        case SOUTH -> {u-=2*size[2]+size[0];v-=size[2];}
                        case UP -> {u-=size[2]+size[0];v-=size[2];direction=Direction.DOWN;}
                        case DOWN -> {u-=size[2]+2*size[0];direction=Direction.UP;}
                    }
                    cubes.add(new ModelPart.Cube(Math.round(u),Math.round(v),x,y,z,size[0],size[1],size[2],0,0,0,false,width,height,Set.of(direction)));
                }
            }
            var descendants=new LinkedHashMap<String,ModelPart>();children.put(name,descendants);
            var part=new ModelPart(cubes,descendants);parts.put(name,part);
            float[] parent=bone.has("parent")?vector(bones.get(bone.get("parent").getAsString()).get("pivot")):new float[]{0,24,0};
            part.setInitialPose(PartPose.offset(pivot[0]-parent[0],parent[1]-pivot[1],pivot[2]-parent[2]));part.resetPose();
        }
        for(var entry:bones.entrySet())if(entry.getValue().has("parent"))
            children.get(entry.getValue().get("parent").getAsString()).put(entry.getKey(),parts.get(entry.getKey()));
        children.get("root").put("hat",new ModelPart(List.of(),Map.of()));
        return new Rig(parts.get("root"),parts,JsonParser.parseReader(motion).getAsJsonObject().getAsJsonObject("animations"));
    }
    private static float[] vector2(JsonArray a) { return new float[]{a.get(0).getAsFloat(),a.get(1).getAsFloat()}; }
    private static float[] vector(JsonElement value) {
        if(value==null)return new float[3];var a=value.getAsJsonArray();
        return new float[]{a.get(0).getAsFloat(),a.get(1).getAsFloat(),a.get(2).getAsFloat()};
    }
    private static Channel channel(JsonElement channel,float fallback) {
        if(channel==null)return new Channel(new double[]{0},new float[][]{{fallback,fallback,fallback}});
        if(channel.isJsonArray())return new Channel(new double[]{0},new float[][]{vector(channel)});
        var keys=new TreeMap<Double,float[]>();
        for(var entry:channel.getAsJsonObject().entrySet())keys.put(Double.parseDouble(entry.getKey()),vector(entry.getValue()));
        return new Channel(keys.keySet().stream().mapToDouble(Double::doubleValue).toArray(),keys.values().toArray(float[][]::new));
    }
    public void pose(String clip,double seconds) {
        rig.parts().values().forEach(ModelPart::resetPose);
        var animation=Objects.requireNonNull(clips.get(clip),clip);double length=animation.length();
        seconds=animation.loop()?seconds%length:Math.min(seconds,length);
        for(var entry:animation.bones().entrySet()) {
            var part=rig.parts().get(entry.getKey());if(part==null)continue;
            var channels=entry.getValue();var r=channels.rotation().at(seconds);var p=channels.position().at(seconds);var scale=channels.scale().at(seconds);
            part.x+=p[0];part.y-=p[1];part.z+=p[2];
            // Gecko's Bedrock adapter flips animation X/Y and mesh X. The vanilla
            // renderer's X/Y flip cancels these: ModelPart Euler angles keep their signs.
            part.xRot=(float)Math.toRadians(r[0]);part.yRot=(float)Math.toRadians(r[1]);part.zRot=(float)Math.toRadians(r[2]);
            part.xScale=scale[0];part.yScale=scale[1];part.zScale=scale[2];
        }
    }
    @Override public void setupAnim(LichUnit unit,float swing,float amount,float age,float yaw,float pitch) {
        var frame=LichUnitVisuals.frame(unit,age);pose(frame.clip(),frame.seconds());
        if(frame.loop()||frame.clip().equals("shoot")) {
            float aimYaw=(float)Math.toRadians(Math.max(-45,Math.min(45,yaw))),aimPitch=(float)Math.toRadians(Math.max(-30,Math.min(30,pitch)));
            head.yRot+=aimYaw;head.xRot+=aimPitch;
            if(frame.clip().equals("shoot")) {rightArm.yRot+=aimYaw;leftArm.yRot+=aimYaw;rightArm.xRot+=aimPitch;leftArm.xRot+=aimPitch;}
        }
    }
    @Override public void renderToBuffer(PoseStack pose,VertexConsumer vertices,int light,int overlay,float r,float g,float b,float a) {
        rig.root().render(pose,vertices,light,overlay,r,g,b,a);
    }
    @Override public void translateToHand(HumanoidArm side,PoseStack pose) {
        rig.root().translateAndRotate(pose);var arm=side==HumanoidArm.RIGHT?rightArm:leftArm;arm.translateAndRotate(pose);
        arm.getChild(side==HumanoidArm.RIGHT?"right_forearm":"left_forearm").translateAndRotate(pose);
        pose.translate(0,-5/16.0,0); // ItemInHandLayer adds the full vanilla ten-unit hand offset.
    }
    public void bodyTransform(PoseStack pose) { rig.root().translateAndRotate(pose);body.translateAndRotate(pose); }
}
