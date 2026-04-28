package org.example.maniacrevolution.network.packets;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import org.example.maniacrevolution.util.ClientOnlyExecutor;

import java.util.function.Supplier;

public class OpenGuidePacket {
    private final int pageTypeId;

    public OpenGuidePacket(String pageTypeName) {
        this.pageTypeId = pageTypeId(pageTypeName);
    }

    public OpenGuidePacket(int pageTypeId) {
        this.pageTypeId = pageTypeId;
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeVarInt(pageTypeId);
    }

    public static OpenGuidePacket decode(FriendlyByteBuf buffer) {
        return new OpenGuidePacket(buffer.readVarInt());
    }

    public boolean handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> ClientOnlyExecutor.openGuidePage(pageTypeId));
        context.setPacketHandled(true);
        return true;
    }

    private static int pageTypeId(String pageTypeName) {
        return switch (pageTypeName) {
            case "PERKS" -> 1;
            case "TUTORIAL" -> 2;
            case "MAPS" -> 3;
            case "CHARACTERS" -> 4;
            default -> 0;
        };
    }
}
