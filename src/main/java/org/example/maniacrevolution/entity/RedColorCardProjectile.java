package org.example.maniacrevolution.entity;

import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.network.NetworkHooks;
import org.example.maniacrevolution.ModItems;
import org.example.maniacrevolution.perk.PerkTeam;
import org.example.maniacrevolution.perk.perks.common.ColorRoulettePerk;

/** Zero-damage survivor projectile that only knocks maniacs away. */
public final class RedColorCardProjectile extends ThrowableItemProjectile {
    public RedColorCardProjectile(EntityType<? extends RedColorCardProjectile> type, Level level) {
        super(type, level);
    }

    public RedColorCardProjectile(Level level, LivingEntity owner) {
        super(ModEntities.RED_COLOR_CARD_PROJECTILE.get(), owner, level);
    }

    @Override
    protected Item getDefaultItem() {
        return ModItems.RED_COLOR_CARD.get();
    }

    @Override
    protected void onHitEntity(EntityHitResult hit) {
        super.onHitEntity(hit);
        if (!level().isClientSide() && hit.getEntity() instanceof ServerPlayer target
                && PerkTeam.fromPlayer(target) == PerkTeam.MANIAC) {
            double dx = getOwner() == null ? getDeltaMovement().x
                    : getOwner().getX() - target.getX();
            double dz = getOwner() == null ? getDeltaMovement().z
                    : getOwner().getZ() - target.getZ();
            target.knockback(ColorRoulettePerk.RED_CARD_KNOCKBACK_LEVEL * 0.5D, dx, dz);
        }
    }

    @Override
    protected void onHit(HitResult hit) {
        super.onHit(hit);
        if (!level().isClientSide()) {
            discard();
        }
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
