package org.example.maniacrevolution.event;

import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.example.maniacrevolution.Maniacrev;
import org.example.maniacrevolution.hud.CustomHud;

/** Draws the custom HUD after ChatScreen, which renders later than GUI overlays. */
@Mod.EventBusSubscriber(modid = Maniacrev.MODID, value = Dist.CLIENT)
public final class ChatHudRenderEvent {
    private ChatHudRenderEvent() {
    }

    @SubscribeEvent
    public static void onScreenRenderPost(ScreenEvent.Render.Post event) {
        if (event.getScreen() instanceof ChatScreen) {
            CustomHud.INSTANCE.renderAboveChat(event.getGuiGraphics(), event.getPartialTick());
        }
    }
}
