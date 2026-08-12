package org.example.maniacrevolution.perk.perks.common;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.PacketDistributor;
import org.example.maniacrevolution.effect.FearEffect;
import org.example.maniacrevolution.effect.ModEffects;
import org.example.maniacrevolution.network.ModNetworking;
import org.example.maniacrevolution.network.packets.ClientParticleEffectPacket;
import org.example.maniacrevolution.network.packets.FearDirectionPacket;
import org.example.maniacrevolution.perk.Perk;
import org.example.maniacrevolution.perk.PerkPhase;
import org.example.maniacrevolution.perk.PerkTeam;
import org.example.maniacrevolution.perk.PerkType;
import org.joml.Vector3f;

import java.util.List;

public class FearWavePerk extends Perk {
    private static final double RADIUS = 10.0;
    private static final int FEAR_DURATION = 5 * 20;
    private static final int WAVE_ANIMATION_TICKS = 10;
    private static final DustParticleOptions PARTICLE_COLOR =
            new DustParticleOptions(new Vector3f(0.05F, 0.15F, 0.05F), 1.0F);

    public FearWavePerk() {
        super(new Builder("fear_wave")
                .type(PerkType.ACTIVE)
                .team(PerkTeam.ALL)
                .phases(PerkPhase.MIDGAME, PerkPhase.REVERSAL)
                .manaCost(10F)
                .cooldown(70));
    }

    @Override
    public Component getDescription() {
        return Component.translatable("perk.maniacrev.fear_wave.desc", (int) RADIUS, FEAR_DURATION / 20);
    }

    @Override
    public void onActivate(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }

        Vec3 center = player.position();
        level.playSound(null, center.x, center.y, center.z,
                SoundEvents.WITHER_SPAWN, SoundSource.PLAYERS, 1.0F, 0.5F);
        ClientParticleEffectPacket visual = ClientParticleEffectPacket.fearWave(
                center, (float) RADIUS, WAVE_ANIMATION_TICKS);
        for (ServerPlayer viewer : level.players()) {
            if (viewer.distanceToSqr(center) <= 64.0 * 64.0) {
                ModNetworking.sendToPlayer(visual, viewer);
            }
        }
        applyFearEffect(level, player, center);
    }

    private void applyFearEffect(ServerLevel level, ServerPlayer caster, Vec3 center) {
        AABB searchBox = new AABB(center, center).inflate(RADIUS);
        List<LivingEntity> entities = level.getEntitiesOfClass(LivingEntity.class, searchBox,
                entity -> entity != caster && entity.distanceToSqr(center) <= RADIUS * RADIUS);

        for (LivingEntity entity : entities) {
            FearEffect.setFearDirection(entity);
            if (entity instanceof ServerPlayer serverPlayer) {
                Vec3 fleeDirection = FearEffect.getFearDirection(entity.getUUID());
                if (fleeDirection != null) {
                    ModNetworking.CHANNEL.send(PacketDistributor.PLAYER.with(() -> serverPlayer),
                            new FearDirectionPacket(fleeDirection));
                }
            }

            entity.addEffect(new MobEffectInstance(ModEffects.FEAR.get(), FEAR_DURATION, 0,
                    false, true, true));
            level.sendParticles(PARTICLE_COLOR,
                    entity.getX(), entity.getY() + entity.getBbHeight() / 2.0, entity.getZ(),
                    20, 0.3, 0.5, 0.3, 0.1);
            level.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                    SoundEvents.GHAST_SCREAM, SoundSource.HOSTILE, 0.5F, 1.5F);
        }
    }
}
