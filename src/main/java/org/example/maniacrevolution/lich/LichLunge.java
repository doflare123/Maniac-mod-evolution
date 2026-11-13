package org.example.maniacrevolution.lich;

import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.phys.Vec3;

final class LichLunge {
    private static final int COOLDOWN=400; // Quarter ticks: 100 ordinary or 80 sprinting ticks.
    private static final int DURATION=5;
    private static final double SPEED=.38;
    private UUID target;
    private Vec3 step;
    private int remaining,cooldown;

    boolean active() {return remaining>0;}
    int cooldown() {return cooldown;}
    void cancel() {remaining=0;target=null;}
    void recover(boolean sprintingNearby) {cooldown=Math.max(0,cooldown-(sprintingNearby?5:4));}

    boolean begin(LichUnit unit,ServerPlayer player,long now) {
        if(unit.kind()!=LichRules.Kind.ZOMBIE&&unit.kind()!=LichRules.Kind.GHOUL)return false;
        double distance=unit.distanceToSqr(player);
        if(cooldown>0||now<unit.nextAttack||distance<=4||distance>9||!canAdvance(unit,player))return false;
        Vec3 direction=player.position().subtract(unit.position()).multiply(1,0,1).normalize().scale(SPEED);
        if(!unit.routing.corridor(unit.position(),unit.position().add(direction)))return false;
        step=direction;
        target=player.getUUID();
        remaining=DURATION;cooldown=COOLDOWN;
        unit.routing.stop();
        return true;
    }

    boolean tick(LichUnit unit,ServerPlayer player,long now) {
        if(!active())return false;
        if(player==null||!target.equals(player.getUUID())||!canAdvance(unit,player)) {cancel();return false;}
        Vec3 start=unit.position();
        if(!unit.routing.corridor(start,start.add(step))) {cancel();return false;}
        unit.move(MoverType.SELF,step);
        if(unit.tryMelee(player,now)||unit.position().distanceToSqr(start)<.01||--remaining==0)cancel();
        // Skip the normal attack decision on the last tick of the attempt.
        return true;
    }

    private static boolean canAdvance(LichUnit unit,ServerPlayer player) {
        return unit.onGround()&&!unit.isInWaterOrBubble()&&Math.abs(unit.getY()-player.getY())<=.75
                &&unit.trackedVisible(player);
    }
}
