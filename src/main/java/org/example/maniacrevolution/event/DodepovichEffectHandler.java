package org.example.maniacrevolution.event;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.example.maniacrevolution.Maniacrev;
import org.example.maniacrevolution.dodepovich.DodepovichCasinoManager;
import org.example.maniacrevolution.effect.ModEffects;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Mod.EventBusSubscriber(modid = Maniacrev.MODID)
public class DodepovichEffectHandler {
    private static final Map<UUID, CreditState> CREDIT_STATES = new ConcurrentHashMap<>();

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        if (player.hasEffect(ModEffects.DODEPOVICH_DAMAGE_BLOCK.get())) {
            player.removeEffect(ModEffects.DODEPOVICH_DAMAGE_BLOCK.get());
            event.setCanceled(true);
            player.displayClientMessage(Component.literal("§bЗащита долга заблокировала урон."), true);
            return;
        }

        if (player.hasEffect(ModEffects.DODEPOVICH_DOUBLE_DAMAGE.get())) {
            player.removeEffect(ModEffects.DODEPOVICH_DOUBLE_DAMAGE.get());
            event.setAmount(event.getAmount() * 2.0f);
            player.displayClientMessage(Component.literal("§cДолг удвоил полученный урон."), true);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.player instanceof ServerPlayer player)) return;

        UUID uuid = player.getUUID();
        if (!player.hasEffect(ModEffects.DODEPOVICH_CREDIT.get())) {
            CREDIT_STATES.remove(uuid);
            return;
        }

        CreditState state = CREDIT_STATES.computeIfAbsent(uuid,
                key -> new CreditState(DodepovichCasinoManager.getHalfMaxHealth(player)));

        var effect = player.getEffect(ModEffects.DODEPOVICH_CREDIT.get());
        if (effect == null) return;

        int duration = effect.getDuration();
        if (duration > DodepovichCasinoManager.CREDIT_DAMAGE_SECONDS * 20) {
            if (player.tickCount % 20 == 0 && state.healed < state.healTotal) {
                float amount = Math.min(state.healTotal / DodepovichCasinoManager.CREDIT_HEAL_SECONDS, state.healTotal - state.healed);
                player.heal(amount);
                state.healed += amount;
            }
        } else if (player.tickCount % 20 == 0 && state.damaged < state.damageTotal) {
            float amount = Math.min(state.damageTotal / DodepovichCasinoManager.CREDIT_DAMAGE_SECONDS, state.damageTotal - state.damaged);
            player.hurt(player.damageSources().magic(), amount);
            state.damaged += amount;
        }
    }

    private static class CreditState {
        private final float healTotal;
        private final float damageTotal;
        private float healed;
        private float damaged;

        private CreditState(float healTotal) {
            this.healTotal = healTotal;
            this.damageTotal = healTotal * DodepovichCasinoManager.CREDIT_DAMAGE_MULTIPLIER;
        }
    }
}
