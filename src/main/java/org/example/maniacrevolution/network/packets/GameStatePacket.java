package org.example.maniacrevolution.network.packets;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import org.example.maniacrevolution.data.ClientGameState;

import java.util.function.Supplier;

public class GameStatePacket {
    private final int phase;
    private final int currentTime;
    private final int maxTime;
    private final boolean timerRunning;

    public GameStatePacket(int phase, int currentTime, int maxTime, boolean running) {
        this.phase = phase;
        this.currentTime = currentTime;
        this.maxTime = maxTime;
        this.timerRunning = running;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(phase);
        buf.writeInt(currentTime);
        buf.writeInt(maxTime);
        buf.writeBoolean(timerRunning);
    }

    public static GameStatePacket decode(FriendlyByteBuf buf) {
        return new GameStatePacket(
                buf.readInt(), buf.readInt(), buf.readInt(), buf.readBoolean()
        );
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ClientGameState.update(phase, currentTime, maxTime, timerRunning);
        });
        ctx.get().setPacketHandled(true);
    }
}
