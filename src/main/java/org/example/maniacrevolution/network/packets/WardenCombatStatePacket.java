package org.example.maniacrevolution.network.packets;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

public record WardenCombatStatePacket(ResourceLocation dimension, long tick, int token, boolean charging, int charge, int melee, int wave) {
    public void encode(FriendlyByteBuf b) { b.writeResourceLocation(dimension); b.writeLong(tick); b.writeVarInt(token); b.writeBoolean(charging); b.writeVarInt(charge); b.writeVarInt(melee); b.writeVarInt(wave); }
    public static WardenCombatStatePacket decode(FriendlyByteBuf b) { return new WardenCombatStatePacket(b.readResourceLocation(), b.readLong(), b.readVarInt(), b.readBoolean(), b.readVarInt(), b.readVarInt(), b.readVarInt()); }
    public boolean valid() { return dimension != null && tick >= 0 && token >= 0 && charge >= 0 && charge <= 20 && melee >= 0 && melee <= 100 && wave >= 0 && wave <= 600 && (charging || charge == 0); }
    public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> org.example.maniacrevolution.warden.client.WardenCombatClient.accept(this)));
        context.get().setPacketHandled(true);
    }
}
