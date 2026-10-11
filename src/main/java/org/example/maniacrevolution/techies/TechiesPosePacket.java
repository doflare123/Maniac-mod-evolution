package org.example.maniacrevolution.techies;

import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

/** Only public hand animation; never contains a hidden mine position. */
public record TechiesPosePacket(UUID player,long tick,int type,long start,long press) {
    public void encode(FriendlyByteBuf b) { b.writeUUID(player); b.writeLong(tick); b.writeVarInt(type); b.writeLong(start); b.writeLong(press); }
    public static TechiesPosePacket decode(FriendlyByteBuf b) { return new TechiesPosePacket(b.readUUID(),b.readLong(),b.readVarInt(),b.readLong(),b.readLong()); }
    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> org.example.maniacrevolution.techies.client.TechiesHands.accept(this)));
        ctx.get().setPacketHandled(true);
    }
}
