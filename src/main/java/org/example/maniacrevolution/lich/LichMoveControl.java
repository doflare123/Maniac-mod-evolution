package org.example.maniacrevolution.lich;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.control.MoveControl;

/** Preserve vanilla node following/jumps, but give the ghoul a slow-turning running cycle. */
final class LichMoveControl extends MoveControl {
    private final LichUnit unit;
    private final LichGhoulRun run=new LichGhoulRun();
    private float turnLimit;
    LichMoveControl(LichUnit unit) {super(unit);this.unit=unit;}
    @Override public void tick() {
        double original=speedModifier;
        speedModifier*=unit.aggroSpeed();
        try {movementTick();} finally {speedModifier=original;}
    }
    private void movementTick() {
        if(unit.kind()!=LichRules.Kind.GHOUL) {run.reset();super.tick();return;}
        boolean walking=operation==Operation.MOVE_TO||operation==Operation.JUMPING;
        boolean advancing=walking&&unit.emerging()==0&&!unit.isInWaterOrBubble()&&!unit.isInLava()
                &&!unit.horizontalCollision&&unit.getDeltaMovement().horizontalDistanceSqr()>.0001;
        turnLimit=run.turnLimit();
        double turn=0;
        if(operation==Operation.MOVE_TO) {
            float desired=(float)(Mth.atan2(wantedZ-unit.getZ(),wantedX-unit.getX())*180/Math.PI)-90;
            float error=Mth.wrapDegrees(desired-unit.getYRot());
            turn=Mth.clamp(error,-turnLimit,turnLimit);
            // At a sharp corner, turn before accelerating into a wall with the old heading.
            speedModifier*=Math.abs(error)>60?0:Math.abs(error)>30?Math.cos(Math.toRadians(error)):1;
        }
        speedModifier*=run.step(advancing,unit.onGround(),turn);
        super.tick();
    }
    @Override protected float rotlerp(float current,float desired,float maximum) {
        return super.rotlerp(current,desired,unit.kind()==LichRules.Kind.GHOUL?turnLimit:maximum);
    }
}
