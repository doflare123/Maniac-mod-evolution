package org.example.maniacrevolution.network.packets;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import org.example.maniacrevolution.settings.GameSettings;

import java.util.function.Supplier;

public class UpdateSettingsPacket {
    private final int computerCount;
    private final int hackPoints;
    private final int hpBoost;
    private final int maniacCount;
    private final int gameTime;
    private final int selectedMap;

    public UpdateSettingsPacket(int computerCount, int hackPoints, int hpBoost,
                                int maniacCount, int gameTime, int selectedMap) {
        this.computerCount = computerCount;
        this.hackPoints = hackPoints;
        this.hpBoost = hpBoost;
        this.maniacCount = maniacCount;
        this.gameTime = gameTime;
        this.selectedMap = selectedMap;
    }

    public static void encode(UpdateSettingsPacket msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.computerCount);
        buf.writeInt(msg.hackPoints);
        buf.writeInt(msg.hpBoost);
        buf.writeInt(msg.maniacCount);
        buf.writeInt(msg.gameTime);
        buf.writeInt(msg.selectedMap);
    }

    public static UpdateSettingsPacket decode(FriendlyByteBuf buf) {
        return new UpdateSettingsPacket(
                buf.readInt(),
                buf.readInt(),
                buf.readInt(),
                buf.readInt(),
                buf.readInt(),
                buf.readInt()
        );
    }

    public static void handle(UpdateSettingsPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null && player.hasPermissions(2)) { // Только операторы
                GameSettings settings = GameSettings.get(player.server);
                settings.setComputerCount(msg.computerCount);
                settings.setHackPoints(msg.hackPoints);
                settings.setHpBoost(msg.hpBoost);
                settings.setManiacCount(msg.maniacCount);
                settings.setGameTime(msg.gameTime);
                settings.setSelectedMap(msg.selectedMap);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}