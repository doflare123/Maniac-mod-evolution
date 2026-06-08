package org.example.maniacrevolution.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.example.maniacrevolution.Maniacrev;

public class StunEffect extends MobEffect {
    private static final String MOVEMENT_SPEED_MODIFIER = "b9e8dd41-5f72-4a48-a15f-cc98b6266014";

    public StunEffect() {
        super(MobEffectCategory.HARMFUL, 0xD5D8E8);
        addAttributeModifier(
                Attributes.MOVEMENT_SPEED,
                MOVEMENT_SPEED_MODIFIER,
                -1.0,
                AttributeModifier.Operation.MULTIPLY_TOTAL
        );
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        if (entity.level().isClientSide) {
            return;
        }

        entity.setDeltaMovement(Vec3.ZERO);
        entity.hurtMarked = true;
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return true;
    }

    @Mod.EventBusSubscriber(modid = Maniacrev.MODID)
    public static final class Events {
        private Events() {
        }

        @SubscribeEvent
        public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
            if (event.phase == TickEvent.Phase.END && event.player.hasEffect(ModEffects.STUN.get())) {
                event.player.setDeltaMovement(Vec3.ZERO);
                event.player.hurtMarked = true;
            }
        }

        @SubscribeEvent
        public static void onAttack(AttackEntityEvent event) {
            if (event.getEntity().hasEffect(ModEffects.STUN.get())) {
                event.setCanceled(true);
            }
        }
    }
}
