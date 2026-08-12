package org.example.maniacrevolution.network.packets;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import org.example.maniacrevolution.client.ClientSelectiveGlowState;

import java.util.UUID;
import java.util.function.Supplier;

public record SelectiveGlowPacket(ResourceLocation dimension, UUID entityUuid, int entityId, boolean enabled) {
    public SelectiveGlowPacket(Entity entity, boolean enabled) {
        this(entity.level().dimension().location(), entity.getUUID(), entity.getId(), enabled);
    }

    public static void encode(SelectiveGlowPacket packet, FriendlyByteBuf buffer) {
        buffer.writeResourceLocation(packet.dimension);
        buffer.writeUUID(packet.entityUuid);
        buffer.writeVarInt(packet.entityId);
        buffer.writeBoolean(packet.enabled);
    }

    public static SelectiveGlowPacket decode(FriendlyByteBuf buffer) {
        return new SelectiveGlowPacket(
                buffer.readResourceLocation(),
                buffer.readUUID(),
                buffer.readVarInt(),
                buffer.readBoolean()
        );
    }

    public static void handle(SelectiveGlowPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ClientSelectiveGlowState.setGlow(
                        packet.dimension, packet.entityUuid, packet.entityId, packet.enabled)));
        context.setPacketHandled(true);
    }
}
