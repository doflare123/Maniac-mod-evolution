package org.example.maniacrevolution.network.packets;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Пакет для синхронизации кулдауна планшета с клиентом
 * Отправляется с сервера на клиент
 */
public class SyncTabletCooldownPacket {

    private final int cooldownSeconds;

    public SyncTabletCooldownPacket(int cooldownSeconds) {
        this.cooldownSeconds = cooldownSeconds;
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeInt(cooldownSeconds);
    }

    public static SyncTabletCooldownPacket decode(FriendlyByteBuf buffer) {
        return new SyncTabletCooldownPacket(buffer.readInt());
    }

    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            // Сохраняем кулдаун на клиенте для отображения в GUI
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null && cooldownSeconds > 0) {
                mc.player.displayClientMessage(
                        net.minecraft.network.chat.Component.literal(
                                String.format("§eКулдаун отслеживания: %d сек", cooldownSeconds)
                        ),
                        true
                );
            }
        });
        context.setPacketHandled(true);
    }

    public int getCooldownSeconds() {
        return cooldownSeconds;
    }
}