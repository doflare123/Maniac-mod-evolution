package org.example.maniacrevolution.event;

import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
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

    private static final Map<UUID, Integer> pendingGuideOpens = new HashMap<>();
    // ИСПРАВЛЕНО: Увеличена задержка до 10 секунд для загрузки текстурпака
    private static final int DELAY_TICKS = 200; // 10 секунд задержки

    // ИСПРАВЛЕНО: Добавлен LOWEST приоритет - выполнится последним
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        CompoundTag persistentData = player.getPersistentData();

        if (!persistentData.getBoolean(NBT_KEY)) {
            persistentData.putBoolean(NBT_KEY, true);

            // Добавляем в очередь с увеличенной задержкой
            pendingGuideOpens.put(player.getUUID(), DELAY_TICKS);

            Maniacrev.LOGGER.info("[FirstJoin] Scheduled guide opening for new player: {} in {} ticks ({}s)",
                    player.getName().getString(), DELAY_TICKS, DELAY_TICKS / 20);
        }
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (event.getServer() == null) return;

        pendingGuideOpens.entrySet().removeIf(entry -> {
            UUID playerId = entry.getKey();
            int remainingTicks = entry.getValue() - 1;

            if (remainingTicks <= 0) {
                ServerPlayer player = event.getServer().getPlayerList().getPlayer(playerId);

                if (player != null) {
                    ModNetworking.sendToPlayer(
                            new OpenGuidePacket(GuidePage.PageType.TUTORIAL),
                            player
                    );
                    Maniacrev.LOGGER.info("[FirstJoin] Opened guide for: {}", player.getName().getString());
                }

                return true;
            } else {
                entry.setValue(remainingTicks);
                return false;
            }
        });
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            pendingGuideOpens.remove(player.getUUID());
        }
    }

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