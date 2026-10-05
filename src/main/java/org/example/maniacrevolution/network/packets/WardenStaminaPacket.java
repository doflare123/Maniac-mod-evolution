package org.example.maniacrevolution.network.packets;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import org.example.maniacrevolution.warden.WardenStamina;
import java.util.function.Supplier;

public record WardenStaminaPacket(ResourceLocation dimension, long tick, int amount, int delay, boolean boosting, boolean exhausted) {
    public boolean valid() {
        return dimension != null && tick >= 0 && amount >= 0 && amount <= WardenStamina.CAPACITY
                && delay >= 0 && delay <= WardenStamina.DELAY && (!boosting || amount > 0 && !exhausted);
    }
    public void encode(FriendlyByteBuf b) {
        b.writeResourceLocation(dimension); b.writeLong(tick); b.writeVarInt(amount); b.writeVarInt(delay);
        b.writeBoolean(boosting); b.writeBoolean(exhausted);
    }
    public static WardenStaminaPacket decode(FriendlyByteBuf b) {
        return new WardenStaminaPacket(b.readResourceLocation(), b.readLong(), b.readVarInt(), b.readVarInt(), b.readBoolean(), b.readBoolean());
    }
    public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> org.example.maniacrevolution.warden.client.WardenMovementClient.accept(this)));
        context.get().setPacketHandled(true);
    }
}
