package org.example.maniacrevolution.network.packets;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import org.example.maniacrevolution.gui.MedicTabletScreen;

import java.util.function.Supplier;

/**
 * Пакет для открытия GUI планшета медика
 * S -> C
 */
public class OpenMedicTabletPacket {

    public OpenMedicTabletPacket() {
    }

    public void encode(FriendlyByteBuf buffer) {
        // Пустой пакет
    }

    public static OpenMedicTabletPacket decode(FriendlyByteBuf buffer) {
        return new OpenMedicTabletPacket();
    }

    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            // Открываем GUI на клиенте
            Minecraft.getInstance().setScreen(new MedicTabletScreen());
        });
        context.setPacketHandled(true);
    }
}