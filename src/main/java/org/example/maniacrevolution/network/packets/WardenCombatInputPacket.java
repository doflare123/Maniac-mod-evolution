package org.example.maniacrevolution.network.packets;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;
import org.example.maniacrevolution.warden.WardenCombatManager;

public record WardenCombatInputPacket(Action action, int token) {
    public enum Action { MELEE, START, RELEASE, CANCEL, KEEP_ALIVE }
    public void encode(FriendlyByteBuf buf) { buf.writeEnum(action); buf.writeVarInt(token); }
    public static WardenCombatInputPacket decode(FriendlyByteBuf buf) { return new WardenCombatInputPacket(buf.readEnum(Action.class), buf.readVarInt()); }
    public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> { var player = context.get().getSender(); if (player != null) WardenCombatManager.input(player, this); });
        context.get().setPacketHandled(true);
    }
}
