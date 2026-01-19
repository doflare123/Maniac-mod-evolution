package org.example.maniacrevolution.network.packet;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import org.example.maniacrevolution.client.screen.MapVotingScreen;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class MapVotingResultPacket {
    private final String winnerMapId;
    private final Map<String, Integer> finalVoteCount;

    public MapVotingResultPacket(String winnerMapId, Map<String, Integer> finalVoteCount) {
        this.winnerMapId = winnerMapId;
        this.finalVoteCount = finalVoteCount;
    }

    public MapVotingResultPacket(FriendlyByteBuf buf) {
        this.winnerMapId = buf.readUtf();

        int size = buf.readInt();
        this.finalVoteCount = new HashMap<>();
        for (int i = 0; i < size; i++) {
            String mapId = buf.readUtf();
            int count = buf.readInt();
            finalVoteCount.put(mapId, count);
        }
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(winnerMapId);

        buf.writeInt(finalVoteCount.size());
        for (Map.Entry<String, Integer> entry : finalVoteCount.entrySet()) {
            buf.writeUtf(entry.getKey());
            buf.writeInt(entry.getValue());
        }
    }

    public boolean handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            if (Minecraft.getInstance().screen instanceof MapVotingScreen screen) {
                screen.showResult(winnerMapId, finalVoteCount);
            }
        });
        return true;
    }
}