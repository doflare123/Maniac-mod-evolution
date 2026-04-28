package org.example.maniacrevolution.network.packets;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import org.example.maniacrevolution.util.ClientOnlyExecutor;

import java.util.function.Supplier;

public class OpenMedicTabletPacket {

    public OpenMedicTabletPacket() {}

    public void encode(FriendlyByteBuf buffer) {}

    public static OpenMedicTabletPacket decode(FriendlyByteBuf buffer) {
        return new OpenMedicTabletPacket();
    }

    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        contextSupplier.get().enqueueWork(ClientOnlyExecutor::openMedicTabletScreen);
        contextSupplier.get().setPacketHandled(true);
    }
}
