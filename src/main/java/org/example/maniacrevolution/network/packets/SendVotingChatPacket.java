package org.example.maniacrevolution.network.packets;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import org.example.maniacrevolution.map.MapVotingManager;

import java.util.function.Supplier;

public class SendVotingChatPacket {

    public SendVotingChatPacket() {
    }

    public SendVotingChatPacket(FriendlyByteBuf buf) {
    }

    public void encode(FriendlyByteBuf buf) {
    }

    public boolean handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            // Вызываем отправку сообщения в чат на сервере
            MapVotingManager.getInstance().sendChatMessage();
        });
        return true;
    }
}