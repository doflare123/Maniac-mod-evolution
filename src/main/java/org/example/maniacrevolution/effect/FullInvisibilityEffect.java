package org.example.maniacrevolution.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
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

        @SubscribeEvent
        public static void onAttack(AttackEntityEvent event) {
            if (event.getEntity().hasEffect(ModEffects.FULL_INVISIBILITY.get())) {
                event.setCanceled(true);
            }
        }
    }
}
