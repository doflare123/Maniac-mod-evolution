package org.example.maniacrevolution.network.packets;

import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import org.example.maniacrevolution.scp173.client.Scp173PlayerForm;

public record Scp173FormPacket(UUID playerId, boolean active) {
    public void encode(FriendlyByteBuf buf) { buf.writeUUID(playerId); buf.writeBoolean(active); }
    public static Scp173FormPacket decode(FriendlyByteBuf buf) {
        return new Scp173FormPacket(buf.readUUID(), buf.readBoolean());
    }
    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> Scp173PlayerForm.update(playerId, active)));
        ctx.get().setPacketHandled(true);
    }
}
