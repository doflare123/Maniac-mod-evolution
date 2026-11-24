package org.example.maniacrevolution.network.packets;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import org.example.maniacrevolution.stamina.Stamina;
import org.example.maniacrevolution.stamina.client.ClientStamina;

import java.util.function.Supplier;

public record StaminaSyncPacket(int playerId, int amount, boolean denied) {
    public void encode(FriendlyByteBuf buffer) {
        buffer.writeVarInt(playerId);
        buffer.writeVarInt(amount);
        buffer.writeBoolean(denied);
    }

    public static StaminaSyncPacket decode(FriendlyByteBuf buffer) {
        return new StaminaSyncPacket(buffer.readVarInt(), buffer.readVarInt(), buffer.readBoolean());
    }

    public void handle(Supplier<NetworkEvent.Context> context) {
        if (amount >= 0 && amount <= Stamina.MAX) {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientStamina.accept(this));
        }
        context.get().setPacketHandled(true);
    }
}
