package org.example.maniacrevolution.network.packets;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import org.example.maniacrevolution.cosmetic.CosmeticSyncHandler;
import org.example.maniacrevolution.data.PlayerData;
import org.example.maniacrevolution.data.PlayerDataManager;

import java.util.function.Supplier;

public class ToggleCosmeticPacket {
    private final String cosmeticId;

    public ToggleCosmeticPacket(String cosmeticId) {
        this.cosmeticId = cosmeticId;
    }

    public ToggleCosmeticPacket(FriendlyByteBuf buf) {
        this.cosmeticId = buf.readUtf(256);
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(cosmeticId, 256);
    }

    public static ToggleCosmeticPacket decode(FriendlyByteBuf buf) {
        return new ToggleCosmeticPacket(buf);
    }

    public boolean handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            PlayerData data = PlayerDataManager.get(player);
            data.getCosmeticData().toggleEnabled(cosmeticId);

            boolean enabled = data.getCosmeticData().isEnabled(cosmeticId);
            String status = enabled ? "§aвключён" : "§cвыключен";
            player.displayClientMessage(
                    Component.literal("Эффект " + status), true);

            // Синхронизируем с клиентом самого игрока
            PlayerDataManager.syncToClient(player);

            // НОВОЕ: Синхронизируем косметику всем остальным игрокам
            CosmeticSyncHandler.syncCosmeticsToAll(player);
        });
        ctx.get().setPacketHandled(true);
        return true;
    }
}