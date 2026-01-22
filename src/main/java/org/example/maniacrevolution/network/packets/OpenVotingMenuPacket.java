package org.example.maniacrevolution.network.packets;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import org.example.maniacrevolution.map.MapVotingManager;
import org.example.maniacrevolution.network.ModNetworking;

import java.util.function.Supplier;

public class OpenVotingMenuPacket {

    public OpenVotingMenuPacket() {
    }

    public OpenVotingMenuPacket(FriendlyByteBuf buf) {
        // Пустой пакет
    }

    public void encode(FriendlyByteBuf buf) {
        // Пустой пакет
    }

    public boolean handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null) {
                MapVotingManager manager = MapVotingManager.getInstance();

                // Открываем меню только если голосование активно
                if (manager.isVotingActive()) {
                    String playerVote = manager.getPlayerVote(player.getUUID());
                    ModNetworking.send(new MapVotingPacket(
                            true,
                            manager.getTimeRemaining(),
                            manager.getVoteCount(),
                            playerVote
                    ), player);
                }
            }
        });
        return true;
    }
}