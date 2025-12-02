package org.example.maniacrevolution.network.packets;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import org.example.maniacrevolution.perk.perks.client.WallhackGlowHandler;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Пакет для управления эффектом свечения (Glowing) на клиенте.
 * Server -> Client
 */
public class WallhackGlowPacket {
    private final Set<Integer> entityIds;
    private final boolean enable; // true = включить, false = выключить

    public WallhackGlowPacket(Set<Integer> entityIds, boolean enable) {
        this.entityIds = entityIds;
        this.enable = enable;
    }

    public WallhackGlowPacket(FriendlyByteBuf buf) {
        int count = buf.readInt();
        this.entityIds = new HashSet<>();

        for (int i = 0; i < count; i++) {
            entityIds.add(buf.readInt());
        }

        this.enable = buf.readBoolean();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(entityIds.size());

        for (Integer id : entityIds) {
            buf.writeInt(id);
        }

        buf.writeBoolean(enable);
    }

    public static void encode(WallhackGlowPacket packet, FriendlyByteBuf buf) {
        packet.toBytes(buf);
    }

    public static WallhackGlowPacket decode(FriendlyByteBuf buf) {
        return new WallhackGlowPacket(buf);
    }

    public static void handle(WallhackGlowPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            if (packet.enable) {
                WallhackGlowHandler.enableGlow(packet.entityIds);
            } else {
                WallhackGlowHandler.disableGlow(packet.entityIds);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}