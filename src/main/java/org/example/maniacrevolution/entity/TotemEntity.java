package org.example.maniacrevolution.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.example.maniacrevolution.util.SelectiveGlowingEffect;

import javax.annotation.Nullable;
import java.util.*;

/**
 * Тотем шамана.
 *
 * Механики:
 *   - Полностью неподвижен (фиксируется на месте спавна)
 *   - Получает урон ТОЛЬКО от игроков в team "maniac"
 *   - Умирает через ровно MAX_HITS ударов (независимо от величины урона)
 *   - В радиусе GLOW_RADIUS подсвечивает владельцу всех team "maniac" в adventure
 *   - Анимация idle: зациклена постоянно
 *   - Анимация hello: играет при первом появлении игрока в зоне видимости.
 *     Если hello уже идёт — новые игроки тоже "захватываются" ею, но новая не запускается.
 */
public class TotemEntity extends PathfinderMob {

    // ── Настройки (меняйте тут) ───────────────────────────────────────────────
    /** Количество ударов до смерти */
    public static final int    MAX_HITS       = 3;
    /** Радиус подсветки и детекции */
    public static final double GLOW_RADIUS    = 5.0;
    /** Продолжительность hello-анимации: 6.375 сек × 20 тиков */
    public static final int    HELLO_DURATION = (int)(6.375f * 20);

    private static final String MANIAC_TEAM = "maniac";
    private static final String TAG_OWNER   = "TotemOwnerUUID";
    private static final String TAG_HITS    = "TotemHitsReceived";

    // ── Synced data: состояние анимации ──────────────────────────────────────
    /** 0 = IDLE, 1 = HELLO */
    public static final EntityDataAccessor<Integer> ANIM_STATE =
            SynchedEntityData.defineId(TotemEntity.class, EntityDataSerializers.INT);
    /** tickCount сущности в момент старта hello — клиент использует для отсчёта времени анимации */
    public static final EntityDataAccessor<Integer> HELLO_START_TICK =
            SynchedEntityData.defineId(TotemEntity.class, EntityDataSerializers.INT);

    public static final int ANIM_IDLE  = 0;
    public static final int ANIM_HELLO = 1;

    // ── Серверные поля ────────────────────────────────────────────────────────
    @Nullable private UUID ownerUUID   = null;
    private int  hitsReceived          = 0;
    private Vec3 lockedPos             = null;

    /** UUID игроков которым уже сыграли hello */
    private final Set<UUID> greetedPlayers = new HashSet<>();
    /** Тик начала hello-анимации (для возврата в idle) */
    private long helloStartTick = -1;

    // ─────────────────────────────────────────────────────────────────────────

    public TotemEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.setPersistenceRequired();
        // Тотем не ходит
        this.setNoAi(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                // Много HP — тотем умирает по числу ударов, не по урону
                .add(Attributes.MAX_HEALTH, 9999.0)
                // Полный резист к откидыванию
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.MOVEMENT_SPEED, 0.0);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(ANIM_STATE, ANIM_IDLE);
        entityData.define(HELLO_START_TICK, 0);
    }

    // ── Tick ─────────────────────────────────────────────────────────────────

    @Override
    public void tick() {
        super.tick();

        // Жёстко фиксируем позицию
        if (lockedPos != null) {
            setPos(lockedPos.x, lockedPos.y, lockedPos.z);
            setDeltaMovement(Vec3.ZERO);
        }

        if (level().isClientSide()) return;
        if (!(level() instanceof ServerLevel sl)) return;

        long gameTick = sl.getGameTime();

        // Возврат из hello в idle
        if (helloStartTick >= 0 && gameTick - helloStartTick >= HELLO_DURATION) {
            helloStartTick = -1;
            setAnimState(ANIM_IDLE);
        }

        // Каждую секунду обновляем подсветку и проверяем новых игроков
        if (gameTick % 20 == 0) {
            tickGlowAndGreet(sl);
        }
    }

    private void tickGlowAndGreet(ServerLevel level) {
        if (ownerUUID == null) return;
        ServerPlayer owner = level.getServer().getPlayerList().getPlayer(ownerUUID);
        if (owner == null) return;

        AABB box = new AABB(
                getX() - GLOW_RADIUS, getY() - 1, getZ() - GLOW_RADIUS,
                getX() + GLOW_RADIUS, getY() + 3, getZ() + GLOW_RADIUS
        );

        boolean helloPending = false;

        for (Player p : level.getEntitiesOfClass(Player.class, box)) {
            if (!(p instanceof ServerPlayer sp)) continue;
            if (!isManiacAdventure(sp)) continue;

            // Подсветка только владельцу (45 тиков — с запасом под следующее обновление)
            SelectiveGlowingEffect.addGlowing(sp, owner, 45);

            // Hello при первой встрече
            if (!greetedPlayers.contains(sp.getUUID())) {
                greetedPlayers.add(sp.getUUID());
                helloPending = true;
            }
        }

        // Запускаем hello один раз (даже если несколько новых игроков — одна анимация)
        if (helloPending && getAnimState() != ANIM_HELLO) {
            setAnimState(ANIM_HELLO);
            helloStartTick = level.getGameTime();
            // Синхронизируем tickCount на клиент чтобы анимация стартовала с нуля
            setHelloStartTick(this.tickCount);
        }
    }

    // ── Урон: только от team maniac, считаем удары ───────────────────────────

    @Override
    public boolean hurt(DamageSource source, float amount) {
        // isInvulnerableTo уже отфильтровал всё кроме ударов от маньяков
        // Дополнительная проверка на случай прямого вызова
        if (!(source.getEntity() instanceof Player attacker)) return false;
        var team = attacker.getTeam();
        if (team == null || !MANIAC_TEAM.equalsIgnoreCase(team.getName())) return false;

        // Визуальный hurt-эффект
        this.hurtTime = 10;
        this.hurtDuration = 10;
        this.lastHurt = 1.0f;

        hitsReceived++;

        if (hitsReceived >= MAX_HITS) {
            forceKill();
        }

        return true;
    }

    /** kill() который точно работает даже при isInvulnerableTo */
    public void forceKill() {
        if (level() instanceof ServerLevel) {
            SelectiveGlowingEffect.removeAllGlowing(this);
        }
        // Снимаем защиту на один тик чтобы discard() прошёл
        this.setHealth(0);
        this.discard();
    }

    /** Тотем уязвим только к ударам от маньяков — всё остальное блокируем в hurt() */
    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        // Пропускаем только прямой урон от игрока (обрабатывается в hurt())
        // Всё остальное (огонь, мир, падение и т.д.) — неуязвим
        // НО: не блокируем системные источники чтобы kill() работал
        if (source.getMsgId().equals("outOfWorld")) return false;
        if (source.getEntity() instanceof Player attacker) {
            var team = attacker.getTeam();
            return team == null || !MANIAC_TEAM.equalsIgnoreCase(team.getName());
        }
        return true;
    }

    // ── Неподвижность ─────────────────────────────────────────────────────────

    @Override public boolean isPushable()              { return false; }
    @Override protected void doPush(Entity entity)    {}
    @Override public void push(double x, double y, double z) {}
    @Override public boolean isPickable()             { return true; } // чтобы можно было кликнуть

    @Override
    protected void registerGoals() {
        // Нет целей — тотем стоит на месте
    }

    // ── Фиксация позиции при появлении ───────────────────────────────────────

    @Override
    public void onAddedToWorld() {
        super.onAddedToWorld();
        lockedPos = new Vec3(getX(), getY(), getZ());
    }

    // ── Анимация ──────────────────────────────────────────────────────────────

    public int getAnimState()       { return entityData.get(ANIM_STATE); }
    public void setAnimState(int s) { entityData.set(ANIM_STATE, s); }
    /** Тик (tickCount) когда стартовала hello-анимация. Клиент читает для корректного времени. */
    public int getHelloStartTick()  { return entityData.get(HELLO_START_TICK); }
    public void setHelloStartTick(int t) { entityData.set(HELLO_START_TICK, t); }

    // ── Владелец ─────────────────────────────────────────────────────────────

    public void setOwner(Player owner) { this.ownerUUID = owner.getUUID(); }
    @Nullable public UUID getOwnerUUID() { return ownerUUID; }

    // ── NBT ───────────────────────────────────────────────────────────────────

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (ownerUUID != null) tag.putUUID(TAG_OWNER, ownerUUID);
        tag.putInt(TAG_HITS, hitsReceived);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.hasUUID(TAG_OWNER)) ownerUUID = tag.getUUID(TAG_OWNER);
        hitsReceived = tag.getInt(TAG_HITS);
        // После загрузки восстанавливаем locked position
        lockedPos = new Vec3(getX(), getY(), getZ());
    }

    // ── Нет дропа и нет звуков ────────────────────────────────────────────────

    @Override protected void dropAllDeathLoot(DamageSource src)  {}
    @Override protected net.minecraft.sounds.SoundEvent getDeathSound() { return null; }
    @Override protected net.minecraft.sounds.SoundEvent getHurtSound(DamageSource src) { return null; }
    @Override protected net.minecraft.sounds.SoundEvent getAmbientSound() { return null; }

    // ── Вспомогательные ──────────────────────────────────────────────────────

    private static boolean isManiacAdventure(ServerPlayer player) {
        // Проверяем только команду — не фильтруем по режиму игры
        // (adventure-режим проверяется на стороне подсветки, но для hello достаточно team)
        var team = player.getTeam();
        return team != null && MANIAC_TEAM.equalsIgnoreCase(team.getName());
    }
}