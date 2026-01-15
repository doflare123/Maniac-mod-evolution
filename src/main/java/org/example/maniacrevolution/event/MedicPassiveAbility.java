package org.example.maniacrevolution.event;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.Team;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

/**
 * Пассивная способность медика: разделение урона с ближайшим союзником
 * Если медика атакуют и рядом (в радиусе 4 блоков) есть союзник,
 * урон делится поровну между медиком и одним союзником
 */
@Mod.EventBusSubscriber
public class MedicPassiveAbility {

    private static final double DAMAGE_SHARE_RADIUS = 4.0;
    private static final String SURVIVORS_TEAM = "survivors";

    @SubscribeEvent
    public static void onPlayerHurt(LivingHurtEvent event) {
        // Проверяем, что пострадавший - игрок
        if (!(event.getEntity() instanceof ServerPlayer victim)) return;

        // Проверяем, что это медик (scoreboardSurvivorClass == 5)
        if (!isMedic(victim)) return;

        // Проверяем, что медик не в spectator/creative
        if (victim.isSpectator() || victim.isCreative()) return;

        // Ищем ближайшего союзника в радиусе 4 блоков
        ServerPlayer nearestAlly = findNearestAlly(victim);
        if (nearestAlly == null) return;

        // Делим урон поровну
        float originalDamage = event.getAmount();
        float sharedDamage = originalDamage * 0.5F;

        // Уменьшаем урон медику
        event.setAmount(sharedDamage);

        // Наносим урон союзнику
        nearestAlly.hurt(victim.damageSources().generic(), sharedDamage);

        // Визуальные эффекты
        showDamageShareEffect(victim, nearestAlly);

        // Сообщения игрокам
        victim.displayClientMessage(
                net.minecraft.network.chat.Component.literal(
                        String.format("§6Урон разделен с %s (%.1f❤)",
                                nearestAlly.getName().getString(), sharedDamage / 2.0F)
                ),
                true
        );

        nearestAlly.displayClientMessage(
                net.minecraft.network.chat.Component.literal(
                        String.format("§6Вы разделили урон с медиком (%.1f❤)", sharedDamage / 2.0F)
                ),
                true
        );
    }

    /**
     * Проверяет, является ли игрок медиком
     */
    private static boolean isMedic(ServerPlayer player) {
        Scoreboard scoreboard = player.getScoreboard();
        if (scoreboard == null) return false;

        try {
            var objective = scoreboard.getObjective("scoreboardSurvivorClass");
            if (objective == null) return false;

            // Правильный способ получить счет игрока в 1.20.1
            var scoreAccess = scoreboard.getOrCreatePlayerScore(player.getScoreboardName(), objective);

            return scoreAccess.getScore() == 5;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Проверяет, является ли игрок валидным союзником
     */
    private static boolean isValidAlly(Player player, Player medic) {
        // Не может быть сам медик
        if (player == medic) return false;

        // Не в spectator/creative
        if (player.isSpectator() || player.isCreative()) return false;

        // Должен быть в команде survivors
        Team team = player.getTeam();
        if (team == null || !SURVIVORS_TEAM.equalsIgnoreCase(team.getName())) {
            return false;
        }

        return true;
    }

    /**
     * Находит ближайшего союзника в радиусе
     */
    private static ServerPlayer findNearestAlly(ServerPlayer medic) {
        AABB searchBox = medic.getBoundingBox().inflate(DAMAGE_SHARE_RADIUS);
        List<ServerPlayer> nearbyPlayers = medic.level().getEntitiesOfClass(
                ServerPlayer.class,
                searchBox,
                p -> isValidAlly(p, medic)
        );

        if (nearbyPlayers.isEmpty()) return null;

        // Находим ближайшего
        ServerPlayer nearest = null;
        double minDistance = Double.MAX_VALUE;

        for (ServerPlayer ally : nearbyPlayers) {
            double distance = medic.distanceTo(ally);
            if (distance < minDistance && distance <= DAMAGE_SHARE_RADIUS) {
                minDistance = distance;
                nearest = ally;
            }
        }

        return nearest;
    }

    /**
     * Показывает визуальный эффект разделения урона
     */
    private static void showDamageShareEffect(ServerPlayer medic, ServerPlayer ally) {
        if (!(medic.level() instanceof net.minecraft.server.level.ServerLevel serverLevel)) {
            return;
        }

        // Партиклы между медиком и союзником
        net.minecraft.world.phys.Vec3 medicPos = medic.position().add(0, 1, 0);
        net.minecraft.world.phys.Vec3 allyPos = ally.position().add(0, 1, 0);
        net.minecraft.world.phys.Vec3 direction = allyPos.subtract(medicPos).normalize();

        double distance = medicPos.distanceTo(allyPos);
        int particleCount = (int) (distance * 5);

        for (int i = 0; i < particleCount; i++) {
            double progress = i / (double) particleCount;
            net.minecraft.world.phys.Vec3 particlePos = medicPos.add(direction.scale(distance * progress));

            serverLevel.sendParticles(
                    net.minecraft.core.particles.ParticleTypes.HEART,
                    particlePos.x, particlePos.y, particlePos.z,
                    1, 0.1, 0.1, 0.1, 0.01
            );
        }

        // Звуковой эффект
        serverLevel.playSound(null, medic.blockPosition(),
                net.minecraft.sounds.SoundEvents.SHIELD_BLOCK,
                net.minecraft.sounds.SoundSource.PLAYERS, 0.5F, 1.2F);
    }
}