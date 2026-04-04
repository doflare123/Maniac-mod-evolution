package org.example.maniacrevolution.network.packets;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class MapVotingPacket {
    private final boolean open;
    private final int timeRemaining;
    private final Map<String, Integer> voteCount;
    private final String playerVotedMapId;

    public MapVotingPacket(boolean open, int timeRemaining,
                           Map<String, Integer> voteCount, String playerVotedMapId) {
        this.open = open;
        this.timeRemaining = timeRemaining;
        this.voteCount = voteCount;
        this.playerVotedMapId = playerVotedMapId;
    }

    public MapVotingPacket(FriendlyByteBuf buf) {
        this.open = buf.readBoolean();
        this.timeRemaining = buf.readInt();
        int size = buf.readInt();
        this.voteCount = new HashMap<>();
        for (int i = 0; i < size; i++) {
            voteCount.put(buf.readUtf(), buf.readInt());
        }
        boolean hasVote = buf.readBoolean();
        this.playerVotedMapId = hasVote ? buf.readUtf() : null;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBoolean(open);
        buf.writeInt(timeRemaining);
        buf.writeInt(voteCount.size());
        for (Map.Entry<String, Integer> entry : voteCount.entrySet()) {
            buf.writeUtf(entry.getKey());
            buf.writeInt(entry.getValue());
        }
        buf.writeBoolean(playerVotedMapId != null);
        if (playerVotedMapId != null) buf.writeUtf(playerVotedMapId);
    }

    public boolean handle(Supplier<NetworkEvent.Context> ctx) {
        boolean capturedOpen = this.open;
        int capturedTime = this.timeRemaining;
        Map<String, Integer> capturedVotes = this.voteCount;
        String capturedMapId = this.playerVotedMapId;
        ctx.get().enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () ->
                        () -> org.example.maniacrevolution.client.ClientScreenHelper
                                .handleMapVotingPacket(capturedOpen, capturedTime, capturedVotes, capturedMapId))
        );
        return true;
    }
}