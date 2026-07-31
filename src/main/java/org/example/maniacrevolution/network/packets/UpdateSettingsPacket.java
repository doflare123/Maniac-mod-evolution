package org.example.maniacrevolution.network.packets;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import org.example.maniacrevolution.command.ApplySettingsCommand;
import org.example.maniacrevolution.data.PlayerData;
import org.example.maniacrevolution.data.PlayerDataManager;
import org.example.maniacrevolution.hack.HackConfig;
import org.example.maniacrevolution.network.ModNetworking;
import org.example.maniacrevolution.settings.GameSettings;

import java.util.function.Supplier;

public class UpdateSettingsPacket {
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

    public UpdateSettingsPacket(int hpBoost,
                                int maniacCount, int gameTime, int selectedMap,
                                float hackPointsRequired, float pointsPerPlayer,
                                float pointsPerSpecialist, int maxBonusPlayers,
                                float hackerRadius, float supportRadius,
                                int qteIntervalMin, int qteIntervalMax,
                                float qteSuccessBonus, float qteCritBonus,
                                int computersNeededForWin,
                                boolean threePerksEnabled) {
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
    }

    public static void encode(UpdateSettingsPacket msg, FriendlyByteBuf buf) {
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
    }

    public static UpdateSettingsPacket decode(FriendlyByteBuf buf) {
        return new UpdateSettingsPacket(
                buf.readInt(),
                buf.readInt(), buf.readInt(), buf.readInt(),
                buf.readFloat(), buf.readFloat(), buf.readFloat(),
                buf.readInt(), buf.readFloat(), buf.readFloat(),
                buf.readInt(), buf.readInt(),
                buf.readFloat(), buf.readFloat(),
                buf.readInt(),
                buf.readBoolean()
        );
    }

    public static void handle(UpdateSettingsPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null || !player.hasPermissions(2)) return;

            GameSettings s = GameSettings.get(player.server);
            boolean wasThreePerksEnabled = s.isThreePerksEnabled();

            // Игра
            s.setHpBoost(msg.hpBoost);
            s.setManiacCount(msg.maniacCount);
            s.setGameTime(msg.gameTime);
            s.setSelectedMap(msg.selectedMap);

            // Компьютеры — сохраняем в GameSettings и применяем в HackConfig
            s.setHackPointsRequired(msg.hackPointsRequired);
            s.setPointsPerPlayer(msg.pointsPerPlayer);
            s.setPointsPerSpecialist(msg.pointsPerSpecialist);
            s.setMaxBonusPlayers(msg.maxBonusPlayers);
            s.setHackerRadius(msg.hackerRadius);
            s.setSupportRadius(msg.supportRadius);
            s.setQteIntervalMin(msg.qteIntervalMin);
            s.setQteIntervalMax(msg.qteIntervalMax);
            s.setQteSuccessBonus(msg.qteSuccessBonus);
            s.setQteCritBonus(msg.qteCritBonus);
            s.setComputersNeededForWin(msg.computersNeededForWin);
            s.setThreePerksEnabled(msg.threePerksEnabled);

            if (wasThreePerksEnabled && !msg.threePerksEnabled) {
                for (ServerPlayer onlinePlayer : player.server.getPlayerList().getPlayers()) {
                    PlayerData data = PlayerDataManager.get(onlinePlayer);
                    data.trimPerksToLimit(onlinePlayer);
                    PlayerDataManager.syncToClient(onlinePlayer);
                }
            }

            // Применяем в HackConfig сразу
            HackConfig.HACK_POINTS_REQUIRED          = msg.hackPointsRequired;
            HackConfig.POINTS_PER_PLAYER_PER_SECOND  = msg.pointsPerPlayer;
            HackConfig.POINTS_PER_SPECIALIST_PER_SECOND = msg.pointsPerSpecialist;
            HackConfig.MAX_BONUS_PLAYERS             = msg.maxBonusPlayers;
            HackConfig.HACKER_RADIUS                 = msg.hackerRadius;
            HackConfig.SUPPORT_RADIUS                = msg.supportRadius;
            HackConfig.QTE_INTERVAL_MIN_SECONDS      = msg.qteIntervalMin;
            HackConfig.QTE_INTERVAL_MAX_SECONDS      = msg.qteIntervalMax;
            HackConfig.QTE_SUCCESS_BONUS             = msg.qteSuccessBonus;
            HackConfig.QTE_CRIT_BONUS                = msg.qteCritBonus;
            HackConfig.COMPUTERS_NEEDED_FOR_WIN      = msg.computersNeededForWin;

            // Применяем и основные настройки сразу, чтобы кнопка "Применить" работала полностью.
            ApplySettingsCommand.applySettings(player.server, true);

            // Лимит перков влияет на клиентские экраны и HUD всех игроков.
            for (ServerPlayer onlinePlayer : player.server.getPlayerList().getPlayers()) {
                ModNetworking.sendToPlayer(SyncSettingsPacket.from(s), onlinePlayer);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
