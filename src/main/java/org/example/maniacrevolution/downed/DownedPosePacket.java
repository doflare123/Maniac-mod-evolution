package org.example.maniacrevolution.downed;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record DownedPosePacket(int playerId, float bodyYaw) {
    public void encode(FriendlyByteBuf buffer) {
        buffer.writeVarInt(playerId);
        buffer.writeFloat(bodyYaw);
    }

    public static DownedPosePacket decode(FriendlyByteBuf buffer) {
        return new DownedPosePacket(buffer.readVarInt(), buffer.readFloat());
    }

    public void handle(Supplier<NetworkEvent.Context> context) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> DownedPresentation.sync(this));
        context.get().setPacketHandled(true);
    }
}
