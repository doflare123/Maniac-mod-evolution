package org.example.maniacrevolution.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;
import org.example.maniacrevolution.capability.PlagueCapabilityProvider;
import org.example.maniacrevolution.capability.PlagueCapability;
import org.example.maniacrevolution.effect.ModEffects;

/**
 * Снаряд "Зелёный сгусток чумы".
 *
 * При попадании в игрока (survivors / Adventure) мгновенно заполняет
 * шкалу чумы до порога (THRESHOLD_TICKS), что немедленно вызовет урон
 * при следующем тике PlagueLanternEventHandler.
 *
 * Визуально: зелёные частицы во время полёта + взрыв частиц при попадании.
 *
 * РЕГИСТРАЦИЯ:
 *   В ModEntities добавьте:
 *     public static final RegistryObject<EntityType<PlagueOrbEntity>> PLAGUE_ORB =
 *         ENTITIES.register("plague_orb", () ->
 *             EntityType.Builder.<PlagueOrbEntity>of(PlagueOrbEntity::new, MobCategory.MISC)
 *                 .sized(0.5f, 0.5f)
 *                 .clientTrackingRange(64)
 *                 .updateInterval(1)
 *                 .build("plague_orb"));
 *
 *   В ClientSetupEvent зарегистрируйте рендерер:
 *     EntityRenderers.register(ModEntities.PLAGUE_ORB.get(), PlagueOrbRenderer::new);
 */
public class PlagueOrbEntity extends Projectile {

    private static final String SURVIVORS_TEAM = "survivors";

    // ── Скорость полёта (как у огненного шара) ───────────────────────────────
    public static final double SPEED = 1.5;

    public PlagueOrbEntity(EntityType<? extends PlagueOrbEntity> type, Level level) {
        super(type, level);
    }

    /** Фабричный метод — создаёт снаряд и задаёт направление по взгляду игрока */
    public static PlagueOrbEntity create(Level level, Player shooter) {
        // Тип подставится при регистрации — используем через ModEntities
        PlagueOrbEntity orb = new PlagueOrbEntity(
                org.example.maniacrevolution.entity.ModEntities.PLAGUE_ORB.get(),
                level
        );
        orb.setOwner(shooter);

        // Спавн чуть впереди глаз игрока, чтобы не застрять в нём
        Vec3 eyePos = shooter.getEyePosition();
        Vec3 lookVec = shooter.getLookAngle();
        orb.setPos(eyePos.x + lookVec.x * 1.2,
                eyePos.y + lookVec.y * 1.2,
                eyePos.z + lookVec.z * 1.2);

        // Задаём скорость в направлении взгляда
        orb.setDeltaMovement(lookVec.scale(SPEED));

        return orb;
    }

    // ─── Тик ─────────────────────────────────────────────────────────────────

    @Override
    public void tick() {
        super.tick();

        // Частицы в полёте (только на клиенте)
        if (level().isClientSide()) {
            spawnTrailParticles();
            return;
        }

        // Серверная сторона: проверяем коллизию
        Vec3 pos    = position();
        Vec3 motion = getDeltaMovement();
        Vec3 nextPos = pos.add(motion);

        HitResult hit = ProjectileUtil.getHitResultOnMoveVector(this, this::canHitEntity);

        if (hit.getType() != HitResult.Type.MISS) {
            onHit(hit);
            return;
        }

        // Двигаемся вперёд
        setPos(nextPos);

        // Спавним зелёные частицы на сервере (видны всем клиентам)
        if (level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(
                    ParticleTypes.COMPOSTER, // зеленоватые частицы
                    getX(), getY() + 0.25, getZ(),
                    3, 0.15, 0.15, 0.15, 0.02
            );
        }

        // Удаляемся через 10 секунд если ни во что не попали
        if (tickCount > 200) {
            discard();
        }
    }

    // ─── Обработка попаданий ──────────────────────────────────────────────────

    @Override
    protected void onHit(HitResult result) {
        if (result instanceof EntityHitResult entityHit) {
            onHitEntity(entityHit);
        } else if (result instanceof BlockHitResult) {
            onHitBlock();
        }
        discard();
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        if (level().isClientSide()) return;
        if (!(result.getEntity() instanceof ServerPlayer target)) return;
        if (!isValidTarget(target)) return;

        PlagueCapability cap = PlagueCapabilityProvider.get(target);
        if (cap == null) return;

        // Сразу наносим урон — не ждём следующего тика
        target.hurt(
                target.level().damageSources().magic(),
                PlagueCapability.PLAGUE_DAMAGE
        );

        // Сбрасываем счётчик в 0 после урона
        cap.setAccumulatedTicks(0);
        cap.syncToClient(target);

        // Частицы попадания
        if (level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.COMPOSTER,
                    target.getX(), target.getY() + 1.0, target.getZ(),
                    40, 0.5, 0.8, 0.5, 0.15);
            serverLevel.sendParticles(ParticleTypes.EFFECT,
                    target.getX(), target.getY() + 1.0, target.getZ(),
                    20, 0.3, 0.5, 0.3, 0.05);
        }
    }

    private void onHitBlock() {
        // При ударе о блок — небольшое облачко частиц
        if (level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(
                    ParticleTypes.COMPOSTER,
                    getX(), getY(), getZ(),
                    15, 0.3, 0.3, 0.3, 0.08
            );
        }
    }

    // ─── Проверка цели ────────────────────────────────────────────────────────

    private boolean isValidTarget(Player target) {
        // Только режим Adventure
        if (((ServerPlayer)target).gameMode.getGameModeForPlayer()
                != net.minecraft.world.level.GameType.ADVENTURE) {
            return false;
        }

        // Только команда survivors
        var scoreboard = target.getServer() != null ? target.getServer().getScoreboard() : null;
        if (scoreboard == null) return false;

        var team = scoreboard.getPlayersTeam(target.getScoreboardName());
        return team != null && team.getName().equals(SURVIVORS_TEAM);
    }

    @Override
    protected boolean canHitEntity(net.minecraft.world.entity.Entity entity) {
        // Не попадаем в себя и во владельца
        if (entity == getOwner()) return false;
        if (!entity.isAlive()) return false;
        // Попадаем только в игроков (прочие entity игнорируем)
        return entity instanceof Player;
    }

    // ─── Клиентские частицы ───────────────────────────────────────────────────

    private void spawnTrailParticles() {
        // Зелёный шлейф
        for (int i = 0; i < 3; i++) {
            double ox = (random.nextDouble() - 0.5) * 0.3;
            double oy = (random.nextDouble() - 0.5) * 0.3;
            double oz = (random.nextDouble() - 0.5) * 0.3;
            level().addParticle(ParticleTypes.COMPOSTER,
                    getX() + ox, getY() + oy + 0.2, getZ() + oz,
                    0, 0.02, 0);
        }
    }

    // ─── Обязательные методы Projectile / Entity ─────────────────────────────

    @Override
    protected void defineSynchedData() {
        // Нет синхронизируемых данных
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        // Снаряды не сохраняются между сессиями — ничего не читаем
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        // Аналогично — ничего не сохраняем
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    // Отключаем гравитацию — летит по прямой как огненный шар
    @Override
    public boolean isNoGravity() {
        return true;
    }
}