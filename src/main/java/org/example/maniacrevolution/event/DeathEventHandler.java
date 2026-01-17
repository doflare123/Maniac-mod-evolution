package org.example.maniacrevolution.event;

import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.effect.MobEffectInstance;
import org.example.maniacrevolution.effect.ModEffects;
import org.example.maniacrevolution.item.DeathScytheItem;

/**
 * Обработчик событий для Смерти
 */
@Mod.EventBusSubscriber
public class DeathEventHandler {

    /**
     * Обработка убийства игрока косой
     */
    @SubscribeEvent
    public static void onPlayerKill(LivingDeathEvent event) {
        // Проверяем, что убитый - игрок
        if (!(event.getEntity() instanceof Player victim)) return;
        if (victim.level().isClientSide()) return;

        // Проверяем, что убийца - игрок
        if (!(event.getSource().getEntity() instanceof ServerPlayer killer)) return;

        // Проверяем, что убийца держит косу
        if (!(killer.getMainHandItem().getItem() instanceof DeathScytheItem)) return;

        // Проверяем, что убийца - Смерть (ManiacClass = 10)
        if (!isDeath(killer)) return;

        // Применяем или усиливаем эффект "Гонка со смертью"
        applyDeathRaceEffect(killer);
    }

    /**
     * Проверяет, является ли игрок Смертью
     */
    private static boolean isDeath(ServerPlayer player) {
        var scoreboard = player.getScoreboard();
        if (scoreboard == null) return false;

        try {
            var objective = scoreboard.getObjective("ManiacClass");
            if (objective == null) return false;

            var scoreAccess = scoreboard.getOrCreatePlayerScore(player.getScoreboardName(), objective);

            return scoreAccess.getScore() == 10; // Смерть имеет класс 10
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Применяет или усиливает эффект "Гонка со смертью"
     */
    private static void applyDeathRaceEffect(ServerPlayer death) {
        MobEffectInstance currentEffect = death.getEffect(ModEffects.DEATH_RACE_EFFECT.get());

        int newAmplifier = 0;
        if (currentEffect != null) {
            newAmplifier = currentEffect.getAmplifier() + 1;
        }

        // Применяем эффект с бесконечной длительностью
        MobEffectInstance newEffect = new MobEffectInstance(
                ModEffects.DEATH_RACE_EFFECT.get(),
                Integer.MAX_VALUE, // Бесконечная длительность
                newAmplifier,
                false,
                true,
                true
        );

        death.addEffect(newEffect);

        // Сообщение игроку
        death.displayClientMessage(
                net.minecraft.network.chat.Component.literal(
                        String.format("§c§lГОНКА СО СМЕРТЬЮ §f§lУровень %d §7(+%d урона)",
                                newAmplifier + 1, (newAmplifier + 1) * 2)
                ),
                true
        );

        // Звуковой эффект
        death.level().playSound(null, death.blockPosition(),
                net.minecraft.sounds.SoundEvents.WITHER_SPAWN,
                net.minecraft.sounds.SoundSource.PLAYERS, 0.5F, 1.5F);

        // Визуальный эффект усиления
        if (death.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            for (int i = 0; i < 20; i++) {
                double angle = i * Math.PI / 10;
                double offsetX = Math.cos(angle) * 1.0;
                double offsetZ = Math.sin(angle) * 1.0;

                serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.SOUL_FIRE_FLAME,
                        death.getX() + offsetX, death.getY() + 1.0, death.getZ() + offsetZ,
                        3, 0.1, 0.3, 0.1, 0.02);
            }
        }
    }

    /**
     * Очистка кулдаунов при выходе игрока
     */
    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            DeathScytheItem.onPlayerLogout(player.getUUID());
        }
    }
}