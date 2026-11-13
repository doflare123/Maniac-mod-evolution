package org.example.maniacrevolution.lich;

import java.util.*;

/** Client-side pose clock only; never changes AI, damage or server timings. */
public final class LichUnitVisuals {
    public record Frame(String clip,double seconds,boolean loop,boolean restart) {}
    private static final Map<LichUnit,Clock> CLOCKS=new WeakHashMap<>();
    private static final class Clock {
        String action="",previous="";double started,phase,lastAge;int swing=-1,hurt;boolean swinging;long bowStart=-1;
    }
    public static Frame frame(LichUnit unit,double age) {
        var clock=CLOCKS.computeIfAbsent(unit,u->new Clock());
        boolean restart=false;String clip;double seconds;
        if(unit.isDeadOrDying()) {clip="death";seconds=Math.max(0,unit.deathTime+(age-unit.tickCount))/20.0*deathLength(unit);}
        else if(unit.emerging()>0) {clip="emerge";seconds=Math.max(0,100-unit.emerging()+age-unit.tickCount)/20.0;clock.action="";}
        else if(unit.kind()==LichRules.Kind.SKELETON&&unit.bowStart()>=0
                &&unit.level().getGameTime()+age-unit.tickCount-unit.bowStart()<23) {
            clip="shoot";seconds=Math.max(0,unit.level().getGameTime()+age-unit.tickCount-unit.bowStart())/20.0;
            restart=clock.bowStart!=unit.bowStart();clock.bowStart=unit.bowStart();clock.action="";
        }
        else {
            if(unit.hurtTime>clock.hurt) {clock.action="hurt";clock.started=age;restart=true;}
            else if(unit.swinging&&(!clock.swinging||unit.swingTime<clock.swing)) {
                clock.action=unit.kind()==LichRules.Kind.SKELETON?"shoot":"attack";clock.started=age;restart=true;
            }
            if(!clock.action.isEmpty()&&age-clock.started>=20*duration(unit,clock.action))clock.action="";
            if(!clock.action.isEmpty()) {clip=clock.action;seconds=Math.max(0,age-clock.started)/20.0;}
            else {
                boolean moving=unit.walkAnimation.speed()>.03F;
                clip=moving?(unit.kind()==LichRules.Kind.GHOUL?"run":"move"):"idle";
                if(moving)clock.phase+=Math.max(0,Math.min(2,age-clock.lastAge))*Math.min(2,unit.walkAnimation.speed()/.15F);
                seconds=(moving?clock.phase:age)/20.0;
            }
        }
        clock.hurt=unit.hurtTime;clock.swing=unit.swingTime;clock.swinging=unit.swinging;clock.lastAge=age;
        restart|=!clip.equals(clock.previous);clock.previous=clip;
        return new Frame(clip,seconds,clip.equals("idle")||clip.equals("move")||clip.equals("run"),restart);
    }
    private static double deathLength(LichUnit unit) {return unit.kind()==LichRules.Kind.BUTCHER?1.65:1.25;}
    private static double duration(LichUnit unit,String clip) {
        if(clip.equals("hurt"))return .35;
        return switch(unit.kind()){case ZOMBIE->.85;case SKELETON->1.15;case GHOUL->.65;case BUTCHER->1.4;};
    }
    private LichUnitVisuals() {}
}
