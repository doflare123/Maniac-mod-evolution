package org.example.maniacrevolution.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import org.example.maniacrevolution.map.MapVotingManager;

import java.util.function.Supplier;

public class PlayerVotePacket {
    private final String mapId;
    private final boolean remove; // true если игрок отменяет голос

    public PlayerVotePacket(String mapId, boolean remove) {
        this.mapId = mapId;
        this.remove = remove;
    }

    public PlayerVotePacket(FriendlyByteBuf buf) {
        this.mapId = buf.readUtf();
        this.remove = buf.readBoolean();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(mapId);
        buf.writeBoolean(remove);
    }

    public boolean handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null) {
                if (remove) {
                    MapVotingManager.getInstance().removeVote(player.getUUID());
                } else {
                    MapVotingManager.getInstance().vote(player.getUUID(), mapId);
                }
            }
        });
        return true;
    }
}