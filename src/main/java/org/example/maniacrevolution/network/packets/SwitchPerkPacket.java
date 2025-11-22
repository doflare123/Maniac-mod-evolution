package org.example.maniacrevolution.network.packets;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import org.example.maniacrevolution.data.PlayerData;
import org.example.maniacrevolution.data.PlayerDataManager;

import java.util.function.Supplier;

public class SwitchPerkPacket {
    public SwitchPerkPacket() {}

    public void encode(FriendlyByteBuf buf) {}

    public static SwitchPerkPacket decode(FriendlyByteBuf buf) {
        return new SwitchPerkPacket();
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            PlayerData data = PlayerDataManager.get(player);
            data.switchActivePerk();
            PlayerDataManager.syncToClient(player);
        });
        ctx.get().setPacketHandled(true);
    }
}
