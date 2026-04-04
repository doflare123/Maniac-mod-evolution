package org.example.maniacrevolution.network.packets;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class OpenMedicTabletPacket {

    public OpenMedicTabletPacket() {}

    public void encode(FriendlyByteBuf buffer) {}

    public static OpenMedicTabletPacket decode(FriendlyByteBuf buffer) {
        return new OpenMedicTabletPacket();
    }

    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        contextSupplier.get().enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () ->
                        org.example.maniacrevolution.client.ClientScreenHelper::openMedicTabletScreen)
        );
        contextSupplier.get().setPacketHandled(true);
    }
}