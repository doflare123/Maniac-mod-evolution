package org.example.maniacrevolution.warden.client;

import com.mojang.blaze3d.vertex.PoseStack;
import io.netty.buffer.Unpooled;
import net.minecraft.client.model.WardenModel;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import org.example.maniacrevolution.network.packets.WardenAnimationPacket;
import org.example.maniacrevolution.warden.WardenAnimationTimeline;
import org.example.maniacrevolution.warden.WardenAnimationTimeline.*;
import java.util.UUID;

public final class WardenAbilityAnimationTest {
    public static void run() {
        var attack = new State(Action.ATTACK, 100, 0, -1, 100);
        check(attack.frame(107).action() == Action.ATTACK && attack.frame(108).action() == Action.NONE, "attack expiry");
        var release = new State(Action.RELEASE, 100, 0, -1, 100);
        check(release.frame(125).action() == Action.RELEASE && release.frame(126).action() == Action.NONE, "sonic recovery expiry");
        var cancel = new State(Action.RECOVER, 100, 20, -1, 100);
        check(cancel.frame(105).action() == Action.RECOVER && cancel.frame(106).action() == Action.NONE, "cancel recovery expiry");
        var sniff = new State(Action.NONE, -1, 0, 100, 160);
        check(sniff.frame(170).sniff() == 70 && sniff.frame(184).sniff() == -1, "late refreshed sniff retains original timeline");
        var held = new State(Action.CHARGE, 100, 0, 100, 140);
        check(held.frame(150).elapsed() == 50 && held.frame(150).sniff() == 50, "combat and sniff have independent clocks");
        check(held.frame(201).equals(Frame.IDLE) && !new State(Action.ATTACK, 200, 0, -1, 100).valid(), "stale lease/future start rejected");
        var other = new State(Action.CHARGE, 130, 0, -1, 140);
        check(other.frame(150).elapsed() == 20 && held.frame(150).elapsed() == 50, "different actors never share clocks");
        var packet = new WardenAnimationPacket(new ResourceLocation("minecraft", "overworld"), UUID.randomUUID(), held);
        var buf = new FriendlyByteBuf(Unpooled.buffer());
        try { packet.encode(buf); check(packet.valid() && packet.equals(WardenAnimationPacket.decode(buf)), "tracking snapshot wire round trip"); }
        finally { buf.release(); }
        check(!new State(Action.NONE, 100, 0, -1, 100).valid() && !new State(Action.RECOVER, 100, 21, -1, 100).valid(), "invalid state bounds");
        var model = new WardenPlayerModel(WardenModel.createBodyLayer().bakeRoot());
        var idle = mesh(model, Frame.IDLE);
        var strike = mesh(model, new Frame(Action.ATTACK, 3, 0, -1));
        var charge = mesh(model, new Frame(Action.CHARGE, 20, 0, -1));
        var sniffMesh = mesh(model, new Frame(Action.NONE, 0, 0, 30));
        check(!idle.equals(strike) && !idle.equals(charge) && !idle.equals(sniffMesh), "real vanilla attack/sonic/sniff deform captured mesh");
        check(charge.equals(mesh(model, new Frame(Action.CHARGE, 200, 0, -1))), "fully charged pose remains held");
        check(charge.equals(mesh(model, new Frame(Action.RECOVER, 0, 20, -1))), "cancellation starts from held pose without snap");
        check(idle.equals(mesh(model, new Frame(Action.RECOVER, 6, 20, -1))), "cancellation fades back to ordinary posture");
        check(!charge.equals(mesh(model, new Frame(Action.RELEASE, 8, 0, -1))), "release progresses past charge pose");
        check(sniffMesh.equals(mesh(model, new Frame(Action.NONE, 0, 0, 30))) && idle.equals(mesh(model, Frame.IDLE)), "repeat captures/reset never accumulate animation transforms");
        var arms = new WardenMeshCapture(false, 0.12, 1024, 12000);
        model.animate(0, 0, 0, 0, 0, false); model.applyAbilities(new Frame(Action.CHARGE, 20, 0, -1));
        for (var side : net.minecraft.world.entity.HumanoidArm.values()) {
            var pose = new PoseStack(); model.renderArm(side, pose, arms.quadBuffer(), 0xF000F0);
        }
        check(!arms.limited() && arms.faces().size() == 12, "first-person uses exactly two animated arm meshes");
        checkFirstPerson();
        checkFirstPersonAttack();
        System.out.println("Warden ability animations: server timelines/expiry/independent actors, tracking packet, real vanilla keyframes, held/cancelled/released poses and arm meshes passed.");
    }
    private static void checkFirstPerson() {
        var root = net.minecraft.client.model.geom.builders.LayerDefinition.create(
                net.minecraft.client.model.PlayerModel.createMesh(net.minecraft.client.model.geom.builders.CubeDeformation.NONE, false), 64, 64).bakeRoot();
        var wardenBody = WardenModel.createBodyLayer().bakeRoot().getChild("bone").getChild("body");
        for (float aspect : new float[]{4F / 3, 16F / 9, 2187F / 943}) {
            var projection = new org.joml.Matrix4f().perspective((float) Math.toRadians(70), aspect, 0.05F, 1000);
            for (var side : net.minecraft.world.entity.HumanoidArm.values()) for (int tick = 0; tick <= 10; tick++)
                checkFirstPersonFrame(root.getChild(side == net.minecraft.world.entity.HumanoidArm.RIGHT ? "right_arm" : "left_arm"),
                        wardenBody.getChild(side == net.minecraft.world.entity.HumanoidArm.RIGHT ? "right_arm" : "left_arm"),
                        projection, side, tick / 10F, aspect);
            var pair = new WardenMeshCapture(false, 0.12, 1024, 12000);
            for (var side : net.minecraft.world.entity.HumanoidArm.values()) {
                var pose = new PoseStack(); WardenFirstPersonArms.positionArm(pose, side, 0, 0);
                var arm = wardenBody.getChild(side == net.minecraft.world.entity.HumanoidArm.RIGHT ? "right_arm" : "left_arm");
                arm.setPos(0, 0, 0); WardenFirstPersonArms.renderArm(arm, side, pose, pair.quadBuffer(), 0xF000F0);
            }
            check(pair.faces().size() == 12 && !pair.limited(), "two properly seated player-sized Warden arms");
            var echoProjection = WardenVisionPrototype.echoProjection(projection);
            for (float distance : new float[]{50, 1000, 1000000}) {
                var point = echoProjection.transform(new org.joml.Vector4f(0, 0, -distance, 1));
                check(point.z <= point.w && point.z >= -point.w, "shrieker echo survives native far-plane clipping");
            }
        }
    }
    private static void checkFirstPersonAttack() {
        var model = new WardenPlayerModel(WardenModel.createBodyLayer().bakeRoot());
        for (float aspect : new float[]{4F / 3, 16F / 9, 2187F / 943}) {
            var projection = new org.joml.Matrix4f().perspective((float) Math.toRadians(70), aspect, 0.05F, 1000);
            for (var side : net.minecraft.world.entity.HumanoidArm.values()) {
                var idle = firstPersonAttackMesh(model, side, Frame.IDLE);
                var raised = firstPersonAttackMesh(model, side, new Frame(Action.ATTACK, 1, 0, -1));
                var strike = firstPersonAttackMesh(model, side, new Frame(Action.ATTACK, 3, 0, -1));
                check(!idle.equals(raised) && !raised.equals(strike), "both first-person arms wind up and strike using Warden keyframes");
                for (int step = 0; step <= 32; step++) {
                    var faces = firstPersonAttackMesh(model, side, new State(Action.ATTACK, 100, 0, -1, 100).frame(100 + step / 4.0));
                    check(faces.size() == 6, "attack preserves all six textured faces per arm");
                    int visible = 0;
                    for (var face : faces) for (var vertex : new net.minecraft.world.phys.Vec3[]{face.a(), face.b(), face.c(), face.d()}) {
                        var p = projection.transform(new org.joml.Vector4f((float) vertex.x, (float) vertex.y, (float) vertex.z, 1));
                        check(Float.isFinite(p.x) && Float.isFinite(p.y) && Float.isFinite(p.z) && p.w > 0.05F,
                                "animated first-person hand remains in front of the near plane");
                        if (Math.abs(p.x / p.w) < 1 && Math.abs(p.y / p.w) < 1) visible++;
                    }
                    check(visible > 0, "hand remains visible during two-arm strike: " + side + " tick " + step / 4.0 + " aspect " + aspect);
                }
                check(idle.equals(firstPersonAttackMesh(model, side, new Frame(Action.ATTACK, 8, 0, -1))), "vanilla strike finishes at original seated hand pose");
                check(idle.equals(firstPersonAttackMesh(model, side, Frame.IDLE)), "attack expiry resets first-person arm transforms");
                check(raised.equals(firstPersonAttackMesh(model, side, new Frame(Action.ATTACK, 1, 0, -1))), "repeated frames never accumulate arm offsets");
            }
        }
    }
    private static java.util.List<WardenSurfaceCache.Face> firstPersonAttackMesh(WardenPlayerModel model,
            net.minecraft.world.entity.HumanoidArm side, Frame frame) {
        WardenFirstPersonArms.prepareAttack(model, frame);
        var pose = new PoseStack(); WardenFirstPersonArms.positionArm(pose, side, 0, 0);
        var arm = model.root().getChild("bone").getChild("body").getChild(
                side == net.minecraft.world.entity.HumanoidArm.RIGHT ? "right_arm" : "left_arm");
        var capture = new WardenMeshCapture(false, 0.12, 1024, 12000);
        WardenFirstPersonArms.renderArm(arm, side, pose, capture.quadBuffer(), 0xF000F0);
        check(!capture.limited(), "first-person attack stays within the mesh budget");
        return capture.faces();
    }
    private static void checkFirstPersonFrame(net.minecraft.client.model.geom.ModelPart arm, net.minecraft.client.model.geom.ModelPart wardenArm, org.joml.Matrix4f projection,
                                            net.minecraft.world.entity.HumanoidArm side, float swing, float aspect) {
        var hands = new WardenMeshCapture(false, 0.12, 1024, 12000);
        var pose = new PoseStack(); WardenFirstPersonArms.positionArm(pose, side, 0, swing);
        arm.render(pose, hands.quadBuffer(), 0xF000F0, net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY);
        var wardenHand = new WardenMeshCapture(false, 0.12, 1024, 12000);
        wardenArm.setPos(0, 0, 0);
        WardenFirstPersonArms.renderArm(wardenArm, side, pose, wardenHand.quadBuffer(), 0xF000F0);
        check(wardenHand.faces().size() == 6, "one Warden textured arm with player dimensions");
        var expected = hands.faces().stream().flatMap(f -> java.util.stream.Stream.of(f.a(), f.b(), f.c(), f.d())).toList();
        for (var face : wardenHand.faces()) for (var vertex : new net.minecraft.world.phys.Vec3[]{face.a(), face.b(), face.c(), face.d()})
            check(expected.stream().anyMatch(v -> v.distanceToSqr(vertex) < 1.0e-10), "Warden UV arm matches actual vanilla player geometry during swing");
        check(hands.faces().size() == 6, "one vanilla arm mesh per main-hand choice");
        int visible = 0;
        for (var face : hands.faces()) for (var v : new net.minecraft.world.phys.Vec3[]{face.a(), face.b(), face.c(), face.d()}) {
            var p = projection.transform(new org.joml.Vector4f((float) v.x, (float) v.y, (float) v.z, 1));
            check(Float.isFinite(p.x) && Float.isFinite(p.y) && Float.isFinite(p.z) && Float.isFinite(p.w), "finite first-person projection");
            if (p.w > 0 && Math.abs(p.x / p.w) < 1 && Math.abs(p.y / p.w) < 1) visible++;
            if (swing == 0 && p.w > 0) {
                check(Math.abs(p.x / p.w) > 0.1, "idle arms keep center aiming area open");
            }
        }
        check(visible > 0, "vanilla hand has visible geometry: " + side + " swing " + swing + " aspect " + aspect);
    }
    private static java.util.List<WardenSurfaceCache.Face> mesh(WardenPlayerModel model, Frame frame) {
        model.animate(0, 0, 0, 0, 0, false); model.applyAbilities(frame);
        var capture = new WardenMeshCapture(false, 0.12, 1024, 12000);
        model.renderToBuffer(new PoseStack(), capture.quadBuffer(), 0xF000F0, 0, 1, 1, 1, 1);
        check(!capture.limited(), "animated model stays within mesh budget");
        return capture.faces();
    }
    private static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
}
