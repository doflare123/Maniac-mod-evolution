package org.example.maniacrevolution.item;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.Team;
import org.example.maniacrevolution.effect.ModEffects;

import java.util.*;

/**
 * Коса Смерти
 * - Двуручное оружие (блокирует вторую руку)
 * - Телепортация к случайному выжившему (ПКМ, 30с кд)
 * - Урон: 2 сердца (4.0)
 * - Накладывает эффект "Гонка со смертью" при убийстве
 */
public class DeathScytheItem extends SwordItem {

    // Кулдауны телепортации для каждого игрока
    private static final Map<UUID, Long> teleportCooldowns = new HashMap<>();
    private static final long TELEPORT_COOLDOWN = 30000; // 30 секунд

    // Урон косы (2 сердца = 4.0 урона)
    private static final int SCYTHE_DAMAGE = -2;
    private static final float SCYTHE_SPEED = -2.4F; // Медленная атака

    public DeathScytheItem(Properties properties) {
        super(Tiers.NETHERITE, SCYTHE_DAMAGE - 1, SCYTHE_SPEED, properties.stacksTo(1));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        // Проверяем, что коса в главной руке
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResultHolder.pass(stack);
        }

        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            // Телепортация к случайному выжившему
            if (tryTeleport(serverPlayer)) {
                return InteractionResultHolder.success(stack);
            }
        }

        return InteractionResultHolder.pass(stack);
    }

    /**
     * Пытается телепортировать игрока к случайному выжившему
     */
    private boolean tryTeleport(ServerPlayer death) {
        // Проверяем кулдаун
        Long lastUse = teleportCooldowns.get(death.getUUID());
        long currentTime = System.currentTimeMillis();

        if (lastUse != null && currentTime - lastUse < TELEPORT_COOLDOWN) {
            long remainingSeconds = (TELEPORT_COOLDOWN - (currentTime - lastUse)) / 1000;
            death.displayClientMessage(
                    Component.literal(String.format("§cКулдаун телепортации: %d секунд", remainingSeconds)),
                    true
            );
            return false;
        }

        // Получаем случайного выжившего
        ServerPlayer target = getRandomSurvivor(death);
        if (target == null) {
            death.displayClientMessage(
                    Component.literal("§cНет доступных целей для телепортации"),
                    true
            );
            return false;
        }

        // Сохраняем старую позицию для анимации
        Vec3 startPos = death.position();

        // Анимация исчезновения
        playDepartureAnimation(death, startPos);

        // Телепортируем
        Vec3 targetPos = target.position();
        death.teleportTo(targetPos.x, targetPos.y, targetPos.z);

        // Анимация появления
        playArrivalAnimation(death, targetPos);

        // Записываем время использования
        teleportCooldowns.put(death.getUUID(), currentTime);

        // Звуковые эффекты
        death.level().playSound(null, startPos.x, startPos.y, startPos.z,
                SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0F, 0.8F);
        death.level().playSound(null, targetPos.x, targetPos.y, targetPos.z,
                SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0F, 1.2F);

        death.displayClientMessage(
                Component.literal("§5Вы телепортировались к " + target.getName().getString()),
                true
        );

        return true;
    }

    /**
     * Анимация исчезновения (частицы пустоты)
     */
    private void playDepartureAnimation(ServerPlayer player, Vec3 pos) {
        if (!(player.level() instanceof ServerLevel serverLevel)) return;

        // Спираль из темных частиц, уходящая вниз
        for (int i = 0; i < 50; i++) {
            double angle = i * Math.PI / 8;
            double radius = 1.5 - (i * 0.03);
            double height = i * 0.1;

            double offsetX = Math.cos(angle) * radius;
            double offsetZ = Math.sin(angle) * radius;

            // Темные частицы пустоты
            serverLevel.sendParticles(ParticleTypes.SMOKE,
                    pos.x + offsetX, pos.y + 1.0 + height, pos.z + offsetZ,
                    3, 0.1, 0.1, 0.1, 0.02);

            serverLevel.sendParticles(ParticleTypes.LARGE_SMOKE,
                    pos.x + offsetX, pos.y + 1.0 + height, pos.z + offsetZ,
                    2, 0.05, 0.05, 0.05, 0.01);
        }

        // Взрыв темных частиц
        serverLevel.sendParticles(ParticleTypes.EXPLOSION,
                pos.x, pos.y + 1.0, pos.z,
                1, 0, 0, 0, 0);

        serverLevel.sendParticles(ParticleTypes.SQUID_INK,
                pos.x, pos.y + 1.0, pos.z,
                30, 0.5, 1.0, 0.5, 0.1);
    }

    /**
     * Анимация появления (частицы из пустоты)
     */
    private void playArrivalAnimation(ServerPlayer player, Vec3 pos) {
        if (!(player.level() instanceof ServerLevel serverLevel)) return;

        // Спираль из темных частиц, поднимающаяся вверх
        for (int i = 0; i < 50; i++) {
            double angle = i * Math.PI / 8;
            double radius = (i * 0.03);
            double height = 3.0 - (i * 0.06);

            double offsetX = Math.cos(angle) * radius;
            double offsetZ = Math.sin(angle) * radius;

            // Темные частицы, появляющиеся снизу
            serverLevel.sendParticles(ParticleTypes.SMOKE,
                    pos.x + offsetX, pos.y + height, pos.z + offsetZ,
                    3, 0.1, 0.1, 0.1, 0.02);

            serverLevel.sendParticles(ParticleTypes.SOUL,
                    pos.x + offsetX, pos.y + height, pos.z + offsetZ,
                    1, 0.05, 0.05, 0.05, 0.01);
        }

        // Взрыв при появлении
        serverLevel.sendParticles(ParticleTypes.EXPLOSION,
                pos.x, pos.y + 1.0, pos.z,
                1, 0, 0, 0, 0);

        serverLevel.sendParticles(ParticleTypes.SQUID_INK,
                pos.x, pos.y + 1.0, pos.z,
                30, 0.5, 1.0, 0.5, 0.1);

        // Дополнительные эффекты - души вокруг
        for (int i = 0; i < 12; i++) {
            double angle = i * Math.PI / 6;
            double offsetX = Math.cos(angle) * 1.5;
            double offsetZ = Math.sin(angle) * 1.5;

            serverLevel.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                    pos.x + offsetX, pos.y + 0.5, pos.z + offsetZ,
                    5, 0.2, 0.5, 0.2, 0.05);
        }
    }

    /**
     * Получает случайного выжившего
     */
    private ServerPlayer getRandomSurvivor(ServerPlayer death) {
        List<ServerPlayer> survivors = new ArrayList<>();

        for (ServerPlayer player : death.getServer().getPlayerList().getPlayers()) {
            if (player == death) continue;
            if (player.isSpectator() || player.isCreative()) continue;

            Team team = player.getTeam();
            if (team != null && "survivors".equalsIgnoreCase(team.getName())) {
                survivors.add(player);
            }
        }

        if (survivors.isEmpty()) return null;

        Random random = new Random();
        return survivors.get(random.nextInt(survivors.size()));
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        // Проверяем, убил ли удар цель
        if (target.getHealth() <= 0 && attacker instanceof ServerPlayer serverPlayer) {
            // Проверяем, что атакующий - Смерть (ManiacClass = 10)
            if (isDeath(serverPlayer) && target instanceof Player) {
                // Применяем или усиливаем эффект "Гонка со смертью"
                applyDeathRaceEffect(serverPlayer);
            }
        }

        return super.hurtEnemy(stack, target, attacker);
    }

    /**
     * Проверяет, является ли игрок Смертью
     */
    private boolean isDeath(ServerPlayer player) {
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
    private void applyDeathRaceEffect(ServerPlayer death) {
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
                Component.literal(String.format("§c§lГОНКА СО СМЕРТЬЮ §f§lУровень %d §7(+%d урона)",
                        newAmplifier + 1, (newAmplifier + 1) * 2)),
                true
        );

        // Звуковой эффект
        death.level().playSound(null, death.blockPosition(),
                SoundEvents.WITHER_SPAWN, SoundSource.PLAYERS, 0.5F, 1.5F);

        // Визуальный эффект усиления
        if (death.level() instanceof ServerLevel serverLevel) {
            for (int i = 0; i < 20; i++) {
                double angle = i * Math.PI / 10;
                double offsetX = Math.cos(angle) * 1.0;
                double offsetZ = Math.sin(angle) * 1.0;

                serverLevel.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                        death.getX() + offsetX, death.getY() + 1.0, death.getZ() + offsetZ,
                        3, 0.1, 0.3, 0.1, 0.02);
            }
        }
    }

    /**
     * Коса занимает обе руки - блокирует использование второй руки
     */
//    @Override
//    public boolean canEquip(ItemStack stack, EquipmentSlot slot, LivingEntity entity) {
//        return slot == EquipmentSlot.MAINHAND;
//    }

    /**
     * Очистка кулдаунов при выходе игрока
     */
    public static void onPlayerLogout(UUID playerId) {
        teleportCooldowns.remove(playerId);
    }

    /**
     * Получает оставшееся время кулдауна в секундах
     */
    public static int getCooldownSeconds(UUID playerId) {
        Long lastUse = teleportCooldowns.get(playerId);
        if (lastUse == null) return 0;

        long currentTime = System.currentTimeMillis();
        long elapsed = currentTime - lastUse;

        if (elapsed >= TELEPORT_COOLDOWN) {
            return 0;
        }

        return (int) ((TELEPORT_COOLDOWN - elapsed) / 1000);
    }
}