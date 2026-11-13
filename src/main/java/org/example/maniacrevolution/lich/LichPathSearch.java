package org.example.maniacrevolution.lich;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.PathNavigationRegion;
import net.minecraft.world.level.pathfinder.*;

/** Resumable A*: path cost is not capped by straight-line follow range. */
final class LichPathSearch {
    private static final int RADIUS=64,MAX_VISITED=16384;
    private final LichUnit unit;
    private final BlockPos origin,target;
    private final Node startingNode;
    private final LichNavigation.Transition excluded;
    private final Map<BlockPos,Vertex> vertices=new HashMap<>();
    private final PriorityQueue<Entry> open=new PriorityQueue<>(Comparator.comparingDouble(Entry::score).thenComparingLong(Entry::sequence));
    private final WalkNodeEvaluator evaluator=new WalkNodeEvaluator();
    private final PathNavigationRegion region;
    private final int minY,maxY;
    private long sequence;
    private int visited,polls;
    private Vertex closest;
    private boolean done;
    private Path result;
    private static final class Vertex {
        final Node node;
        double cost=Double.POSITIVE_INFINITY;
        Vertex parent;
        boolean closed;
        Vertex(Node node) {this.node=node;}
    }
    private record Entry(Vertex vertex,double cost,double score,long sequence) {}
    LichPathSearch(LichUnit unit,BlockPos target) {
        this(unit,target,null,null);
    }
    LichPathSearch(LichUnit unit,BlockPos target,Node start,LichNavigation.Transition excluded) {
        this.unit=unit;this.origin=start==null?unit.blockPosition():start.asBlockPos();this.target=target.immutable();
        this.startingNode=start;this.excluded=excluded;
        minY=Math.max(unit.level().getMinBuildHeight(),Math.min(origin.getY(),target.getY())-8);
        maxY=Math.min(unit.level().getMaxBuildHeight()-1,Math.max(origin.getY(),target.getY())+8);
        region=new PathNavigationRegion(unit.level(),new BlockPos(origin.getX()-RADIUS-2,minY-2,origin.getZ()-RADIUS-2),
                new BlockPos(origin.getX()+RADIUS+2,maxY+3,origin.getZ()+RADIUS+2));
        var nativeRules=unit.getNavigation().getNodeEvaluator();
        evaluator.setCanOpenDoors(nativeRules.canOpenDoors());evaluator.setCanPassDoors(nativeRules.canPassDoors());
        evaluator.setCanFloat(nativeRules.canFloat());evaluator.setCanWalkOverFences(nativeRules.canWalkOverFences());
    }
    boolean done() {return done;}
    Path result() {return result;}
    int polls() {return polls;}
    private double heuristic(Node node) {
        double x=node.x-target.getX(),y=node.y-target.getY(),z=node.z-target.getZ();
        return Math.sqrt(x*x+y*y+z*z);
    }
    private boolean allowed(Node node) {
        // Leave a margin for the native evaluator's collision probes at the loaded boundary.
        return Math.abs(node.x-origin.getX())<=RADIUS&&Math.abs(node.z-origin.getZ())<=RADIUS
                &&node.y>=minY&&node.y<=maxY&&unit.level().hasChunksAt(node.x-2,minY,node.z-2,node.x+2,maxY,node.z+2);
    }
    void step(int budget) {
        if(done)return;
        evaluator.prepare(region,unit);
        try {
            if(vertices.isEmpty()) {
                var node=startingNode==null?evaluator.getStart():startingNode;
                if(node==null) {done=true;return;}
                var start=new Vertex(node);start.cost=0;closest=start;vertices.put(node.asBlockPos(),start);
                open.add(new Entry(start,0,heuristic(node),sequence++));
            }
            var neighbors=new Node[32];
            for(int expanded=0;expanded<budget&&!open.isEmpty();expanded++) {
                var entry=open.poll();polls++;var current=entry.vertex;
                if(current.closed||entry.cost!=current.cost)continue;
                current.closed=true;visited++;
                if(current.node.asBlockPos().equals(target)) {finish(current,true);return;}
                if(heuristic(current.node)<heuristic(closest.node))closest=current;
                if(visited>=MAX_VISITED) {finishPartial();return;}
                if(!allowed(current.node))continue;
                int count=evaluator.getNeighbors(neighbors,current.node);
                for(int i=0;i<count;i++) {
                    var node=neighbors[i];if(node.costMalus<0||!allowed(node))continue;
                    if(excluded!=null&&excluded.matches(current.node,node))continue;
                    var next=vertices.computeIfAbsent(node.asBlockPos(),key->new Vertex(node));
                    if(next.closed)continue;
                    double dx=node.x-current.node.x,dy=node.y-current.node.y,dz=node.z-current.node.z;
                    double cost=current.cost+Math.sqrt(dx*dx+dy*dy+dz*dz)+Math.max(0,node.costMalus);
                    if(cost>=next.cost)continue;
                    if(!LichGroundNavigation.fitsElevation(unit,current.node,node))continue;
                    next.cost=cost;next.parent=current;
                    open.add(new Entry(next,cost,cost+heuristic(node),sequence++));
                }
            }
            if(open.isEmpty())finishPartial();
        } finally {evaluator.done();}
    }
    private void finishPartial() {
        // Inside this region, a failed full search must not send the mob to the nearest wall again.
        boolean distant=Math.abs(target.getX()-origin.getX())>RADIUS||Math.abs(target.getZ()-origin.getZ())>RADIUS;
        if(distant&&closest!=null&&closest.parent!=null&&closest.node.distanceTo(new Node(origin.getX(),origin.getY(),origin.getZ()))>8)
            finish(closest,false);
        else done=true;
    }
    private void finish(Vertex end,boolean reached) {
        var nodes=new ArrayList<Node>();for(var cursor=end;cursor!=null;cursor=cursor.parent)nodes.add(cursor.node);
        Collections.reverse(nodes);result=new Path(nodes,target,reached);done=true;
    }
}
