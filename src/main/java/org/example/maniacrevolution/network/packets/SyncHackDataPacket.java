package org.example.maniacrevolution.network.packets;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import org.example.maniacrevolution.hack.client.ClientHackData;

import java.util.function.Supplier;

/**
 * Пакет синхронизации прогресса взломов компьютеров. Server → Client.
 *
 * Регистрация в ModNetworking (добавить рядом с другими пакетами):
 *   CHANNEL.registerMessage(
 *       nextId(),
 *       SyncHackDataPacket.class,
 *       SyncHackDataPacket::encode,
 *       SyncHackDataPacket::decode,
 *       SyncHackDataPacket::handle);
 *
 * Отправка (например из HackManager.onComputerHacked):
 *   ModNetworking.sendToAll(server, new SyncHackDataPacket(totalHacked, goal));
 */
public class SyncHackDataPacket {
    private final int totalHacked;
    private final int goal;

    public SyncHackDataPacket(int totalHacked, int goal) {
        this.totalHacked = totalHacked;
        this.goal = goal;
    }

    public static void encode(SyncHackDataPacket p, FriendlyByteBuf buf) {
        buf.writeInt(p.totalHacked);
        buf.writeInt(p.goal);
    }

    public static SyncHackDataPacket decode(FriendlyByteBuf buf) {
        return new SyncHackDataPacket(buf.readInt(), buf.readInt());
    }

    public static void handle(SyncHackDataPacket p, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> ClientHackData.update(p.totalHacked, p.goal));
        ctx.get().setPacketHandled(true);
    }
}