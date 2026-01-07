package org.example.maniacrevolution.network.packets;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import org.example.maniacrevolution.mana.ClientManaData;

import java.util.function.Supplier;

public class SyncManaPacket {
    private final float mana;
    private final float maxMana;
    private final float regenRate;

    public SyncManaPacket(float mana, float maxMana, float regenRate) {
        this.mana = mana;
        this.maxMana = maxMana;
        this.regenRate = regenRate;
    }

    public static void encode(SyncManaPacket packet, FriendlyByteBuf buf) {
        buf.writeFloat(packet.mana);
        buf.writeFloat(packet.maxMana);
        buf.writeFloat(packet.regenRate);
    }

    public static SyncManaPacket decode(FriendlyByteBuf buf) {
        return new SyncManaPacket(buf.readFloat(), buf.readFloat(), buf.readFloat());
    }

    public static void handle(SyncManaPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            // CLIENT SIDE
            ClientManaData.set(packet.mana, packet.maxMana, packet.regenRate);
        });
        ctx.get().setPacketHandled(true);
    }
}