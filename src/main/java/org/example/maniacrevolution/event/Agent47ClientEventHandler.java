package org.example.maniacrevolution.event;

import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.example.maniacrevolution.gui.Agent47TabletScreen;
import org.example.maniacrevolution.item.Agent47TabletItem;

@Mod.EventBusSubscriber(value = Dist.CLIENT)
public class Agent47ClientEventHandler {

    @SubscribeEvent
    public static void onRightClick(InputEvent.InteractionKeyMappingTriggered event) {
        Minecraft mc = Minecraft.getInstance();
        var player = mc.player;

        if (player == null || event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }

        if (player.getItemInHand(InteractionHand.MAIN_HAND).getItem() instanceof Agent47TabletItem) {
            mc.setScreen(new Agent47TabletScreen());
            event.setCanceled(true);
        }
    }
}
