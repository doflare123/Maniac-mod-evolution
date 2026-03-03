package org.example.maniacrevolution.network.packets;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import org.example.maniacrevolution.client.ClientFurySwipesData;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Отправляется АТАКУЮЩЕМУ: данные стаков на ЖЕРТВЕ (UUID жертвы + стаки).
 * Используется для рендера иконки + цифры над головой жертвы.
 */
public class SyncFurySwipesTargetPacket {

    private final UUID targetUuid;
    private final List<Long> stackExpireTicks;

    public SyncFurySwipesTargetPacket(UUID targetUuid, List<Long> stackExpireTicks) {
        this.targetUuid = targetUuid;
        this.stackExpireTicks = new ArrayList<>(stackExpireTicks);
    }

    public static void encode(SyncFurySwipesTargetPacket p, FriendlyByteBuf buf) {
        buf.writeUUID(p.targetUuid);
        buf.writeVarInt(p.stackExpireTicks.size());
        for (long t : p.stackExpireTicks) buf.writeLong(t);
    }

    public static SyncFurySwipesTargetPacket decode(FriendlyByteBuf buf) {
        UUID uuid = buf.readUUID();
        int size = buf.readVarInt();
        List<Long> list = new ArrayList<>(size);
        for (int i = 0; i < size; i++) list.add(buf.readLong());
        return new SyncFurySwipesTargetPacket(uuid, list);
    }

    public static void handle(SyncFurySwipesTargetPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() ->
                ClientFurySwipesData.updateTarget(packet.targetUuid, packet.stackExpireTicks)
        );
        ctx.get().setPacketHandled(true);
    }
}