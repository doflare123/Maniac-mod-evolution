package org.example.maniacrevolution.network.packets;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import org.example.maniacrevolution.client.ClientParticleEffects;

import java.util.UUID;
import java.util.function.Supplier;

public record ClientParticleEffectPacket(
        EffectType type,
        double x,
        double y,
        double z,
        float primaryRadius,
        float secondaryRadius,
        int durationTicks,
        int targetEntityId,
        UUID targetUuid
) {
    private static final UUID NO_TARGET = new UUID(0L, 0L);

    public static ClientParticleEffectPacket fearWave(Vec3 center, float radius, int durationTicks) {
        return new ClientParticleEffectPacket(EffectType.FEAR_WAVE, center.x, center.y, center.z,
                radius, 0.0F, durationTicks, -1, NO_TARGET);
    }

    public static ClientParticleEffectPacket guidingLight(Entity target, int durationTicks) {
        return new ClientParticleEffectPacket(EffectType.GUIDING_LIGHT, 0.0, 0.0, 0.0,
                0.0F, 0.0F, durationTicks, target.getId(), target.getUUID());
    }

    public static ClientParticleEffectPacket hackRadius(Vec3 center, float supportRadius, float hackerRadius) {
        return new ClientParticleEffectPacket(EffectType.HACK_RADIUS, center.x, center.y, center.z,
                supportRadius, hackerRadius, 0, -1, NO_TARGET);
    }

    public static void encode(ClientParticleEffectPacket packet, FriendlyByteBuf buffer) {
        buffer.writeByte(packet.type.ordinal());
        buffer.writeDouble(packet.x);
        buffer.writeDouble(packet.y);
        buffer.writeDouble(packet.z);
        buffer.writeFloat(packet.primaryRadius);
        buffer.writeFloat(packet.secondaryRadius);
        buffer.writeVarInt(packet.durationTicks);
        buffer.writeVarInt(packet.targetEntityId);
        buffer.writeUUID(packet.targetUuid);
    }

    public static ClientParticleEffectPacket decode(FriendlyByteBuf buffer) {
        EffectType type = EffectType.values()[buffer.readUnsignedByte()];
        return new ClientParticleEffectPacket(
                type,
                buffer.readDouble(),
                buffer.readDouble(),
                buffer.readDouble(),
                buffer.readFloat(),
                buffer.readFloat(),
                buffer.readVarInt(),
                buffer.readVarInt(),
                buffer.readUUID()
        );
    }

    public static void handle(ClientParticleEffectPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ClientParticleEffects.accept(packet)));
        context.setPacketHandled(true);
    }

    public enum EffectType {
        FEAR_WAVE,
        GUIDING_LIGHT,
        HACK_RADIUS
    }
}
