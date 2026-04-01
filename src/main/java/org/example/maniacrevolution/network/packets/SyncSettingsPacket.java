package org.example.maniacrevolution.network.packets;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import org.example.maniacrevolution.settings.ClientGameSettings;

import java.util.function.Supplier;

public class SyncSettingsPacket {
    // Игра
    private final int   hpBoost;
    private final int   maniacCount;
    private final int   gameTime;
    private final int   selectedMap;
    // Компьютеры
    private final float hackPointsRequired;
    private final float pointsPerPlayer;
    private final float pointsPerSpecialist;
    private final int   maxBonusPlayers;
    private final float hackerRadius;
    private final float supportRadius;
    private final int   qteIntervalMin;
    private final int   qteIntervalMax;
    private final float qteSuccessBonus;
    private final float qteCritBonus;
    private final int   computersNeededForWin;

    public SyncSettingsPacket(int hpBoost,
                              int maniacCount, int gameTime, int selectedMap,
                              float hackPointsRequired, float pointsPerPlayer,
                              float pointsPerSpecialist, int maxBonusPlayers,
                              float hackerRadius, float supportRadius,
                              int qteIntervalMin, int qteIntervalMax,
                              float qteSuccessBonus, float qteCritBonus,
                              int computersNeededForWin) {
        this.hpBoost              = hpBoost;
        this.maniacCount          = maniacCount;
        this.gameTime             = gameTime;
        this.selectedMap          = selectedMap;
        this.hackPointsRequired   = hackPointsRequired;
        this.pointsPerPlayer      = pointsPerPlayer;
        this.pointsPerSpecialist  = pointsPerSpecialist;
        this.maxBonusPlayers      = maxBonusPlayers;
        this.hackerRadius         = hackerRadius;
        this.supportRadius        = supportRadius;
        this.qteIntervalMin       = qteIntervalMin;
        this.qteIntervalMax       = qteIntervalMax;
        this.qteSuccessBonus      = qteSuccessBonus;
        this.qteCritBonus         = qteCritBonus;
        this.computersNeededForWin = computersNeededForWin;
    }

    /** Удобный конструктор из GameSettings */
    public static SyncSettingsPacket from(org.example.maniacrevolution.settings.GameSettings s) {
        return new SyncSettingsPacket(
                s.getHpBoost(),
                s.getManiacCount(), s.getGameTime(), s.getSelectedMap(),
                s.getHackPointsRequired(), s.getPointsPerPlayer(),
                s.getPointsPerSpecialist(), s.getMaxBonusPlayers(),
                s.getHackerRadius(), s.getSupportRadius(),
                s.getQteIntervalMin(), s.getQteIntervalMax(),
                s.getQteSuccessBonus(), s.getQteCritBonus(),
                s.getComputersNeededForWin()
        );
    }

    public static void encode(SyncSettingsPacket msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.hpBoost);
        buf.writeInt(msg.maniacCount);
        buf.writeInt(msg.gameTime);
        buf.writeInt(msg.selectedMap);
        buf.writeFloat(msg.hackPointsRequired);
        buf.writeFloat(msg.pointsPerPlayer);
        buf.writeFloat(msg.pointsPerSpecialist);
        buf.writeInt(msg.maxBonusPlayers);
        buf.writeFloat(msg.hackerRadius);
        buf.writeFloat(msg.supportRadius);
        buf.writeInt(msg.qteIntervalMin);
        buf.writeInt(msg.qteIntervalMax);
        buf.writeFloat(msg.qteSuccessBonus);
        buf.writeFloat(msg.qteCritBonus);
        buf.writeInt(msg.computersNeededForWin);
    }

    public static SyncSettingsPacket decode(FriendlyByteBuf buf) {
        return new SyncSettingsPacket(
                buf.readInt(),
                buf.readInt(), buf.readInt(), buf.readInt(),
                buf.readFloat(), buf.readFloat(), buf.readFloat(),
                buf.readInt(), buf.readFloat(), buf.readFloat(),
                buf.readInt(), buf.readInt(),
                buf.readFloat(), buf.readFloat(),
                buf.readInt()
        );
    }

    public static void handle(SyncSettingsPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ClientGameSettings.setSettings(
                    msg.hpBoost,
                    msg.maniacCount, msg.gameTime, msg.selectedMap);
            ClientGameSettings.setComputerSettings(
                    msg.hackPointsRequired, msg.pointsPerPlayer,
                    msg.pointsPerSpecialist, msg.maxBonusPlayers,
                    msg.hackerRadius, msg.supportRadius,
                    msg.qteIntervalMin, msg.qteIntervalMax,
                    msg.qteSuccessBonus, msg.qteCritBonus,
                    msg.computersNeededForWin);
        });
        ctx.get().setPacketHandled(true);
    }
}
