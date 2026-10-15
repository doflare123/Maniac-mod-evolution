package org.example.maniacrevolution.network.packets;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import org.example.maniacrevolution.scp173.client.Scp173Client;

import java.util.UUID;
import java.util.function.Supplier;

/** Only the controlled player's authoritative hold and physics snapshot. */
public record Scp173HoldPacket(UUID player, ResourceLocation dimension, boolean held,
                               float yaw, float pitch, Vec3 position, Vec3 velocity) {
    public void encode(FriendlyByteBuf b) {
        b.writeUUID(player); b.writeResourceLocation(dimension); b.writeBoolean(held);
        b.writeFloat(yaw); b.writeFloat(pitch);
        b.writeDouble(position.x); b.writeDouble(position.y); b.writeDouble(position.z);
        b.writeDouble(velocity.x); b.writeDouble(velocity.y); b.writeDouble(velocity.z);
    }

    public static Scp173HoldPacket decode(FriendlyByteBuf b) {
        return new Scp173HoldPacket(b.readUUID(), b.readResourceLocation(), b.readBoolean(), b.readFloat(), b.readFloat(),
                new Vec3(b.readDouble(), b.readDouble(), b.readDouble()), new Vec3(b.readDouble(), b.readDouble(), b.readDouble()));
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> Scp173Client.accept(this)));
        ctx.get().setPacketHandled(true);
    }
}
