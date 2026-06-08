package org.example.maniacrevolution.network.packets;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import org.example.maniacrevolution.util.ClientOnlyExecutor;

import java.util.function.Supplier;

public class OpenGuiPacket {
    private final GuiType guiType;

    public OpenGuiPacket(GuiType type) {
        this.guiType = type;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeEnum(guiType);
    }

    public static OpenGuiPacket decode(FriendlyByteBuf buf) {
        return new OpenGuiPacket(buf.readEnum(GuiType.class));
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> ClientOnlyExecutor.openGuiByType(guiType.name()));
        ctx.get().setPacketHandled(true);
    }

    public enum GuiType {
        PERK_SELECTION,
        SHOP,
        GUIDE
    }
}
