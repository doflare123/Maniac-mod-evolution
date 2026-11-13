package org.example.maniacrevolution.lich;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

final class LichGroundNavigation extends GroundPathNavigation {
    private final LichUnit unit;

    LichGroundNavigation(LichUnit unit,Level level) {
        super(unit,level);
        this.unit=unit;
    }

    @Override protected void followThePath() {
        if(unit.kind()!=LichRules.Kind.BUTCHER||unit.isInWaterOrBubble()) {
            super.followThePath();
            return;
        }
        // Use the same center and support height for steering and advancing a node.
        Vec3 point=waypoint(level,path.getNextNodePos());
        if(Math.abs(unit.getY()-point.y)<.2&&unit.position().subtract(point).horizontalDistanceSqr()<=.22*.22)
            path.advance();
    }

    static Vec3 waypoint(Level level,BlockPos node) {
        Vec3 center=Vec3.atBottomCenterOf(node);
        return level.getBlockState(node.below()).isAir()?center:
                new Vec3(center.x,WalkNodeEvaluator.getFloorLevel(level,node),center.z);
    }

    static BlockPos targetNode(Level level,Vec3 feet) {
        BlockPos pos=BlockPos.containing(feet.add(0,.05,0));
        if(!level.hasChunkAt(pos))return pos;
        var block=level.getBlockState(pos).getBlock();
        if((block instanceof SlabBlock||block instanceof StairBlock)
                &&feet.y>pos.getY()+.05&&waypoint(level,pos.above()).y>=feet.y-.05)return pos.above();
        return pos;
    }

    static boolean fitsElevation(LichUnit unit,Node from,Node to) {
        if(unit.kind()!=LichRules.Kind.BUTCHER||from.type!=BlockPathTypes.WALKABLE||to.type!=BlockPathTypes.WALKABLE)return true;
        Level level=unit.level();
        Vec3 start=waypoint(level,from.asBlockPos()),end=waypoint(level,to.asBlockPos());
        if(Math.abs(start.y-end.y)<.01)return true;
        double low=Math.min(start.y,end.y),high=Math.max(start.y,end.y);
        double half=unit.getBbWidth()/2.;
        // Probe the body over the actual top faces of stairs/slabs along the transition.
        int steps=(int)Math.ceil(start.subtract(end).horizontalDistance()*8);
        double previous=start.y;
        for(int i=0;i<=steps;i++) {
            Vec3 point=start.lerp(end,(double)i/steps);
            var footprint=new AABB(point.x-half,low-.05,point.z-half,point.x+half,high+.001,point.z+half);
            double floor=low;
            for(var shape:level.getBlockCollisions(unit,footprint)) {
                for(var box:shape.toAabbs()) {
                    if(box.maxY<=high+.001&&box.maxX>footprint.minX&&box.minX<footprint.maxX
                            &&box.maxZ>footprint.minZ&&box.minZ<footprint.maxZ)floor=Math.max(floor,box.maxY);
                }
            }
            if(!clearBody(unit,point,floor))return false;
            if(floor-previous>unit.getStepHeight()+.001) {
                // A full-height ledge needs jump clearance above the takeoff footprint.
                if(i==0||floor-previous>1.125||!clearBody(unit,start.lerp(end,(double)(i-1)/steps),floor))return false;
            } else if(previous>floor&&!clearBody(unit,point,previous))return false;
            previous=floor;
        }
        return true;
    }

    private static boolean clearBody(LichUnit unit,Vec3 point,double y) {
        double half=unit.getBbWidth()/2.;
        var body=new AABB(point.x-half,y+.001,point.z-half,point.x+half,y+unit.getBbHeight(),point.z+half);
        return !unit.level().getBlockCollisions(unit,body).iterator().hasNext();
    }
}
