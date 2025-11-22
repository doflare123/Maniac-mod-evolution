package org.example.maniacrevolution.network.packets;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import org.example.maniacrevolution.data.PlayerData;
import org.example.maniacrevolution.data.PlayerDataManager;

import java.util.function.Supplier;

public class ApplyPresetPacket {
    private final int presetIndex;

    public ApplyPresetPacket(int index) {
        this.presetIndex = index;
    }

    public ApplyPresetPacket(FriendlyByteBuf buf) {
        this.presetIndex = buf.readInt();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(presetIndex);
    }

    public static ApplyPresetPacket decode(FriendlyByteBuf buf) {
        return new ApplyPresetPacket(buf);
    }

    public boolean handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            PlayerData data = PlayerDataManager.get(player);

            if (data.applyPreset(presetIndex, player)) {
                player.displayClientMessage(
                        Component.literal("§aПресет применён!"), false);
            } else {
                player.displayClientMessage(
                        Component.literal("§cПресет не найден!"), true);
            }

            PlayerDataManager.syncToClient(player);
        });
        ctx.get().setPacketHandled(true);
        return true;
    }
}
