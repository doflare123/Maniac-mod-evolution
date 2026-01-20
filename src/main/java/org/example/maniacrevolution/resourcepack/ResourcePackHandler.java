package org.example.maniacrevolution.resourcepack;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.example.maniacrevolution.Maniacrev;
import org.example.maniacrevolution.command.ResourcePackCommand;

@Mod.EventBusSubscriber(modid = Maniacrev.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ResourcePackHandler {

    // Измени dl=0 на dl=1 в конце ссылки!
    private static final String RESOURCE_PACK_URL = "https://www.dropbox.com/scl/fi/w6mc7iov78yg4vxw9tzce/resources.zip?rlkey=5yf4wly16qrzn4glun7j4a9to&st=m41a0fyj&dl=1";

    // Оставь пустым, если не хочешь использовать проверку хеша
    private static final String RESOURCE_PACK_HASH = "";

    // true = игрок обязан принять ресурс-пак
    private static final boolean REQUIRED = true;

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            // Проверяем включен ли ресурс-пак
            if (!ResourcePackCommand.isResourcePackEnabled()) {
                Maniacrev.LOGGER.info("Resource pack is disabled, skipping for player: {}",
                        player.getName().getString());
                return;
            }

            // Сообщение, которое увидит игрок
            Component message = Component.literal(
                    "§6§lManiacRev Ресурс-Пак\n" +
                            "§eЭтот сервер требует специальный ресурс-пак.\n" +
                            "§cЗагрузка обязательна для игры!"
            );

            // Отправляем ресурс-пак игроку
            player.sendTexturePack(
                    RESOURCE_PACK_URL,
                    RESOURCE_PACK_HASH,
                    REQUIRED,
                    message
            );

            Maniacrev.LOGGER.info("Resource pack sent to player: {}", player.getName().getString());
        }
    }
}