package org.example.maniacrevolution.network.packets;

import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import org.example.maniacrevolution.scp173.client.Scp173GameplayClient;

public record Scp173StatusPacket(ResourceLocation dimension, long tick, boolean blinkEnabled, int blink,
        double meter, double pressure, int blackout, boolean statue, boolean held,
        int melee, double light, float cost, int impact, int lightDuration) {
    public void encode(FriendlyByteBuf b) {
        b.writeResourceLocation(dimension); b.writeLong(tick); b.writeBoolean(blinkEnabled); b.writeVarInt(blink);
        b.writeDouble(meter); b.writeDouble(pressure); b.writeVarInt(blackout); b.writeBoolean(statue); b.writeBoolean(held);
        b.writeVarInt(melee); b.writeDouble(light); b.writeFloat(cost); b.writeVarInt(impact);
        b.writeVarInt(lightDuration);
    }
    public static Scp173StatusPacket decode(FriendlyByteBuf b) {
        return new Scp173StatusPacket(b.readResourceLocation(), b.readLong(), b.readBoolean(), b.readVarInt(),
                b.readDouble(), b.readDouble(), b.readVarInt(), b.readBoolean(), b.readBoolean(),
                b.readVarInt(), b.readDouble(), b.readFloat(), b.readVarInt(), b.readVarInt());
    }
    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> Scp173GameplayClient.accept(this)));
        ctx.get().setPacketHandled(true);
    }
}
