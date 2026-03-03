package org.example.maniacrevolution.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.Bee;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.scores.Team;

import java.util.UUID;

public class RageBeeEntity extends Bee {

    private static final String TAG_OWNER = "RageBeeOwner";
    private static final String SURVIVORS_TEAM = "survivors";

    private UUID ownerUUID;

    public RageBeeEntity(Level level, Player owner) {
        super(EntityType.BEE, level);
        this.ownerUUID = owner.getUUID();
        this.setRemainingPersistentAngerTime(Integer.MAX_VALUE);
        this.setPersistenceRequired();
    }

    public RageBeeEntity(EntityType<? extends Bee> type, Level level) {
        super(type, level);
        this.setRemainingPersistentAngerTime(Integer.MAX_VALUE);
    }

    @Override
    protected void registerGoals() {
        // ОБЯЗАТЕЛЬНО вызывать super - он инициализирует beePollinateGoal.
        // Без этого Bee.aiStep() падает с NullPointerException.
        super.registerGoals();

        // Атаковать выживших (приоритет 1)
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(
                this, Player.class, 8, true, false, this::isSurvivorTarget));

        // Следовать за владельцем когда нет цели (приоритет 2)
        this.goalSelector.addGoal(2, new FollowOwnerGoal(this));
    }


    private boolean isSurvivorTarget(net.minecraft.world.entity.LivingEntity entity) {
        if (!(entity instanceof Player p)) return false;
        if (p.getUUID().equals(ownerUUID)) return false;
        Team team = p.getTeam();
        return team != null && SURVIVORS_TEAM.equalsIgnoreCase(team.getName());
    }

    /**
     * После жала убиваем пчелу немедленно.
     * Ванильная пчела ждёт таймер смерти — нам это не нужно.
     */
    @Override
    public boolean doHurtTarget(net.minecraft.world.entity.Entity target) {
        boolean result = super.doHurtTarget(target);
        if (result) {
            this.kill();
        }
        return result;
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide() && !isOwnerAlive()) {
            this.kill();
        }
    }

    public boolean isOwnerAlive() {
        if (ownerUUID == null) return false;
        if (!(level() instanceof ServerLevel sl)) return true;
        Player owner = sl.getPlayerByUUID(ownerUUID);
        return owner != null && owner.isAlive();
    }

    public UUID getOwnerUUID() { return ownerUUID; }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (ownerUUID != null) tag.putUUID(TAG_OWNER, ownerUUID);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.hasUUID(TAG_OWNER)) ownerUUID = tag.getUUID(TAG_OWNER);
    }

    private static class FollowOwnerGoal extends Goal {
        private final RageBeeEntity bee;
        private Player owner;

        FollowOwnerGoal(RageBeeEntity bee) { this.bee = bee; }

        @Override
        public boolean canUse() {
            if (bee.ownerUUID == null || !(bee.level() instanceof ServerLevel sl)) return false;
            owner = sl.getPlayerByUUID(bee.ownerUUID);
            return owner != null && bee.getTarget() == null
                    && bee.distanceToSqr(owner) > 4.0;
        }

        @Override
        public void tick() {
            if (owner != null)
                bee.getMoveControl().setWantedPosition(
                        owner.getX(), owner.getY() + 1, owner.getZ(), 1.2);
        }
    }
}