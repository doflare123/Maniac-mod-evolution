package org.example.maniacrevolution.lich;

import java.util.ArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Bounded route planning; neither failed plans nor partial paths count as arrival. */
final class LichNavigation {
    private final LichUnit unit;
    private Vec3 goal,plannedGoal,sample,directGoal;
    private int nextPlan,sampledAt,failures;
    private LichPathSearch search;
    private Path route;
    private Path prefetched,prefetchedFrom;
    private Vec3 prefetchGoal;
    private boolean prefetching;
    boolean prefetchEnabled=true;
    private int plansStarted,plansCompleted,searchPolls;
    private Transition stalled,excluded;
    private int stalledCount,excludeUntil,progressAt;
    private double bestDistance;
    private Transition activeTransition;
    record Transition(BlockPos from,BlockPos to) {
        boolean matches(Node start,Node end) {return from.equals(start.asBlockPos())&&to.equals(end.asBlockPos());}
    }
    LichNavigation(LichUnit unit) {this.unit=unit;}
    void stop() {
        unit.getNavigation().stop();goal=plannedGoal=sample=directGoal=null;route=null;
        nextPlan=failures=0;search=null;LichPathScheduler.cancel(this);clearPrefetch();
        stalled=excluded=activeTransition=null;stalledCount=0;
        unit.getMoveControl().setWantedPosition(unit.getX(),unit.getY(),unit.getZ(),0);
        unit.setZza(0);
    }
    boolean failed() {return search==null&&directGoal==null&&failures>0&&unit.getNavigation().isDone();}
    boolean urgent() {return unit.combatTarget!=null;}
    boolean speculative() {return prefetching;}
    boolean pending() {return search!=null&&unit.isAlive()&&!unit.isRemoved();}
    int plansStarted() {return plansStarted;}
    int plansCompleted() {return plansCompleted;}
    int searchPolls() {return searchPolls;}
    Vec3 goal() {return goal;}
    int advanceSearch(int budget) {
        if(!pending()) {search=null;return 0;}
        int before=search.polls();search.step(budget);int used=search.polls()-before;searchPolls+=used;
        if(search.done()) {
            var path=search.result();search=null;plansCompleted++;accept(path,unit.tickCount);
        }
        return used;
    }
    void tick() {
        if(unit.kind()==LichRules.Kind.BUTCHER&&directGoal==null)watchTransition();
        if(directGoal==null) {
            continueRoute();
            return;
        }
        if(!corridor(unit.position(),nextStep(directGoal))) {
            unit.getMoveControl().setWantedPosition(unit.getX(),unit.getY(),unit.getZ(),0);unit.setZza(0);
            directGoal=null;resume(route);nextPlan=0;
            if(goal!=null)move(goal);
            return;
        }
        // MoveControl consumes MOVE_TO each AI tick; refresh independently of five-tick decisions.
        unit.getMoveControl().setWantedPosition(directGoal.x,directGoal.y,directGoal.z,1);
    }
    private void continueRoute() {
        var navigation=unit.getNavigation();
        if(navigation.isDone()) {
            if(prefetchedFrom!=null&&!prefetchedFrom.isDone()) {
                if(prefetching) {search=null;LichPathScheduler.cancel(this);}
                clearPrefetch();
                return;
            }
            if(prefetching&&search!=null) {
                LichPathScheduler.cancel(this);
                prefetching=false;plannedGoal=prefetchGoal;
                LichPathScheduler.submit(this);
            } else if(prefetched!=null) {
                Path path=prefetched;
                prefetched=null;
                accept(path,unit.tickCount);
            }
            return;
        }
        if(!prefetchEnabled||route==null||route.canReach()||prefetchedFrom==route||search!=null
                ||route!=navigation.getPath()||route.getNodeCount()-route.getNextNodeIndex()>10)return;
        var end=route.getEndNode();
        if(end.type!=BlockPathTypes.WALKABLE||goal==null)return;
        var target=LichGroundNavigation.targetNode(unit.level(),goal);
        if(end.asBlockPos().distSqr(target)<64)return;
        prefetchedFrom=route;prefetchGoal=goal;prefetching=true;
        search=new LichPathSearch(unit,target,end,unit.tickCount<excludeUntil?excluded:null);
        plansStarted++;LichPathScheduler.submit(this);
    }
    private void clearPrefetch() {
        prefetched=null;prefetchedFrom=null;prefetchGoal=null;prefetching=false;
    }
    private void watchTransition() {
        var path=unit.getNavigation().getPath();
        if(path==null||path.isDone()||path.getNextNodeIndex()==0) {activeTransition=null;return;}
        var edge=new Transition(path.getNodePos(path.getNextNodeIndex()-1),path.getNextNodePos());
        var point=LichGroundNavigation.waypoint(unit.level(),edge.to());
        double distance=unit.position().distanceTo(point);
        int now=unit.tickCount;
        if(!edge.equals(activeTransition)||distance<bestDistance-.1) {
            activeTransition=edge;bestDistance=distance;progressAt=now;return;
        }
        if(now-progressAt<35)return;
        stalledCount=edge.equals(stalled)?stalledCount+1:1;stalled=edge;
        if(stalledCount>=2) {excluded=edge;excludeUntil=now+100;}
        unit.getNavigation().stop();route=null;activeTransition=null;
        search=null;LichPathScheduler.cancel(this);clearPrefetch();
        nextPlan=now+10;
    }
    static boolean arrived(Vec3 actual,Vec3 target,double horizontalSquared) {
        return actual.subtract(target).horizontalDistanceSqr()<=horizontalSquared&&Math.abs(actual.y-target.y)<=.75;
    }
    void move(Vec3 requested) {
        if(requested==null)return;
        int now=unit.tickCount;
        goal=requested;
        if(prefetchGoal!=null&&prefetchGoal.distanceToSqr(requested)>4) {
            if(prefetching) {search=null;LichPathScheduler.cancel(this);}
            clearPrefetch();
        }
        var navigation=unit.getNavigation();
        if(corridor(unit.position(),requested)&&corridor(unit.position(),nextStep(requested))) {
            if(directGoal==null) {route=navigation.getPath();navigation.stop();}
            directGoal=requested;failures=0;
            // An unobstructed, supported corridor needs no graph search.
            search=null;LichPathScheduler.cancel(this);clearPrefetch();
            // Do not re-enter tick(): different probe lengths can disagree at a block edge.
            unit.getMoveControl().setWantedPosition(requested.x,requested.y,requested.z,1);return;
        }
        if(directGoal!=null&&arrived(unit.position(),directGoal,.25)) {
            directGoal=null;resume(route);
        }
        boolean stuck=false;
        if(sample!=null&&now-sampledAt>=40) {
            stuck=(directGoal!=null||!navigation.isDone())&&unit.position().distanceToSqr(sample)<.04;
            sample=unit.position();sampledAt=now;
        }
        if(stuck) {
            navigation.stop();directGoal=null;route=null;nextPlan=0;
            if(prefetching) {search=null;LichPathScheduler.cancel(this);}
            clearPrefetch();
        }
        // Coalesce moving-target updates. Never discard an unfinished search for a new position.
        if(search!=null)return;
        boolean changed=plannedGoal==null||plannedGoal.distanceToSqr(requested)>4||Math.abs(plannedGoal.y-requested.y)>.5;
        if(!stuck&&!changed&&(directGoal!=null||!navigation.isDone()))return;
        if(now<nextPlan)return;
        plannedGoal=requested;nextPlan=now+10;plansStarted++;
        if(sample==null) {sample=unit.position();sampledAt=now;}
        search=new LichPathSearch(unit,LichGroundNavigation.targetNode(unit.level(),requested),null,now<excludeUntil?excluded:null);
        LichPathScheduler.submit(this);
    }
    private Vec3 nextStep(Vec3 target) {
        var delta=target.subtract(unit.position());
        return unit.position().add(delta.length()>1?delta.normalize():delta);
    }
    private void accept(Path path,int now) {
        if(prefetching) {prefetching=false;prefetched=path;return;}
        if(directGoal!=null&&goal!=null&&corridor(unit.position(),goal))return;
        if(resume(path)) {
            directGoal=null;route=path;
            nextPlan=now+10;failures=0;sample=unit.position();sampledAt=now;
        } else {failures++;nextPlan=now+Math.min(60,10*failures);}
    }
    private boolean resume(Path path) {
        if(!useful(path))return false;
        // Planning started at an older position. Join a nearby safe node, never walk back to its start.
        int join=-1;double distance=4;
        for(int i=0;i<path.getNodeCount();i++) {
            var point=LichGroundNavigation.waypoint(unit.level(),path.getNodePos(i));double d=unit.position().distanceToSqr(point);
            if(d<=distance&&(d<.3||corridor(unit.position(),point))) {distance=d;join=i;}
        }
        if(join<0)return false;
        path.setNextNodeIndex(join);
        return unit.getNavigation().moveTo(path,1);
    }
    /** Conservative flat shortcut: swept body clearance, ground support and vanilla hazard rules. */
    boolean corridor(Vec3 start,Vec3 end) {
        if(Math.abs(start.y-end.y)>.15||start.distanceToSqr(end)>24*24||unit.isInWaterOrBubble())return false;
        double distance=start.distanceTo(end);
        int steps=Math.max(1,(int)Math.ceil(distance*4));
        double half=unit.getBbWidth()/2.;
        var level=unit.level();
        BlockPos lastFeet=null;
        // Reuse collision shapes over a short group of samples. Each call sees the current world and entities.
        for(int first=0;first<=steps;first+=16) {
            int last=Math.min(steps,first+15);
            Vec3 a=sample(start,end,distance,first),b=sample(start,end,distance,last);
            AABB bounds=new AABB(Math.min(a.x,b.x)-half,Math.min(a.y,b.y)-.1,Math.min(a.z,b.z)-half,
                    Math.max(a.x,b.x)+half,Math.max(a.y,b.y)+unit.getBbHeight(),Math.max(a.z,b.z)+half);
            var min=BlockPos.containing(Math.min(a.x,b.x),Math.min(a.y,b.y)+.05,Math.min(a.z,b.z)).offset(-1,-1,-1);
            var max=BlockPos.containing(Math.max(a.x,b.x),Math.max(a.y,b.y)+.05,Math.max(a.z,b.z)).offset(1,3,1);
            if(!level.hasChunksAt(min,max))return false;
            var blocks=new ArrayList<AABB>();
            for(var shape:level.getBlockCollisions(unit,bounds))blocks.addAll(shape.toAabbs());
            var entities=new ArrayList<AABB>();
            for(var shape:level.getEntityCollisions(unit,bounds))entities.addAll(shape.toAabbs());
            var border=level.getWorldBorder();
            for(int i=first;i<=last;i++) {
                Vec3 point=sample(start,end,distance,i);
                var body=new AABB(point.x-half,point.y+.01,point.z-half,point.x+half,point.y+unit.getBbHeight(),point.z+half);
                var support=new AABB(body.minX,point.y-.1,body.minZ,body.maxX,point.y,body.maxZ);
                if(overlaps(blocks,body)||overlaps(entities,body.inflate(1e-7))||!overlaps(blocks,support))return false;
                if(border.isInsideCloseToBorder(unit,body)&&!level.noCollision(unit,body))return false;
                var feet=BlockPos.containing(point.add(0,.05,0));
                if(!feet.equals(lastFeet)&&WalkNodeEvaluator.getBlockPathTypeStatic(level,feet.mutable())!=BlockPathTypes.WALKABLE)return false;
                lastFeet=feet;
            }
        }
        return true;
    }
    private static Vec3 sample(Vec3 start,Vec3 end,double distance,int i) {
        return start.lerp(end,distance<1e-8?0:Math.min(1,i*.25/distance));
    }
    private static boolean overlaps(ArrayList<AABB> boxes,AABB area) {
        for(var box:boxes)if(box.intersects(area))return true;
        return false;
    }
    private Path plan(Vec3 point) {
        // Explicit XYZ, zero target tolerance: a node on the floor below is not the destination.
        return unit.getNavigation().createPath(LichGroundNavigation.targetNode(unit.level(),point),0,32);
    }
    Vec3 reachableGround(Vec3 desired) {
        var point=ground(desired);if(point==null)return null;
        var path=plan(point);
        return path!=null&&path.canReach()?point:null;
    }
    private boolean useful(Path path) {
        return path!=null&&path.getEndNode()!=null&&path.getNodeCount()>1
                &&unit.position().distanceToSqr(LichGroundNavigation.waypoint(unit.level(),path.getEndNode().asBlockPos()))>.25;
    }
    /** Project an imprecise patrol/search hint to nearby supported feet, including steps. */
    Vec3 ground(Vec3 desired) {
        var origin=BlockPos.containing(desired);
        for(int height=0;height<=4;height++)for(int direction:new int[]{1,-1}) {
            if(height==0&&direction==-1)continue;
            for(int radius=0;radius<=1;radius++)for(int x=-radius;x<=radius;x++)for(int z=-radius;z<=radius;z++) {
                if(Math.max(Math.abs(x),Math.abs(z))!=radius)continue;
                var feet=origin.offset(x,height*direction,z);
                if(!unit.level().hasChunkAt(feet))continue;
                var surface=unit.level().getBlockState(feet.below()).getBlock();
                if(!(surface instanceof SlabBlock)&&!(surface instanceof StairBlock)
                        &&!unit.getNavigation().isStableDestination(feet))continue;
                Vec3 point=LichGroundNavigation.waypoint(unit.level(),feet);
                if(unit.level().noCollision(unit,AABB.ofSize(point.add(0,unit.getBbHeight()/2,0),unit.getBbWidth(),unit.getBbHeight(),unit.getBbWidth())))return point;
            }
        }
        return null;
    }
}
