package org.example.maniacrevolution.network.packets;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import org.example.maniacrevolution.nightmare.ClientNightmareData;
import org.example.maniacrevolution.nightmare.NightmareTrialType;

import java.util.function.Supplier;

public class SyncNightmarePacket {
    private final boolean visible;
    private final float sanity;
    private final float maxSanity;
    private final NightmareTrialType trialType;
    private final int trialSecondsLeft;

    public SyncNightmarePacket(boolean visible, float sanity, float maxSanity,
                               NightmareTrialType trialType, int trialSecondsLeft) {
        this.visible = visible;
        this.sanity = sanity;
        this.maxSanity = maxSanity;
        this.trialType = trialType;
        this.trialSecondsLeft = trialSecondsLeft;
    }

    public static void encode(SyncNightmarePacket packet, FriendlyByteBuf buf) {
        buf.writeBoolean(packet.visible);
        buf.writeFloat(packet.sanity);
        buf.writeFloat(packet.maxSanity);
        buf.writeEnum(packet.trialType);
        buf.writeVarInt(packet.trialSecondsLeft);
    }

    public static SyncNightmarePacket decode(FriendlyByteBuf buf) {
        return new SyncNightmarePacket(
                buf.readBoolean(),
                buf.readFloat(),
                buf.readFloat(),
                buf.readEnum(NightmareTrialType.class),
                buf.readVarInt()
        );
    }

    public static void handle(SyncNightmarePacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> ClientNightmareData.update(
                packet.visible,
                packet.sanity,
                packet.maxSanity,
                packet.trialType,
                packet.trialSecondsLeft
        ));
        ctx.get().setPacketHandled(true);
    }
}
