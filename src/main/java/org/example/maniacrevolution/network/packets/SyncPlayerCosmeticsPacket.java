package org.example.maniacrevolution.network.packets;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import org.example.maniacrevolution.cosmetic.client.ClientCosmeticCache;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Пакет для синхронизации косметики других игроков на клиент
 * Отправляется при входе игрока и при изменении косметики
 */
public class SyncPlayerCosmeticsPacket {
    private final UUID playerUuid;
    private final Set<String> enabledCosmetics;

    public SyncPlayerCosmeticsPacket(UUID playerUuid, Set<String> enabledCosmetics) {
        this.playerUuid = playerUuid;
        this.enabledCosmetics = enabledCosmetics;
    }

    public SyncPlayerCosmeticsPacket(FriendlyByteBuf buf) {
        this.playerUuid = buf.readUUID();

        int count = buf.readVarInt();
        this.enabledCosmetics = new HashSet<>();
        for (int i = 0; i < count; i++) {
            this.enabledCosmetics.add(buf.readUtf(256));
        }
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUUID(playerUuid);
        buf.writeVarInt(enabledCosmetics.size());
        for (String cosmetic : enabledCosmetics) {
            buf.writeUtf(cosmetic, 256);
        }
    }

    public static SyncPlayerCosmeticsPacket decode(FriendlyByteBuf buf) {
        return new SyncPlayerCosmeticsPacket(buf);
    }

    public boolean handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            // Обновляем кэш на клиенте
            ClientCosmeticCache.updatePlayerCosmetics(playerUuid, enabledCosmetics);
        });
        ctx.get().setPacketHandled(true);
        return true;
    }
}