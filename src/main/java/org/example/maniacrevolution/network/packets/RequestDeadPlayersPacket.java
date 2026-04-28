package org.example.maniacrevolution.network.packets;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import org.example.maniacrevolution.network.ModNetworking;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

public class RequestDeadPlayersPacket {

    public RequestDeadPlayersPacket() {
    }

    public static RequestDeadPlayersPacket decode(FriendlyByteBuf buffer) {
        return new RequestDeadPlayersPacket();
    }

    public void encode(FriendlyByteBuf buffer) {
        // Пустой пакет
    }

    public boolean handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer sender = context.getSender();
            if (sender == null) {
                return;
            }

            // Собираем список мёртвых игроков
            List<DeadPlayerInfo> deadPlayers = new ArrayList<>();

            for (ServerPlayer player : sender.getServer().getPlayerList().getPlayers()) {
                if (player.isSpectator()) {
                    var team = player.getTeam();

                    if (team != null && "survivors".equalsIgnoreCase(team.getName())) {
                        deadPlayers.add(new DeadPlayerInfo(
                                player.getUUID(),
                                player.getName().getString()
                        ));
                    }
                }
            }

            // Отправляем список обратно
            ModNetworking.sendToPlayer(
                    new SyncDeadPlayersPacket(deadPlayers),
                    sender
            );
        });
        context.setPacketHandled(true);
        return true;
    }

    public static class DeadPlayerInfo {
        public UUID uuid;
        public String name;

        public DeadPlayerInfo(UUID uuid, String name) {
            this.uuid = uuid;
            this.name = name;
        }
    }
}
