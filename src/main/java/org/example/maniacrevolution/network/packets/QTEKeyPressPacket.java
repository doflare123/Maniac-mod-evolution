package org.example.maniacrevolution.network.packets;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class QTEKeyPressPacket {
    private final int keyIndex;
    private final int generatorNumber;
    private final boolean success;

    public QTEKeyPressPacket(int keyIndex, int generatorNumber, boolean success) {
        this.keyIndex = keyIndex;
        this.generatorNumber = generatorNumber;
        this.success = success;
    }

    public static void encode(QTEKeyPressPacket packet, FriendlyByteBuf buf) {
        buf.writeInt(packet.keyIndex);
        buf.writeInt(packet.generatorNumber);
        buf.writeBoolean(packet.success);
    }

    public static QTEKeyPressPacket decode(FriendlyByteBuf buf) {
        return new QTEKeyPressPacket(
                buf.readInt(),
                buf.readInt(),
                buf.readBoolean()
        );
    }

    public static void handle(QTEKeyPressPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null && packet.success) {
                String command = String.format("function maniac:hacks/hack_qte%d", packet.generatorNumber);
                player.getServer().getCommands().performPrefixedCommand(
                        player.createCommandSourceStack(),
                        command
                );
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
