package org.example.maniacrevolution.network.packets;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import org.example.maniacrevolution.settings.ClientGameSettings;
import org.example.maniacrevolution.settings.GraphicsMode;

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
    // Эксперименты
    private final boolean threePerksEnabled;
    private final GraphicsMode graphicsMode;

    public SyncSettingsPacket(int hpBoost,
                              int maniacCount, int gameTime, int selectedMap,
                              float hackPointsRequired, float pointsPerPlayer,
                              float pointsPerSpecialist, int maxBonusPlayers,
                              float hackerRadius, float supportRadius,
                              int qteIntervalMin, int qteIntervalMax,
                              float qteSuccessBonus, float qteCritBonus,
                              int computersNeededForWin,
                              boolean threePerksEnabled, GraphicsMode graphicsMode) {
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
        this.threePerksEnabled    = threePerksEnabled;
        this.graphicsMode = graphicsMode == null ? GraphicsMode.NORMAL : graphicsMode;
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
                s.getComputersNeededForWin(),
                s.isThreePerksEnabled(), s.getGraphicsMode()
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
        buf.writeBoolean(msg.threePerksEnabled);
        buf.writeVarInt(msg.graphicsMode.id());
    }

    public static SyncSettingsPacket decode(FriendlyByteBuf buf) {
        return new SyncSettingsPacket(
                buf.readInt(),
                buf.readInt(), buf.readInt(), buf.readInt(),
                buf.readFloat(), buf.readFloat(), buf.readFloat(),
                buf.readInt(), buf.readFloat(), buf.readFloat(),
                buf.readInt(), buf.readInt(),
                buf.readFloat(), buf.readFloat(),
                buf.readInt(),
                buf.readBoolean(), GraphicsMode.fromId(buf.readVarInt())
        );
    }

    public static void handle(SyncSettingsPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(msg::applyToClient);
        ctx.get().setPacketHandled(true);
    }

    public void applyToClient() {
        ClientGameSettings.setSettings(hpBoost, maniacCount, gameTime, selectedMap);
        ClientGameSettings.setComputerSettings(
                hackPointsRequired, pointsPerPlayer, pointsPerSpecialist, maxBonusPlayers,
                hackerRadius, supportRadius, qteIntervalMin, qteIntervalMax,
                qteSuccessBonus, qteCritBonus, computersNeededForWin);
        ClientGameSettings.setExperimentSettings(threePerksEnabled);
        ClientGameSettings.setGraphicsMode(graphicsMode);
    }
}
