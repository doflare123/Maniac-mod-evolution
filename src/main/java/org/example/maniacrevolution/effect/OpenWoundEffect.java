package org.example.maniacrevolution.effect;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.example.maniacrevolution.entity.BloodMarkerEntity;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Map;

/**
 * Эффект "Открытая рана"
 * Заставляет жертву оставлять кровавый след на земле в виде облаков частиц
 */
public class OpenWoundEffect extends MobEffect {

    private static final int PARTICLE_SPAWN_INTERVAL = 30; // Каждые 3 секунды (60 тиков)
    private static final int BLOOD_TRAIL_DURATION = 100; // След держится 5 секунд (100 тиков)

    // Темно-красный цвет для частиц крови
    private static final DustParticleOptions BLOOD_PARTICLE =
            new DustParticleOptions(new Vector3f(0.8f, 0.0f, 0.0f), 1.2f);

    // Храним последние позиции следов для каждой сущности
    private static final Map<Integer, BlockPos> lastTrailPositions = new HashMap<>();

    public OpenWoundEffect() {
        super(MobEffectCategory.HARMFUL, 0x8B0000); // Темно-красный цвет
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        if (entity.level().isClientSide) return;

        // Получаем текущую длительность эффекта
        int duration = entity.getEffect(this) != null ?
                entity.getEffect(this).getDuration() : 0;

        // Вычисляем, сколько тиков прошло с начала эффекта
        int totalDuration = 200; // 10 секунд
        int ticksSinceStart = totalDuration - duration;

        // Проверяем, нужно ли оставить след (каждые 3 секунды)
        if (ticksSinceStart % PARTICLE_SPAWN_INTERVAL == 0) {
            createBloodTrail(entity);
        }
    }

    /**
     * Создаёт кровавый след на земле, используя BloodMarkerEntity
     */
    private void createBloodTrail(LivingEntity entity) {
        if (!(entity.level() instanceof ServerLevel serverLevel)) return;

        Vec3 pos = entity.position();
        BlockPos blockPos = entity.blockPosition();

        // Проверяем, не создавали ли мы недавно след на этой позиции
        BlockPos lastPos = lastTrailPositions.get(entity.getId());
        if (lastPos != null && lastPos.equals(blockPos)) {
            return; // Не спавним следы на той же позиции
        }

        // Находим поверхность под игроком
        BlockPos groundPos = findGroundPosition(entity, blockPos);
        if (groundPos == null) return;

        double groundY = groundPos.getY() + 1.02; // Чуть выше поверхности блока

        // Создаём маркер крови на земле
        BloodMarkerEntity marker = new BloodMarkerEntity(
                serverLevel,
                pos.x,
                groundY,
                pos.z
        );

        // Спавним маркер в мир
        serverLevel.addFreshEntity(marker);

        // Дополнительные частицы для момента создания следа
        spawnAdditionalParticles(serverLevel, pos.x, groundY, pos.z);

        // Сохраняем позицию последнего следа
        lastTrailPositions.put(entity.getId(), blockPos);
    }

    /**
     * Спавнит дополнительные частицы для усиления визуального эффекта
     */
    private void spawnAdditionalParticles(ServerLevel level, double x, double y, double z) {
        // Красные споры
        for (int i = 0; i < 8; i++) {
            double offsetX = (level.random.nextDouble() - 0.5) * 0.6;
            double offsetZ = (level.random.nextDouble() - 0.5) * 0.6;

            level.sendParticles(
                    ParticleTypes.CRIMSON_SPORE,
                    x + offsetX,
                    y + 0.02,
                    z + offsetZ,
                    3,
                    0.1, 0.0, 0.1,
                    0.0
            );
        }

        // Эффект капель
        level.sendParticles(
                ParticleTypes.LANDING_LAVA,
                x,
                y + 0.1,
                z,
                5,
                0.3, 0.0, 0.3,
                0.0
        );
    }

    /**
     * Находит позицию земли под сущностью
     */
    private BlockPos findGroundPosition(LivingEntity entity, BlockPos startPos) {
        // Проверяем блоки вниз до 5 блоков
        for (int i = 0; i <= 5; i++) {
            BlockPos checkPos = startPos.below(i);
            BlockState state = entity.level().getBlockState(checkPos);

            // Если нашли твердый блок
            if (!state.isAir() && state.isSolid()) {
                return checkPos;
            }
        }
        return null; // Не нашли землю
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        // Вызывается каждый тик
        return true;
    }

    @Override
    public boolean isInstantenous() {
        return false;
    }

    /**
     * Очистка при удалении эффекта
     */
    public static void cleanupEntity(int entityId) {
        lastTrailPositions.remove(entityId);
    }
}