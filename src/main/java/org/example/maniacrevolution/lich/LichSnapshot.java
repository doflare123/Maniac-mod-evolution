package org.example.maniacrevolution.lich;

import java.util.function.Supplier;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

/** Paged snapshots avoid a packet size limit becoming a gameplay army limit. */
public record LichSnapshot(CompoundTag data) {
    public static void encode(LichSnapshot p, FriendlyByteBuf b) { b.writeNbt(p.data); }
    public static LichSnapshot decode(FriendlyByteBuf b) { return new LichSnapshot(b.readNbt()); }
    public static void handle(LichSnapshot p, Supplier<NetworkEvent.Context> c) {
        var ctx = c.get(); ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> org.example.maniacrevolution.lich.client.LichClient.accept(p.data))); ctx.setPacketHandled(true);
    }
}
