package org.example.maniacrevolution.atmosphere.client;

import com.mojang.blaze3d.shaders.FogShape;
import net.minecraft.client.Minecraft;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.material.FogType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.client.event.RenderNameTagEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.example.maniacrevolution.Maniacrev;
import org.example.maniacrevolution.atmosphere.AtmosphereRules;
import org.example.maniacrevolution.effect.ModEffects;

/** Shared visual range, including vanilla fallback and labels rendered without fog. */
@Mod.EventBusSubscriber(modid = Maniacrev.MODID, value = Dist.CLIENT)
public final class AtmosphereVisibility {
    private AtmosphereVisibility() {}

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void fog(ViewportEvent.RenderFog event) {
        if (!AtmosphereClient.hasFogLimit()) return;
        // Layer the volumetric shader over a far-range vanilla fallback. Stronger
        // vanilla/SCP blindness still wins because we only reduce existing ranges.
        float near = AtmosphereClient.profile() == AtmosphereRules.Profile.OFF
                ? AtmosphereRules.FOG_NEAR : AtmosphereRules.FOG_FADE_START;
        event.setNearPlaneDistance(Math.min(event.getNearPlaneDistance(), near));
        event.setFarPlaneDistance(Math.min(event.getFarPlaneDistance(), AtmosphereRules.FOG_END));
        event.setFogShape(FogShape.SPHERE);
        event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void fogColor(ViewportEvent.ComputeFogColor event) {
        var mc = Minecraft.getInstance();
        if (!AtmosphereClient.hasFogLimit() || event.getCamera().getFluidInCamera() != FogType.NONE
                || mc.player.hasEffect(MobEffects.BLINDNESS) || mc.player.hasEffect(MobEffects.DARKNESS)
                || mc.player.hasEffect(ModEffects.FORESTER_BLIND.get())) return;
        event.setRed(0.19F);
        event.setGreen(0.215F);
        event.setBlue(0.205F);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void living(RenderLivingEvent.Pre<?, ?> event) {
        if (AtmosphereClient.hasFogLimit() && AtmosphereRules.beyondFog(event.getEntity()
                .getPosition(event.getPartialTick()).distanceToSqr(Minecraft.getInstance().gameRenderer.getMainCamera().getPosition()))) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void name(RenderNameTagEvent event) {
        if (AtmosphereClient.hasFogLimit() && AtmosphereRules.beyondFog(event.getEntity()
                .getPosition(event.getPartialTick()).distanceToSqr(Minecraft.getInstance().gameRenderer.getMainCamera().getPosition()))) {
            event.setResult(Event.Result.DENY);
        }
    }
}
