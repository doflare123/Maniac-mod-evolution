package org.example.maniacrevolution.strange;

import dev.kosmx.playerAnim.api.TransformType;
import dev.kosmx.playerAnim.api.layered.KeyframeAnimationPlayer;
import dev.kosmx.playerAnim.core.data.gson.AnimationSerializing;
import dev.kosmx.playerAnim.core.util.Vec3f;
import org.example.maniacrevolution.skill.ToggleSkill;
import java.nio.file.*;
import java.util.List;

public final class StrangeTest {
    public static void main(String[] args) throws Exception {
        var skill = new ToggleSkill<>("test", List.of("A", "B", "C"));
        check(skill.next(skill.next(skill.next(skill.initial()))).equals("A"), "Three-state cycle");
        try { skill.next("unknown"); throw new AssertionError("Unknown stance accepted"); } catch (IllegalArgumentException expected) {}
        try { new ToggleSkill<>("bad", List.of("A", "A")); throw new AssertionError("Duplicate stances accepted"); } catch (IllegalArgumentException expected) {}
        for (float[] sample : new float[][]{{100,30},{60,30},{40,20},{1,.5f},{0,0}})
            check(StrangeRules.portalCost(sample[0]) == sample[1], "Dynamic portal price " + sample[0]);
        check(StrangeRules.shieldCost(7.5f) == 15, "Shield price");
        check(StrangeRules.WHIP_MANA > 0, "Whip must spend mana");
        check(WhipMotion.raise(0)==0 && WhipMotion.raise(10)==1 && WhipMotion.raise(13)==0, "First-person overhead wind-up then strike");
        check(WhipMotion.thrust(10)==0 && WhipMotion.thrust(13)==1 && WhipMotion.thrust(24)==0, "First-person impact matches server hit and recovers");
        for (int i=-2;i<=26;i++) {
            check(Float.isFinite(WhipMotion.raise(i)) && WhipMotion.raise(i)>=0 && WhipMotion.raise(i)<=1, "Bounded arm raise");
            check(Float.isFinite(WhipMotion.thrust(i)) && WhipMotion.thrust(i)>=0 && WhipMotion.thrust(i)<=1, "Bounded arm thrust");
        }
        check(StrangeRules.collapseDamage(20) == 5 && StrangeRules.collapseDamage(40) == 10, "Closing portal removes 25% of maximum health");
        check(StrangeRules.COLLAPSE_TICKS == 20, "One-second closing warning");
        check(StrangeRules.PORTAL_TICKS == 15 * 20, "15-second portal");
        check(StrangeRules.OPEN_CAST_TICKS==5*20 && StrangeRules.CLOSE_CAST_TICKS==30, "Five-second open, 1.5-second manual close");
        check(StrangeRules.openingScale(0)==0 && StrangeRules.openingScale(100)==1, "Portal appears over entire cast");
        check(StrangeRules.closingScale(0,30)==1 && StrangeRules.closingScale(29,30)>0
                && StrangeRules.closingScale(30,30)==0, "Premature collapse completes at 1.5 seconds");
        for(int tick=1;tick<=100;tick++) check(StrangeRules.openingScale(tick)>=StrangeRules.openingScale(tick-1), "No reversed opening frame");
        check(Math.abs(StrangeRules.gestureAngle(100,false)-(float)(6*Math.PI))<.0001
                && Math.abs(StrangeRules.gestureAngle(30,true)+(float)(2*Math.PI))<.0001, "Opening circles and closing reverse direction");
        check(StrangeRules.entersPortal(0,0,2,0,0,-2), "Fast crossing");
        check(StrangeRules.entersPortal(0,0,-2,0,0,2), "Reverse crossing");
        check(!StrangeRules.entersPortal(2,0,2,2,0,-2), "Crossing outside ring width");
        check(!StrangeRules.entersPortal(0,3,2,0,3,-2), "Crossing above ring");
        check(!StrangeRules.entersPortal(0,0,2,0,0,1), "No plane crossing");
        for (String clip : List.of("defense", "whip")) {
            try (var stream = Files.newInputStream(Path.of("src/main/resources/assets/maniacrev/player_animation/strange_" + clip + ".json"))) {
                var animations = AnimationSerializing.deserializeAnimation(stream);
                check(animations.size() == 1, "One player clip");
                var data = animations.get(0);
                check(data.extraData.get("name").equals("animation.strange_player." + clip), "Registry name");
                check(data.isInfinite == clip.equals("defense"), "Loop flag");
                var player = new KeyframeAnimationPlayer(data, clip.equals("whip") ? 10 : 5);
                player.setupAnim(0);
                var arm = player.get3DTransform("rightArm", TransformType.ROTATION, 0, new Vec3f(0,0,0));
                System.out.println(clip + " arm pitch=" + Math.toDegrees(arm.getX()) + " roll=" + Math.toDegrees(arm.getZ()));
                check(arm.getX() < -.5, "Arm must rise forwards, not backwards");
                check(Math.abs(arm.getZ()) < .8, "No T-pose arm spread");
                var leg = player.get3DTransform("leftLeg", TransformType.ROTATION, 0, new Vec3f(.3f,.2f,.1f));
                check(Math.abs(leg.getX() - .3) < .0001, "Magic must preserve leg/hover animation");
                if (clip.equals("whip")) {
                    check(data.endTick == StrangeRules.WHIP_TICKS, "Whip completion matches server");
                    var impact = new KeyframeAnimationPlayer(data, StrangeRules.WINDUP_TICKS);
                    impact.setupAnim(0);
                    var impactArm = impact.get3DTransform("rightArm", TransformType.ROTATION, 0, new Vec3f(0,0,0));
                    check(impactArm.getX() > arm.getX(), "Impact follows overhead wind-up");
                }
            }
        }
        for (String clip : List.of("portal_open","portal_open_left","portal_close","portal_close_left")) {
            try(var stream=Files.newInputStream(Path.of("src/main/resources/assets/maniacrev/player_animation/strange_"+clip+".json"))) {
                var data=AnimationSerializing.deserializeAnimation(stream).get(0);
                boolean closing=clip.startsWith("portal_close"), left=clip.endsWith("_left");
                check(!data.isInfinite && data.endTick==(closing?StrangeRules.CLOSE_CAST_TICKS:StrangeRules.OPEN_CAST_TICKS), "Gesture duration: "+clip);
                check(data.extraData.get("name").equals("animation.strange_player."+clip), "Gesture registry name");
                var gesture=new KeyframeAnimationPlayer(data,closing?8:20); gesture.setupAnim(0);
                var focus=gesture.get3DTransform(left?"rightArm":"leftArm",TransformType.ROTATION,0,new Vec3f(0,0,0));
                check(Math.abs(Math.toDegrees(focus.getX())+75)<.1 && Math.abs(focus.getY())<.0001, "Focus arm points steadily: "+clip);
                var draw=gesture.get3DTransform(left?"leftArm":"rightArm",TransformType.ROTATION,0,new Vec3f(0,0,0));
                check(Math.abs(draw.getY())>.02, "Amulet arm draws a circle: "+clip);
                var leg=gesture.get3DTransform("leftLeg",TransformType.ROTATION,0,new Vec3f(.3f,.2f,.1f));
                check(Math.abs(leg.getX()-.3)<.0001,"Portal gestures preserve legs");
            }
        }
        System.out.println("Strange checks passed: toggle states, mana costs, portal crossings, player poses and hit timing.");
    }
    private static void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
