package org.example.maniacrevolution.lich;

import java.util.function.Supplier;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

/** Small intent messages. All entity IDs, map IDs, resources and ownership are checked server-side. */
public record LichRequest(String action, CompoundTag data) {
    public static void encode(LichRequest p, FriendlyByteBuf b) { b.writeUtf(p.action, 32); b.writeNbt(p.data); }
    public static LichRequest decode(FriendlyByteBuf b) { return new LichRequest(b.readUtf(32), b.readNbt()); }
    public static void handle(LichRequest p, Supplier<NetworkEvent.Context> c) {
        var ctx = c.get(); ctx.enqueueWork(() -> {
            if (ctx.getSender() != null && p.data != null) LichManager.request(ctx.getSender(), p.action, p.data);
        }); ctx.setPacketHandled(true);
    }
}
