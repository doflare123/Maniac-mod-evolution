package org.example.maniacrevolution.network.packets;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import org.example.maniacrevolution.Maniacrev;
import org.example.maniacrevolution.readiness.ReadinessManager;

import java.util.function.Supplier;

/**
 * Пакет для изменения статуса готовности игрока (Client -> Server)
 */
public class ReadyStatusPacket {
    private final boolean ready;

    public ReadyStatusPacket(boolean ready) {
        this.ready = ready;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBoolean(ready);
    }

    public static ReadyStatusPacket decode(FriendlyByteBuf buf) {
        return new ReadyStatusPacket(buf.readBoolean());
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            ReadinessManager.setPlayerReady(player, ready);

            Maniacrev.LOGGER.info("Player {} ready status: {}",
                    player.getName().getString(), ready);
        });
        ctx.get().setPacketHandled(true);
    }
}