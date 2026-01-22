package org.example.maniacrevolution.event;

import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.example.maniacrevolution.Maniacrev;
import org.example.maniacrevolution.gui.GuideScreen;
import org.example.maniacrevolution.gui.pages.GuidePage;
import org.example.maniacrevolution.network.ModNetworking;
import org.example.maniacrevolution.network.packets.OpenGuidePacket;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = Maniacrev.MODID)
public class FirstJoinHandler {

    private static final String NBT_KEY = "HasSeenGuide";

    // НОВОЕ: Очередь игроков, ожидающих открытия гайда
    private static final Map<UUID, Integer> pendingGuideOpens = new HashMap<>();
    private static final int DELAY_TICKS = 60; // 3 секунды задержки для загрузки текстурпака

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        CompoundTag persistentData = player.getPersistentData();

        // Проверяем, видел ли игрок гайд
        if (!persistentData.getBoolean(NBT_KEY)) {
            // Помечаем как увиденный
            persistentData.putBoolean(NBT_KEY, true);

            // ИСПРАВЛЕНО: Добавляем в очередь с задержкой вместо немедленной отправки
            pendingGuideOpens.put(player.getUUID(), DELAY_TICKS);

            System.out.println("[FirstJoin] Scheduled guide opening for new player: " +
                    player.getName().getString() + " in " + DELAY_TICKS + " ticks");
        }
    }

    /**
     * НОВОЕ: Тикер для отложенного открытия гайда
     */
    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (event.getServer() == null) return;

        // Обрабатываем очередь
        pendingGuideOpens.entrySet().removeIf(entry -> {
            UUID playerId = entry.getKey();
            int remainingTicks = entry.getValue() - 1;

            if (remainingTicks <= 0) {
                // Время истекло - открываем гайд
                ServerPlayer player = event.getServer().getPlayerList().getPlayer(playerId);

                if (player != null) {
                    ModNetworking.sendToPlayer(
                            new OpenGuidePacket(GuidePage.PageType.TUTORIAL),
                            player
                    );
                    System.out.println("[FirstJoin] Opened guide for: " + player.getName().getString());
                }

                return true; // Удаляем из очереди
            } else {
                // Обновляем оставшееся время
                entry.setValue(remainingTicks);
                return false; // Оставляем в очереди
            }
        });
    }

    /**
     * НОВОЕ: Очистка очереди при выходе игрока
     */
    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            pendingGuideOpens.remove(player.getUUID());
        }
    }

    /**
     * Клиентская обработка
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