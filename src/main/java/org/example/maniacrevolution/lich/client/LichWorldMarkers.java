package org.example.maniacrevolution.lich.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.example.maniacrevolution.Maniacrev;
import org.example.maniacrevolution.ModItems;
import org.example.maniacrevolution.lich.LichRules;
import org.example.maniacrevolution.lich.LichUnit;

@Mod.EventBusSubscriber(modid=Maniacrev.MODID,value=Dist.CLIENT)
public final class LichWorldMarkers {
    private static final int DURATION=16;
    private static final float PREVIEW_ALPHA=.48f;
    private record Cue(String kind,Vec3 point,int target,long start) {}
    private static final Map<Integer,Long> PULSES=new HashMap<>();
    private static ClientLevel level;
    private static LocalPlayer player;
    private static Cue cue;
    private static String status="";
    private static long statusUntil,swingUntil;

    private LichWorldMarkers() {}
    static void clear() {
        PULSES.clear();cue=null;status="";statusUntil=swingUntil=0;level=null;player=null;
    }
    private static boolean scope() {
        var mc=Minecraft.getInstance();
        if(mc==null)return false;
        if(mc.level!=level||mc.player!=player) {clear();level=mc.level;player=mc.player;}
        return level!=null&&player!=null;
    }
    static void accept(CompoundTag tag) {
        if(!scope()||!tag.getString("dimension").equals(level.dimension().location().toString()))return;
        long now=level.getGameTime();
        String kind=tag.getString("kind");
        for(int id:tag.getIntArray("units"))PULSES.put(id,now);
        Vec3 point=tag.contains("x")?new Vec3(tag.getDouble("x"),tag.getDouble("y"),tag.getDouble("z")):null;
        int target=tag.contains("target")?tag.getInt("target"):-1;
        if(point!=null||target>=0)cue=new Cue(kind,point,target,now);
        String message=switch(kind) {
            case "group" -> "Группа "+tag.getInt("detail")+" сохранена";
            case "aggressive" -> "До конца";
            case "hold" -> "Держать приказ";
            case "RETURN" -> "↑ Вернуться";
            case "FOLLOW" -> "↓ Следовать";
            case "SEARCH" -> "Поиск";
            default -> "";
        };
        if(!message.isEmpty()) {status=message;statusUntil=now+24;}
    }
    static String status() {return scope()&&level.getGameTime()<statusUntil?status:"";}
    static void swing() {if(scope())swingUntil=level.getGameTime()+5;}
    static boolean swingFlash() {return scope()&&level.getGameTime()<swingUntil;}
    static int pulseAlpha(int entityId) {
        if(!scope())return 0;
        Long start=PULSES.get(entityId);
        return start==null?0:Math.round(255*fade(level.getGameTime()-start));
    }

    @SubscribeEvent public static void render(RenderLevelStageEvent event) {
        if(event.getStage()!=RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS||!scope())return;
        var mc=Minecraft.getInstance();
        double now=level.getGameTime()+event.getPartialTick();
        PULSES.values().removeIf(start->now-start>=DURATION);
        if(cue!=null&&now-cue.start>=DURATION)cue=null;
        if(mc.options.hideGui||mc.screen!=null||!mc.options.getCameraType().isFirstPerson())return;
        boolean editor=player.hasPermissions(2)&&player.getMainHandItem().is(ModItems.LICH_CORPSE_EDITOR.get());
        boolean active=LichReticle.visible();
        if(!active&&!editor)return;
        var pose=event.getPoseStack();
        var camera=event.getCamera().getPosition();
        var buffers=mc.renderBuffers().bufferSource();
        pose.pushPose();
        try {
            var lines=new Lines(pose.last(),buffers.getBuffer(RenderType.lines()),camera);
            if(active) {
                var ritual=LichReticle.PROGRESS.frame(now);
                if(ritual.visible()&&ritual.casting()) {
                    var focus=level.getEntity(LichReticle.PROGRESS.focus());
                    if(focus!=null&&!focus.isRemoved()) {
                        Vec3 point=focus.getPosition(event.getPartialTick()).add(0,.026,0);
                        double radius=Math.max(.42,focus.getBbWidth()*.58);
                        lines.arc(point,radius,1,.13f,32);
                        lines.arc(point,radius,ritual.progress(),.58f,32);
                    }
                } else preview(lines,event.getPartialTick(),mc.hitResult);
                for(var entry:PULSES.entrySet()) {
                    if(level.getEntity(entry.getKey()) instanceof LichUnit unit
                            &&unit.syncedOwner().filter(player.getUUID()::equals).isPresent()&&unit.distanceToSqr(player)<48*48) {
                        pulse(lines,unit.getPosition(event.getPartialTick()),now-entry.getValue(),unit.getBbWidth()*.5);
                    }
                }
            }
            if(editor)editorPreview(lines,mc.hitResult);
            if(cue!=null&&(active||editor&&isEditorCue(cue.kind)))renderCue(lines,now,event.getPartialTick());
            buffers.endBatch(RenderType.lines());
        } finally {pose.popPose();}
    }

    private static void preview(Lines lines,float partial,HitResult hit) {
        var aim=LichReticle.aimHint();
        if(aim.entity()!=null&&aim.entity().level()==level&&!aim.entity().isRemoved()&&!aim.entity().isInvisibleTo(player)) {
            Entity target=aim.entity();
            Vec3 position=target.getPosition(partial);
            lines.corners(target.getBoundingBox().move(position.subtract(target.position())).inflate(.035),PREVIEW_ALPHA);
            if(target instanceof LichUnit unit&&unit.selected())lines.arc(position.add(0,.024,0),unit.getBbWidth()*.65,1,.38f,16);
        } else if(aim.block()!=null&&level.hasChunkAt(aim.block().getBlockPos())) {
            var block=aim.block();
            var pos=block.getBlockPos();
            var shape=level.getBlockState(pos).getShape(level,pos);
            var box=shape.isEmpty()?new AABB(pos):shape.bounds().move(pos);
            if(aim.volume())lines.corners(box.inflate(.008),PREVIEW_ALPHA);
            else {
                lines.face(box,block,PREVIEW_ALPHA);
                var point=block.getLocation().add(Vec3.atLowerCornerOf(block.getDirection().getNormal()).scale(.012));
                lines.cross(point,block.getDirection().getAxis(),.075,.56f,true);
            }
        }
        if(player.getMainHandItem().is(ModItems.LICH_PHYLACTERY.get())&&!LichClient.army.getBoolean("placed")
                &&LichClient.army.getInt("hp")>0&&hit instanceof BlockHitResult block&&hit.getType()==HitResult.Type.BLOCK
                &&block.getDirection()==Direction.UP&&player.distanceToSqr(Vec3.atCenterOf(block.getBlockPos()))<=36) {
            var pos=block.getBlockPos().above();
            if(LichRules.safeSurface(level,pos,.6,.8))lines.corners(new AABB(pos).deflate(.19,0,.19).contract(0,.21,0),PREVIEW_ALPHA);
        }
    }
    private static void editorPreview(Lines lines,HitResult hit) {
        if(player.isShiftKeyDown()||!CorpsePointMarkers.validMap()||!(hit instanceof BlockHitResult block)
                ||hit.getType()!=HitResult.Type.BLOCK)return;
        var pos=block.getBlockPos().relative(block.getDirection());
        if(!LichRules.safeSurface(level,pos,.8,.5))return;
        var point=Vec3.atBottomCenterOf(pos);
        lines.corners(new AABB(point.add(-.4,.016,-.4),point.add(.4,.48,.4)),PREVIEW_ALPHA);
        lines.cross(point.add(0,.035,0),Direction.Axis.Y,.14,.64f,!CorpsePointMarkers.contains(pos));
    }
    private static boolean isEditorCue(String kind) {return kind.equals("addPoint")||kind.equals("removePoint");}
    private static void renderCue(Lines lines,double now,float partial) {
        double age=Math.max(0,now-cue.start);
        float opacity=fade(age);
        if(cue.target>=0) {
            var target=level.getEntity(cue.target);
            if(target!=null&&!target.isRemoved()&&!target.isInvisibleTo(player)&&visible(target.getEyePosition(partial))) {
                lines.corners(target.getBoundingBox().move(target.getPosition(partial).subtract(target.position())).inflate(.06),opacity);
            }
        } else if(cue.point!=null&&cue.point.distanceToSqr(player.position())<=128*128) {
            var point=cue.point;
            if(cue.kind.equals("PATROL")) {
                pulse(lines,point,age,.38);
                for(int i=0;i<8;i++)lines.arc(point.add(0,.035,0),LichRules.PATROL_RADIUS,i/8.0,.038,opacity*.62f,5);
            } else {
                pulse(lines,point,age,cue.kind.equals("GUARD")?.68:.22);
                if(isEditorCue(cue.kind))lines.cross(point.add(0,.035,0),Direction.Axis.Y,.14,opacity,cue.kind.equals("addPoint"));
            }
        }
    }
    private static boolean visible(Vec3 target) {
        return level.clip(new ClipContext(player.getEyePosition(),target,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,player))
                .getType()==HitResult.Type.MISS;
    }
    private static void pulse(Lines lines,Vec3 point,double age,double radius) {
        lines.arc(point.add(0,.025,0),radius+.32*age/DURATION,1,fade(age),24);
    }
    static float fade(double age) {return .62f*(float)Math.max(0,Math.min(1,(DURATION-age)/10));}

    record Lines(PoseStack.Pose pose,VertexConsumer out,Vec3 camera) {
        void line(Vec3 from,Vec3 to,float alpha) {
            Vec3 direction=to.subtract(from).normalize();
            vertex(from,direction,alpha);vertex(to,direction,alpha);
        }
        private void vertex(Vec3 p,Vec3 normal,float alpha) {
            out.vertex(pose.pose(),(float)(p.x-camera.x),(float)(p.y-camera.y),(float)(p.z-camera.z)).color(.84f,1,.93f,alpha)
                    .normal(pose.normal(),(float)normal.x,(float)normal.y,(float)normal.z).endVertex();
        }
        void corners(AABB box,float alpha) {
            for(int i=0;i<8;i++) {
                boolean x=(i&1)==0,y=(i&2)==0,z=(i&4)==0;
                var p=new Vec3(x?box.minX:box.maxX,y?box.minY:box.maxY,z?box.minZ:box.maxZ);
                line(p,p.add((x?1:-1)*box.getXsize()*.28,0,0),alpha);
                line(p,p.add(0,(y?1:-1)*box.getYsize()*.23,0),alpha);
                line(p,p.add(0,0,(z?1:-1)*box.getZsize()*.28),alpha);
            }
        }
        void face(AABB box,BlockHitResult hit,float alpha) {
            var axis=hit.getDirection().getAxis();
            double plane=axis.choose(hit.getLocation().x,hit.getLocation().y,hit.getLocation().z)+hit.getDirection().getAxisDirection().getStep()*.008;
            double u0=axis==Direction.Axis.X?box.minY:box.minX,u1=axis==Direction.Axis.X?box.maxY:box.maxX;
            double v0=axis==Direction.Axis.Z?box.minY:box.minZ,v1=axis==Direction.Axis.Z?box.maxY:box.maxZ;
            for(int i=0;i<4;i++) {
                boolean lowU=(i&1)==0,lowV=(i&2)==0;
                double u=lowU?u0:u1,v=lowV?v0:v1;
                var point=onPlane(axis,plane,u,v);
                line(point,onPlane(axis,plane,u+(lowU?1:-1)*(u1-u0)*.24,v),alpha);
                line(point,onPlane(axis,plane,u,v+(lowV?1:-1)*(v1-v0)*.24),alpha);
            }
        }
        void cross(Vec3 p,Direction.Axis axis,double r,float alpha,boolean plus) {
            Vec3 u=axis==Direction.Axis.X?new Vec3(0,r,0):new Vec3(r,0,0);
            Vec3 v=axis==Direction.Axis.Z?new Vec3(0,r,0):new Vec3(0,0,r);
            line(p.subtract(u),p.add(u),alpha);
            if(plus)line(p.subtract(v),p.add(v),alpha);
        }
        void arc(Vec3 center,double radius,double fraction,float alpha,int segments) {
            arc(center,radius,0,fraction,alpha,segments);
        }
        void arc(Vec3 center,double radius,double from,double fraction,float alpha,int segments) {
            if(fraction<=0)return;
            Vec3 previous=point(center,radius,from);
            for(int i=1;i<=segments;i++) {
                Vec3 next=point(center,radius,from+fraction*i/segments);
                line(previous,next,alpha);previous=next;
            }
        }
        private static Vec3 point(Vec3 center,double radius,double turn) {
            double angle=turn*2*Math.PI;
            return center.add(Math.cos(angle)*radius,0,Math.sin(angle)*radius);
        }
        private static Vec3 onPlane(Direction.Axis axis,double plane,double u,double v) {
            return switch(axis) {
                case X -> new Vec3(plane,u,v);
                case Y -> new Vec3(u,plane,v);
                case Z -> new Vec3(u,v,plane);
            };
        }
    }
}
