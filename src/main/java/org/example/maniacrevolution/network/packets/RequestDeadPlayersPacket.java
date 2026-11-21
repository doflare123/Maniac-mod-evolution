package org.example.maniacrevolution.network.packets;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import org.example.maniacrevolution.necromancer.NecromancerProvider;
import org.example.maniacrevolution.necromancer.NecromancerResurrection;
import org.example.maniacrevolution.network.ModNetworking;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

public class RequestDeadPlayersPacket {
    private final UUID ritualId;

    public RequestDeadPlayersPacket(UUID ritualId) {
        this.ritualId = ritualId;
    }

    public static RequestDeadPlayersPacket decode(FriendlyByteBuf buffer) {
        return new RequestDeadPlayersPacket(buffer.readUUID());
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeUUID(ritualId);
    }

    public boolean handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer sender = context.getSender();
            if (sender == null || !NecromancerResurrection.canCast(sender)) {
                return;
            }

            var necromancer = sender.getCapability(NecromancerProvider.NECROMANCER).orElse(null);
            if (necromancer == null || !necromancer.hasPendingResurrection(ritualId, sender.server.getTickCount())) {
                return;
            }

            // Собираем список мёртвых игроков
            List<DeadPlayerInfo> deadPlayers = new ArrayList<>();

            for (ServerPlayer player : sender.server.getPlayerList().getPlayers()) {
                if (NecromancerResurrection.isEligibleTarget(player)) {
                    deadPlayers.add(new DeadPlayerInfo(player.getUUID(), player.getName().getString()));
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
