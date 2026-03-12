package org.example.maniacrevolution.stats;

import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.example.maniacrevolution.Maniacrev;

/**
 * Отправляет уведомление о сборе статистики при входе игрока в мир.
 *
 * Регистрация: автоматическая через @Mod.EventBusSubscriber
 *
 * Сообщение содержит:
 *   - Информацию о том, что именно собирается
 *   - Ссылку на GitHub
 *   - Команду для отказа от сбора
 */
@Mod.EventBusSubscriber(modid = Maniacrev.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class StatsNotifyHandler {

    private static final String GITHUB_URL =
            "https://github.com/doflare123/Maniac-mod-evolution";

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        // Небольшая задержка чтобы сообщение появилось после стандартных join-сообщений
        player.getServer().execute(() -> sendNotice(player));
    }

    private static void sendNotice(ServerPlayer player) {
        // Разделитель
        player.sendSystemMessage(Component.literal(
                "§8§m─────────────────────────────────────────"));

        // Заголовок
        player.sendSystemMessage(Component.literal(
                "§e§l[Maniacrev] §fСбор статистики"));

        // Суть
        player.sendSystemMessage(Component.literal(
                "§7Мы собираем статистику игр: выбранные §bперки§7, §bклассы§7, " +
                        "§bколичество игроков в командах§7 и §bкто победил§7 — " +
                        "для совершенствования баланса карты."));

        // Прозрачность
        player.sendSystemMessage(Component.literal(
                "§7Никаких персональных данных не собирается. " +
                        "Исходный код открыт — вы можете убедиться в этом сами:"));

        // Кликабельная ссылка на GitHub
        player.sendSystemMessage(
                Component.literal("§9§n" + GITHUB_URL)
                        .setStyle(Style.EMPTY.withClickEvent(
                                new ClickEvent(ClickEvent.Action.OPEN_URL, GITHUB_URL))));

        // Команда отказа — тоже кликабельная
        player.sendSystemMessage(
                Component.literal("§7Если вы не хотите участвовать — нажмите: ")
                        .append(Component.literal("§c/maniacrev offsendstats")
                                .setStyle(Style.EMPTY.withClickEvent(
                                        new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND,
                                                "/maniacrev offsendstats")))));

        // Разделитель
        player.sendSystemMessage(Component.literal(
                "§8§m─────────────────────────────────────────"));
    }
}