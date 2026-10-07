package org.example.maniacrevolution.strange;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

public record StrangeActionPacket(Action action) {
    public enum Action { STANCE, WHIP, MARK, PORTAL, CAPTURE }
    public void encode(FriendlyByteBuf buffer) { buffer.writeEnum(action); }
    public static StrangeActionPacket decode(FriendlyByteBuf buffer) { return new StrangeActionPacket(buffer.readEnum(Action.class)); }
    public void handle(Supplier<NetworkEvent.Context> supplier) {
        var context = supplier.get();
        context.enqueueWork(() -> {
            if (context.getSender() != null) StrangeManager.action(context.getSender(), action);
        });
        context.setPacketHandled(true);
    }
}
