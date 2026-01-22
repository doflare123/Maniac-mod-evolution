package org.example.maniacrevolution.event;

import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.PlayerEvent;
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

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        CompoundTag persistentData = player.getPersistentData();

        // Проверяем, видел ли игрок гайд
        if (!persistentData.getBoolean(NBT_KEY)) {
            // Помечаем как увиденный
            persistentData.putBoolean(NBT_KEY, true);

            // Отправляем пакет клиенту для открытия гайда
            ModNetworking.sendToPlayer(new OpenGuidePacket(GuidePage.PageType.TUTORIAL), player);

            System.out.println("[FirstJoin] Opening guide for new player: " + player.getName().getString());
        }
    }

    /**
     * Клиентская обработка (если нужно)
     */
    @Mod.EventBusSubscriber(value = Dist.CLIENT, modid = Maniacrev.MODID)
    public static class ClientHandler {

        public static void openGuide(GuidePage.PageType page) {
            Minecraft mc = Minecraft.getInstance();
            mc.execute(() -> {
                mc.setScreen(new GuideScreen(page));
            });
        }
    }
}