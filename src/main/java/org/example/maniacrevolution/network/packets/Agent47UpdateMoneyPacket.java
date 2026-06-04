package org.example.maniacrevolution.network.packets;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import org.example.maniacrevolution.gui.Agent47TabletScreen;

import java.util.function.Supplier;

/**
 * Пакет для обновления баланса игрока
 * S -> C
 */
public class Agent47UpdateMoneyPacket {

    private final int money;

    public Agent47UpdateMoneyPacket(int money) {
        this.money = money;
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeInt(money);
    }

    public static Agent47UpdateMoneyPacket decode(FriendlyByteBuf buffer) {
        return new Agent47UpdateMoneyPacket(buffer.readInt());
    }

    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            // Обновляем баланс в GUI
            Agent47TabletScreen.updateMoney(money);
        });
        context.setPacketHandled(true);
    }
}