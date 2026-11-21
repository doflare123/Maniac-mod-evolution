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
        boolean keeper = AtmosphereClient.profile() == AtmosphereRules.Profile.KEEPER;
        event.setRed(keeper ? 0.163F : 0.19F);
        event.setGreen(keeper ? 0.177F : 0.215F);
        event.setBlue(keeper ? 0.218F : 0.205F);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void living(RenderLivingEvent.Pre<?, ?> event) {
        var mc = Minecraft.getInstance();
        // Feed glowing models into the normal outline buffer even beyond the world's fog limit.
        // Atmosphere erases their ordinary body first; vanilla then composites the authorized outline.
        if (AtmosphereClient.hasFogLimit() && !mc.shouldEntityAppearGlowing(event.getEntity())
                && !event.getEntity().hasCustomOutlineRendering(mc.player) && AtmosphereRules.beyondFog(event.getEntity()
                .getPosition(event.getPartialTick()).distanceToSqr(mc.gameRenderer.getMainCamera().getPosition()))) {
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
