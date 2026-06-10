package org.example.maniacrevolution.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.example.maniacrevolution.Maniacrev;

public class FullInvisibilityEffect extends MobEffect {
    public FullInvisibilityEffect() {
        super(MobEffectCategory.BENEFICIAL, 0x7F8392);
    }

    @Mod.EventBusSubscriber(modid = Maniacrev.MODID)
    public static final class Events {
        private Events() {
        }

        @SubscribeEvent(priority = EventPriority.HIGHEST)
        public static void onAttack(AttackEntityEvent event) {
            if (event.getEntity().hasEffect(ModEffects.FULL_INVISIBILITY.get())) {
                event.setCanceled(true);
            }
        }

        @SubscribeEvent(priority = EventPriority.HIGHEST)
        public static void onDamage(LivingAttackEvent event) {
            if (event.getSource().getEntity() instanceof Player player
                    && player.hasEffect(ModEffects.FULL_INVISIBILITY.get())) {
                event.setCanceled(true);
            }
        }
    }
}
