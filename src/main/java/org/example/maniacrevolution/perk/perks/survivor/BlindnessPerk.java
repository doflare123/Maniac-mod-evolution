package org.example.maniacrevolution.perk.perks.survivor;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.AABB;
import org.example.maniacrevolution.perk.*;

import java.util.List;

/**
 * Слепота (Гибрид для выжившего) (Охота/Мидгейм)
 * Пассивная: вокруг даёт слепоту всем маньякам (радиус - 2 блока, время - 0-1 сек.)
 * Активная: Даёт слепоту всем маньякам на карте (кд 120 сек)
 */
public class BlindnessPerk extends Perk {

    private static final double PASSIVE_RADIUS = 2.0;
    private static final int PASSIVE_DURATION_TICKS = 60; // 1 секунда
    private static final int ACTIVE_DURATION_TICKS = 200; // 1 секунда
    private static final int COOLDOWN_SECONDS = 120;

    public BlindnessPerk() {
        super(new Builder("blindness")
                .type(PerkType.HYBRID)
                .team(PerkTeam.SURVIVOR)
                .phases(PerkPhase.HUNT, PerkPhase.MIDGAME)
                .cooldown(COOLDOWN_SECONDS)
        );
    }

    @Override
    public void onTick(ServerPlayer player) {
        // Пассивный эффект: слепота маньякам в радиусе 2 блоков
        applyBlindnessAround(player, PASSIVE_RADIUS, PASSIVE_DURATION_TICKS);
    }

    @Override
    public void onActivate(ServerPlayer player) {
        // Активный эффект: слепота всем маньякам на карте
        applyBlindnessToAllManiacs(player, ACTIVE_DURATION_TICKS);
    }

    /**
     * Применяет слепоту маньякам в радиусе вокруг игрока
     */
    private void applyBlindnessAround(ServerPlayer player, double radius, int durationTicks) {
        if (player.gameMode.getGameModeForPlayer() != GameType.ADVENTURE) return;
        ServerLevel level = player.serverLevel();

        // Создаем AABB для поиска игроков в радиусе
        AABB searchBox = new AABB(
                player.getX() - radius, player.getY() - radius, player.getZ() - radius,
                player.getX() + radius, player.getY() + radius, player.getZ() + radius
        );

        // Находим всех игроков в радиусе
        List<ServerPlayer> nearbyPlayers = level.getPlayers(
                p -> p != player && p.position().distanceTo(player.position()) <= radius
        );

        // Применяем слепоту только маньякам
        for (ServerPlayer target : nearbyPlayers) {
            if (isManiac(target)) {
                target.addEffect(new MobEffectInstance(
                        MobEffects.BLINDNESS,
                        durationTicks,
                        0, // Уровень эффекта (0 = уровень 1)
                        false, // ambient
                        false  // showParticles
                ));
            }
        }
    }

    /**
     * Применяет слепоту всем маньякам на карте
     */
    private void applyBlindnessToAllManiacs(ServerPlayer player, int durationTicks) {
        ServerLevel level = player.serverLevel();

        // Находим всех игроков на сервере
        for (ServerPlayer target : level.getServer().getPlayerList().getPlayers()) {
            if (target != player && isManiac(target)) {
                target.addEffect(new MobEffectInstance(
                        MobEffects.BLINDNESS,
                        durationTicks,
                        0,
                        false,
                        false
                ));
            }
        }
    }

    /**
     * Проверяет, является ли игрок маньяком
     */
    private boolean isManiac(ServerPlayer player) {
        PerkTeam team = PerkTeam.fromPlayer(player);
        return team == PerkTeam.MANIAC;
    }
}