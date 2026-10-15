package org.example.maniacrevolution.network.packets;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import org.example.maniacrevolution.warden.WardenAnimationTimeline;
import java.util.UUID;
import java.util.function.Supplier;

public record WardenAnimationPacket(ResourceLocation dimension, UUID player, WardenAnimationTimeline.State state) {
    public boolean valid() { return dimension != null && player != null && state != null && state.valid(); }
    public void encode(FriendlyByteBuf b) {
        b.writeResourceLocation(dimension); b.writeUUID(player); b.writeEnum(state.action()); b.writeLong(state.begin());
        b.writeVarInt(state.charge()); b.writeLong(state.sniff()); b.writeLong(state.tick());
    }
    public static WardenAnimationPacket decode(FriendlyByteBuf b) {
        return new WardenAnimationPacket(b.readResourceLocation(), b.readUUID(), new WardenAnimationTimeline.State(
                b.readEnum(WardenAnimationTimeline.Action.class), b.readLong(), b.readVarInt(), b.readLong(), b.readLong()));
    }
    public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> org.example.maniacrevolution.warden.client.WardenAnimationsClient.accept(this)));
        context.get().setPacketHandled(true);
    }
}
