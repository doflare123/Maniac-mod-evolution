package org.example.maniacrevolution.network.packets;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import org.example.maniacrevolution.gui.ResurrectionScreen;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

public class SyncDeadPlayersPacket {

    private final List<RequestDeadPlayersPacket.DeadPlayerInfo> deadPlayers;

    public SyncDeadPlayersPacket(List<RequestDeadPlayersPacket.DeadPlayerInfo> deadPlayers) {
        this.deadPlayers = deadPlayers;
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeInt(deadPlayers.size());
        System.out.println("[SyncDeadPlayersPacket] Encoding " + deadPlayers.size() + " players");

        for (var player : deadPlayers) {
            buffer.writeUUID(player.uuid);
            buffer.writeUtf(player.name);
            System.out.println("[SyncDeadPlayersPacket] Encoded: " + player.name);
        }
    }

    public static SyncDeadPlayersPacket decode(FriendlyByteBuf buffer) {
        int size = buffer.readInt();
        System.out.println("[SyncDeadPlayersPacket] Decoding " + size + " players");

        List<RequestDeadPlayersPacket.DeadPlayerInfo> players = new ArrayList<>();

        for (int i = 0; i < size; i++) {
            UUID uuid = buffer.readUUID();
            String name = buffer.readUtf();
            players.add(new RequestDeadPlayersPacket.DeadPlayerInfo(uuid, name));
            System.out.println("[SyncDeadPlayersPacket] Decoded: " + name);
        }

        return new SyncDeadPlayersPacket(players);
    }

    public boolean handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            System.out.println("[SyncDeadPlayersPacket] Handling on client - " + deadPlayers.size() + " players");

            // Обновляем GUI
            ResurrectionScreen.updateDeadPlayers(deadPlayers);
        });
        context.setPacketHandled(true);
        return true;
    }
}