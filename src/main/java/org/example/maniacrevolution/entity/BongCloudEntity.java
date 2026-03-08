package org.example.maniacrevolution.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.List;
import java.util.UUID;

/**
 * Облако дыма от Bong.
 *
 * - Живёт DURATION_TICKS (100 тиков = 5 сек)
 * - Каждый тик спавнит частицы дыма в радиусе RADIUS
 * - Каждый тик применяет SLOW_FALLING на 5 сек всем игрокам в радиусе,
 *   кроме владельца (ownerUUID)
 * - Не имеет хитбокса (isPickable = false), не толкает игроков
 */
public class BongCloudEntity extends Entity {

    public static final double RADIUS         = 5.0;
    public static final int    DURATION_TICKS = 100; // 5 секунд
    // Эффект обновляется каждый тик с длиной 5 сек — игрок внутри всегда
    // имеет активный эффект, и он не спадёт пока он в облаке
    private static final int   EFFECT_TICKS   = 100; // 5 сек

    private UUID ownerUUID = null;
    private int  liveTicks = 0;

    public BongCloudEntity(EntityType<? extends BongCloudEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    /** Создаёт и добавляет облако в мир. */
    public static void spawn(ServerLevel level, Player owner) {
        BongCloudEntity cloud = new BongCloudEntity(ModEntities.BONG_CLOUD.get(), level);
        cloud.setPos(owner.getX(), owner.getY(), owner.getZ());
        cloud.ownerUUID = owner.getUUID();
        level.addFreshEntity(cloud);
    }

    // ── Тик ──────────────────────────────────────────────────────────────────

    @Override
    public void tick() {
        super.tick();
        liveTicks++;

        if (level().isClientSide()) {
            spawnParticlesClient();
            return;
        }

        // Сервер: применяем эффекты
        applyEffects();

        if (liveTicks >= DURATION_TICKS) {
            discard();
        }
    }

    /** Клиентские частицы — вызываются каждый тик. */
    private void spawnParticlesClient() {
        // ~10 частиц в тик → плотное облако
        for (int i = 0; i < 30; i++) {
            double angle = random.nextDouble() * Math.PI * 2;
            double r     = random.nextDouble() * RADIUS;
            double yOff  = random.nextDouble() * 2.5;
            level().addParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE,
                    getX() + Math.cos(angle) * r,
                    getY() + yOff,
                    getZ() + Math.sin(angle) * r,
                    0, 0.02, 0);
        }
    }

    /** Серверная проверка — применяет эффект игрокам в радиусе. */
    private void applyEffects() {
        AABB box = new AABB(
                getX() - RADIUS, getY() - 1, getZ() - RADIUS,
                getX() + RADIUS, getY() + 3, getZ() + RADIUS);

        List<Player> players = level().getEntitiesOfClass(Player.class, box);
        for (Player p : players) {
            // Пропускаем владельца
            if (ownerUUID != null && p.getUUID().equals(ownerUUID)) continue;
            // Дополнительная проверка — игрок реально в радиусе (AABB квадратный)
            if (p.distanceTo(this) > RADIUS + 0.5) continue;

            p.addEffect(new MobEffectInstance(
                    MobEffects.SLOW_FALLING,
                    EFFECT_TICKS, 0,
                    false,  // ambient
                    true,   // показывать частицы
                    true)); // показывать иконку
        }
    }

    // ── Entity boilerplate ────────────────────────────────────────────────────

    @Override
    protected void defineSynchedData() {
        // Нет синхронизированных данных
    }

    @Override
    public boolean isPickable() { return false; }

    @Override
    public boolean isPushable() { return false; }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        liveTicks = tag.getInt("LiveTicks");
        if (tag.hasUUID("OwnerUUID")) {
            ownerUUID = tag.getUUID("OwnerUUID");
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("LiveTicks", liveTicks);
        if (ownerUUID != null) {
            tag.putUUID("OwnerUUID", ownerUUID);
        }
    }
}