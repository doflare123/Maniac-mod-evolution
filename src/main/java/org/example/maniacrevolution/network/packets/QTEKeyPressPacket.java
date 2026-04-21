package org.example.maniacrevolution.network.packets;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import org.example.maniacrevolution.Config;
import org.example.maniacrevolution.hack.HackConfig;
import org.example.maniacrevolution.hack.HackManager;
import org.example.maniacrevolution.perk.perks.maniac.CatchMistakesPerk;
import org.example.maniacrevolution.perk.perks.survivor.IdealychPerk;
import org.example.maniacrevolution.util.ScoreboardUtil;

import java.util.function.Supplier;

/**
 * Пакет нажатия клавиши QTE. Client → Server.
 *
 * Изменение: при успешном QTE теперь добавляет бонус к взлому компьютера
 * (HackConfig.QTE_SUCCESS_BONUS очков), если игрок участвует в активной сессии.
 */
public class QTEKeyPressPacket {
    private final int keyIndex;
    private final int generatorNumber;
    private final boolean success;
    private final boolean critical;

    public QTEKeyPressPacket(int keyIndex, int generatorNumber, boolean success) {
        this(keyIndex, generatorNumber, success, false);
    }

    public QTEKeyPressPacket(int keyIndex, int generatorNumber, boolean success, boolean critical) {
        this.keyIndex = keyIndex;
        this.generatorNumber = generatorNumber;
        this.success = success;
        this.critical = critical;
    }

    public static void encode(QTEKeyPressPacket packet, FriendlyByteBuf buf) {
        buf.writeInt(packet.keyIndex);
        buf.writeInt(packet.generatorNumber);
        buf.writeBoolean(packet.success);
        buf.writeBoolean(packet.critical);
    }

    public static QTEKeyPressPacket decode(FriendlyByteBuf buf) {
        return new QTEKeyPressPacket(buf.readInt(), buf.readInt(), buf.readBoolean(), buf.readBoolean());
    }

    public static void handle(QTEKeyPressPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            if (packet.success) {
                int rewardAmount = Config.getHackQTEReward();
                ScoreboardUtil.addHackProgress(player, packet.generatorNumber, rewardAmount);
                applyComputerHackBonus(player, packet.critical);

                // Идеалыч
                if (packet.critical) {
                    IdealychPerk.onCriticalHit(player);
                } else {
                    IdealychPerk.onNormalHit(player);
                }
            } else {
                boolean perkActivated = CatchMistakesPerk.onQTEFailed(player);
                // Идеалыч
                IdealychPerk.onMiss(player);
            }
        });
        ctx.get().setPacketHandled(true);
    }

    /**
     * Добавляет QTE_SUCCESS_BONUS к активной сессии взлома компьютера,
     * если игрок является хакером или помощником.
     */
    private static void applyComputerHackBonus(ServerPlayer player, boolean critical) {
        HackManager.get().applyQTEBonus(player, critical);
    }
}
