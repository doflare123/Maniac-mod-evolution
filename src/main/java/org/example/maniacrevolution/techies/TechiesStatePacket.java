package org.example.maniacrevolution.techies;

import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

/** Operation 0: clear cache; 1: upsert authorised mine; 2: remove; 3: owner HUD. */
public record TechiesStatePacket(int operation, ResourceLocation dimension, long tick, int id,
        int type, BlockPos pos, long started, int selected, long red, long green, long blue) {
    public void encode(FriendlyByteBuf b) {
        b.writeVarInt(operation); b.writeResourceLocation(dimension); b.writeLong(tick); b.writeVarInt(id);
        b.writeVarInt(type); b.writeBlockPos(pos); b.writeLong(started); b.writeVarInt(selected);
        b.writeLong(red); b.writeLong(green); b.writeLong(blue);
    }
    public static TechiesStatePacket decode(FriendlyByteBuf b) {
        return new TechiesStatePacket(b.readVarInt(),b.readResourceLocation(),b.readLong(),b.readVarInt(),
                b.readVarInt(),b.readBlockPos(),b.readLong(),b.readVarInt(),b.readLong(),b.readLong(),b.readLong());
    }
    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> org.example.maniacrevolution.techies.client.TechiesClient.accept(this)));
        ctx.get().setPacketHandled(true);
    }
}
