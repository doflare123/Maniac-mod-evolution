package org.example.maniacrevolution.entity;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import org.joml.Vector3f;

public class BloodMarkerEntity extends Entity {
    private static final int LIFETIME_TICKS = 100;
    private static final int PARTICLE_SPAWN_INTERVAL = 4;
    private static final DustParticleOptions BLOOD_PARTICLE =
            new DustParticleOptions(new Vector3f(0.8F, 0.0F, 0.0F), 1.0F);
    private static final DustParticleOptions DARK_BLOOD_PARTICLE =
            new DustParticleOptions(new Vector3f(0.5F, 0.0F, 0.0F), 0.8F);

    private int age;

    public BloodMarkerEntity(EntityType<?> type, Level level) {
        super(type, level);
        noPhysics = true;
        setInvisible(true);
    }

    public BloodMarkerEntity(Level level, double x, double y, double z) {
        this(ModEntities.BLOOD_MARKER.get(), level);
        setPos(x, y, z);
    }

    @Override
    public void tick() {
        super.tick();
        age++;

        if (age >= LIFETIME_TICKS) {
            discard();
            return;
        }

        if (level().isClientSide) {
            if (age == 1) {
                spawnInitialBurst();
            }
            if (age % PARTICLE_SPAWN_INTERVAL == 0) {
                spawnBloodParticles();
            }
        }
    }

    private void spawnBloodParticles() {
        for (int i = 0; i < 3; i++) {
            double offsetX = (random.nextDouble() - 0.5) * 0.4;
            double offsetZ = (random.nextDouble() - 0.5) * 0.4;
            level().addParticle(BLOOD_PARTICLE,
                    getX() + offsetX, getY() + 0.01, getZ() + offsetZ,
                    0.0, 0.0, 0.0);
            level().addParticle(DARK_BLOOD_PARTICLE,
                    getX() + offsetX, getY() + 0.02, getZ() + offsetZ,
                    0.0, 0.0, 0.0);
        }

        if (age % (PARTICLE_SPAWN_INTERVAL * 3) == 0) {
            level().addParticle(ParticleTypes.CRIMSON_SPORE,
                    getX(), getY() + 0.03, getZ(), 0.0, 0.0, 0.0);
        }
    }

    private void spawnInitialBurst() {
        for (int i = 0; i < 8; i++) {
            double offsetX = (random.nextDouble() - 0.5) * 0.6;
            double offsetZ = (random.nextDouble() - 0.5) * 0.6;
            level().addParticle(ParticleTypes.CRIMSON_SPORE,
                    getX() + offsetX, getY() + 0.02, getZ() + offsetZ,
                    0.0, 0.0, 0.0);
        }
        for (int i = 0; i < 5; i++) {
            level().addParticle(ParticleTypes.LANDING_LAVA,
                    getX() + (random.nextDouble() - 0.5) * 0.3,
                    getY() + 0.1,
                    getZ() + (random.nextDouble() - 0.5) * 0.3,
                    0.0, 0.0, 0.0);
        }
    }

    @Override
    protected void defineSynchedData() {
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        age = tag.getInt("Age");
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("Age", age);
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return new ClientboundAddEntityPacket(this);
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 4096.0;
    }
}
