package org.example.maniacrevolution.network.packets;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import org.example.maniacrevolution.warden.WardenScentPoint;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/** Complete bounded snapshot; an empty list removes previously delivered traces. */
public record WardenScentPacket(ResourceLocation dimension, long tick, int preparation, int duration,
                                int cooldown, List<WardenScentPoint> points) {
    public static final int MAX_POINTS = 256;
    public WardenScentPacket { points = List.copyOf(points); if (points.size() > MAX_POINTS) throw new IllegalArgumentException("scent budget"); }
    public boolean valid() {
        return dimension != null && tick >= 0 && preparation >= 0 && preparation <= 10 && duration >= 0 && duration <= 100
                && cooldown >= 0 && cooldown <= 200 && !(preparation > 0 && duration > 0)
                && preparation <= cooldown && duration <= cooldown
                && (duration > 0 || points.isEmpty()) && points.stream().allMatch(p -> p.valid() && p.tick() <= tick && tick - p.tick() < WardenScentPoint.LIFE);
    }
    public void encode(FriendlyByteBuf buf) {
        buf.writeResourceLocation(dimension); buf.writeLong(tick);
        buf.writeVarInt(preparation); buf.writeVarInt(duration); buf.writeVarInt(cooldown); buf.writeVarInt(points.size());
        for (var point : points) {
            buf.writeDouble(point.position().x); buf.writeDouble(point.position().y); buf.writeDouble(point.position().z);
            buf.writeFloat(point.strength()); buf.writeLong(point.tick());
        }
    }
    public static WardenScentPacket decode(FriendlyByteBuf buf) {
        var dimension = buf.readResourceLocation(); long tick = buf.readLong();
        int prep = buf.readVarInt(), duration = buf.readVarInt(), cooldown = buf.readVarInt(), size = buf.readVarInt();
        if (size < 0 || size > MAX_POINTS) throw new IllegalArgumentException("scent budget");
        var points = new ArrayList<WardenScentPoint>(size);
        for (int i = 0; i < size; i++) points.add(new WardenScentPoint(new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble()), buf.readFloat(), buf.readLong()));
        return new WardenScentPacket(dimension, tick, prep, duration, cooldown, points);
    }
    public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> org.example.maniacrevolution.warden.client.WardenSniffClient.accept(this)));
        context.get().setPacketHandled(true);
    }
}
