package org.example.maniacrevolution.network.packets;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import org.example.maniacrevolution.data.PlayerData;
import org.example.maniacrevolution.data.PlayerDataManager;
import org.example.maniacrevolution.perk.Perk;
import org.example.maniacrevolution.perk.PerkRegistry;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class SelectPerkPacket {
    private final List<String> perkIds;

    public SelectPerkPacket(List<String> perkIds) {
        this.perkIds = new ArrayList<>(perkIds);
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(perkIds.size());
        for (String id : perkIds) {
            buf.writeUtf(id);
        }
    }

    public static SelectPerkPacket decode(FriendlyByteBuf buf) {
        int count = buf.readInt();
        List<String> ids = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            ids.add(buf.readUtf());
        }
        return new SelectPerkPacket(ids);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            PlayerData data = PlayerDataManager.get(player);

            // Очищаем текущие перки
            data.clearPerks(player);

            // Добавляем новые (максимум 2)
            int added = 0;
            for (String id : perkIds) {
                if (added >= 2) break;

                Perk perk = PerkRegistry.getPerk(id);
                if (perk != null && data.selectPerk(perk, player)) {
                    added++;
                }
            }

            PlayerDataManager.syncToClient(player);
            player.displayClientMessage(
                    Component.literal("§aВыбрано перков: " + added), true);
        });
        ctx.get().setPacketHandled(true);
    }
}
