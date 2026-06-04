package org.example.maniacrevolution.network.packets;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraftforge.network.NetworkEvent;
import org.example.maniacrevolution.nightmare.ClientNightmareData;
import org.example.maniacrevolution.sound.ModSounds;

import java.util.function.Supplier;

public class NightmareScreamerPacket {
    private final int durationTicks;

    public NightmareScreamerPacket(int durationTicks) {
        this.durationTicks = durationTicks;
    }

    public static void encode(NightmareScreamerPacket packet, FriendlyByteBuf buf) {
        buf.writeVarInt(packet.durationTicks);
    }

    public static NightmareScreamerPacket decode(FriendlyByteBuf buf) {
        return new NightmareScreamerPacket(buf.readVarInt());
    }

    public static void handle(NightmareScreamerPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ClientNightmareData.showScreamer(packet.durationTicks);
            if (Minecraft.getInstance().player != null) {
                Minecraft.getInstance().player.playSound(ModSounds.SCREAM_AUDIO.get(), 1.0F, 1.0F);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
