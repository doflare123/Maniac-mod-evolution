package org.example.maniacrevolution.network.packets;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import org.example.maniacrevolution.character.CharacterType;
import org.example.maniacrevolution.data.ClientPlayerData;

import java.util.function.Supplier;

/**
 * Сервер → клиент: сообщает игроку его выбранный класс.
 * Отправляется после SelectCharacterPacket и при логине (если класс уже был выбран).
 */
public class SyncPlayerClassPacket {

    private final CharacterType type;
    private final int classId;

    public SyncPlayerClassPacket(CharacterType type, int classId) {
        this.type    = type;
        this.classId = classId;
    }

    public static void encode(SyncPlayerClassPacket p, FriendlyByteBuf buf) {
        buf.writeEnum(p.type);
        buf.writeVarInt(p.classId);
    }

    public static SyncPlayerClassPacket decode(FriendlyByteBuf buf) {
        CharacterType type = buf.readEnum(CharacterType.class);
        int classId = buf.readVarInt();
        return new SyncPlayerClassPacket(type, classId);
    }

    public static void handle(SyncPlayerClassPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                    if (packet.type == CharacterType.MANIAC) {
                        ClientPlayerData.setManiacClass(packet.classId); // -1 = нет класса
                    } else {
                        ClientPlayerData.setSurvivorClass(packet.classId);
                    }
                })
        );
        ctx.get().setPacketHandled(true);
    }
}