package org.example.maniacrevolution.warden;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.example.maniacrevolution.Maniacrev;
import org.example.maniacrevolution.effect.ModEffects;

/** The passive follows the same server eligibility as Warden form and vision. */
@Mod.EventBusSubscriber(modid = Maniacrev.MODID)
public final class WardenArmorManager {
    private WardenArmorManager() {}
    @SubscribeEvent public static void tick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) return;
        var effect = ModEffects.WARDEN_ARMOR.get();
        if (!WardenCombatManager.active(player)) {
            if (player.hasEffect(effect)) player.removeEffect(effect);
            return;
        }
        var current = player.getEffect(effect);
        if (current != null && current.isInfiniteDuration() && current.getAmplifier() == 0
                && !current.isVisible() && current.showIcon()) return;
        if (current != null) player.removeEffect(effect);
        player.addEffect(new MobEffectInstance(effect, -1, 0, false, false, true));
    }
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) player.removeEffect(ModEffects.WARDEN_ARMOR.get());
    }
}
