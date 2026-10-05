package org.example.maniacrevolution.network.packets;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

/** A short-lived sonic ring, never a player echo. */
public record WardenWavePacket(ResourceLocation dimension, Vec3 position, Vec3 direction, long tick, float strength) {
    public void encode(FriendlyByteBuf b) { b.writeResourceLocation(dimension); for (var v : new Vec3[]{position, direction}) { b.writeDouble(v.x); b.writeDouble(v.y); b.writeDouble(v.z); } b.writeLong(tick); b.writeFloat(strength); }
    public static WardenWavePacket decode(FriendlyByteBuf b) { return new WardenWavePacket(b.readResourceLocation(), new Vec3(b.readDouble(), b.readDouble(), b.readDouble()), new Vec3(b.readDouble(), b.readDouble(), b.readDouble()), b.readLong(), b.readFloat()); }
    public boolean valid() {
        return dimension != null && position != null && direction != null && Double.isFinite(position.x) && Double.isFinite(position.y) && Double.isFinite(position.z)
                && Double.isFinite(direction.lengthSqr()) && Math.abs(direction.lengthSqr() - 1) < 0.001 && tick >= 0 && Float.isFinite(strength) && strength > 0 && strength <= 1;
    }
    public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> org.example.maniacrevolution.warden.client.WardenVisionPrototype.acceptWave(this)));
        context.get().setPacketHandled(true);
    }
}
