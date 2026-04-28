package org.example.maniacrevolution.network.packets;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import org.example.maniacrevolution.gui.PerkSelectionScreen;

import java.util.function.Supplier;

public class ClosePerkScreenPacket {

    public ClosePerkScreenPacket() {}

    public ClosePerkScreenPacket(FriendlyByteBuf buf) {}

    public void encode(FriendlyByteBuf buf) {}

    public static ClosePerkScreenPacket decode(FriendlyByteBuf buf) {
        return new ClosePerkScreenPacket();
    }

    public static void handle(ClosePerkScreenPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.screen instanceof PerkSelectionScreen) {
                mc.setScreen(null);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}