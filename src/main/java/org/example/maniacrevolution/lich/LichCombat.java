package org.example.maniacrevolution.lich;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.event.entity.living.LivingKnockBackEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.example.maniacrevolution.Maniacrev;
import org.example.maniacrevolution.ModItems;
import org.example.maniacrevolution.downed.*;
import org.example.maniacrevolution.effect.ModEffects;
import org.example.maniacrevolution.entity.ModEntities;
import org.example.maniacrevolution.event.PenaltySlotManager;
import org.example.maniacrevolution.util.ManaUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import org.example.maniacrevolution.network.ModNetworking;

@Mod.EventBusSubscriber(modid=Maniacrev.MODID)
public final class LichCombat {
    public static final float DAMAGE=.5f;
    public static final int COOLDOWN=40;
    private static final ThreadLocal<Entity> QUIET_HIT=new ThreadLocal<>();
    private static final ThreadLocal<ServerPlayer> AUTHORIZED=new ThreadLocal<>();
    private LichCombat() {}
    private static boolean ready(ServerPlayer player) {
        var army=LichManager.owner(player.getUUID());var downed=DownedCapability.get(player);
        return LichManager.active(player)&&army!=null&&player.server.getTickCount()>=army.melee
                &&!player.hasEffect(ModEffects.STUN.get())&&!player.hasEffect(ModEffects.FULL_INVISIBILITY.get())
                &&!PenaltySlotManager.isInPenaltySlot(player)&&(downed==null||downed.getState()!=DownedState.DOWNED);
    }
    public static boolean authorized(ServerPlayer player) {return AUTHORIZED.get()==player;}
    public static void input(ServerPlayer player) {
        ModNetworking.sendToPlayer(new LichSnapshot(feedback(player)),player);
    }
    static CompoundTag feedback(ServerPlayer player) {
        var tag=new CompoundTag();tag.putString("mode","combat");
        boolean accepted=player.getMainHandItem().is(ModItems.LICH_BOOK.get())?shoot(player,tag):melee(player);
        var army=LichManager.owner(player.getUUID());
        tag.putString("dimension",player.level().dimension().location().toString());
        tag.putLong("tick",player.level().getGameTime());
        tag.putInt("cooldown",army==null?0:(int)Math.max(0,army.melee-player.server.getTickCount()));
        tag.putBoolean("swing",accepted);
        return tag;
    }
    /** An accepted swing consumes its whole cycle, including a miss, like Warden melee. */
    public static boolean melee(ServerPlayer player) {
        if(!ready(player)||player.containerMenu!=player.inventoryMenu)return false;
        LichManager.owner(player.getUUID()).melee=player.server.getTickCount()+COOLDOWN;
        var start=player.getEyePosition();
        var end=player.level().clip(new ClipContext(start,start.add(player.getLookAngle().scale(3)),
                ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,player)).getLocation();
        Entity target=null;double distance=start.distanceToSqr(end);
        for(var entity:player.level().getEntities(player,new AABB(start,end).inflate(.2),e->e.isPickable()&&!e.isSpectator())) {
            var box=entity.getBoundingBox().inflate(entity.getPickRadius());
            var hit=box.contains(start)?java.util.Optional.of(start):box.clip(start,end);
            if(hit.isPresent()&&start.distanceToSqr(hit.get())<=distance&&player.canReach(entity,0)&&player.hasLineOfSight(entity)) {
                target=entity;distance=start.distanceToSqr(hit.get());
            }
        }
        player.swing(InteractionHand.MAIN_HAND,true);
        if(target!=null) {
            AUTHORIZED.set(player);
            try {
                if(!MinecraftForge.EVENT_BUS.post(new AttackEntityEvent(player,target))
                        &&target.hurt(player.damageSources().playerAttack(player),DAMAGE))player.setLastHurtMob(target);
            } finally {AUTHORIZED.remove();}
        }
        return true;
    }
    public static boolean shoot(ServerPlayer player) {
        return shoot(player,null);
    }
    private static boolean shoot(ServerPlayer player,CompoundTag feedback) {
        if(!ready(player)||player.containerMenu!=player.inventoryMenu||!player.getMainHandItem().is(ModItems.LICH_BOOK.get()))return false;
        var bolt=ModEntities.LICH_BOLT.get().create(player.serverLevel());if(bolt==null)return false;
        bolt.setOwner(player);bolt.setPos(player.getX(),player.getEyeY()-.1,player.getZ());
        bolt.shootFromRotation(player,player.getXRot(),player.getYRot(),0,1.5f,0);
        if(!ManaUtil.consumeMana(player,1)) {
            if(feedback!=null)feedback.putString("failure","no_mana");
            return false;
        }
        if(!player.serverLevel().addFreshEntity(bolt)) {ManaUtil.addMana(player,1);return false;}
        LichManager.owner(player.getUUID()).melee=player.server.getTickCount()+COOLDOWN;
        player.swing(InteractionHand.MAIN_HAND,true);
        return true;
    }
    static boolean boltDamage(LichBolt bolt,Entity target) {
        Entity previous=QUIET_HIT.get();QUIET_HIT.set(target);
        try {return target.hurt(bolt.damageSources().indirectMagic(bolt,bolt.getOwner()),DAMAGE);}
        finally {if(previous==null)QUIET_HIT.remove();else QUIET_HIT.set(previous);}
    }
    @SubscribeEvent(priority=EventPriority.HIGHEST)
    public static void knockback(LivingKnockBackEvent event) {
        if(QUIET_HIT.get()==event.getEntity())event.setCanceled(true);
    }
}
