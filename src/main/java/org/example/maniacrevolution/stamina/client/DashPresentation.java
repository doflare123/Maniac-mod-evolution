package org.example.maniacrevolution.stamina.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.example.maniacrevolution.Maniacrev;

@Mod.EventBusSubscriber(modid = Maniacrev.MODID, value = Dist.CLIENT)
public final class DashPresentation {
    private DashPresentation() {}

    public static float rollAngle(AbstractClientPlayer player, float partialTick) {
        if (player == null) return 0;
        float age = PlayerLocomotionAnimations.dashAge(player) + partialTick;
        if (age < 0 || age >= PlayerLocomotionAnimations.DASH_POSE_TICKS) return 0;
        return 360 * (float) Mth.smoothstep(Mth.clamp((age - 0.5f) / 8.0f, 0, 1));
    }

    static float rollLift(float angle) {
        return 0.24f * Mth.sin(angle * Mth.DEG_TO_RAD * 0.5f);
    }

    public static void rollModel(AbstractClientPlayer player, PoseStack pose, float partialTick, double pivot) {
        float angle = rollAngle(player, partialTick);
        if (angle <= 0 || angle >= 360) return;
        pose.translate(0, pivot + rollLift(angle), 0);
        pose.mulPose(Axis.XP.rotationDegrees(-angle));
        pose.translate(0, -pivot, 0);
    }

    public static float lunge(AbstractClientPlayer player, float partialTick) {
        if (player == null) return 0;
        float age = PlayerLocomotionAnimations.dashAge(player) + partialTick;
        if (age < 0 || age >= PlayerLocomotionAnimations.DASH_POSE_TICKS) return 0;
        if (age < 2) return Mth.lerp((float) Mth.smoothstep(age / 2), 0.35f, 1.0f);
        return 1 - (float) Mth.smoothstep((age - 2) / (PlayerLocomotionAnimations.DASH_POSE_TICKS - 2));
    }

    @SubscribeEvent
    public static void camera(ViewportEvent.ComputeCameraAngles event) {
        var mc = Minecraft.getInstance();
        if (mc.player == null || event.getCamera().getEntity() != mc.player
                || !mc.options.getCameraType().isFirstPerson()) return;
        float amount = lunge(mc.player, (float) event.getPartialTick());
        event.setPitch(event.getPitch() + 12 * amount);
        event.setRoll(event.getRoll() - 2.5f * amount);
    }

    @SubscribeEvent
    public static void fieldOfView(ViewportEvent.ComputeFov event) {
        var mc = Minecraft.getInstance();
        if (mc.player == null || event.getCamera().getEntity() != mc.player
                || !mc.options.getCameraType().isFirstPerson() || !event.usedConfiguredFov()) return;
        event.setFOV(event.getFOV() + 3 * lunge(mc.player, (float) event.getPartialTick())
                * mc.options.fovEffectScale().get());
    }

    public static void hands(LocalPlayer player, PoseStack pose, float partialTick) {
        float amount = lunge(player, partialTick);
        if (amount <= 0) return;
        pose.translate(-0.13 * amount, 0.08 * amount, -0.17 * amount);
        pose.mulPose(Axis.XP.rotationDegrees(23 * amount));
        pose.mulPose(Axis.ZP.rotationDegrees(9 * amount));
    }
}
