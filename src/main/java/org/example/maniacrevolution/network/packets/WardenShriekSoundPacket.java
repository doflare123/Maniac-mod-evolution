package org.example.maniacrevolution.network.packets;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

/** Audible to nearby players of any class, separately from the private Warden echo. */
public record WardenShriekSoundPacket(ResourceLocation dimension, BlockPos pos, boolean start) {
    public void encode(FriendlyByteBuf buf) { buf.writeResourceLocation(dimension); buf.writeBlockPos(pos); buf.writeBoolean(start); }
    public static WardenShriekSoundPacket decode(FriendlyByteBuf buf) { return new WardenShriekSoundPacket(buf.readResourceLocation(), buf.readBlockPos(), buf.readBoolean()); }
    public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> org.example.maniacrevolution.warden.client.WardenShriekerSounds.accept(this)));
        context.get().setPacketHandled(true);
    }
}
