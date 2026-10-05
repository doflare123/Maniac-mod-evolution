package org.example.maniacrevolution.network.packets;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import org.example.maniacrevolution.warden.client.WardenPlayerForm;

import java.util.UUID;
import java.util.function.Supplier;

/** Server -> client visual form flag, not a request to transform or spawn an entity. */
public record WardenFormPacket(UUID playerId, boolean active) {
    public void encode(FriendlyByteBuf buf) {
        buf.writeUUID(playerId);
        buf.writeBoolean(active);
    }

    public static WardenFormPacket decode(FriendlyByteBuf buf) {
        return new WardenFormPacket(buf.readUUID(), buf.readBoolean());
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> WardenPlayerForm.update(playerId, active)));
        ctx.get().setPacketHandled(true);
    }
}
