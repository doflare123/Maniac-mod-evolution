package org.example.maniacrevolution.hud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.example.maniacrevolution.Maniacrev;
import org.example.maniacrevolution.effect.ModEffects;
import org.example.maniacrevolution.perk.perks.survivor.RoseColoredGlassesPerk;

@Mod.EventBusSubscriber(modid = Maniacrev.MODID, value = Dist.CLIENT)
public final class RoseColoredGlassesClientEvents {
    private static float previousStrength;
    private static float strength;

    private RoseColoredGlassesClientEvents() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft minecraft = Minecraft.getInstance();
        previousStrength = strength;
        boolean active = minecraft.player != null
                && minecraft.player.hasEffect(ModEffects.ROSE_COLORED_GLASSES.get());
        float step = 1.0F / RoseColoredGlassesPerk.FILTER_FADE_TICKS;
        strength = Mth.clamp(strength + (active ? step : -step), 0.0F, 1.0F);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onScreenRenderPost(ScreenEvent.Render.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        renderFilter(event.getGuiGraphics(), event.getPartialTick(),
                minecraft.getWindow().getGuiScaledWidth(),
                minecraft.getWindow().getGuiScaledHeight());
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        previousStrength = 0.0F;
        strength = 0.0F;
    }

    public static void renderFilter(GuiGraphics graphics, float partialTick,
                                    int width, int height) {
        float renderedStrength = Mth.lerp(partialTick, previousStrength, strength);
        RoseColoredGlassesOverlay.draw(graphics, width, height, renderedStrength);
    }
}
