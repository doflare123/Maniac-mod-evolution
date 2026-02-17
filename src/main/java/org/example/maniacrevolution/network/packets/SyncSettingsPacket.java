package org.example.maniacrevolution.network.packets;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import org.example.maniacrevolution.settings.ClientGameSettings;

import java.util.function.Supplier;

public class SyncSettingsPacket {
    private final int computerCount;
    private final int hackPoints;
    private final int hpBoost;
    private final int maniacCount;
    private final int gameTime;
    private final int selectedMap;

    public SyncSettingsPacket(int computerCount, int hackPoints, int hpBoost,
                              int maniacCount, int gameTime, int selectedMap) {
        this.computerCount = computerCount;
        this.hackPoints = hackPoints;
        this.hpBoost = hpBoost;
        this.maniacCount = maniacCount;
        this.gameTime = gameTime;
        this.selectedMap = selectedMap;
    }

    public static void encode(SyncSettingsPacket msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.computerCount);
        buf.writeInt(msg.hackPoints);
        buf.writeInt(msg.hpBoost);
        buf.writeInt(msg.maniacCount);
        buf.writeInt(msg.gameTime);
        buf.writeInt(msg.selectedMap);
    }

    public static SyncSettingsPacket decode(FriendlyByteBuf buf) {
        return new SyncSettingsPacket(
                buf.readInt(),
                buf.readInt(),
                buf.readInt(),
                buf.readInt(),
                buf.readInt(),
                buf.readInt()
        );
    }

    public static void handle(SyncSettingsPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ClientGameSettings.setSettings(
                    msg.computerCount,
                    msg.hackPoints,
                    msg.hpBoost,
                    msg.maniacCount,
                    msg.gameTime,
                    msg.selectedMap
            );
        });
        ctx.get().setPacketHandled(true);
    }
}