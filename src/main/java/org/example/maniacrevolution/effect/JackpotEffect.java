package org.example.maniacrevolution.effect;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import org.example.maniacrevolution.network.ModNetworking;
import org.example.maniacrevolution.network.packets.SyncJackpotMusicPacket;

public class JackpotEffect extends MobEffect {
    private static final String SPEED_MODIFIER = "78b443ba-cb26-46ae-872d-996f2eb173bc";
    private static final String DAMAGE_MODIFIER = "92785887-7394-4d10-8b38-89a1d39dcae8";

    public JackpotEffect() {
        super(MobEffectCategory.BENEFICIAL, 0xE13232);
        addAttributeModifier(Attributes.MOVEMENT_SPEED, SPEED_MODIFIER, 0.6,
                AttributeModifier.Operation.MULTIPLY_TOTAL);
        addAttributeModifier(Attributes.ATTACK_DAMAGE, DAMAGE_MODIFIER, 9.0,
                AttributeModifier.Operation.ADDITION);
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        if (!(entity.level() instanceof ServerLevel level)) {
            return;
        }

        if (entity.getHealth() < entity.getMaxHealth()) {
            entity.heal(1.0f);
        }

        if (entity instanceof ServerPlayer player && entity.tickCount % 40 == 0) {
            ModNetworking.sendToAllPlayers(new SyncJackpotMusicPacket(player.getUUID(), true));
        }

        if (entity.tickCount % 2 == 0) {
            double angle = entity.tickCount * 0.45;
            double radius = 0.75;
            level.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                    entity.getX() + Math.cos(angle) * radius,
                    entity.getY() + 0.15,
                    entity.getZ() + Math.sin(angle) * radius,
                    1, 0.02, 0.02, 0.02, 0.0);
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                    entity.getX(), entity.getY() + 1.0, entity.getZ(),
                    2, 0.45, 0.75, 0.45, 0.02);
        }

        if (entity.tickCount % 5 == 0) {
            level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING,
                    entity.getX(), entity.getY() + 1.1, entity.getZ(),
                    3, 0.4, 0.7, 0.4, 0.04);
            level.sendParticles(ParticleTypes.END_ROD,
                    entity.getX(), entity.getY() + 1.8, entity.getZ(),
                    1, 0.25, 0.2, 0.25, 0.01);
        }
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return true;
    }

    @Override
    public void removeAttributeModifiers(LivingEntity entity, AttributeMap attributes, int amplifier) {
        super.removeAttributeModifiers(entity, attributes, amplifier);
        if (entity instanceof ServerPlayer player) {
            ModNetworking.sendToAllPlayers(new SyncJackpotMusicPacket(player.getUUID(), false));
        }
    }
}
