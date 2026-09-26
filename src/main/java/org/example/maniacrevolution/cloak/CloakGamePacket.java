package org.example.maniacrevolution.cloak;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

public record CloakGamePacket(int cloakId, int remaining, float bird, int runTicks, int score) {
    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(cloakId); buf.writeVarInt(remaining); buf.writeFloat(bird);
        buf.writeVarInt(runTicks); buf.writeVarInt(score);
    }
    public static CloakGamePacket decode(FriendlyByteBuf buf) {
        return new CloakGamePacket(buf.readVarInt(), buf.readVarInt(), buf.readFloat(), buf.readVarInt(), buf.readVarInt());
    }
    public void handle(Supplier<NetworkEvent.Context> supplier) {
        supplier.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> org.example.maniacrevolution.cloak.client.CloakClient.game(this)));
        supplier.get().setPacketHandled(true);
    }
}
