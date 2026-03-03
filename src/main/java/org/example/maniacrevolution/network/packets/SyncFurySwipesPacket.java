package org.example.maniacrevolution.network.packets;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import org.example.maniacrevolution.client.ClientFurySwipesData;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Синхронизирует стаки Fury Swipes игрока клиенту.
 * Отправляется как самому игроку (для HUD), так и атакующему (для отображения над головой).
 */
public class SyncFurySwipesPacket {

    private final List<Long> stackExpireTicks;

    public SyncFurySwipesPacket(List<Long> stackExpireTicks) {
        this.stackExpireTicks = new ArrayList<>(stackExpireTicks);
    }

    public static void encode(SyncFurySwipesPacket p, FriendlyByteBuf buf) {
        buf.writeVarInt(p.stackExpireTicks.size());
        for (long t : p.stackExpireTicks) buf.writeLong(t);
    }

    public static SyncFurySwipesPacket decode(FriendlyByteBuf buf) {
        int size = buf.readVarInt();
        List<Long> list = new ArrayList<>(size);
        for (int i = 0; i < size; i++) list.add(buf.readLong());
        return new SyncFurySwipesPacket(list);
    }

    public static void handle(SyncFurySwipesPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() ->
                ClientFurySwipesData.updateSelf(packet.stackExpireTicks)
        );
        ctx.get().setPacketHandled(true);
    }
}