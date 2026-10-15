package org.example.maniacrevolution.network.packets;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

/** Sent only for an attack actually accepted by the server, including a miss. */
public record WardenMeleeFeedbackPacket(ResourceLocation dimension, long tick, boolean hit) {
    public void encode(FriendlyByteBuf b) { b.writeResourceLocation(dimension); b.writeLong(tick); b.writeBoolean(hit); }
    public static WardenMeleeFeedbackPacket decode(FriendlyByteBuf b) { return new WardenMeleeFeedbackPacket(b.readResourceLocation(), b.readLong(), b.readBoolean()); }
    public boolean valid() { return dimension != null && tick >= 0; }
    public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> org.example.maniacrevolution.warden.client.WardenCombatClient.acceptFeedback(this)));
        context.get().setPacketHandled(true);
    }
}
