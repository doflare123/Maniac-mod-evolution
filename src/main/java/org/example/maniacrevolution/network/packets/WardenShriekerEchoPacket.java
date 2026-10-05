package org.example.maniacrevolution.network.packets;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import org.example.maniacrevolution.warden.WardenEchoPose;
import java.util.function.Supplier;

/** One frozen pose and its source impulse. No UUID, range gate or follow-up tracking. */
public record WardenShriekerEchoPacket(ResourceLocation dimension, Vec3 origin, long tick, WardenEchoPose pose) {
    private static void vector(FriendlyByteBuf buf, Vec3 value) { buf.writeDouble(value.x); buf.writeDouble(value.y); buf.writeDouble(value.z); }
    private static Vec3 vector(FriendlyByteBuf buf) { return new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble()); }
    public void encode(FriendlyByteBuf buf) {
        buf.writeResourceLocation(dimension); vector(buf, origin); buf.writeLong(tick); vector(buf, pose.position());
        buf.writeFloat(pose.bodyYaw()); buf.writeFloat(pose.headYaw()); buf.writeFloat(pose.pitch());
        buf.writeFloat(pose.limb()); buf.writeFloat(pose.speed()); buf.writeFloat(pose.attack()); buf.writeFloat(pose.swim());
        buf.writeVarInt(pose.age()); buf.writeEnum(pose.pose()); buf.writeBoolean(pose.riding()); buf.writeBoolean(pose.flying());
    }
    public static WardenShriekerEchoPacket decode(FriendlyByteBuf buf) {
        var dimension = buf.readResourceLocation(); var origin = vector(buf); long tick = buf.readLong();
        return new WardenShriekerEchoPacket(dimension, origin, tick, new WardenEchoPose(vector(buf),
                buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readFloat(),
                buf.readVarInt(), buf.readEnum(Pose.class), buf.readBoolean(), buf.readBoolean()));
    }
    public boolean valid() {
        return dimension != null && origin != null && Double.isFinite(origin.x) && Double.isFinite(origin.y)
                && Double.isFinite(origin.z) && tick >= 0 && pose != null && pose.valid();
    }
    public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> org.example.maniacrevolution.warden.client.WardenVisionPrototype.acceptShriekerEcho(this)));
        context.get().setPacketHandled(true);
    }
}
