package org.example.maniacrevolution.network.packets;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import org.example.maniacrevolution.stamina.client.PlayerLocomotionAnimations;

import java.util.function.Supplier;

public record DashStartedPacket(int playerId) {
    public void encode(FriendlyByteBuf buffer) {
        buffer.writeVarInt(playerId);
    }

    public static DashStartedPacket decode(FriendlyByteBuf buffer) {
        return new DashStartedPacket(buffer.readVarInt());
    }

    public void handle(Supplier<NetworkEvent.Context> context) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> PlayerLocomotionAnimations.dashStarted(playerId));
        context.get().setPacketHandled(true);
    }
}
