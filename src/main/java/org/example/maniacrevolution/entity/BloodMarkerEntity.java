package org.example.maniacrevolution.entity;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import org.joml.Vector3f;

/**
 * Невидимая сущность-маркер, которая спавнит частицы крови на земле
 * Автоматически удаляется через 5 секунд
 */
public class BloodMarkerEntity extends Entity {

    private static final int LIFETIME_TICKS = 100; // 5 секунд
    private static final int PARTICLE_SPAWN_INTERVAL = 4; // Каждые 4 тика (0.2 сек)

    // Красные частицы крови
    private static final DustParticleOptions BLOOD_PARTICLE =
            new DustParticleOptions(new Vector3f(0.8f, 0.0f, 0.0f), 1.0f);

    private static final DustParticleOptions DARK_BLOOD_PARTICLE =
            new DustParticleOptions(new Vector3f(0.5f, 0.0f, 0.0f), 0.8f);

    private int age = 0;

    public BloodMarkerEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = true; // Проходит сквозь блоки
        this.setInvisible(true); // Невидимая
    }

    // Конструктор для создания в коде
    public BloodMarkerEntity(Level level, double x, double y, double z) {
        this(ModEntities.BLOOD_MARKER.get(), level);
        this.setPos(x, y, z);
    }

    @Override
    public void tick() {
        super.tick();

        age++;

        // Удаляем маркер после истечения времени
        if (age >= LIFETIME_TICKS) {
            this.discard();
            return;
        }

        // Спавним частицы только на сервере
        if (!level().isClientSide && age % PARTICLE_SPAWN_INTERVAL == 0) {
            spawnBloodParticles();
        }
    }

    /**
     * Спавнит частицы крови на поверхности
     */
    private void spawnBloodParticles() {
        if (!(level() instanceof ServerLevel serverLevel)) return;

        double x = getX();
        double y = getY();
        double z = getZ();

        // Основные красные частицы (плотный след)
        for (int i = 0; i < 3; i++) {
            double offsetX = (random.nextDouble() - 0.5) * 0.4;
            double offsetZ = (random.nextDouble() - 0.5) * 0.4;

            // Светлые красные частицы
            serverLevel.sendParticles(
                    BLOOD_PARTICLE,
                    x + offsetX,
                    y + 0.01,
                    z + offsetZ,
                    1,
                    0.01, 0.0, 0.01,
                    0.0
            );

            // Темные красные частицы для контраста
            serverLevel.sendParticles(
                    DARK_BLOOD_PARTICLE,
                    x + offsetX,
                    y + 0.02,
                    z + offsetZ,
                    1,
                    0.01, 0.0, 0.01,
                    0.0
            );
        }

        // Дополнительные эффекты (реже)
        if (age % (PARTICLE_SPAWN_INTERVAL * 3) == 0) {
            // Красные споры
            serverLevel.sendParticles(
                    ParticleTypes.CRIMSON_SPORE,
                    x,
                    y + 0.03,
                    z,
                    4,
                    0.2, 0.0, 0.2,
                    0.0
            );
        }
    }

    @Override
    protected void defineSynchedData() {
        // Нет синхронизируемых данных
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        this.age = tag.getInt("Age");
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("Age", this.age);
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return new ClientboundAddEntityPacket(this);
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 4096.0; // Видна в радиусе 64 блоков
    }
}