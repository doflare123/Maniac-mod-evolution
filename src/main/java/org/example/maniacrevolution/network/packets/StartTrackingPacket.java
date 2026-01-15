package org.example.maniacrevolution.network.packets;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import org.example.maniacrevolution.item.MedicTabletItem;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * Пакет для начала отслеживания игрока через планшет медика
 * Отправляется с клиента на сервер
 */
public class StartTrackingPacket {

    private final UUID targetPlayerUUID;

    public StartTrackingPacket(UUID targetPlayerUUID) {
        this.targetPlayerUUID = targetPlayerUUID;
    }

    /**
     * Кодирование пакета в буфер
     */
    public void encode(FriendlyByteBuf buffer) {
        buffer.writeUUID(targetPlayerUUID);
    }

    /**
     * Декодирование пакета из буфера
     */
    public static StartTrackingPacket decode(FriendlyByteBuf buffer) {
        return new StartTrackingPacket(buffer.readUUID());
    }

    /**
     * Обработка пакета на сервере
     */
    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer sender = context.getSender();
            if (sender == null) return;

            // Находим целевого игрока
            ServerPlayer target = sender.getServer().getPlayerList().getPlayer(targetPlayerUUID);
            if (target == null) {
                sender.displayClientMessage(
                        net.minecraft.network.chat.Component.literal("§cИгрок не найден"),
                        false
                );
                return;
            }

            // Активируем отслеживание
            MedicTabletItem.startTracking(sender, target);
        });
        context.setPacketHandled(true);
    }
}