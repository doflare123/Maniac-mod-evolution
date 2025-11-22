package org.example.maniacrevolution.network.packets;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import org.example.maniacrevolution.data.PlayerData;
import org.example.maniacrevolution.data.PlayerDataManager;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class SavePresetPacket {
    private final String name;
    private final List<String> perkIds;

    public SavePresetPacket(String name, List<String> perkIds) {
        this.name = name;
        this.perkIds = new ArrayList<>(perkIds);
    }

    public SavePresetPacket(FriendlyByteBuf buf) {
        this.name = buf.readUtf(64);
        int count = buf.readInt();
        this.perkIds = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            perkIds.add(buf.readUtf(64));
        }
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(name, 64);
        buf.writeInt(perkIds.size());
        for (String id : perkIds) {
            buf.writeUtf(id, 64);
        }
    }

    public static SavePresetPacket decode(FriendlyByteBuf buf) {
        return new SavePresetPacket(buf);
    }

    public boolean handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            PlayerData data = PlayerDataManager.get(player);

            if (data.createPreset(name, perkIds)) {
                player.displayClientMessage(
                        Component.literal("§aПресет '" + name + "' сохранён!"), false);
            } else {
                player.displayClientMessage(
                        Component.literal("§cНе удалось создать пресет. Слоты заполнены!"), true);
            }

            PlayerDataManager.syncToClient(player);
        });
        ctx.get().setPacketHandled(true);
        return true;
    }
}

