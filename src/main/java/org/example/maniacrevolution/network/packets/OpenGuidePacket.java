package org.example.maniacrevolution.network.packets;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import org.example.maniacrevolution.event.FirstJoinHandler;
import org.example.maniacrevolution.gui.pages.GuidePage;

import java.util.function.Supplier;

public class OpenGuidePacket {
    private final GuidePage.PageType pageType;

    public OpenGuidePacket(GuidePage.PageType pageType) {
        this.pageType = pageType;
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeEnum(pageType);
    }

    public static OpenGuidePacket decode(FriendlyByteBuf buffer) {
        return new OpenGuidePacket(buffer.readEnum(GuidePage.PageType.class));
    }

    public boolean handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            // Открываем гайд на клиенте
            FirstJoinHandler.ClientHandler.openGuide(pageType);
        });
        context.setPacketHandled(true);
        return true;
    }
}