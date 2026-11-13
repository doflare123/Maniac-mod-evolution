package org.example.maniacrevolution.lich;

import java.util.UUID;
import net.minecraft.world.phys.Vec3;

/** Stores the last direct observation, independently of close-range tracking. */
final class LichPursuit {
    private UUID target;
    private Vec3 position,direction=Vec3.ZERO;
    private long observedAt,lostAt=-1,searchUntil;
    UUID target() {return target;}
    Vec3 position() {return position;}
    Vec3 direction() {return direction;}
    long searchUntil() {return searchUntil;}
    boolean remembers(UUID id,Vec3 point,long now) {
        return id.equals(target)&&position!=null&&now-observedAt<40&&position.distanceToSqr(point)<=9;
    }
    void clear() {target=null;position=null;direction=Vec3.ZERO;lostAt=-1;searchUntil=0;}
    void observe(UUID id,Vec3 point,Vec3 velocity,long now) {
        Vec3 movement=id.equals(target)&&position!=null&&now>observedAt?point.subtract(position):velocity;
        // Do not interpret teleportation or vertical-only movement as a running direction.
        direction=movement.horizontalDistanceSqr()>.0025&&movement.horizontalDistanceSqr()<64
                ?movement.multiply(1,0,1).normalize():Vec3.ZERO;
        target=id;position=point;observedAt=now;lostAt=-1;searchUntil=0;
    }
    void lose(long now) {if(lostAt<0)lostAt=now;}
    boolean approaching() {return searchUntil==0;}
    void startSearch(long now) {if(searchUntil==0)searchUntil=now+100;}
    boolean expired(long now) {return searchUntil>0?now>=searchUntil:lostAt>=0&&now-lostAt>=400;}
    Vec3 candidate(int index) {
        if(direction.lengthSqr()<.01) {
            double angle=index*Math.PI/2;
            return position.add(Math.cos(angle)*2,0,Math.sin(angle)*2);
        }
        if(index<2)return position.add(direction.scale(index==0?3:5));
        var side=new Vec3(-direction.z,0,direction.x).scale(index==2?2:-2);
        return position.add(direction.scale(3)).add(side);
    }
}
