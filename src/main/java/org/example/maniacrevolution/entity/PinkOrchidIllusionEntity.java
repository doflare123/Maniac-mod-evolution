package org.example.maniacrevolution.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraftforge.network.NetworkHooks;
import org.example.maniacrevolution.perk.PerkTeam;
import org.example.maniacrevolution.perk.perks.survivor.PinkOrchidPerk;
import org.example.maniacrevolution.pinkorchid.PinkOrchidManager;
import org.example.maniacrevolution.pinkorchid.PinkOrchidRouteFrame;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.joml.Vector3f;

/** Серверно управляемая копия записанного игрока: без ИИ, взаимодействий и физической коллизии. */
public final class PinkOrchidIllusionEntity extends LivingEntity {
    private static final EntityDataAccessor<Optional<UUID>> OWNER_ID =
            SynchedEntityData.defineId(PinkOrchidIllusionEntity.class,
                    EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<ItemStack> MAIN_HAND =
            SynchedEntityData.defineId(PinkOrchidIllusionEntity.class,
                    EntityDataSerializers.ITEM_STACK);
    private static final EntityDataAccessor<ItemStack> OFF_HAND =
            SynchedEntityData.defineId(PinkOrchidIllusionEntity.class,
                    EntityDataSerializers.ITEM_STACK);
    private static final EntityDataAccessor<ItemStack> FEET =
            SynchedEntityData.defineId(PinkOrchidIllusionEntity.class,
                    EntityDataSerializers.ITEM_STACK);
    private static final EntityDataAccessor<ItemStack> LEGS =
            SynchedEntityData.defineId(PinkOrchidIllusionEntity.class,
                    EntityDataSerializers.ITEM_STACK);
    private static final EntityDataAccessor<ItemStack> CHEST =
            SynchedEntityData.defineId(PinkOrchidIllusionEntity.class,
                    EntityDataSerializers.ITEM_STACK);
    private static final EntityDataAccessor<ItemStack> HEAD =
            SynchedEntityData.defineId(PinkOrchidIllusionEntity.class,
                    EntityDataSerializers.ITEM_STACK);
    private static final EntityDataAccessor<Boolean> LEFT_HANDED =
            SynchedEntityData.defineId(PinkOrchidIllusionEntity.class,
                    EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> USING_ITEM =
            SynchedEntityData.defineId(PinkOrchidIllusionEntity.class,
                    EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> USING_OFF_HAND =
            SynchedEntityData.defineId(PinkOrchidIllusionEntity.class,
                    EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> MATERIALIZE_TICKS =
            SynchedEntityData.defineId(PinkOrchidIllusionEntity.class,
                    EntityDataSerializers.INT);
    private static final Vector3f CLIENT_PINK = new Vector3f(1.0F, 0.06F, 0.66F);
    private static final float CLIENT_DUST_SIZE = 0.65F;
    private static final int CLIENT_PARTICLES_PER_TICK = 2;
    private static final double CLIENT_PARTICLE_RADIUS = 0.42D;
    private static final double CLIENT_PARTICLE_HEIGHT = 1.8D;
    private static final double CLIENT_PARTICLE_LIFT = 0.035D;

    private List<PinkOrchidRouteFrame> route = Collections.emptyList();
    private int routeIndex;
    private boolean lastSwinging;
    private boolean managerRemoval;

    public PinkOrchidIllusionEntity(EntityType<? extends PinkOrchidIllusionEntity> type,
                                    Level level) {
        super(type, level);
        setNoGravity(true);
        noPhysics = false;
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(OWNER_ID, Optional.empty());
        entityData.define(MAIN_HAND, ItemStack.EMPTY);
        entityData.define(OFF_HAND, ItemStack.EMPTY);
        entityData.define(FEET, ItemStack.EMPTY);
        entityData.define(LEGS, ItemStack.EMPTY);
        entityData.define(CHEST, ItemStack.EMPTY);
        entityData.define(HEAD, ItemStack.EMPTY);
        entityData.define(LEFT_HANDED, false);
        entityData.define(USING_ITEM, false);
        entityData.define(USING_OFF_HAND, false);
        entityData.define(MATERIALIZE_TICKS, PinkOrchidPerk.MATERIALIZE_TICKS);
    }

    public void initialize(ServerPlayer owner, List<PinkOrchidRouteFrame> frames,
                           List<ItemStack> armorSnapshot) {
        entityData.set(OWNER_ID, Optional.of(owner.getUUID()));
        entityData.set(LEFT_HANDED, owner.getMainArm() == HumanoidArm.LEFT);
        route = frames.stream().map(PinkOrchidRouteFrame::copyFrame).toList();
        routeIndex = 0;
        setItemSlot(EquipmentSlot.FEET, armorSnapshot.get(0));
        setItemSlot(EquipmentSlot.LEGS, armorSnapshot.get(1));
        setItemSlot(EquipmentSlot.CHEST, armorSnapshot.get(2));
        setItemSlot(EquipmentSlot.HEAD, armorSnapshot.get(3));
        if (!route.isEmpty()) applyFrame(route.get(0), false);
    }

    @Override
    public void tick() {
        super.tick();
        setDeltaMovement(Vec3.ZERO);
        if (level().isClientSide()) {
            spawnClientMaterializeParticles();
            return;
        }

        int materialize = entityData.get(MATERIALIZE_TICKS);
        if (materialize > 0) entityData.set(MATERIALIZE_TICKS, materialize - 1);
        if (route.isEmpty() || routeIndex >= route.size()) {
            PinkOrchidManager.finishIllusion(this, false);
            return;
        }

        PinkOrchidRouteFrame frame = route.get(routeIndex);
        if (!canOccupy(frame)) {
            PinkOrchidManager.finishIllusion(this, true);
            return;
        }
        applyFrame(frame, true);
        routeIndex++;
    }

    private void spawnClientMaterializeParticles() {
        if (getMaterializeTicks() <= 0) return;
        for (int index = 0; index < CLIENT_PARTICLES_PER_TICK; index++) {
            double offsetX = (random.nextDouble() * 2.0D - 1.0D) * CLIENT_PARTICLE_RADIUS;
            double offsetY = random.nextDouble() * CLIENT_PARTICLE_HEIGHT;
            double offsetZ = (random.nextDouble() * 2.0D - 1.0D) * CLIENT_PARTICLE_RADIUS;
            level().addParticle(index == 0
                            ? new DustParticleOptions(CLIENT_PINK, CLIENT_DUST_SIZE)
                            : ParticleTypes.CHERRY_LEAVES,
                    getX() + offsetX, getY() + offsetY, getZ() + offsetZ,
                    offsetX * 0.04D, CLIENT_PARTICLE_LIFT, offsetZ * 0.04D);
        }
    }

    private boolean canOccupy(PinkOrchidRouteFrame frame) {
        Pose targetPose = frame.pose();
        AABB box = getDimensions(targetPose).makeBoundingBox(frame.position());
        return level().getWorldBorder().isWithinBounds(
                net.minecraft.core.BlockPos.containing(frame.position()))
                && !level().getBlockCollisions(this, box).iterator().hasNext();
    }

    private void applyFrame(PinkOrchidRouteFrame frame, boolean playAnimation) {
        setPose(frame.pose());
        setPos(frame.position().x, frame.position().y, frame.position().z);
        setYRot(frame.bodyYaw());
        yBodyRot = frame.bodyYaw();
        yBodyRotO = frame.bodyYaw();
        setYHeadRot(frame.headYaw());
        yHeadRotO = frame.headYaw();
        setXRot(frame.pitch());
        xRotO = frame.pitch();
        setSprinting(frame.sprinting());
        setItemSlot(EquipmentSlot.MAINHAND, frame.mainHand());
        setItemSlot(EquipmentSlot.OFFHAND, frame.offHand());
        entityData.set(USING_ITEM, frame.usingItem());
        entityData.set(USING_OFF_HAND,
                frame.usingItem() && frame.usedHand() == InteractionHand.OFF_HAND);
        if (playAnimation && frame.swinging() && !lastSwinging) {
            swing(frame.swingingHand(), true);
        }
        lastSwinging = frame.swinging();
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (level().isClientSide() || !source.is(DamageTypes.PLAYER_ATTACK)
                || !(source.getDirectEntity() instanceof ServerPlayer attacker)
                || PerkTeam.fromPlayer(attacker) != PerkTeam.MANIAC) {
            return false;
        }
        PinkOrchidManager.finishIllusion(this, true);
        return true;
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public void push(double x, double y, double z) {
    }

    @Override
    public boolean canBeCollidedWith() {
        return false;
    }

    @Override
    public Iterable<ItemStack> getArmorSlots() {
        List<ItemStack> armor = new ArrayList<>(4);
        armor.add(entityData.get(FEET));
        armor.add(entityData.get(LEGS));
        armor.add(entityData.get(CHEST));
        armor.add(entityData.get(HEAD));
        return armor;
    }

    @Override
    public ItemStack getItemBySlot(EquipmentSlot slot) {
        return switch (slot) {
            case MAINHAND -> entityData.get(MAIN_HAND);
            case OFFHAND -> entityData.get(OFF_HAND);
            case FEET -> entityData.get(FEET);
            case LEGS -> entityData.get(LEGS);
            case CHEST -> entityData.get(CHEST);
            case HEAD -> entityData.get(HEAD);
        };
    }

    @Override
    public void setItemSlot(EquipmentSlot slot, ItemStack stack) {
        ItemStack value = stack.isEmpty() ? ItemStack.EMPTY : stack.copy();
        switch (slot) {
            case MAINHAND -> entityData.set(MAIN_HAND, value);
            case OFFHAND -> entityData.set(OFF_HAND, value);
            case FEET -> entityData.set(FEET, value);
            case LEGS -> entityData.set(LEGS, value);
            case CHEST -> entityData.set(CHEST, value);
            case HEAD -> entityData.set(HEAD, value);
        }
    }

    @Override
    public HumanoidArm getMainArm() {
        return entityData.get(LEFT_HANDED) ? HumanoidArm.LEFT : HumanoidArm.RIGHT;
    }

    @Nullable
    public UUID getOwnerUUID() {
        return entityData.get(OWNER_ID).orElse(null);
    }

    public boolean isVisuallyUsingItem() {
        return entityData.get(USING_ITEM);
    }

    public InteractionHand getVisualUsedHand() {
        return entityData.get(USING_OFF_HAND) ? InteractionHand.OFF_HAND
                : InteractionHand.MAIN_HAND;
    }

    public int getMaterializeTicks() {
        return entityData.get(MATERIALIZE_TICKS);
    }

    public void discardFromManager() {
        managerRemoval = true;
        discard();
    }

    @Override
    public void remove(RemovalReason reason) {
        super.remove(reason);
        if (!level().isClientSide() && !managerRemoval) {
            PinkOrchidManager.onUnexpectedIllusionRemoval(this);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        discard();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
