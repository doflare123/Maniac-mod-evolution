package org.example.maniacrevolution.network.packets;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import org.example.maniacrevolution.warden.WardenNoise;
import org.example.maniacrevolution.warden.client.WardenVisionPrototype;
import java.util.function.Supplier;

/** Visible source pulse, or an optional frozen pose for a hidden player (never a shrieker activation). */
public record WardenNoisePacket(WardenNoise noise, WardenShriekerEchoPacket echo) {
    public WardenNoisePacket(WardenNoise noise) { this(noise, null); }
    public boolean valid() {
        return noise != null && noise.valid() && (echo == null || noise.source() != null && noise.kind() != WardenNoise.Kind.WAVE
                && echo.valid() && echo.dimension().equals(noise.dimension()) && echo.origin().equals(noise.position()) && echo.tick() == noise.tick());
    }
    public void encode(FriendlyByteBuf buf) {
        buf.writeResourceLocation(noise.dimension());
        buf.writeBoolean(noise.source() != null);
        if (noise.source() != null) buf.writeUUID(noise.source());
        buf.writeDouble(noise.position().x); buf.writeDouble(noise.position().y); buf.writeDouble(noise.position().z);
        buf.writeEnum(noise.kind()); buf.writeFloat(noise.strength());
        buf.writeDouble(noise.hearingRange()); buf.writeLong(noise.tick());
        buf.writeBoolean(echo != null); if (echo != null) echo.encode(buf);
    }
    public static WardenNoisePacket decode(FriendlyByteBuf buf) {
        var dimension = buf.readResourceLocation();
        var source = buf.readBoolean() ? buf.readUUID() : null;
        var position = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
        var noise = new WardenNoise(dimension, source, position, buf.readEnum(WardenNoise.Kind.class),
                buf.readFloat(), buf.readDouble(), buf.readLong());
        return new WardenNoisePacket(noise, buf.readBoolean() ? WardenShriekerEchoPacket.decode(buf) : null);
    }
    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> { if (valid()) WardenVisionPrototype.acceptServerNoise(noise, echo); }));
        ctx.get().setPacketHandled(true);
    }
}
