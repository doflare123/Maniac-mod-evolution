package org.example.maniacrevolution.hook;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.world.phys.Vec3;
import org.example.maniacrevolution.client.HookAimClient;
import org.example.maniacrevolution.client.HookAimState;

public final class HookAimStateTest {
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    public static void main(String[] args) {
        var aim = new HookAimState();
        Object hook = new Object();
        check(!aim.release(), "Idle release must not cast");
        aim.begin(2, hook);
        aim.begin(3, new Object());
        aim.validate(2, hook);
        check(aim.isAiming(), "Repeated aim callbacks must not reset the selected weapon");
        check(aim.release(), "Release after holding must cast");
        check(!aim.release(), "Release must cast exactly once");

        aim.begin(2, hook);
        aim.cancelWithButton();
        aim.updateCancelKey(true);
        check(!aim.isAiming() && aim.blocksCancel(), "LMB must cancel and consume repeats");
        aim.begin(2, hook);
        check(!aim.release(), "LMB cancellation must not cast on RMB release");
        aim.begin(2, hook);
        check(!aim.isAiming(), "Held cancellation button must not restart aiming");
        aim.updateCancelKey(false);
        check(!aim.blocksCancel(), "LMB release must restore normal item use");
        check(!aim.release(), "An aim press made while cancel is held must be released before rearming");
        aim.begin(2, hook);
        check(aim.release(), "New click after cancellation must work");

        aim.begin(2, hook);
        aim.validate(3, hook);
        aim.validate(2, hook);
        aim.begin(2, hook);
        check(!aim.release(), "Switching away and back while holding must cancel");
        aim.begin(2, hook);
        aim.validate(2, new Object());
        check(!aim.release(), "Replacing the stack in the same slot must cancel");
        aim.begin(2, hook);
        aim.cancel();
        check(!aim.release(), "Losing focus, opening a GUI or dying must cancel");
        aim.begin(2, hook);
        aim.reset();
        check(!aim.release() && !aim.blocksCancel(), "Disconnect must clear all input state");

        int[] vertices = {0};
        var consumer = (VertexConsumer) Proxy.newProxyInstance(HookAimStateTest.class.getClassLoader(),
                new Class<?>[]{VertexConsumer.class}, (proxy, method, values) -> {
                    if (method.isDefault()) return InvocationHandler.invokeDefault(proxy, method, values);
                    if (method.getName().equals("vertex")) {
                        for (Object value : values)
                            check(Double.isFinite(((Number) value).doubleValue()), "Preview emitted a non-finite vertex");
                        double x = ((Number) values[0]).doubleValue();
                        check(x >= 0 && x <= 24, "Preview extends past its hit point");
                    }
                    if (method.getName().equals("endVertex")) vertices[0]++;
                    return method.getReturnType() == VertexConsumer.class ? proxy : null;
                });
        HookAimClient.drawGuide(new PoseStack(), consumer, Vec3.ZERO, new Vec3(24, 0, 0),
                new Vec3(0, 0, 1), new Vec3(0, 1, 0), 105, 195, 200);
        check(vertices[0] == 82, "Expected 37 dashes and four hollow marker edges, got " + vertices[0]);
        vertices[0] = 0;
        HookAimClient.drawGuide(new PoseStack(), consumer, Vec3.ZERO, Vec3.ZERO,
                new Vec3(0, 0, 1), new Vec3(0, 1, 0), 105, 195, 200);
        check(vertices[0] == 0, "Zero-length preview must not emit invalid geometry");
        vertices[0] = 0;
        HookAimClient.drawGroundProjection(new PoseStack(), consumer, new Vec3(5, 12, 7),
                new Vec3(5, 3, 7), true, 105, 195, 200);
        check(vertices[0] == 6, "Ground projection must contain one vertical line and a floor cross");
        vertices[0] = 0;
        HookAimClient.drawGroundProjection(new PoseStack(), consumer, new Vec3(5, 12, 7),
                new Vec3(5, -64, 7), false, 105, 195, 200);
        check(vertices[0] == 2, "Void projection must not draw a fake floor marker");
        for (float yaw : new float[]{0, 90, 180, -90}) {
            for (float pitch : new float[]{-80, 0, 45, 80}) {
                Vec3 right = new Vec3(-Math.cos(Math.toRadians(yaw)), 0, -Math.sin(Math.toRadians(yaw)));
                double yawRadians = Math.toRadians(yaw);
                double pitchRadians = Math.toRadians(pitch);
                Vec3 forward = new Vec3(-Math.sin(yawRadians) * Math.cos(pitchRadians),
                        -Math.sin(pitchRadians), Math.cos(yawRadians) * Math.cos(pitchRadians));
                Vec3 rightHand = org.example.maniacrevolution.entity.HookTrajectory.handOffset(yaw, pitch, true);
                Vec3 leftHand = org.example.maniacrevolution.entity.HookTrajectory.handOffset(yaw, pitch, false);
                check(Math.abs(rightHand.dot(right) - 0.28) < 1.0E-5, "Right-hand hook must appear on the right");
                check(Math.abs(leftHand.dot(right) + 0.28) < 1.0E-5, "Left-hand hook must mirror the right");
                check(Math.abs(rightHand.dot(forward) - 0.55) < 1.0E-5, "Hook must start ahead of the camera");
            }
        }
        System.out.println("Hook aiming: hold/release, LMB repeats, slot/stack changes, focus loss, disconnect and guide geometry passed");
    }
}
