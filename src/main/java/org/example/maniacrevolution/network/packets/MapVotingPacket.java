package org.example.maniacrevolution.network.packet;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import org.example.maniacrevolution.client.screen.MapVotingScreen;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class MapVotingPacket {
    private final boolean open;
    private final int timeRemaining;
    private final Map<String, Integer> voteCount;

    public MapVotingPacket(boolean open, int timeRemaining, Map<String, Integer> voteCount) {
        this.open = open;
        this.timeRemaining = timeRemaining;
        this.voteCount = voteCount;
    }

    public MapVotingPacket(FriendlyByteBuf buf) {
        this.open = buf.readBoolean();
        this.timeRemaining = buf.readInt();

        int size = buf.readInt();
        this.voteCount = new HashMap<>();
        for (int i = 0; i < size; i++) {
            String mapId = buf.readUtf();
            int count = buf.readInt();
            voteCount.put(mapId, count);
        }
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBoolean(open);
        buf.writeInt(timeRemaining);

        buf.writeInt(voteCount.size());
        for (Map.Entry<String, Integer> entry : voteCount.entrySet()) {
            buf.writeUtf(entry.getKey());
            buf.writeInt(entry.getValue());
        }
    }

    public boolean handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            // Обновление экрана если он уже открыт
            if (Minecraft.getInstance().screen instanceof MapVotingScreen screen) {
                screen.updateVoting(timeRemaining, voteCount);
            } else if (open) {
                // Открываем экран только если его еще нет
                Minecraft.getInstance().setScreen(new MapVotingScreen(timeRemaining, voteCount));
            }
        });
        return true;
    }
}