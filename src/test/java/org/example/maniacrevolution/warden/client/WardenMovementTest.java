package org.example.maniacrevolution.warden.client;

import com.mojang.blaze3d.vertex.PoseStack;
import io.netty.buffer.Unpooled;
import net.minecraft.client.model.WardenModel;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.ai.attributes.*;
import org.example.maniacrevolution.effect.WardenPaceEffect;
import org.example.maniacrevolution.network.packets.WardenStaminaPacket;
import org.example.maniacrevolution.warden.WardenAnimationTimeline;
import org.example.maniacrevolution.warden.WardenStamina;
import java.util.UUID;

public final class WardenMovementTest {
    public static void main(String[] args) {
        net.minecraft.SharedConstants.tryDetectVersion();
        net.minecraft.server.Bootstrap.bootStrap();
        run();
        org.example.maniacrevolution.warden.WardenCombatTest.run();
        WardenAbilityAnimationTest.run();
    }
    public static void run() {
        var stamina = new WardenStamina();
        for (int i = 0; i < 99; i++) stamina.tick(true, true);
        check(stamina.amount() == 3 && stamina.boosting(), "five seconds, last running tick available");
        stamina.tick(true, true);
        check(stamina.amount() == 0 && stamina.exhausted() && !stamina.boosting(), "pool empty at 100 ticks");
        for (int i = 0; i < 20; i++) stamina.tick(false, false);
        check(stamina.amount() == 0 && stamina.delay() == 0, "one-second recovery delay");
        for (int i = 0; i < 299; i++) stamina.tick(false, false);
        check(stamina.amount() == 299, "fifteen seconds to refill");
        stamina.tick(false, false);
        check(stamina.amount() == 300 && !stamina.exhausted(), "refilled at 300 recovery ticks");
        for (int i = 0; i < 500; i++) stamina.tick(true, false);
        check(stamina.amount() == 300, "standing against a wall does not spend stamina");
        for (int i = 0; i < 100; i++) stamina.tick(true, true);
        for (int i = 0; i < 320; i++) stamina.tick(true, true);
        check(stamina.amount() == 300 && !stamina.boosting() && stamina.exhausted(), "holding sprint after exhaustion does not pulse automatically");
        stamina.tick(false, false); stamina.tick(true, true);
        check(stamina.boosting() && stamina.amount() == 297, "release and repress rearm sprint");
        speeds();
        var packet = new WardenStaminaPacket(new ResourceLocation("minecraft", "overworld"), 500, 297, 20, true, false);
        var buffer = new FriendlyByteBuf(Unpooled.buffer());
        try { packet.encode(buffer); check(packet.valid() && packet.equals(WardenStaminaPacket.decode(buffer)), "stamina wire round trip"); }
        finally { buffer.release(); }
        check(!new WardenStaminaPacket(packet.dimension(), 0, 301, 0, false, false).valid()
                && !new WardenStaminaPacket(packet.dimension(), 0, 0, 0, true, true).valid(), "invalid pool and boost rejected");
        swimming();
        System.out.println("Warden movement: 5s sprint, 1s delay, 15s recovery, exhaustion latch, wall idle, exact speed effects, wire packet, swim transitions/mesh passed.");
    }
    private static void speeds() {
        var map = new AttributeMap(AttributeSupplier.builder().add(Attributes.MOVEMENT_SPEED, 0.1).build());
        var speed = map.getInstance(Attributes.MOVEMENT_SPEED);
        var weight = new WardenPaceEffect(false); var burst = new WardenPaceEffect(true);
        weight.addAttributeModifiers(null, map, 9);
        near(speed.getValue(), 0.09, "level ten means minus ten percent");
        weight.removeAttributeModifiers(null, map, 9); burst.addAttributeModifiers(null, map, 59);
        near(speed.getValue(), 0.16, "level sixty means plus sixty percent");
        burst.addAttributeModifiers(null, map, 59);
        near(speed.getValue(), 0.16, "refresh never stacks");
        speed.addTransientModifier(new AttributeModifier(UUID.randomUUID(), "vanilla sprint", 0.3, AttributeModifier.Operation.MULTIPLY_TOTAL));
        speed.addTransientModifier(new AttributeModifier(UUID.randomUUID(), "sprint correction", 1.0 / 1.3 - 1, AttributeModifier.Operation.MULTIPLY_TOTAL));
        near(speed.getValue(), 0.16, "vanilla sprint bonus compensated");
        burst.removeAttributeModifiers(null, map, 59); weight.addAttributeModifiers(null, map, 9);
        near(speed.getValue(), 0.09, "exhausted native sprint uses walking penalty");
        weight.removeAttributeModifiers(null, map, 9);
        near(speed.getValue(), 0.1, "class modifiers removed without deleting other speed sources");
    }
    private static void swimming() {
        var model = new WardenPlayerModel(WardenModel.createBodyLayer().bakeRoot());
        String idle = pose(model, 0, 0), swimming = pose(model, 1, 0);
        check(!idle.equals(swimming) && !swimming.equals(pose(model, 1, 8)), "swim deforms real mesh and progresses stroke");
        check(swimming.equals(pose(model, 1, 0)) && idle.equals(pose(model, 0, 0)), "leaving water/repeated captures reset pose");
        check(!pose(model, 0.5F, 0).equals(idle) && !pose(model, 0.5F, 0).equals(swimming), "smooth intermediate swim pose");
        model.animate(0, 0, 0, 0, 0, false); model.swim(1, 0);
        model.applyAbilities(new WardenAnimationTimeline.Frame(WardenAnimationTimeline.Action.ATTACK, 3, 0, -1));
        check(!signature(model).equals(swimming), "combat animation still affects swimming mesh");
        for (var side : HumanoidArm.values()) {
            var still = new PoseStack(); WardenFirstPersonArms.swimmingArm(still, side, 0, 8);
            check(still.last().pose().equals(new org.joml.Matrix4f()), "dry first-person hand transform unchanged");
            WardenFirstPersonArms.swimmingArm(still, side, 1, 8);
            check(!still.last().pose().equals(new org.joml.Matrix4f()), "first-person hand strokes");
        }
    }
    private static String pose(WardenPlayerModel model, float weight, float age) {
        model.animate(0, 0, 0, 0, 0, false); model.swim(weight, age); return signature(model);
    }
    private static String signature(WardenPlayerModel model) {
        var capture = new WardenMeshCapture(false, 0.12, 1024, 12000);
        model.renderToBuffer(new PoseStack(), capture.quadBuffer(), 0xF000F0, 0, 1, 1, 1, 1);
        return capture.faces().toString();
    }
    private static void near(double actual, double expected, String message) { check(Math.abs(actual - expected) < 1E-8, message); }
    private static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
}
