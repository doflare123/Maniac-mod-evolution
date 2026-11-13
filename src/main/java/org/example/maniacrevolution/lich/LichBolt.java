package org.example.maniacrevolution.lich;

import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.projectile.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.*;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.network.NetworkHooks;
import org.joml.Vector3f;

/** Straight, swept projectile: native hit eligibility, no team or mob whitelist. */
public final class LichBolt extends Projectile {
    private UUID session=LichManager.session();
    private static final DustParticleOptions DUST=new DustParticleOptions(new Vector3f(.38f,.86f,.65f),.65f);
    public LichBolt(EntityType<? extends LichBolt> type,Level level) {super(type,level);setNoGravity(true);}
    @Override protected void defineSynchedData() {}
    @Override public void tick() {
        super.tick();
        Vec3 motion=getDeltaMovement();
        if(level().isClientSide) {
            level().addParticle(DUST,getX(),getY(),getZ(),0,0,0);
            if(tickCount%2==0)level().addParticle(ParticleTypes.SOUL_FIRE_FLAME,getX(),getY(),getZ(),0,0,0);
        } else {
            if(!(getOwner() instanceof ServerPlayer player)||!LichManager.active(player)||!LichManager.session().equals(session)
                    ||tickCount>200||!level().hasChunkAt(BlockPos.containing(position().add(motion)))) {discard();return;}
            HitResult hit=ProjectileUtil.getHitResultOnMoveVector(this,this::canHitEntity);
            if(hit.getType()!=HitResult.Type.MISS&&!ForgeEventFactory.onProjectileImpact(this,hit))onHit(hit);
            if(isRemoved())return;
        }
        setPos(position().add(motion));updateRotation();
    }
    @Override protected void onHit(HitResult hit) {
        super.onHit(hit);
        if(!level().isClientSide) {
            if(level() instanceof ServerLevel server) {
                var pos=hit.getLocation();server.sendParticles(DUST,pos.x,pos.y,pos.z,12,.12,.12,.12,.02);
            }
            discard();
        }
    }
    @Override protected void onHitEntity(EntityHitResult hit) {
        if(level().isClientSide)return;
        Entity target=hit.getEntity();
        if(LichCombat.boltDamage(this,target)&&target instanceof LivingEntity living)
            living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,20,0),getOwner());
    }
    @Override public Packet<ClientGamePacketListener> getAddEntityPacket() {return NetworkHooks.getEntitySpawningPacket(this);}
    @Override protected void addAdditionalSaveData(CompoundTag tag) {super.addAdditionalSaveData(tag);if(session!=null)tag.putUUID("LichSession",session);}
    @Override protected void readAdditionalSaveData(CompoundTag tag) {super.readAdditionalSaveData(tag);session=tag.hasUUID("LichSession")?tag.getUUID("LichSession"):null;setNoGravity(true);}
}
