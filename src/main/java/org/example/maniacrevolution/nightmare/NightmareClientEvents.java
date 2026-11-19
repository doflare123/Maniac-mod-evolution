package org.example.maniacrevolution.nightmare;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.example.maniacrevolution.Maniacrev;

@Mod.EventBusSubscriber(modid = Maniacrev.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class NightmareClientEvents {
    private NightmareClientEvents() {}

    @SubscribeEvent
    public static void onCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        float corruption = org.example.maniacrevolution.atmosphere.client.AtmosphereClient.cameraCorruption();
        if (corruption < 0.82F) return;

        var mc = net.minecraft.client.Minecraft.getInstance();
        if (mc.level == null) return;
        double time = (mc.level.getGameTime() % 240000 + (mc.isPaused() ? 0 : mc.getFrameTime())) / 20.0D;
        float strength = (corruption - 0.82F) / 0.18F;
        strength *= org.example.maniacrevolution.atmosphere.AtmosphereConfig.DISTORTION.get().floatValue();
        float pulse = 0.3F + 0.7F * (float) (0.5 + 0.5 * Math.sin(time * 1.65 + Math.sin(time * 0.43) * 0.45));
        float yaw = (float) Math.sin(time * 1.9) * 0.35F * strength * pulse;
        float pitch = (float) Math.cos(time * 1.3) * 0.20F * strength * pulse;
        float roll = (float) Math.sin(time * 1.1) * 0.70F * strength * pulse;

        event.setYaw(event.getYaw() + yaw);
        event.setPitch(event.getPitch() + pitch);
        event.setRoll(event.getRoll() + roll);
    }
}
