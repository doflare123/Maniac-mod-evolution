package org.example.maniacrevolution.network.packets;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import org.example.maniacrevolution.gui.ResurrectionScreen;

import java.util.function.Supplier;

public class OpenResurrectionGuiPacket {

    public OpenResurrectionGuiPacket() {
    }

    public OpenResurrectionGuiPacket(FriendlyByteBuf buf) {
        // Нет данных для чтения
    }

    public void toBytes(FriendlyByteBuf buf) {
        // Нет данных для записи
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context ctx = supplier.get();
        ctx.enqueueWork(() -> {
            // Открываем GUI на клиенте
            Minecraft.getInstance().setScreen(new ResurrectionScreen());
        });
        return true;
    }
}