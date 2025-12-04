package org.example.maniacrevolution.network.packets;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import org.example.maniacrevolution.perk.perks.CatchMistakesPerk;

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
            if (player == null) {
                System.out.println("[QTE] ERROR: Player is null!");
                return;
            }

            // ОТЛАДКА
            System.out.println("=== QTE Packet Received ===");
            System.out.println("Player: " + player.getName().getString());
            System.out.println("KeyIndex: " + packet.keyIndex);
            System.out.println("Generator: " + packet.generatorNumber);
            System.out.println("Success: " + packet.success);
            System.out.println("===========================");

            if (packet.success) {
                System.out.println("[QTE] Success branch - executing hack command");
                // Успешный хак
                String command = String.format("function maniac:hacks/hack_qte%d", packet.generatorNumber);
                player.getServer().getCommands().performPrefixedCommand(
                        player.createCommandSourceStack(),
                        command
                );
            } else {
                System.out.println("[QTE] Fail branch - checking Catch Mistakes perk");
                // Промах - активируем перк "Ловля на ошибках"
                boolean perkActivated = CatchMistakesPerk.onQTEFailed(player);

                if (perkActivated) {
                    System.out.println("[QTE] Catch Mistakes perk activated! Player " +
                            player.getName().getString() + " is now glowing!");
                } else {
                    System.out.println("[QTE] Catch Mistakes perk NOT activated (no ready holders or wrong team)");
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }
}