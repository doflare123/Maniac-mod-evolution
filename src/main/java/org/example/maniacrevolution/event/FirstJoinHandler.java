package org.example.maniacrevolution.event;

import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.example.maniacrevolution.Maniacrev;
import org.example.maniacrevolution.gui.GuideScreen;
import org.example.maniacrevolution.gui.pages.GuidePage;
import org.example.maniacrevolution.network.ModNetworking;
import org.example.maniacrevolution.network.packets.OpenGuidePacket;

@Mod.EventBusSubscriber(modid = Maniacrev.MODID)
public class FirstJoinHandler {
    private static final String NBT_KEY = "HasSeenGuide";

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        CompoundTag persistentData = player.getPersistentData();
        if (persistentData.getBoolean(NBT_KEY)) {
            return;
        }

        persistentData.putBoolean(NBT_KEY, true);
        ModNetworking.sendToPlayer(new OpenGuidePacket(GuidePage.PageType.TUTORIAL), player);
        Maniacrev.LOGGER.info("[FirstJoin] Opened guide for new player: {}", player.getName().getString());
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (!event.getOriginal().getPersistentData().getBoolean(NBT_KEY)) {
            return;
        }
        event.getEntity().getPersistentData().putBoolean(NBT_KEY, true);
    }

    @Mod.EventBusSubscriber(value = Dist.CLIENT, modid = Maniacrev.MODID)
    public static class ClientHandler {
        public static void openGuide(GuidePage.PageType page) {
            Minecraft mc = Minecraft.getInstance();
            mc.execute(() -> mc.setScreen(new GuideScreen(page)));
        }
    }
}
