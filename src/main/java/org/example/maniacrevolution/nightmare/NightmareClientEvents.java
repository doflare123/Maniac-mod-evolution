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
        if (!ClientNightmareData.isVisible()) return;

        float corruption = 1.0F - ClientNightmareData.getSanityPercent();
        if (corruption < 0.82F) return;

        double time = System.currentTimeMillis() / 36.0D;
        float strength = (corruption - 0.82F) / 0.18F;
        float yaw = (float) Math.sin(time) * 0.65F * strength;
        float pitch = (float) Math.cos(time * 1.17D) * 0.45F * strength;
        float roll = (float) Math.sin(time * 0.73D) * 1.25F * strength;

        event.setYaw(event.getYaw() + yaw);
        event.setPitch(event.getPitch() + pitch);
        event.setRoll(event.getRoll() + roll);
    }
}
