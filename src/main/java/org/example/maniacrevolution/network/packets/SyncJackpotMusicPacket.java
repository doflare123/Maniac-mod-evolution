package org.example.maniacrevolution.network.packets;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import org.example.maniacrevolution.client.JackpotMusicHandler;

import java.util.UUID;
import java.util.function.Supplier;

public class SyncJackpotMusicPacket {
    private final UUID playerId;
    private final boolean active;

    public SyncJackpotMusicPacket(UUID playerId, boolean active) {
        this.playerId = playerId;
        this.active = active;
    }

    public static void encode(SyncJackpotMusicPacket packet, FriendlyByteBuf buf) {
        buf.writeUUID(packet.playerId);
        buf.writeBoolean(packet.active);
    }

    public static SyncJackpotMusicPacket decode(FriendlyByteBuf buf) {
        return new SyncJackpotMusicPacket(buf.readUUID(), buf.readBoolean());
    }

    public static void handle(SyncJackpotMusicPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                        JackpotMusicHandler.setJackpotActive(packet.playerId, packet.active)));
        ctx.get().setPacketHandled(true);
    }
}
