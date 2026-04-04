package org.example.maniacrevolution.network.packets;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

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
        ctx.get().enqueueWork(() -> {
            String typeName = guiType.name();
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () ->
                    () -> org.example.maniacrevolution.client.ClientScreenHelper
                            .openGuiByType(typeName));
        });
        ctx.get().setPacketHandled(true);
    }

    public enum GuiType {
        PERK_SELECTION,
        SHOP,
        GUIDE
    }
}